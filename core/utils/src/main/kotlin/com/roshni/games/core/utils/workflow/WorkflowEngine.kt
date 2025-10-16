package com.roshni.games.core.utils.workflow

import kotlinx.coroutines.flow.Flow

/**
 * Core interface for the workflow engine that manages workflow registration,
 * execution, and state machine operations across the gaming platform.
 */
interface WorkflowEngine {

    /**
     * Register a new workflow with the engine
     *
     * @param workflow The workflow to register
     * @return true if registration was successful, false otherwise
     */
    suspend fun registerWorkflow(workflow: Workflow): Boolean

    /**
     * Unregister a workflow from the engine
     *
     * @param workflowId The ID of the workflow to unregister
     * @return true if unregistration was successful, false otherwise
     */
    suspend fun unregisterWorkflow(workflowId: String): Boolean

    /**
     * Get a registered workflow by its ID
     *
     * @param workflowId The ID of the workflow to retrieve
     * @return The workflow if found, null otherwise
     */
    suspend fun getWorkflow(workflowId: String): Workflow?

    /**
     * Get all registered workflows
     *
     * @return List of all registered workflows
     */
    suspend fun getAllWorkflows(): List<Workflow>

    /**
     * Get workflows by category
     *
     * @param category The category to filter by
     * @return List of workflows in the specified category
     */
    suspend fun getWorkflowsByCategory(category: String): List<Workflow>

    /**
     * Get workflows by tags
     *
     * @param tags The tags to filter by
     * @return List of workflows that have any of the specified tags
     */
    suspend fun getWorkflowsByTags(tags: List<String>): List<Workflow>

    /**
     * Start execution of a workflow
     *
     * @param workflowId The ID of the workflow to execute
     * @param context Initial context for workflow execution
     * @return The execution ID for tracking the workflow instance
     */
    suspend fun startWorkflow(workflowId: String, context: WorkflowContext): String

    /**
     * Start execution of a workflow with minimal context
     *
     * @param workflowId The ID of the workflow to execute
     * @param userId The user ID for the workflow context
     * @param initialVariables Initial variables for the workflow
     * @return The execution ID for tracking the workflow instance
     */
    suspend fun startWorkflow(
        workflowId: String,
        userId: String,
        initialVariables: Map<String, Any> = emptyMap()
    ): String

    /**
     * Pause a running workflow execution
     *
     * @param executionId The execution ID of the workflow to pause
     * @return true if the workflow was paused successfully, false otherwise
     */
    suspend fun pauseWorkflow(executionId: String): Boolean

    /**
     * Resume a paused workflow execution
     *
     * @param executionId The execution ID of the workflow to resume
     * @return true if the workflow was resumed successfully, false otherwise
     */
    suspend fun resumeWorkflow(executionId: String): Boolean

    /**
     * Cancel a running workflow execution
     *
     * @param executionId The execution ID of the workflow to cancel
     * @param reason Reason for cancellation
     * @return true if the workflow was cancelled successfully, false otherwise
     */
    suspend fun cancelWorkflow(executionId: String, reason: String = "Workflow cancelled"): Boolean

    /**
     * Get the current status of a workflow execution
     *
     * @param executionId The execution ID to check
     * @return The current execution status, null if not found
     */
    suspend fun getWorkflowStatus(executionId: String): WorkflowExecutionStatus?

    /**
     * Get the result of a completed workflow execution
     *
     * @param executionId The execution ID to get results for
     * @return The workflow result, null if not found or still running
     */
    suspend fun getWorkflowResult(executionId: String): WorkflowResult?

    /**
     * Get all active workflow executions
     *
     * @return List of all active workflow execution statuses
     */
    suspend fun getActiveExecutions(): List<WorkflowExecutionStatus>

    /**
     * Get workflow executions for a specific user
     *
     * @param userId The user ID to filter by
     * @return List of workflow execution statuses for the user
     */
    suspend fun getExecutionsForUser(userId: String): List<WorkflowExecutionStatus>

