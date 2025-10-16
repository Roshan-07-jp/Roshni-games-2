package com.roshni.games.core.utils.workflow

/**
 * Workflow for managing game session lifecycle
 */
class GameSessionWorkflow : BaseWorkflow() {

    override val id = "game_session_workflow"
    override val name = "Game Session Management"
    override val description = "Manages the complete lifecycle of a game session from start to finish"
    override val category = "game_management"
    override val version = 1
    override val enabled = true
    override val tags = listOf("game_session", "lifecycle", "gameplay")
    override val defaultTimeoutMs = 7200000L // 2 hours
    override val createdAt = System.currentTimeMillis()
    override val modifiedAt = System.currentTimeMillis()

    override val workflowStates = listOf(
        WorkflowState(
            id = "session_start",
            name = "Session Start",
            description = "Initialize new game session",
            type = WorkflowState.StateType.INITIAL,
            isInitial = true,
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "init_session",
                    name = "Initialize Session",
                    updates = mapOf(
                        "session_start_time" to System.currentTimeMillis(),
                        "session_status" to "active",
                        "score" to 0,
                        "lives" to 3
                    )
                ),
                WorkflowAction.LogEvent(
                    id = "log_session_start",
                    name = "Log Session Start",
                    message = "New game session started",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "game_loading",
            name = "Game Loading",
            description = "Load game assets and prepare gameplay",
            entryActions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_game_loading",
                    name = "Log Game Loading",
                    message = "Loading game assets and initializing gameplay",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "active_gameplay",
            name = "Active Gameplay",
            description = "Main gameplay state where user is actively playing",
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "start_gameplay",
                    name = "Start Gameplay",
                    updates = mapOf("gameplay_start_time" to System.currentTimeMillis())
                )
            )
        ),
        WorkflowState(
            id = "pause_check",
            name = "Pause Check",
            description = "Check if game should be paused or resumed",
            type = WorkflowState.StateType.DECISION
        ),
        WorkflowState(
            id = "game_paused",
            name = "Game Paused",
            description = "Game is paused waiting for user input",
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "notify_pause",
                    name = "Notify Game Paused",
                    title = "Game Paused",
                    message = "Tap to resume your game",
                    type = WorkflowAction.SendNotification.NotificationType.INFO
                )
            )
        ),
        WorkflowState(
            id = "game_over_check",
            name = "Game Over Check",
            description = "Check if game has ended",
            type = WorkflowState.StateType.DECISION
        ),
        WorkflowState(
            id = "game_over",
            name = "Game Over",
            description = "Game has ended, show results and options",
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "finalize_session",
                    name = "Finalize Session",
                    updates = mapOf(
                        "session_end_time" to System.currentTimeMillis(),
                        "session_status" to "completed"
                    )
                ),
                WorkflowAction.LogEvent(
                    id = "log_game_over",
                    name = "Log Game Over",
                    message = "Game session ended",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "session_cleanup",
            name = "Session Cleanup",
            description = "Clean up session resources and save data",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true,
            entryActions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_session_cleanup",
                    name = "Log Session Cleanup",
                    message = "Cleaning up session resources",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                ),
                WorkflowAction.CustomAction(
                    id = "save_game_data",
                    name = "Save Game Data",
                    executor = { context ->
                        // TODO: Integrate with save state system
                        println("Saving game data for session ${context.executionId}")
                        true
                    }
                )
            )
        )
    )

    override val workflowTransitions = listOf(
        WorkflowTransition(
            id = "start_to_loading",
            name = "Start to Loading",
            description = "Automatic transition from session start to game loading",
            fromState = workflowStates[0], // session_start
            toState = workflowStates[1],   // game_loading
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after initialization"),
            actions = listOf(
                WorkflowAction.Wait(
                    id = "wait_loading",
                    name = "Wait for Loading",
                    durationMs = 2000 // Simulate loading time
                )
            )
        ),
        WorkflowTransition(
            id = "loading_to_gameplay",
            name = "Loading to Gameplay",
            description = "Transition after game assets are loaded",
            fromState = workflowStates[1], // game_loading
            toState = workflowStates[2],   // active_gameplay
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after loading")
        ),
        WorkflowTransition(
            id = "gameplay_to_pause_check",
            name = "Gameplay to Pause Check",
            description = "Check if game should be paused",
            fromState = workflowStates[2], // active_gameplay
            toState = workflowStates[3],   // pause_check
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("pause_requested"),
                description = "Pause event must be received"
            )
        ),
        WorkflowTransition(
            id = "pause_check_to_paused",
            name = "Pause Check to Paused",
            description = "Transition to paused state",
            fromState = workflowStates[3], // pause_check
            toState = workflowStates[4],   // game_paused
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.hasEvent("pause_requested")
                },
                description = "Game should be paused"
            )
        ),
        WorkflowTransition(
            id = "pause_check_to_gameplay",
            name = "Pause Check to Gameplay",
            description = "Continue gameplay if not paused",
            fromState = workflowStates[3], // pause_check
            toState = workflowStates[2],   // active_gameplay
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    !context.hasEvent("pause_requested")
                },
                description = "Game should continue"
            )
        ),
        WorkflowTransition(
            id = "paused_to_gameplay",
            name = "Paused to Gameplay",
            description = "Resume gameplay from paused state",
            fromState = workflowStates[4], // game_paused
            toState = workflowStates[2],   // active_gameplay
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("resume_requested"),
                description = "Resume event must be received"
            )
        ),
        WorkflowTransition(
            id = "gameplay_to_game_over_check",
            name = "Gameplay to Game Over Check",
            description = "Check if game has ended",
            fromState = workflowStates[2], // active_gameplay
            toState = workflowStates[5],   // game_over_check
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("game_ended", "lives_exhausted", "level_completed"),
                description = "Game end condition must be met"
            )
        ),
        WorkflowTransition(
            id = "game_over_check_to_game_over",
            name = "Game Over Check to Game Over",
            description = "Transition to game over state",
            fromState = workflowStates[5], // game_over_check
            toState = workflowStates[6],   // game_over
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.hasAnyEvent("game_ended", "lives_exhausted") ||
                    context.getVariable<Int>("lives") == 0
                },
                description = "Game has ended"
            )
        ),
        WorkflowTransition(
            id = "game_over_check_to_gameplay",
            name = "Game Over Check to Gameplay",
            description = "Continue gameplay if game not over",
            fromState = workflowStates[5], // game_over_check
            toState = workflowStates[2],   // active_gameplay
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    val lives = context.getVariable<Int>("lives") ?: 0
                    !context.hasAnyEvent("game_ended", "lives_exhausted") && lives > 0
                },
                description = "Game should continue"
            )
        ),
        WorkflowTransition(
            id = "game_over_to_cleanup",
            name = "Game Over to Cleanup",
            description = "Final transition to session cleanup",
            fromState = workflowStates[6], // game_over
            toState = workflowStates[7],   // session_cleanup
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after game over"),
            actions = listOf(
                WorkflowAction.Wait(
                    id = "wait_cleanup_delay",
                    name = "Wait for Cleanup Delay",
                    durationMs = 3000 // Show results for 3 seconds
                )
            )
        )
    )

    override val workflowGlobalActions = listOf(
        WorkflowAction.LogEvent(
            id = "log_session_lifecycle",
            name = "Log Session Lifecycle",
            message = "Game session lifecycle event",
            level = WorkflowAction.LogEvent.LogLevel.DEBUG
        )
    )

    override suspend fun canExecute(context: WorkflowContext): Boolean {
        // Check if user is allowed to start a game session
        val ruleContext = context.toRuleContext()
        if (ruleContext != null) {
            // TODO: Integrate with RuleEngine to check session permissions
            // For now, assume any authenticated user can start a session
        }

        return super.canExecute(context) && context.userId.isNotBlank()
    }

    override fun getEstimatedExecutionTimeMs(): Long {
        return 600000L // 10 minutes estimated average session
    }
}