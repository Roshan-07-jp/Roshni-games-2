package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Context information for business rule validation
 */
data class ValidationContext(
    val operation: BusinessOperation,
    val userContext: UserContext,
    val applicationContext: ApplicationContext,
    val environmentContext: EnvironmentContext,
    val validationScope: ValidationScope = ValidationScope.OPERATION,
    val validationLevel: ValidationLevel = ValidationLevel.STRICT,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Convert to RuleContext for compatibility with existing rule engine
     */
    fun toRuleContext(): RuleContext {
        return RuleContext(
            userId = userContext.userId,
            gameId = applicationContext.gameId,
            sessionId = userContext.sessionId,
            timestamp = timestamp,
            deviceInfo = environmentContext.deviceInfo,
            userProfile = userContext.userProfile,
            gameState = applicationContext.gameState,
            metadata = metadata + mapOf(
                "validationScope" to validationScope.name,
                "validationLevel" to validationLevel.name,
                "businessOperation" to operation.operationType.name
            )
        )
    }

    /**
     * Create a child context for nested validation
     */
    fun createChildContext(
        operation: BusinessOperation,
        additionalMetadata: Map<String, Any> = emptyMap()
    ): ValidationContext {
        return copy(
            operation = operation,
            timestamp = System.currentTimeMillis(),
            metadata = metadata + additionalMetadata
        )
    }
}

/**
 * User-specific context for validation
 */
data class UserContext(
    val userId: String,
    val sessionId: String? = null,
    val userProfile: com.roshni.games.core.utils.rules.RuleContext.UserProfile,
    val permissions: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val authenticationLevel: AuthenticationLevel = AuthenticationLevel.USER,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val userMetadata: Map<String, Any> = emptyMap()
) {

    enum class AuthenticationLevel {
        GUEST, USER, PREMIUM, ADMIN, SYSTEM
    }

    enum class AccountStatus {
        PENDING, ACTIVE, SUSPENDED, BANNED, DELETED
    }
}

/**
 * Application-specific context for validation
 */
data class ApplicationContext(
    val appId: String,
    val appVersion: String,
    val gameId: String? = null,
    val gameState: com.roshni.games.core.utils.rules.RuleContext.GameState,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val configuration: Map<String, Any> = emptyMap(),
    val appMetadata: Map<String, Any> = emptyMap()
)

/**
 * Environment-specific context for validation
 */
data class EnvironmentContext(
    val deviceInfo: com.roshni.games.core.utils.rules.RuleContext.DeviceInfo,
    val networkInfo: NetworkInfo = NetworkInfo(),
    val locationInfo: LocationInfo? = null,
    val timeInfo: TimeInfo = TimeInfo(),
    val environmentMetadata: Map<String, Any> = emptyMap()
) {

    data class NetworkInfo(
        val networkType: String = "unknown",
        val bandwidth: Long = 0,
        val latency: Long = 0,
        val isConnected: Boolean = true,
        val restrictions: List<String> = emptyList()
    )

    data class LocationInfo(
        val country: String? = null,
        val region: String? = null,
        val city: String? = null,
        val timezone: String = "UTC",
        val isRestrictedLocation: Boolean = false
    )

    data class TimeInfo(
        val currentTime: Long = System.currentTimeMillis(),
        val serverTime: Long = System.currentTimeMillis(),
        val timeDifference: Long = 0,
        val isBusinessHours: Boolean = true
    )
}

/**
 * Scope of validation to perform
 */
enum class ValidationScope {
    /**
     * Validate only the specific operation
     */
    OPERATION,

    /**
     * Validate the operation and immediate dependencies
     */
    DEPENDENCIES,

    /**
     * Validate the entire user session
     */
    SESSION,

    /**
     * Validate across all user data and history
     */
    GLOBAL
}

/**
 * Level of strictness for validation
 */
enum class ValidationLevel {
    /**
     * Most lenient validation - only critical rules
     */
    LENIENT,

    /**
     * Standard validation level
     */
    NORMAL,

    /**
     * Strict validation - all applicable rules
     */
    STRICT,

    /**
     * Maximum validation - includes experimental rules
     */
    MAXIMUM
}