    /**
     * Send an event to a workflow execution
     *
     * @param executionId The execution ID of the workflow
     * @param event The event to send
     * @return true if the event was processed successfully, false otherwise
     */
    suspend fun sendEvent(executionId: String, event: String): Boolean

    /**
     * Update variables in a running workflow execution
     *
     * @param executionId The execution ID of the workflow
     * @param variables Variables to update
     * @return true if the variables were updated successfully, false otherwise
     */
    suspend fun updateVariables(executionId: String, variables: Map<String, Any>): Boolean

    /**
     * Get workflow execution statistics
     *
     * @param workflowId The ID of the workflow to get statistics for (null for all workflows)
     * @return Workflow execution statistics
     */
    suspend fun getWorkflowStatistics(workflowId: String? = null): WorkflowStatistics

    /**
     * Clear workflow execution statistics
     */
    suspend fun clearStatistics()

    /**
     * Validate all registered workflows
     *
     * @return Validation result containing any errors or warnings
     */
    suspend fun validateAllWorkflows(): WorkflowValidationResult

    /**
     * Export workflows configuration for backup or migration
     *
     * @return Exported workflows configuration as a map
     */
    suspend fun exportWorkflows(): Map<String, Any>

    /**
     * Import workflows configuration
     *
     * @param configuration The configuration to import
     * @return true if import was successful, false otherwise
     */
    suspend fun importWorkflows(configuration: Map<String, Any>): Boolean

    /**
     * Get the current engine status
     */
    suspend fun getEngineStatus(): WorkflowEngineStatus

    /**
     * Shutdown the workflow engine and clean up resources
     */
    suspend fun shutdown()

    /**
     * Observe workflow execution results as a flow
     *
     * @param workflowId Optional workflow ID to filter results (null for all workflows)
     * @return Flow of workflow results
     */
    fun observeWorkflowResults(workflowId: String? = null): Flow<WorkflowResult>

    /**
     * Observe workflow execution status changes
     *
     * @param executionId Optional execution ID to filter (null for all executions)
     * @return Flow of workflow execution status updates
     */
    fun observeExecutionStatus(executionId: String? = null): Flow<WorkflowExecutionStatus>
}

/**
 * Status of a workflow execution
 */
data class WorkflowExecutionStatus(
    val executionId: String,
    val workflowId: String,
    val status: WorkflowResult.WorkflowStatus,
    val currentState: WorkflowState,
    val startTime: Long,
    val lastUpdateTime: Long,
    val userId: String,
    val progress: Float = 0.0f,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Statistics about workflow execution performance and results
 */
data class WorkflowStatistics(
    val totalExecutions: Long = 0,
    val completedExecutions: Long = 0,
    val failedExecutions: Long = 0,
    val cancelledExecutions: Long = 0,
    val timedOutExecutions: Long = 0,
    val averageExecutionTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val lastExecutionTime: Long? = null,
    val workflowsExecuted: Map<String, Long> = emptyMap(), // workflowId -> execution count
    val categoryStatistics: Map<String, CategoryStats> = emptyMap()
) {

    data class CategoryStats(
        val executionCount: Long = 0,
        val averageExecutionTimeMs: Double = 0.0,
        val successRate: Double = 0.0
    )
}

/**
 * Result of workflow validation
 */
data class WorkflowValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val workflowValidationResults: Map<String, WorkflowValidationResult> = emptyMap()
) {

    data class WorkflowValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
}

/**
 * Current status of the workflow engine
 */
data class WorkflowEngineStatus(
    val isRunning: Boolean,
    val registeredWorkflowCount: Int,
    val activeExecutionCount: Int,
    val completedExecutionCount: Long,
    val failedExecutionCount: Long,
    val lastActivityTime: Long?,
    val memoryUsage: Long = 0,
    val errorCount: Long = 0,
    val uptimeMs: Long = 0
)