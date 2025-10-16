package com.roshni.games.core.utils.workflow

/**
 * Result of a workflow execution
 *
 * @property workflowId The ID of the workflow that was executed
 * @property executionId Unique identifier for this execution instance
 * @property status The final status of the workflow execution
 * @property startTime When the workflow execution started
 * @property endTime When the workflow execution ended
 * @property finalState The final state of the workflow
 * @property executedTransitions List of transitions that were executed
 * @property executedActions List of actions that were executed
 * @property variables Final state of workflow variables
 * @property events Events that occurred during execution
 * @property error Error information if the workflow failed
 * @property metadata Additional metadata about the execution
 */
data class WorkflowResult(
    val workflowId: String,
    val executionId: String,
    val status: WorkflowStatus,
    val startTime: Long,
    val endTime: Long,
    val finalState: WorkflowState,
    val executedTransitions: List<WorkflowTransition> = emptyList(),
    val executedActions: List<WorkflowAction> = emptyList(),
    val variables: Map<String, Any> = emptyMap(),
    val events: Set<String> = emptySet(),
    val error: WorkflowError? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Status of workflow execution
     */
    enum class WorkflowStatus {
        COMPLETED,      // Workflow completed successfully
        FAILED,         // Workflow failed due to an error
        TIMEOUT,        // Workflow timed out
        CANCELLED,      // Workflow was cancelled
        PAUSED          // Workflow is paused and can be resumed
    }

    /**
     * Error information for failed workflows
     */
    data class WorkflowError(
        val code: String,
        val message: String,
        val cause: Throwable? = null,
        val state: WorkflowState? = null,
        val transition: WorkflowTransition? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get the total execution time in milliseconds
     */
    fun getExecutionTimeMs(): Long {
        return endTime - startTime
    }

    /**
     * Check if the workflow completed successfully
     */
    fun isSuccessful(): Boolean {
        return status == WorkflowStatus.COMPLETED
    }

    /**
     * Check if the workflow failed
     */
    fun isFailed(): Boolean {
        return status == WorkflowStatus.FAILED
    }

    /**
     * Check if the workflow was cancelled
     */
    fun isCancelled(): Boolean {
        return status == WorkflowStatus.CANCELLED
    }

    /**
     * Check if the workflow timed out
     */
    fun isTimedOut(): Boolean {
        return status == WorkflowStatus.TIMEOUT
    }

    /**
     * Check if the workflow is paused
     */
    fun isPaused(): Boolean {
        return status == WorkflowStatus.PAUSED
    }

    /**
     * Get the error message if the workflow failed
     */
    fun getErrorMessage(): String? {
        return error?.message
    }

    /**
     * Get the error code if the workflow failed
     */
    fun getErrorCode(): String? {
        return error?.code
    }

    /**
     * Check if the workflow reached a terminal state
     */
    fun reachedTerminalState(): Boolean {
        return finalState.isTerminal || isFailed() || isCancelled() || isTimedOut()
    }

    /**
     * Get a summary of the workflow execution
     */
    fun getSummary(): String {
        return "Workflow $workflowId ($executionId) ${status.name} in ${getExecutionTimeMs()}ms, " +
               "ended in state ${finalState.name}${if (error != null) ", error: ${error.message}" else ""}"
    }

    /**
     * Create a successful workflow result
     */
    companion object {
        fun success(
            workflowId: String,
            executionId: String,
            startTime: Long,
            endTime: Long,
            finalState: WorkflowState,
            executedTransitions: List<WorkflowTransition> = emptyList(),
            executedActions: List<WorkflowAction> = emptyList(),
            variables: Map<String, Any> = emptyMap(),
            events: Set<String> = emptySet(),
            metadata: Map<String, Any> = emptyMap()
        ): WorkflowResult {
            return WorkflowResult(
                workflowId = workflowId,
                executionId = executionId,
                status = WorkflowStatus.COMPLETED,
                startTime = startTime,
                endTime = endTime,
                finalState = finalState,
                executedTransitions = executedTransitions,
                executedActions = executedActions,
                variables = variables,
                events = events,
                error = null,
                metadata = metadata
            )
        }

        /**
         * Create a failed workflow result
         */
        fun failure(
            workflowId: String,
            executionId: String,
            startTime: Long,
            endTime: Long,
            finalState: WorkflowState,
            error: WorkflowError,
            executedTransitions: List<WorkflowTransition> = emptyList(),
            executedActions: List<WorkflowAction> = emptyList(),
            variables: Map<String, Any> = emptyMap(),
            events: Set<String> = emptySet(),
            metadata: Map<String, Any> = emptyMap()
        ): WorkflowResult {
            return WorkflowResult(
                workflowId = workflowId,
                executionId = executionId,
                status = WorkflowStatus.FAILED,
                startTime = startTime,
                endTime = endTime,
                finalState = finalState,
                executedTransitions = executedTransitions,
                executedActions = executedActions,
                variables = variables,
                events = events,
                error = error,
                metadata = metadata
            )
        }

        /**
         * Create a timed out workflow result
         */
        fun timeout(
            workflowId: String,
            executionId: String,
            startTime: Long,
            endTime: Long,
            finalState: WorkflowState,
            timeoutMs: Long,
            executedTransitions: List<WorkflowTransition> = emptyList(),
            executedActions: List<WorkflowAction> = emptyList(),
            variables: Map<String, Any> = emptyMap(),
            events: Set<String> = emptySet(),
            metadata: Map<String, Any> = emptyMap()
        ): WorkflowResult {
            val error = WorkflowError(
                code = "WORKFLOW_TIMEOUT",
                message = "Workflow timed out after ${timeoutMs}ms",
                state = finalState,
                timestamp = endTime
            )

            return WorkflowResult(
                workflowId = workflowId,
                executionId = executionId,
                status = WorkflowStatus.TIMEOUT,
                startTime = startTime,
                endTime = endTime,
                finalState = finalState,
                executedTransitions = executedTransitions,
                executedActions = executedActions,
                variables = variables,
                events = events,
                error = error,
                metadata = metadata
            )
        }

        /**
         * Create a cancelled workflow result
         */
        fun cancelled(
            workflowId: String,
            executionId: String,
            startTime: Long,
            endTime: Long,
            finalState: WorkflowState,
            reason: String = "Workflow was cancelled",
            executedTransitions: List<WorkflowTransition> = emptyList(),
            executedActions: List<WorkflowAction> = emptyList(),
            variables: Map<String, Any> = emptyMap(),
            events: Set<String> = emptySet(),
            metadata: Map<String, Any> = emptyMap()
        ): WorkflowResult {
            val error = WorkflowError(
                code = "WORKFLOW_CANCELLED",
                message = reason,
                state = finalState,
                timestamp = endTime
            )

            return WorkflowResult(
                workflowId = workflowId,
                executionId = executionId,
                status = WorkflowStatus.CANCELLED,
                startTime = startTime,
                endTime = endTime,
                finalState = finalState,
                executedTransitions = executedTransitions,
                executedActions = executedActions,
                variables = variables,
                events = events,
                error = error,
                metadata = metadata
            )
        }
    }
}