package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Base interface for all business rules in the App Rules Engine
 */
interface BusinessRule {

    /**
     * Unique identifier for this business rule
     */
    val id: String

    /**
     * Human-readable name for this rule
     */
    val name: String

    /**
     * Detailed description of what this rule does
     */
    val description: String

    /**
     * The category this rule belongs to
     */
    val category: RuleCategory

    /**
     * The type of business rule
     */
    val ruleType: BusinessRuleType

    /**
     * Whether this rule is currently enabled
     */
    val enabled: Boolean

    /**
     * Priority level for rule execution (higher values = higher priority)
     */
    val priority: Int

    /**
     * Tags for organizing and filtering rules
     */
    val tags: List<String>

    /**
     * Creation timestamp
     */
    val createdAt: Long

    /**
     * Last modification timestamp
     */
    val modifiedAt: Long

    /**
     * Version number for tracking rule changes
     */
    val version: Int

    /**
     * Evaluate this business rule against the provided context
     *
     * @param context The context to evaluate the rule against
     * @return The result of the business rule evaluation
     */
    suspend fun evaluate(context: RuleContext): BusinessRuleResult

    /**
     * Validate that this business rule is properly configured
     *
     * @return true if the rule is valid, false otherwise
     */
    suspend fun validate(): Boolean

    /**
     * Get metadata about this rule for debugging and monitoring
     */
    suspend fun getMetadata(): Map<String, Any>

    /**
     * Check if this rule applies to the given operation
     *
     * @param operation The operation to check
     * @return true if this rule applies to the operation, false otherwise
     */
    suspend fun appliesTo(operation: BusinessOperation): Boolean

    /**
     * Get the execution order for this rule within its priority level
     */
    fun getExecutionOrder(): Int
}

/**
 * Categories of business rules
 */
enum class RuleCategory(
    val displayName: String,
    val description: String,
    val priority: Int
) {
    GAMEPLAY("Gameplay", "Rules related to game mechanics and player progression", 1),
    MONETIZATION("Monetization", "Rules related to in-app purchases and revenue", 2),
    SOCIAL("Social", "Rules related to social features and interactions", 3),
    SECURITY("Security", "Rules related to security and access control", 4),
    COMPLIANCE("Compliance", "Rules related to legal and regulatory compliance", 5),
    PERFORMANCE("Performance", "Rules related to app performance and optimization", 6),
    ACCESSIBILITY("Accessibility", "Rules related to accessibility features", 7),
    CUSTOM("Custom", "Custom business rules", 8)
}

/**
 * Types of business rules
 */
enum class BusinessRuleType {
    VALIDATION,     // Rules that validate operations
    ENFORCEMENT,    // Rules that enforce specific behaviors
    AUTHORIZATION,  // Rules that control access permissions
    AUTOMATION,     // Rules that trigger automated actions
    MONITORING,     // Rules that monitor and report on activities
    CUSTOM          // Custom rule types
}

/**
 * Types of business operations that rules can apply to
 */
enum class BusinessOperationType {
    // Gameplay Operations
    GAME_START,
    GAME_END,
    LEVEL_COMPLETE,
    ACHIEVEMENT_UNLOCK,
    PROGRESS_SAVE,
    GAME_STATE_CHANGE,

    // Monetization Operations
    PURCHASE_INITIATE,
    PURCHASE_COMPLETE,
    SUBSCRIPTION_CHANGE,
    REWARD_CLAIM,
    CURRENCY_SPEND,
    AD_VIEW,

    // Social Operations
    FRIEND_ADD,
    MESSAGE_SEND,
    LEADERBOARD_UPDATE,
    SOCIAL_SHARE,
    PROFILE_UPDATE,
    GROUP_JOIN,

    // Security Operations
    AUTHENTICATION,
    AUTHORIZATION,
    DATA_ACCESS,
    FEATURE_ACCESS,
    CONTENT_ACCESS,

    // System Operations
    APP_START,
    APP_UPDATE,
    DATA_SYNC,
    ANALYTICS_LOG,
    ERROR_REPORT,

    // Custom Operations
    CUSTOM
}

/**
 * Business operation data class
 */
