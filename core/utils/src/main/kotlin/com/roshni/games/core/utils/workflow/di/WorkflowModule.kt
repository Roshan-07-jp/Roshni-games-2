package com.roshni.games.core.utils.workflow.di

import com.roshni.games.core.utils.workflow.WorkflowEngine
import com.roshni.games.core.utils.workflow.WorkflowEngineImpl
import com.roshni.games.core.utils.workflow.WorkflowService
import org.koin.dsl.module

/**
 * Dependency injection module for workflow engine components
 */
val workflowModule = module {
    // Provide singleton instance of WorkflowEngine
    single<WorkflowEngine> {
        WorkflowEngineImpl.getInstance()
    }

    // Provide WorkflowService as the main API
    single<WorkflowService> {
        WorkflowService(get())
    }

    // Provide workflow factories
    factory { (workflowId: String) ->
        when (workflowId) {
            "onboarding_workflow" -> com.roshni.games.core.utils.workflow.OnboardingWorkflow()
            "game_session_workflow" -> com.roshni.games.core.utils.workflow.GameSessionWorkflow()
            "purchase_workflow" -> com.roshni.games.core.utils.workflow.PurchaseWorkflow()
            else -> throw IllegalArgumentException("Unknown workflow: $workflowId")
        }
    }

    // Provide specific workflow instances
    single(com.roshni.games.core.utils.workflow.di.WorkflowQualifiers.ONBOARDING_WORKFLOW) {
        com.roshni.games.core.utils.workflow.OnboardingWorkflow()
    }

    single(com.roshni.games.core.utils.workflow.di.WorkflowQualifiers.GAME_SESSION_WORKFLOW) {
        com.roshni.games.core.utils.workflow.GameSessionWorkflow()
    }

    single(com.roshni.games.core.utils.workflow.di.WorkflowQualifiers.PURCHASE_WORKFLOW) {
        com.roshni.games.core.utils.workflow.PurchaseWorkflow()
    }
}

/**
 * Workflow-related qualifiers for dependency injection
 */
object WorkflowQualifiers {
    const val ONBOARDING_WORKFLOW = "onboarding_workflow"
    const val GAME_SESSION_WORKFLOW = "game_session_workflow"
    const val PURCHASE_WORKFLOW = "purchase_workflow"
}