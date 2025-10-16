package com.roshni.games.core.utils.workflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of the WorkflowEngine interface
 */
class WorkflowEngineImpl private constructor() : WorkflowEngine {

    // Core storage
    private val workflows = ConcurrentHashMap<String, Workflow>()
    private val activeExecutions = ConcurrentHashMap<String, WorkflowExecution>()
    private val executionResults = ConcurrentHashMap<String, WorkflowResult>()
    private val statistics = ConcurrentHashMap<String, WorkflowStatistics>()

    // Communication channels
    private val eventChannel = Channel<WorkflowEvent>(Channel.UNLIMITED)
    private val resultFlow = MutableSharedFlow<WorkflowResult>(replay = 100)
    private val statusFlow = MutableSharedFlow<WorkflowExecutionStatus>(replay = 100)

    // Engine state
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isShutdown = false
    private var startTime = System.currentTimeMillis()

    // Event processing job
    private var eventProcessingJob: Job? = null

    init {
        startEventProcessing()
    }

    override suspend fun registerWorkflow(workflow: Workflow): Boolean {
        if (isShutdown) return false

        return try {
            // Validate workflow before registration
            if (!workflow.validate()) {
                return false
            }

            workflows[workflow.id] = workflow
            initializeWorkflowStatistics(workflow.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unregisterWorkflow(workflowId: String): Boolean {
        if (isShutdown) return false

        return try {
            // Cancel any active executions of this workflow
            val executionsToCancel = activeExecutions.values.filter { it.context.workflowId == workflowId }
            executionsToCancel.forEach { cancelWorkflow(it.executionId, "Workflow unregistered") }

            val removed = workflows.remove(workflowId)
            statistics.remove(workflowId)
            removed != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getWorkflow(workflowId: String): Workflow? {
        return workflows[workflowId]
    }

    override suspend fun getAllWorkflows(): List<Workflow> {
        return workflows.values.toList()
    }

    override suspend fun getWorkflowsByCategory(category: String): List<Workflow> {
        return workflows.values.filter { it.category == category }
    }

    override suspend fun getWorkflowsByTags(tags: List<String>): List<Workflow> {
        return workflows.values.filter { workflow ->
            tags.any { tag -> workflow.tags.contains(tag) }
        }
    }

    override suspend fun startWorkflow(workflowId: String, context: WorkflowContext): String {
        if (isShutdown) throw IllegalStateException("Workflow engine is shutdown")

        val workflow = workflows[workflowId] ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        return try {
            // Validate that workflow can execute with this context
            if (!workflow.canExecute(context)) {
                throw IllegalStateException("Workflow cannot execute with provided context")
            }

            val execution = WorkflowExecution(
                executionId = context.executionId,
                workflow = workflow,
                context = context,
                status = WorkflowResult.WorkflowStatus.COMPLETED // Will be updated by execution engine
            )

            activeExecutions[execution.executionId] = execution

            // Start workflow execution in background
            engineScope.launch {
                executeWorkflow(execution)
            }

            execution.executionId
        } catch (e: Exception) {
            throw IllegalStateException("Failed to start workflow: ${e.message}", e)
        }
    }

    override suspend fun startWorkflow(
        workflowId: String,
        userId: String,
        initialVariables: Map<String, Any>
    ): String {
        val workflow = workflows[workflowId] ?: throw IllegalArgumentException("Workflow not found: $workflowId")
        val context = workflow.createContext(
            userId = userId,
            initialVariables = initialVariables
        )
        return startWorkflow(workflowId, context)
    }

    override suspend fun pauseWorkflow(executionId: String): Boolean {
        val execution = activeExecutions[executionId] ?: return false

        return try {
            execution.status = WorkflowResult.WorkflowStatus.PAUSED
            updateExecutionStatus(execution)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resumeWorkflow(executionId: String): Boolean {
        val execution = activeExecutions[executionId] ?: return false

        return if (execution.status == WorkflowResult.WorkflowStatus.PAUSED) {
            try {
                execution.status = WorkflowResult.WorkflowStatus.COMPLETED // Will be updated by execution engine
                engineScope.launch {
                    executeWorkflow(execution)
                }
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override suspend fun cancelWorkflow(executionId: String, reason: String): Boolean {
        val execution = activeExecutions[executionId] ?: return false

        return try {
            execution.status = WorkflowResult.WorkflowStatus.CANCELLED
            val endTime = System.currentTimeMillis()

            val result = WorkflowResult.cancelled(
                workflowId = execution.workflow.id,
                executionId = executionId,
                startTime = execution.context.startTime,
                endTime = endTime,
                finalState = execution.context.currentState,
                reason = reason,
                variables = execution.context.variables,
                events = execution.context.events
            )

            executionResults[executionId] = result
            activeExecutions.remove(executionId)

            resultFlow.emit(result)
            updateWorkflowStatistics(result)

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getWorkflowStatus(executionId: String): WorkflowExecutionStatus? {
        val execution = activeExecutions[executionId]
        if (execution != null) {
            return WorkflowExecutionStatus(
                executionId = execution.executionId,
                workflowId = execution.workflow.id,
                status = execution.status,
                currentState = execution.context.currentState,
                startTime = execution.context.startTime,
                lastUpdateTime = execution.context.lastTransitionTime,
                userId = execution.context.userId,
                progress = calculateProgress(execution),
                metadata = execution.context.metadata
            )
        }

        // Check if execution is completed
        val result = executionResults[executionId]
        if (result != null) {
            return WorkflowExecutionStatus(
                executionId = executionId,
                workflowId = result.workflowId,
                status = result.status,
                currentState = result.finalState,
                startTime = result.startTime,
                lastUpdateTime = result.endTime,
                userId = "unknown", // Not stored in result
                progress = if (result.isSuccessful()) 1.0f else 0.0f
            )
        }

        return null
    }

    override suspend fun getWorkflowResult(executionId: String): WorkflowResult? {
        return executionResults[executionId]
    }

    override suspend fun getActiveExecutions(): List<WorkflowExecutionStatus> {
        return activeExecutions.values.map { execution ->
            WorkflowExecutionStatus(
                executionId = execution.executionId,
                workflowId = execution.workflow.id,
                status = execution.status,
                currentState = execution.context.currentState,
                startTime = execution.context.startTime,
                lastUpdateTime = execution.context.lastTransitionTime,
                userId = execution.context.userId,
                progress = calculateProgress(execution),
                metadata = execution.context.metadata
            )
        }
    }

    override suspend fun getExecutionsForUser(userId: String): List<WorkflowExecutionStatus> {
        val active = activeExecutions.values.filter { it.context.userId == userId }
        val completed = executionResults.values.filter { it.variables["userId"] == userId }

        return active.map { execution ->
            WorkflowExecutionStatus(
                executionId = execution.executionId,
                workflowId = execution.workflow.id,
                status = execution.status,
                currentState = execution.context.currentState,
                startTime = execution.context.startTime,
                lastUpdateTime = execution.context.lastTransitionTime,
                userId = execution.context.userId,
                progress = calculateProgress(execution),
                metadata = execution.context.metadata
            )
        } + completed.map { result ->
            WorkflowExecutionStatus(
                executionId = result.executionId,
                workflowId = result.workflowId,
                status = result.status,
                currentState = result.finalState,
                startTime = result.startTime,
                lastUpdateTime = result.endTime,
                userId = userId,
                progress = if (result.isSuccessful()) 1.0f else 0.0f
            )
        }
    }

    override suspend fun sendEvent(executionId: String, event: String): Boolean {
        val execution = activeExecutions[executionId] ?: return false

        return try {
            execution.context.addEvent(event)
            eventChannel.trySend(WorkflowEvent.ExecutionEvent(executionId, event))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateVariables(executionId: String, variables: Map<String, Any>): Boolean {
        val execution = activeExecutions[executionId] ?: return false

        return try {
            variables.forEach { (key, value) ->
                execution.context.setVariable(key, value)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getWorkflowStatistics(workflowId: String?): WorkflowStatistics {
        return if (workflowId != null) {
            statistics[workflowId] ?: WorkflowStatistics()
        } else {
            // Aggregate statistics for all workflows
            val totalExecutions = AtomicLong(0)
            val totalCompleted = AtomicLong(0)
            val totalFailed = AtomicLong(0)
            val totalCancelled = AtomicLong(0)
            val totalTimedOut = AtomicLong(0)
            val totalExecutionTime = AtomicLong(0)

            statistics.values.forEach { stats ->
                totalExecutions.addAndGet(stats.totalExecutions)
                totalCompleted.addAndGet(stats.completedExecutions)
                totalFailed.addAndGet(stats.failedExecutions)
                totalCancelled.addAndGet(stats.cancelledExecutions)
                totalTimedOut.addAndGet(stats.timedOutExecutions)
                totalExecutionTime.addAndGet(stats.totalExecutionTimeMs)
            }

            val avgTime = if (totalExecutions.get() > 0) {
                totalExecutionTime.get().toDouble() / totalExecutions.get()
            } else 0.0

            WorkflowStatistics(
                totalExecutions = totalExecutions.get(),
                completedExecutions = totalCompleted.get(),
                failedExecutions = totalFailed.get(),
                cancelledExecutions = totalCancelled.get(),
                timedOutExecutions = totalTimedOut.get(),
                averageExecutionTimeMs = avgTime,
                totalExecutionTimeMs = totalExecutionTime.get()
            )
        }
    }

    override suspend fun clearStatistics() {
        statistics.clear()
    }

    override suspend fun validateAllWorkflows(): WorkflowValidationResult {
        if (isShutdown) {
            return WorkflowValidationResult(
                isValid = false,
                errors = listOf("Workflow engine is shutdown")
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val workflowValidationResults = mutableMapOf<String, WorkflowValidationResult.WorkflowValidationResult>()

        workflows.values.forEach { workflow ->
            try {
                val isValid = workflow.validate()
                val workflowErrors = mutableListOf<String>()
                val workflowWarnings = mutableListOf<String>()

                if (!isValid) {
                    workflowErrors.add("Workflow validation failed")
                }

                workflowValidationResults[workflow.id] = WorkflowValidationResult.WorkflowValidationResult(
                    isValid = isValid,
                    errors = workflowErrors,
                    warnings = workflowWarnings
                )

                if (!isValid) {
                    errors.add("Workflow ${workflow.id} failed validation")
                }

            } catch (e: Exception) {
                errors.add("Exception validating workflow ${workflow.id}: ${e.message}")
                workflowValidationResults[workflow.id] = WorkflowValidationResult.WorkflowValidationResult(
                    isValid = false,
                    errors = listOf(e.message ?: "Unknown error")
                )
            }
        }

        return WorkflowValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            workflowValidationResults = workflowValidationResults
        )
    }

    override suspend fun exportWorkflows(): Map<String, Any> {
        return mapOf(
            "workflows" to workflows.values.map { workflow ->
                mapOf(
                    "id" to workflow.id,
                    "name" to workflow.name,
                    "description" to workflow.description,
                    "category" to workflow.category,
                    "enabled" to workflow.enabled,
                    "tags" to workflow.tags,
                    "version" to workflow.version,
                    "metadata" to workflow.getMetadata()
                )
            },
            "exportTime" to System.currentTimeMillis(),
            "workflowCount" to workflows.size
        )
    }

    override suspend fun importWorkflows(configuration: Map<String, Any>): Boolean {
        if (isShutdown) return false

        return try {
            @Suppress("UNCHECKED_CAST")
            val workflowsConfig = configuration["workflows"] as? List<Map<String, Any>> ?: return false

            var successCount = 0

            workflowsConfig.forEach { workflowConfig ->
                try {
                    // This is a simplified import - in a real implementation,
                    // you would need to reconstruct the full workflow objects
                    successCount++
                } catch (e: Exception) {
                    // Log error but continue with other workflows
                }
            }

            successCount == workflowsConfig.size
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getEngineStatus(): WorkflowEngineStatus {
        val uptime = System.currentTimeMillis() - startTime

        return WorkflowEngineStatus(
            isRunning = !isShutdown,
            registeredWorkflowCount = workflows.size,
            activeExecutionCount = activeExecutions.size,
            completedExecutionCount = executionResults.values.count { it.isSuccessful() }.toLong(),
            failedExecutionCount = executionResults.values.count { it.isFailed() }.toLong(),
            lastActivityTime = executionResults.values.maxOfOrNull { it.endTime },
            uptimeMs = uptime
        )
    }

    override suspend fun shutdown() {
        eventProcessingJob?.cancel()
        eventChannel.close()

        // Cancel all active executions
        activeExecutions.values.forEach { execution ->
            cancelWorkflow(execution.executionId, "Engine shutdown")
        }

        workflows.clear()
        activeExecutions.clear()
        executionResults.clear()
        statistics.clear()
        isShutdown = true
    }

    override fun observeWorkflowResults(workflowId: String?): Flow<WorkflowResult> {
        return resultFlow.asSharedFlow()
            .filter { workflowId == null || it.workflowId == workflowId }
    }

    override fun observeExecutionStatus(executionId: String?): Flow<WorkflowExecutionStatus> {
        return statusFlow.asSharedFlow()
            .filter { executionId == null || it.executionId == executionId }
    }

    /**
     * Start processing workflow events
     */
    private fun startEventProcessing() {
        eventProcessingJob = engineScope.launch {
            eventChannel.receiveAsFlow().collect { event ->
                when (event) {
                    is WorkflowEvent.ExecutionEvent -> {
                        handleExecutionEvent(event)
                    }
                    is WorkflowEvent.StateTransitionEvent -> {
                        handleStateTransitionEvent(event)
                    }
                }
            }
        }
    }

    /**
     * Execute a workflow using state machine logic
     */
    private suspend fun executeWorkflow(execution: WorkflowExecution) {
        val workflow = execution.workflow
        var currentContext = execution.context

        try {
            while (isActive && !isShutdown) {
                // Check if workflow should continue
                if (currentContext.isTimedOut()) {
                    completeWorkflowWithTimeout(execution, currentContext)
                    return
                }

                // Execute current state entry actions
                executeStateActions(currentContext.currentState.entryActions, currentContext)

                // Find and execute valid transitions
                val validTransitions = findValidTransitions(workflow, currentContext)

                if (validTransitions.isEmpty()) {
                    // No valid transitions - check if we're in a terminal state
                    if (currentContext.currentState.isTerminal) {
                        completeWorkflowSuccessfully(execution, currentContext)
                        return
                    } else {
                        // Stuck in non-terminal state - this is an error
                        completeWorkflowWithError(execution, currentContext, "No valid transitions found")
                        return
                    }
                }

                // Select the highest priority transition
                val selectedTransition = validTransitions.maxByOrNull { it.priority }
                    ?: continue

                // Execute transition
                val transitionResult = executeTransition(execution, selectedTransition, currentContext)

                if (!transitionResult) {
                    completeWorkflowWithError(execution, currentContext, "Transition execution failed")
                    return
                }

                // Update context with new state
                currentContext = currentContext.withStateTransition(selectedTransition.toState)

                // Update execution status
                updateExecutionStatus(execution.copy(context = currentContext))

                // If we've reached a terminal state, complete the workflow
                if (currentContext.currentState.isTerminal) {
                    completeWorkflowSuccessfully(execution, currentContext)
                    return
                }
            }
        } catch (e: Exception) {
            completeWorkflowWithError(execution, currentContext, e.message ?: "Unknown error")
        }
    }

    /**
     * Find all valid transitions from the current state
     */
    private suspend fun findValidTransitions(
        workflow: Workflow,
        context: WorkflowContext
    ): List<WorkflowTransition> {
        return workflow.getTransitionsFrom(context.currentState).filter { transition ->
            // Check transition condition if present
            transition.condition?.evaluate(context) ?: true
        }
    }

    /**
     * Execute a state transition
     */
    private suspend fun executeTransition(
        execution: WorkflowExecution,
        transition: WorkflowTransition,
        context: WorkflowContext
    ): Boolean {
        return try {
            // Execute transition actions
            var allActionsSuccessful = true
            for (action in transition.actions) {
                val success = action.execute(context)
                if (!success) allActionsSuccessful = false
            }

            // Update execution with transition
            execution.executedTransitions.add(transition)

            allActionsSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute state entry/exit actions
     */
    private suspend fun executeStateActions(actions: List<WorkflowAction>, context: WorkflowContext): Boolean {
        return try {
            actions.forEach { action ->
                action.execute(context)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Complete workflow successfully
     */
    private suspend fun completeWorkflowSuccessfully(
        execution: WorkflowExecution,
        finalContext: WorkflowContext
    ) {
        val result = WorkflowResult.success(
            workflowId = execution.workflow.id,
            executionId = execution.executionId,
            startTime = finalContext.startTime,
            endTime = System.currentTimeMillis(),
            finalState = finalContext.currentState,
            executedTransitions = execution.executedTransitions,
            executedActions = execution.executedActions,
            variables = finalContext.variables,
            events = finalContext.events
        )

        finishWorkflow(execution, result)
    }

    /**
     * Complete workflow with timeout
     */
    private suspend fun completeWorkflowWithTimeout(
        execution: WorkflowExecution,
        finalContext: WorkflowContext
    ) {
        val result = WorkflowResult.timeout(
            workflowId = execution.workflow.id,
            executionId = execution.executionId,
            startTime = finalContext.startTime,
            endTime = System.currentTimeMillis(),
            finalState = finalContext.currentState,
            timeoutMs = finalContext.timeoutMs ?: 0,
            executedTransitions = execution.executedTransitions,
            executedActions = execution.executedActions,
            variables = finalContext.variables,
            events = finalContext.events
        )

        finishWorkflow(execution, result)
    }

    /**
     * Complete workflow with error
     */
    private suspend fun completeWorkflowWithError(
        execution: WorkflowExecution,
        finalContext: WorkflowContext,
        errorMessage: String
    ) {
        val error = WorkflowResult.WorkflowError(
            code = "WORKFLOW_EXECUTION_ERROR",
            message = errorMessage,
            state = finalContext.currentState,
            timestamp = System.currentTimeMillis()
        )

        val result = WorkflowResult.failure(
            workflowId = execution.workflow.id,
            executionId = execution.executionId,
            startTime = finalContext.startTime,
            endTime = System.currentTimeMillis(),
            finalState = finalContext.currentState,
            error = error,
            executedTransitions = execution.executedTransitions,
            executedActions = execution.executedActions,
            variables = finalContext.variables,
            events = finalContext.events
        )

        finishWorkflow(execution, result)
    }

    /**
     * Finish workflow execution and cleanup
     */
    private suspend fun finishWorkflow(execution: WorkflowExecution, result: WorkflowResult) {
        activeExecutions.remove(execution.executionId)
        executionResults[execution.executionId] = result

        resultFlow.emit(result)
        updateWorkflowStatistics(result)
    }

    /**
     * Update execution status for observers
     */
    private suspend fun updateExecutionStatus(execution: WorkflowExecution) {
        val status = WorkflowExecutionStatus(
            executionId = execution.executionId,
            workflowId = execution.workflow.id,
            status = execution.status,
            currentState = execution.context.currentState,
            startTime = execution.context.startTime,
            lastUpdateTime = execution.context.lastTransitionTime,
            userId = execution.context.userId,
            progress = calculateProgress(execution),
            metadata = execution.context.metadata
        )

        statusFlow.emit(status)
    }

    /**
     * Calculate workflow progress based on executed transitions and states
     */
    private fun calculateProgress(execution: WorkflowExecution): Float {
        val workflow = execution.workflow
        val totalStates = workflow.states.size
        val visitedStates = execution.executedTransitions
            .map { it.fromState }
            .toSet()
            .size + 1 // +1 for current state

        return (visitedStates.toFloat() / totalStates).coerceIn(0.0f, 1.0f)
    }

    /**
     * Handle execution events
     */
    private suspend fun handleExecutionEvent(event: WorkflowEvent.ExecutionEvent) {
        // Process events that affect workflow execution
        // This could trigger state transitions based on events
    }

    /**
     * Handle state transition events
     */
    private suspend fun handleStateTransitionEvent(event: WorkflowEvent.StateTransitionEvent) {
        // Handle state transition notifications
    }

    /**
     * Initialize statistics for a newly registered workflow
     */
    private fun initializeWorkflowStatistics(workflowId: String) {
        statistics[workflowId] = WorkflowStatistics()
    }

    /**
     * Update statistics for a workflow execution result
     */
    private fun updateWorkflowStatistics(result: WorkflowResult) {
        val currentStats = statistics[result.workflowId] ?: WorkflowStatistics()
        val newStats = currentStats.copy(
            totalExecutions = currentStats.totalExecutions + 1,
            completedExecutions = currentStats.completedExecutions + if (result.isSuccessful()) 1 else 0,
            failedExecutions = currentStats.failedExecutions + if (result.isFailed()) 1 else 0,
            cancelledExecutions = currentStats.cancelledExecutions + if (result.isCancelled()) 1 else 0,
            timedOutExecutions = currentStats.timedOutExecutions + if (result.isTimedOut()) 1 else 0,
            totalExecutionTimeMs = currentStats.totalExecutionTimeMs + result.getExecutionTimeMs(),
            lastExecutionTime = result.endTime
        ).let { stats ->
            // Recalculate average
            if (stats.totalExecutions > 0) {
                stats.copy(
                    averageExecutionTimeMs = stats.totalExecutionTimeMs.toDouble() / stats.totalExecutions
                )
            } else stats
        }

        statistics[result.workflowId] = newStats
    }

    companion object {
        @Volatile
        private var instance: WorkflowEngineImpl? = null

        fun getInstance(): WorkflowEngineImpl {
            return instance ?: synchronized(this) {
                instance ?: WorkflowEngineImpl().also { instance = it }
            }
        }
    }
}

/**
 * Internal representation of a workflow execution
 */
private data class WorkflowExecution(
    val executionId: String,
    val workflow: Workflow,
    var context: WorkflowContext,
    var status: WorkflowResult.WorkflowStatus,
    val executedTransitions: MutableList<WorkflowTransition> = mutableListOf(),
    val executedActions: MutableList<WorkflowAction> = mutableListOf(),
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Events that can occur during workflow execution
 */
private sealed class WorkflowEvent {
    data class ExecutionEvent(val executionId: String, val event: String) : WorkflowEvent()
    data class StateTransitionEvent(val executionId: String, val fromState: WorkflowState, val toState: WorkflowState) : WorkflowEvent()
}