data class BusinessOperation(
    val operationType: BusinessOperationType,
    val operationId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Map<String, Any> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Business scenario for rule evaluation
 */
data class BusinessScenario(
    val scenarioId: String,
    val scenarioType: String,
    val triggerConditions: List<String>,
    val expectedOutcomes: List<String>,
    val contextData: Map<String, Any> = emptyMap()
)

/**
 * Result of a business rule evaluation
 */
data class BusinessRuleResult(
    val ruleId: String,
    val ruleType: BusinessRuleType,
    val isAllowed: Boolean,
    val confidence: Float = 1.0f,
    val reason: String = "",
    val actions: List<BusinessRuleAction> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
) {

    /**
     * Check if the rule result has any actions to execute
     */
    fun hasActions(): Boolean = actions.isNotEmpty()

    /**
     * Get actions of a specific type
     */
    inline fun <reified T : BusinessRuleAction> getActionsOfType(): List<T> {
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
    fun getHighestPriorityAction(): BusinessRuleAction? {
        return actions.maxByOrNull { it.priority }
    }

    companion object {
        fun success(
            ruleId: String,
            ruleType: BusinessRuleType,
            reason: String = "Business rule evaluation passed",
            actions: List<BusinessRuleAction> = emptyList(),
            metadata: Map<String, Any> = emptyMap(),
            executionTimeMs: Long = 0L
        ): BusinessRuleResult {
            return BusinessRuleResult(
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

        fun failure(
            ruleId: String,
            ruleType: BusinessRuleType,
            reason: String,
            actions: List<BusinessRuleAction> = emptyList(),
            metadata: Map<String, Any> = emptyMap(),
            executionTimeMs: Long = 0L,
            warnings: List<String> = emptyList(),
            errors: List<String> = emptyList()
        ): BusinessRuleResult {
            return BusinessRuleResult(
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
    }
}

/**
 * Actions that can be triggered by business rule evaluation
 */
sealed class BusinessRuleAction {

    /**
     * Unique identifier for this action
     */
    abstract val actionId: String

    /**
     * Priority level for action execution
     */
    abstract val priority: Int

    /**
     * Whether this action should be executed immediately
     */
    abstract val immediate: Boolean

    /**
     * Metadata associated with this action
     */
    abstract val metadata: Map<String, Any>

    /**
     * Execute this action with the provided context
     */
    abstract suspend fun execute(context: ExecutionContext): Boolean

    /**
     * Check if this action can be executed in the current context
     */
    abstract suspend fun canExecute(context: ExecutionContext): Boolean

    /**
     * Block or allow a business operation
     */
    data class OperationControl(
        override val actionId: String,
        val operationType: BusinessOperationType,
        val allowOperation: Boolean,
        val reason: String = "",
        override val priority: Int = 1,
        override val immediate: Boolean = true,
        override val metadata: Map<String, Any> = emptyMap()
    ) : BusinessRuleAction() {

        override suspend fun execute(context: ExecutionContext): Boolean {
            // Implementation would integrate with operation control system
            return true
        }

        override suspend fun canExecute(context: ExecutionContext): Boolean {
            return true
        }
    }

    /**
     * Modify business operation data
     */
    data class DataModification(
        override val actionId: String,
        val modifications: Map<String, Any>,
        val reason: String = "",
        override val priority: Int = 2,
        override val immediate: Boolean = true,
        override val metadata: Map<String, Any> = emptyMap()
    ) : BusinessRuleAction() {

        override suspend fun execute(context: ExecutionContext): Boolean {
            // Implementation would modify operation data
            return true
        }

        override suspend fun canExecute(context: ExecutionContext): Boolean {
            return true
        }
    }

    /**
     * Trigger notifications or alerts
     */
    data class Notification(
        override val actionId: String,
        val notificationType: NotificationType,
        val message: String,
        val recipients: List<String>,
        val urgency: UrgencyLevel = UrgencyLevel.NORMAL,
        override val priority: Int = 1,
        override val immediate: Boolean = true,
        override val metadata: Map<String, Any> = emptyMap()
    ) : BusinessRuleAction() {

        enum class NotificationType {
            INFO, WARNING, ERROR, SUCCESS, ALERT
        }

        enum class UrgencyLevel {
            LOW, NORMAL, HIGH, CRITICAL
        }

        override suspend fun execute(context: ExecutionContext): Boolean {
            // Implementation would send notifications
            return true
        }

        override suspend fun canExecute(context: ExecutionContext): Boolean {
            return recipients.isNotEmpty()
        }
    }

    /**
     * Log business events for analytics or auditing
     */
    data class AuditLog(
        override val actionId: String,
        val eventType: String,
        val eventData: Map<String, Any>,
        val logLevel: LogLevel = LogLevel.INFO,
        override val priority: Int = 0,
        override val immediate: Boolean = false,
        override val metadata: Map<String, Any> = emptyMap()
    ) : BusinessRuleAction() {

        enum class LogLevel {
            DEBUG, INFO, WARN, ERROR, CRITICAL
        }

        override suspend fun execute(context: ExecutionContext): Boolean {
            // Implementation would log to audit system
            return true
        }

        override suspend fun canExecute(context: ExecutionContext): Boolean {
            return true
        }
    }

    /**
     * Custom business rule action
     */
    data class CustomAction(
        override val actionId: String,
        val executor: suspend (ExecutionContext) -> Boolean,
        val canExecuteChecker: suspend (ExecutionContext) -> Boolean = { true },
        override val priority: Int = 1,
        override val immediate: Boolean = true,
        override val metadata: Map<String, Any> = emptyMap()
    ) : BusinessRuleAction() {

        override suspend fun execute(context: ExecutionContext): Boolean {
            return try {
                executor(context)
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: ExecutionContext): Boolean {
            return try {
                canExecuteChecker(context)
            } catch (e: Exception) {
                false
            }
        }
    }
}