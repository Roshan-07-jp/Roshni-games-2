package com.roshni.games.core.utils.workflow

/**
 * Represents an action that can be executed during workflow transitions or state changes
 */
sealed class WorkflowAction {

    /**
     * Unique identifier for this action
     */
    abstract val id: String

    /**
     * Human-readable name for this action
     */
    abstract val name: String

    /**
     * Detailed description of what this action does
     */
    abstract val description: String

    /**
     * Priority level for action execution (higher values = higher priority)
     */
    abstract val priority: Int

    /**
     * Whether this action should be executed immediately or can be deferred
     */
    abstract val immediate: Boolean

    /**
     * Execute this action with the provided context
     *
     * @param context The workflow context containing all necessary information
     * @return true if the action was executed successfully, false otherwise
     */
    abstract suspend fun execute(context: WorkflowContext): Boolean

    /**
     * Check if this action can be executed in the current context
     *
     * @param context The workflow context containing all necessary information
     * @return true if the action can be executed, false otherwise
     */
    abstract suspend fun canExecute(context: WorkflowContext): Boolean

    /**
     * Rollback this action (undo its effects)
     *
     * @param context The workflow context containing all necessary information
     * @return true if the action was rolled back successfully, false otherwise
     */
    abstract suspend fun rollback(context: WorkflowContext): Boolean

    /**
     * Send a notification or message to the user
     */
    data class SendNotification(
        override val id: String,
        override val name: String,
        val title: String,
        val message: String,
        val type: NotificationType = NotificationType.INFO,
        override val description: String = "Send notification to user",
        override val priority: Int = 1,
        override val immediate: Boolean = true
    ) : WorkflowAction() {

        enum class NotificationType {
            INFO, SUCCESS, WARNING, ERROR
        }

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                // TODO: Integrate with notification system
                println("Sending notification: $title - $message")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean = true

        override suspend fun rollback(context: WorkflowContext): Boolean {
            // Notifications typically cannot be rolled back
            return true
        }
    }

    /**
     * Update workflow variables
     */
    data class UpdateVariables(
        override val id: String,
        override val name: String,
        val updates: Map<String, Any>,
        override val description: String = "Update workflow variables",
        override val priority: Int = 2,
        override val immediate: Boolean = true
    ) : WorkflowAction() {

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                updates.forEach { (key, value) ->
                    context.setVariable(key, value)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean = true

        override suspend fun rollback(context: WorkflowContext): Boolean {
            return try {
                // For rollback, we would need to track previous values
                // This is a simplified implementation
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Execute a rule using the RuleEngine
     */
    data class ExecuteRule(
        override val id: String,
        override val name: String,
        val ruleId: String,
        val executeActions: Boolean = true,
        override val description: String = "Execute rule using RuleEngine",
        override val priority: Int = 3,
        override val immediate: Boolean = true
    ) : WorkflowAction() {

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                // TODO: Integrate with RuleEngine
                println("Executing rule: $ruleId")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean = true

        override suspend fun rollback(context: WorkflowContext): Boolean {
            // Rule execution typically cannot be rolled back
            return true
        }
    }

    /**
     * Wait for a specified duration or condition
     */
    data class Wait(
        override val id: String,
        override val name: String,
        val durationMs: Long? = null,
        val condition: WaitCondition? = null,
        val timeoutMs: Long? = null,
        override val description: String = "Wait for duration or condition",
        override val priority: Int = 0,
        override val immediate: Boolean = false
    ) : WorkflowAction() {

        sealed class WaitCondition {
            data class ForEvent(val event: String) : WaitCondition()
            data class ForVariableChange(val variable: String) : WaitCondition()
            data class ForStateChange(val targetState: String) : WaitCondition()
        }

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                if (durationMs != null) {
                    kotlinx.coroutines.delay(durationMs)
                }
                // TODO: Implement condition waiting logic
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean = true

        override suspend fun rollback(context: WorkflowContext): Boolean {
            // Waiting cannot be rolled back
            return true
        }
    }

    /**
     * Log an event or message
     */
    data class LogEvent(
        override val id: String,
        override val name: String,
        val message: String,
        val level: LogLevel = LogLevel.INFO,
        val data: Map<String, Any> = emptyMap(),
        override val description: String = "Log workflow event",
        override val priority: Int = 0,
        override val immediate: Boolean = false
    ) : WorkflowAction() {

        enum class LogLevel {
            DEBUG, INFO, WARN, ERROR
        }

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                // TODO: Integrate with logging system
                println("[$level] Workflow ${context.workflowId}: $message")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean = true

        override suspend fun rollback(context: WorkflowContext): Boolean {
            // Logging cannot be rolled back
            return true
        }
    }

    /**
     * Custom action with user-defined execution logic
     */
    data class CustomAction(
        override val id: String,
        override val name: String,
        val executor: suspend (WorkflowContext) -> Boolean,
        val rollbackExecutor: suspend (WorkflowContext) -> Boolean = { true },
        val canExecuteChecker: suspend (WorkflowContext) -> Boolean = { true },
        override val description: String = "Custom workflow action",
        override val priority: Int = 1,
        override val immediate: Boolean = true
    ) : WorkflowAction() {

        override suspend fun execute(context: WorkflowContext): Boolean {
            return try {
                executor(context)
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: WorkflowContext): Boolean {
            return try {
                canExecuteChecker(context)
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun rollback(context: WorkflowContext): Boolean {
            return try {
                rollbackExecutor(context)
            } catch (e: Exception) {
                false
            }
        }
    }
}