package com.roshni.games.core.utils.workflow

import com.roshni.games.core.utils.rules.RuleContext
import kotlinx.coroutines.flow.Flow

/**
 * High-level service for workflow operations
 * Provides a simplified API for common workflow operations
 */
class WorkflowService(private val workflowEngine: WorkflowEngine) {

    /**
     * Start an onboarding workflow for a new user
     */
    suspend fun startOnboardingWorkflow(
        userId: String,
        ruleContext: RuleContext? = null
    ): String {
        val workflow = OnboardingWorkflow()
        val context = workflow.createContext(
            userId = userId,
            ruleContext = ruleContext
        )

        return workflowEngine.startWorkflow(workflow.id, context)
    }

    /**
     * Start a game session workflow
     */
    suspend fun startGameSessionWorkflow(
        userId: String,
        gameId: String,
        sessionId: String? = null,
        ruleContext: RuleContext? = null
    ): String {
        val workflow = GameSessionWorkflow()
        val context = workflow.createContext(
            userId = userId,
            sessionId = sessionId,
            initialVariables = mapOf("game_id" to gameId),
            ruleContext = ruleContext
        )

        return workflowEngine.startWorkflow(workflow.id, context)
    }

    /**
     * Start a purchase workflow
     */
    suspend fun startPurchaseWorkflow(
        userId: String,
        itemId: String,
        ruleContext: RuleContext? = null
    ): String {
        val workflow = PurchaseWorkflow()
        val context = workflow.createContext(
            userId = userId,
            initialVariables = mapOf("selected_item_id" to itemId),
            ruleContext = ruleContext
        )

        return workflowEngine.startWorkflow(workflow.id, context)
    }

    /**
     * Register a custom workflow
     */
    suspend fun registerWorkflow(workflow: Workflow): Boolean {
        return workflowEngine.registerWorkflow(workflow)
    }

    /**
     * Get workflow execution status
     */
    suspend fun getWorkflowStatus(executionId: String): WorkflowExecutionStatus? {
        return workflowEngine.getWorkflowStatus(executionId)
    }

    /**
     * Cancel a workflow execution
     */
    suspend fun cancelWorkflow(executionId: String, reason: String = "Cancelled by user"): Boolean {
        return workflowEngine.cancelWorkflow(executionId, reason)
    }

    /**
     * Send an event to a workflow
     */
    suspend fun sendEventToWorkflow(executionId: String, event: String): Boolean {
        return workflowEngine.sendEvent(executionId, event)
    }

    /**
     * Update workflow variables
     */
    suspend fun updateWorkflowVariables(
        executionId: String,
        variables: Map<String, Any>
    ): Boolean {
        return workflowEngine.updateVariables(executionId, variables)
    }

    /**
     * Observe workflow results
     */
    fun observeWorkflowResults(workflowId: String? = null): Flow<WorkflowResult> {
        return workflowEngine.observeWorkflowResults(workflowId)
    }

    /**
     * Observe workflow execution status changes
     */
    fun observeExecutionStatus(executionId: String? = null): Flow<WorkflowExecutionStatus> {
        return workflowEngine.observeExecutionStatus(executionId)
    }

    /**
     * Get workflow statistics
     */
    suspend fun getWorkflowStatistics(workflowId: String? = null): WorkflowStatistics {
        return workflowEngine.getWorkflowStatistics(workflowId)
    }

    /**
     * Validate all workflows
     */
    suspend fun validateAllWorkflows(): WorkflowValidationResult {
        return workflowEngine.validateAllWorkflows()
    }

    /**
     * Get engine status
     */
    suspend fun getEngineStatus(): WorkflowEngineStatus {
        return workflowEngine.getEngineStatus()
    }

    /**
     * Shutdown the workflow service
     */
    suspend fun shutdown() {
        workflowEngine.shutdown()
    }

    companion object {
        // Workflow event constants
        const val EVENT_ONBOARDING_SKIP = "onboarding_skip"
        const val EVENT_PAUSE_REQUESTED = "pause_requested"
        const val EVENT_RESUME_REQUESTED = "resume_requested"
        const val EVENT_GAME_ENDED = "game_ended"
        const val EVENT_LIVES_EXHAUSTED = "lives_exhausted"
        const val EVENT_LEVEL_COMPLETED = "level_completed"
        const val EVENT_PURCHASE_CANCELLED = "purchase_cancelled"

        // Workflow variable constants
        const val VAR_PROFILE_COMPLETED = "profile_completed"
        const val VAR_TUTORIAL_COMPLETED = "tutorial_completed"
        const val VAR_PREFERENCES_COMPLETED = "preferences_completed"
        const val VAR_SELECTED_ITEM_ID = "selected_item_id"
        const val VAR_PAYMENT_METHOD = "payment_method"
        const val VAR_VALIDATION_PASSED = "validation_passed"
        const val VAR_PAYMENT_VERIFIED = "payment_verified"
        const val VAR_GAME_ID = "game_id"
        const val VAR_SCORE = "score"
        const val VAR_LIVES = "lives"
    }
}