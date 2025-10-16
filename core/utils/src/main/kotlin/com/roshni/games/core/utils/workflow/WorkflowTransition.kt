package com.roshni.games.core.utils.workflow

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Represents a transition between workflow states
 *
 * @property id Unique identifier for this transition
 * @property name Human-readable name for this transition
 * @property description Detailed description of when this transition occurs
 * @property fromState The state this transition originates from
 * @property toState The state this transition leads to
 * @property condition The condition that must be met for this transition to occur
 * @property actions Actions to execute during this transition
 * @property priority Priority of this transition (higher values = higher priority)
 * @property isAutomatic Whether this transition happens automatically or requires external trigger
 * @property timeoutMs Timeout in milliseconds for waiting for this transition
 * @property metadata Additional metadata associated with this transition
 */
data class WorkflowTransition(
    val id: String,
    val name: String,
    val description: String = "",
    val fromState: WorkflowState,
    val toState: WorkflowState,
    val condition: TransitionCondition? = null,
    val actions: List<WorkflowAction> = emptyList(),
    val priority: Int = 0,
    val isAutomatic: Boolean = false,
    val timeoutMs: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Condition that determines when a transition can occur
     */
    sealed class TransitionCondition {
        abstract val description: String

        /**
         * Evaluate the condition against the provided context
         */
        abstract suspend fun evaluate(context: WorkflowContext): Boolean

        /**
         * Always true condition (unconditional transition)
         */
        data class AlwaysTrue(override val description: String = "Always allows transition") : TransitionCondition() {
            override suspend fun evaluate(context: WorkflowContext): Boolean = true
        }

        /**
         * Rule-based condition that uses the RuleEngine for evaluation
         */
        data class RuleBased(
            val ruleId: String,
            override val description: String = "Rule-based transition condition"
        ) : TransitionCondition() {
            override suspend fun evaluate(context: WorkflowContext): Boolean {
                // This would integrate with the RuleEngine
                // For now, return true as placeholder
                return true
            }
        }

        /**
         * Time-based condition
         */
        data class TimeBased(
            val minDurationMs: Long? = null,
            val maxDurationMs: Long? = null,
            override val description: String = "Time-based transition condition"
        ) : TransitionCondition() {
            override suspend fun evaluate(context: WorkflowContext): Boolean {
                val elapsedTime = System.currentTimeMillis() - context.startTime

                if (minDurationMs != null && elapsedTime < minDurationMs) {
                    return false
                }

                if (maxDurationMs != null && elapsedTime > maxDurationMs) {
                    return false
                }

                return true
            }
        }

        /**
         * Event-based condition that waits for specific events
         */
        data class EventBased(
            val requiredEvents: List<String>,
            val forbiddenEvents: List<String> = emptyList(),
            override val description: String = "Event-based transition condition"
        ) : TransitionCondition() {
            override suspend fun evaluate(context: WorkflowContext): Boolean {
                val occurredEvents = context.events

                // Check that all required events have occurred
                val hasAllRequired = requiredEvents.all { it in occurredEvents }

                // Check that no forbidden events have occurred
                val hasForbidden = forbiddenEvents.any { it in occurredEvents }

                return hasAllRequired && !hasForbidden
            }
        }

        /**
         * Custom condition with user-defined logic
         */
        data class CustomCondition(
            val evaluator: suspend (WorkflowContext) -> Boolean,
            override val description: String = "Custom transition condition"
        ) : TransitionCondition() {
            override suspend fun evaluate(context: WorkflowContext): Boolean {
                return try {
                    evaluator(context)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    /**
     * Check if this transition is valid for the given states
     */
    fun isValid(): Boolean {
        return fromState.canTransitionTo(toState) && fromState.id != toState.id
    }

    /**
     * Get the timeout for this transition in milliseconds
     */
    fun getTimeoutMs(): Long? {
        return timeoutMs ?: metadata["timeoutMs"] as? Long
    }

    /**
     * Check if this transition has a timeout configured
     */
    fun hasTimeout(): Boolean {
        return getTimeoutMs() != null
    }
}