/**
 * Result of business rule validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val operation: BusinessOperation,
    val context: ValidationContext,
    val ruleResults: List<BusinessRuleResult> = emptyList(),
    val summary: ValidationSummary = ValidationSummary(),
    val recommendations: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Get all rule results that passed
     */
    fun getPassedRules(): List<BusinessRuleResult> {
        return ruleResults.filter { it.isAllowed }
    }

    /**
     * Get all rule results that failed
     */
    fun getFailedRules(): List<BusinessRuleResult> {
        return ruleResults.filter { !it.isAllowed }
    }

    /**
     * Get rule results by category
     */
    fun getRulesByCategory(category: RuleCategory): List<BusinessRuleResult> {
        return ruleResults.filter { result ->
            // This would need access to the actual rule to get its category
            // For now, return empty list
            false
        }
    }

    /**
     * Get rule results by type
     */
    fun getRulesByType(ruleType: BusinessRuleType): List<BusinessRuleResult> {
        return ruleResults.filter { it.ruleType == ruleType }
    }

    /**
     * Check if validation has any warnings
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Check if validation has any errors
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Get the primary reason for validation failure
     */
    fun getPrimaryFailureReason(): String? {
        return getFailedRules().firstOrNull()?.reason
    }

    /**
     * Merge multiple validation results
     */
    fun mergeWith(other: ValidationResult): ValidationResult {
        return ValidationResult(
            isValid = this.isValid && other.isValid,
            operation = this.operation,
            context = this.context,
            ruleResults = this.ruleResults + other.ruleResults,
            summary = this.summary.mergeWith(other.summary),
            recommendations = this.recommendations + other.recommendations,
            warnings = this.warnings + other.warnings,
            errors = this.errors + other.errors,
            executionTimeMs = this.executionTimeMs + other.executionTimeMs,
            metadata = this.metadata + other.metadata
        )
    }
}

/**
 * Summary of validation results
 */
data class ValidationSummary(
    val totalRules: Int = 0,
    val passedRules: Int = 0,
    val failedRules: Int = 0,
    val skippedRules: Int = 0,
    val averageConfidence: Float = 0.0f,
    val averageExecutionTimeMs: Double = 0.0,
    val categoriesValidated: Set<RuleCategory> = emptySet(),
    val ruleTypesValidated: Set<BusinessRuleType> = emptySet()
) {

    val successRate: Float
        get() = if (totalRules > 0) passedRules.toFloat() / totalRules else 0.0f

    fun mergeWith(other: ValidationSummary): ValidationSummary {
        return ValidationSummary(
            totalRules = this.totalRules + other.totalRules,
            passedRules = this.passedRules + other.passedRules,
            failedRules = this.failedRules + other.failedRules,
            skippedRules = this.skippedRules + other.skippedRules,
            averageConfidence = (this.averageConfidence + other.averageConfidence) / 2,
            averageExecutionTimeMs = (this.averageExecutionTimeMs + other.averageExecutionTimeMs) / 2,
            categoriesValidated = this.categoriesValidated + other.categoriesValidated,
            ruleTypesValidated = this.ruleTypesValidated + other.ruleTypesValidated
        )
    }
}

/**
 * Context for business rule enforcement
 */
data class EnforcementContext(
    val operation: BusinessOperation,
    val validationResult: ValidationResult,
    val userContext: UserContext,
    val applicationContext: ApplicationContext,
    val environmentContext: EnvironmentContext,
    val enforcementMode: EnforcementMode = EnforcementMode.STANDARD,
    val rollbackOnFailure: Boolean = true,
    val notifyOnFailure: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Convert to RuleContext for compatibility with existing rule engine
     */
    fun toRuleContext(): RuleContext {
        return RuleContext(
            userId = userContext.userId,
            gameId = applicationContext.gameId,
            sessionId = userContext.sessionId,
            timestamp = timestamp,
            deviceInfo = environmentContext.deviceInfo,
            userProfile = userContext.userProfile,
            gameState = applicationContext.gameState,
            metadata = metadata + mapOf(
                "enforcementMode" to enforcementMode.name,
                "rollbackOnFailure" to rollbackOnFailure,
                "notifyOnFailure" to notifyOnFailure,
                "validationPassed" to validationResult.isValid
            )
        )
    }
}

/**
 * Mode for rule enforcement
 */
