package com.roshni.games.core.utils.rules

/**
 * Result of a rule evaluation
 *
 * @property ruleId The unique identifier of the rule that was evaluated
 * @property ruleType The type/category of the rule
 * @property isAllowed Whether the rule evaluation passed (true) or failed (false)
 * @property confidence The confidence level of the evaluation result (0.0 to 1.0)
 * @property reason Human-readable explanation of why the rule passed or failed
 * @property actions List of actions to be executed as a result of this rule evaluation
 * @property metadata Additional data associated with the rule result
 * @property executionTimeMs Time taken to evaluate the rule in milliseconds
 * @property timestamp When the rule was evaluated
 * @property contextSnapshot Snapshot of the context at evaluation time
 * @property warnings List of non-critical issues encountered during evaluation
 * @property errors List of errors that occurred during rule evaluation
 */
data class RuleResult(
    val ruleId: String,
    val ruleType: RuleType,
    val isAllowed: Boolean,
    val confidence: Float = 1.0f,
    val reason: String = "",
    val actions: List<RuleAction> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val contextSnapshot: Map<String, Any> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {

    /**
     * Types of rules in the system
     */
    enum class RuleType {
        GAMEPLAY,
        PERMISSION,
        FEATURE_GATE,
        CONTENT_RESTRICTION,
        PARENTAL_CONTROL,
        CUSTOM
    }

    /**
     * Actions that can be triggered by rule evaluation
     */
    sealed class RuleAction {
        abstract val actionId: String
        abstract val priority: Int
        abstract val metadata: Map<String, Any>

        /**
         * Show a message to the user
         */
        data class ShowMessage(
            override val actionId: String,
            val message: String,
            val messageType: MessageType = MessageType.INFO,
            override val priority: Int = 1,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction() {

            enum class MessageType {
                INFO, WARNING, ERROR, SUCCESS
            }
        }

        /**
         * Block or allow access to content
         */
        data class ContentAccess(
            override val actionId: String,
            val contentId: String,
            val accessGranted: Boolean,
            val reason: String = "",
            override val priority: Int = 2,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction()

        /**
         * Modify gameplay behavior
         */
        data class GameplayModification(
            override val actionId: String,
            val modifications: Map<String, Any>,
            val reason: String = "",
            override val priority: Int = 3,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction()

        /**
         * Log an event for analytics
         */
        data class LogEvent(
            override val actionId: String,
            val eventName: String,
            val eventData: Map<String, Any>,
            override val priority: Int = 0,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction()

        /**
         * Trigger a feature flag change
         */
        data class FeatureToggle(
            override val actionId: String,
            val featureId: String,
            val enabled: Boolean,
            val reason: String = "",
            override val priority: Int = 1,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction()

        /**
         * Execute a custom action
         */
        data class CustomAction(
            override val actionId: String,
            val actionData: Map<String, Any>,
            override val priority: Int = 1,
            override val metadata: Map<String, Any> = emptyMap()
        ) : RuleAction()
    }

    /**
     * Create a successful rule result
     */
    companion object {
        fun success(
            ruleId: String,
            ruleType: RuleType,
            reason: String = "Rule evaluation passed",
            actions: List<RuleAction> = emptyList(),
            metadata: Map<String, Any> = emptyMap(),
            executionTimeMs: Long = 0L
        ): RuleResult {
            return RuleResult(
                ruleId = ruleId,
                ruleType = ruleType,
                isAllowed = true,
                confidence = 1.0f,
                reason = reason,
                actions = actions,
                metadata = metadata,
                executionTimeMs = executionTimeMs
            )
        }

        /**
         * Create a failed rule result
         */
        fun failure(
            ruleId: String,
            ruleType: RuleType,
            reason: String,
            actions: List<RuleAction> = emptyList(),
            metadata: Map<String, Any> = emptyMap(),
            executionTimeMs: Long = 0L,
            warnings: List<String> = emptyList(),
            errors: List<String> = emptyList()
        ): RuleResult {
            return RuleResult(
                ruleId = ruleId,
                ruleType = ruleType,
                isAllowed = false,
                confidence = 1.0f,
                reason = reason,
                actions = actions,
                metadata = metadata,
                executionTimeMs = executionTimeMs,
                warnings = warnings,
                errors = errors
            )
        }

        /**
         * Create a rule result with low confidence
         */
        fun uncertain(
            ruleId: String,
            ruleType: RuleType,
            confidence: Float,
            reason: String = "Rule evaluation had low confidence",
            actions: List<RuleAction> = emptyList(),
            metadata: Map<String, Any> = emptyMap(),
            executionTimeMs: Long = 0L
        ): RuleResult {
            return RuleResult(
                ruleId = ruleId,
                ruleType = ruleType,
                isAllowed = confidence >= 0.5f,
                confidence = confidence,
                reason = reason,
                actions = actions,
                metadata = metadata,
                executionTimeMs = executionTimeMs
            )
        }
    }

    /**
     * Check if the rule result has any actions to execute
     */
    fun hasActions(): Boolean = actions.isNotEmpty()

    /**
     * Get actions of a specific type
     */
    inline fun <reified T : RuleAction> getActionsOfType(): List<T> {
        return actions.filterIsInstance<T>()
    }

    /**
     * Check if the rule result has any warnings
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Check if the rule result has any errors
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Get the highest priority action
     */
    fun getHighestPriorityAction(): RuleAction? {
        return actions.maxByOrNull { it.priority }
    }

    /**
     * Merge multiple rule results into a single result
     */
    fun mergeWith(other: RuleResult): RuleResult {
        return RuleResult(
            ruleId = "${this.ruleId}_${other.ruleId}",
            ruleType = this.ruleType,
            isAllowed = this.isAllowed && other.isAllowed,
            confidence = (this.confidence + other.confidence) / 2,
            reason = "${this.reason}; ${other.reason}",
            actions = this.actions + other.actions,
            metadata = this.metadata + other.metadata,
            executionTimeMs = this.executionTimeMs + other.executionTimeMs,
            warnings = this.warnings + other.warnings,
            errors = this.errors + other.errors
        )
    }
}