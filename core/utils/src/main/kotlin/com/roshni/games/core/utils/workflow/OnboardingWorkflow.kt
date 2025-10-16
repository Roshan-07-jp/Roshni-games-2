package com.roshni.games.core.utils.workflow

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Workflow for handling user onboarding process
 */
class OnboardingWorkflow : BaseWorkflow() {

    override val id = "onboarding_workflow"
    override val name = "User Onboarding"
    override val description = "Guides new users through the initial app setup and tutorial process"
    override val category = "user_management"
    override val version = 1
    override val enabled = true
    override val tags = listOf("onboarding", "tutorial", "user_setup")
    override val defaultTimeoutMs = 300000L // 5 minutes
    override val createdAt = System.currentTimeMillis()
    override val modifiedAt = System.currentTimeMillis()

    override val workflowStates = listOf(
        WorkflowState(
            id = "welcome",
            name = "Welcome Screen",
            description = "Initial welcome and introduction to the app",
            type = WorkflowState.StateType.INITIAL,
            isInitial = true,
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "show_welcome",
                    name = "Show Welcome",
                    title = "Welcome to Roshni Games!",
                    message = "Let's get you started with your gaming journey.",
                    type = WorkflowAction.SendNotification.NotificationType.INFO
                )
            )
        ),
        WorkflowState(
            id = "profile_setup",
            name = "Profile Setup",
            description = "User profile creation and preferences",
            entryActions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_profile_start",
                    name = "Log Profile Setup Start",
                    message = "User started profile setup",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "tutorial",
            name = "Game Tutorial",
            description = "Interactive tutorial showing game features",
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "start_tutorial",
                    name = "Start Tutorial",
                    title = "Let's Learn!",
                    message = "We'll show you how to play our games.",
                    type = WorkflowAction.SendNotification.NotificationType.INFO
                )
            )
        ),
        WorkflowState(
            id = "preferences",
            name = "Preferences Setup",
            description = "Configure user preferences and accessibility settings",
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "init_preferences",
                    name = "Initialize Preferences",
                    updates = mapOf(
                        "preferences_completed" to false,
                        "tutorial_completed" to false
                    )
                )
            )
        ),
        WorkflowState(
            id = "completion",
            name = "Onboarding Complete",
            description = "Onboarding process completed successfully",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true,
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "onboarding_complete",
                    name = "Onboarding Complete",
                    title = "Welcome aboard!",
                    message = "You're all set to start playing. Enjoy!",
                    type = WorkflowAction.SendNotification.NotificationType.SUCCESS
                ),
                WorkflowAction.UpdateVariables(
                    id = "mark_onboarding_complete",
                    name = "Mark Onboarding Complete",
                    updates = mapOf(
                        "onboarding_completed" to true,
                        "onboarding_completed_at" to System.currentTimeMillis()
                    )
                )
            )
        )
    )

    override val workflowTransitions = listOf(
        WorkflowTransition(
            id = "welcome_to_profile",
            name = "Welcome to Profile Setup",
            description = "Transition from welcome screen to profile setup",
            fromState = workflowStates[0], // welcome
            toState = workflowStates[1],   // profile_setup
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after welcome"),
            actions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_welcome_complete",
                    name = "Log Welcome Complete",
                    message = "User completed welcome screen",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowTransition(
            id = "profile_to_tutorial",
            name = "Profile Setup to Tutorial",
            description = "Transition after profile setup completion",
            fromState = workflowStates[1], // profile_setup
            toState = workflowStates[2],   // tutorial
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("profile_completed") == true
                },
                description = "Profile setup must be completed"
            ),
            actions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_tutorial_start",
                    name = "Log Tutorial Start",
                    message = "Starting tutorial after profile setup",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowTransition(
            id = "tutorial_to_preferences",
            name = "Tutorial to Preferences",
            description = "Transition after tutorial completion",
            fromState = workflowStates[2], // tutorial
            toState = workflowStates[3],   // preferences
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("tutorial_completed") == true
                },
                description = "Tutorial must be completed"
            )
        ),
        WorkflowTransition(
            id = "preferences_to_completion",
            name = "Preferences to Completion",
            description = "Final transition to onboarding completion",
            fromState = workflowStates[3], // preferences
            toState = workflowStates[4],   // completion
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("preferences_completed") == true
                },
                description = "Preferences setup must be completed"
            ),
            actions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_onboarding_complete",
                    name = "Log Onboarding Complete",
                    message = "User completed entire onboarding process",
                    level = WorkflowAction.LogEvent.LogLevel.INFO,
                    data = mapOf(
                        "completion_time_ms" to context.getElapsedTimeMs()
                    )
                )
            )
        )
    )

    override val workflowGlobalActions = listOf(
        WorkflowAction.LogEvent(
            id = "log_onboarding_start",
            name = "Log Onboarding Start",
            message = "User started onboarding workflow",
            level = WorkflowAction.LogEvent.LogLevel.INFO
        )
    )

    override suspend fun canExecute(context: WorkflowContext): Boolean {
        // Check if user has already completed onboarding
        val ruleContext = context.toRuleContext()
        if (ruleContext != null) {
            // TODO: Integrate with RuleEngine to check onboarding status
            // For now, assume any user can run onboarding
        }

        return super.canExecute(context) && !context.hasEvent("onboarding_skip")
    }

    override fun getEstimatedExecutionTimeMs(): Long {
        return 180000L // 3 minutes estimated
    }
}