enum class EnforcementMode {
    /**
     * Standard enforcement - execute actions as normal
     */
    STANDARD,

    /**
     * Dry run - validate but don't execute actions
     */
    DRY_RUN,

    /**
     * Force execute - execute actions even if validation failed
     */
    FORCE_EXECUTE,

    /**
     * Preview mode - show what would be executed without doing it
     */
    PREVIEW
}

/**
 * Result of business rule enforcement
 */
data class EnforcementResult(
    val isSuccessful: Boolean,
    val operation: BusinessOperation,
    val context: EnforcementContext,
    val executedActions: List<ExecutedAction> = emptyList(),
    val failedActions: List<FailedAction> = emptyList(),
    val rollbackActions: List<ExecutedAction> = emptyList(),
    val notifications: List<NotificationResult> = emptyList(),
    val summary: EnforcementSummary = EnforcementSummary(),
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Check if enforcement completed without any failures
     */
    fun isCompletelySuccessful(): Boolean {
        return isSuccessful && failedActions.isEmpty()
    }

    /**
     * Check if any rollback was performed
     */
    fun hasRollbacks(): Boolean = rollbackActions.isNotEmpty()

    /**
     * Get all action results (both successful and failed)
     */
    fun getAllActionResults(): List<ActionResult> {
        return executedActions + failedActions
    }

    /**
     * Merge multiple enforcement results
     */
    fun mergeWith(other: EnforcementResult): EnforcementResult {
        return EnforcementResult(
            isSuccessful = this.isSuccessful && other.isSuccessful,
            operation = this.operation,
            context = this.context,
            executedActions = this.executedActions + other.executedActions,
            failedActions = this.failedActions + other.failedActions,
            rollbackActions = this.rollbackActions + other.rollbackActions,
            notifications = this.notifications + other.notifications,
            summary = this.summary.mergeWith(other.summary),
            executionTimeMs = this.executionTimeMs + other.executionTimeMs,
            metadata = this.metadata + other.metadata
        )
    }
}

/**
 * Summary of enforcement results
 */
data class EnforcementSummary(
    val totalActions: Int = 0,
    val executedActions: Int = 0,
    val failedActions: Int = 0,
    val rollbackActions: Int = 0,
    val notificationsSent: Int = 0,
    val averageExecutionTimeMs: Double = 0.0,
    val actionTypes: Set<String> = emptySet()
) {

    val successRate: Float
        get() = if (totalActions > 0) executedActions.toFloat() / totalActions else 0.0f

    fun mergeWith(other: EnforcementSummary): EnforcementSummary {
        return EnforcementSummary(
            totalActions = this.totalActions + other.totalActions,
            executedActions = this.executedActions + other.executedActions,
            failedActions = this.failedActions + other.failedActions,
            rollbackActions = this.rollbackActions + other.rollbackActions,
            notificationsSent = this.notificationsSent + other.notificationsSent,
            averageExecutionTimeMs = (this.averageExecutionTimeMs + other.averageExecutionTimeMs) / 2,
            actionTypes = this.actionTypes + other.actionTypes
        )
    }
}

/**
 * Result of executing a single action
 */
data class ExecutedAction(
    val actionId: String,
    val actionType: String,
    val executionTimeMs: Long,
    val result: Any? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of a failed action execution
 */
data class FailedAction(
    val actionId: String,
    val actionType: String,
    val error: String,
    val errorCode: String? = null,
    val canRetry: Boolean = false,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of sending a notification
 */
data class NotificationResult(
    val notificationId: String,
    val notificationType: String,
    val recipient: String,
    val sentSuccessfully: Boolean,
    val error: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Common interface for action results
 */
sealed class ActionResult {
    abstract val actionId: String
    abstract val timestamp: Long
}

/**
 * Context for business rule action execution
 */
data class ExecutionContext(
    val operation: BusinessOperation,
    val businessRuleResults: List<BusinessRuleResult>,
    val userContext: UserContext,
    val applicationContext: ApplicationContext,
    val environmentContext: EnvironmentContext,
    val executionMode: ExecutionMode = ExecutionMode.SYNCHRONOUS,
    val timeoutMs: Long = 30000,
    val retryCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {

    enum class ExecutionMode {
        SYNCHRONOUS, ASYNCHRONOUS, BATCH, IMMEDIATE
    }
}