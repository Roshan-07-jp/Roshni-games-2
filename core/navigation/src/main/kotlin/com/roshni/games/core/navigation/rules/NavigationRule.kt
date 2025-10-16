package com.roshni.games.core.navigation.rules

import com.roshni.games.core.navigation.model.NavigationContext
import com.roshni.games.core.navigation.model.NavigationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base interface for all navigation rules
 */
interface NavigationRule {

    /**
     * Unique identifier for this rule
     */
    val id: String

    /**
     * Human-readable name for this rule
     */
    val name: String

    /**
     * Description of what this rule does
     */
    val description: String

    /**
     * Priority of this rule (higher values execute first)
     */
    val priority: Int

    /**
     * Whether this rule is currently enabled
     */
    val isEnabled: StateFlow<Boolean>

    /**
     * Categories this rule belongs to
     */
    val categories: Set<RuleCategory>

    /**
     * Tags for organizing and filtering rules
     */
    val tags: Set<String>

    /**
     * Configuration parameters for this rule
     */
    val config: RuleConfig

    /**
     * Evaluate this rule against the given navigation context
     *
     * @param context The navigation context to evaluate against
     * @return Rule evaluation result
     */
    suspend fun evaluate(context: NavigationContext): RuleEvaluationResult

    /**
     * Check if this rule should be applied to the given context
     * This is called before evaluate() to determine if the rule is relevant
     *
     * @param context The navigation context
     * @return true if this rule should be evaluated, false otherwise
     */
    suspend fun shouldEvaluate(context: NavigationContext): Boolean

    /**
     * Get suggested alternative destinations if this rule blocks navigation
     *
     * @param context The navigation context
     * @return List of suggested alternative destinations, empty if no suggestions
     */
    suspend fun getAlternativeDestinations(context: NavigationContext): List<String>

    /**
     * Enable this rule
     */
    fun enable()

    /**
     * Disable this rule
     */
    fun disable()

    /**
     * Update rule configuration
     *
     * @param newConfig New configuration parameters
     */
    fun updateConfig(newConfig: RuleConfig)

    /**
     * Validate rule configuration
     *
     * @return Validation result
     */
    fun validateConfig(): RuleValidationResult

    /**
     * Reset rule to initial state
     */
    fun reset()

    /**
     * Get rule statistics
     */
    fun getStatistics(): RuleStatistics

    /**
     * Observe rule state changes
     */
    fun observeState(): Flow<RuleStateChange>
}

/**
 * Result of rule evaluation
 */
data class RuleEvaluationResult(
    /**
     * Whether the rule passed (allows navigation)
     */
    val passed: Boolean,

    /**
     * Whether navigation should be blocked
     */
    val blocked: Boolean = !passed,

    /**
     * Suggested alternative destination if navigation is blocked
     */
    val suggestedDestination: String? = null,

    /**
     * Reason for rule failure/success
     */
    val reason: String,

    /**
     * Additional metadata from rule evaluation
     */
    val metadata: Map<String, Any> = emptyMap(),

    /**
     * Execution time in milliseconds
     */
    val executionTimeMs: Long,

    /**
     * Timestamp when evaluation completed
     */
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Check if rule evaluation was successful
     */
    val isSuccessful: Boolean get() = passed

    /**
     * Check if navigation should be blocked
     */
    val shouldBlock: Boolean get() = blocked

    /**
     * Check if there's a suggested alternative
     */
    val hasAlternative: Boolean get() = suggestedDestination != null
}

/**
 * Configuration for navigation rules
 */
data class RuleConfig(
    val parameters: Map<String, Any> = emptyMap(),
    val timeoutMs: Long = 5000,
    val retryCount: Int = 3,
    val enabledByDefault: Boolean = true,
    val cacheResults: Boolean = true,
    val cacheDurationMs: Long = 30000
) {

    /**
     * Get parameter value by key
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getParameter(key: String): T? = parameters[key] as? T

    /**
     * Get parameter value with default
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getParameter(key: String, defaultValue: T): T = parameters[key] as? T ?: defaultValue

    /**
     * Check if parameter exists
     */
    fun hasParameter(key: String): Boolean = parameters.containsKey(key)
}

/**
 * Categories for organizing navigation rules
 */
enum class RuleCategory {
    SECURITY,
    PERMISSION,
    FEATURE_GATE,
    CONDITIONAL,
    PERFORMANCE,
    ACCESSIBILITY,
    PARENTAL_CONTROLS,
    DEVICE_COMPATIBILITY,
    NETWORK_DEPENDENT,
    USER_PREFERENCE,
    BUSINESS_LOGIC,
    VALIDATION
}

/**
 * Validation result for rule configuration
 */
data class RuleValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Statistics for rule execution
 */
data class RuleStatistics(
    val totalEvaluations: Long = 0,
    val passedEvaluations: Long = 0,
    val failedEvaluations: Long = 0,
    val blockedNavigations: Long = 0,
    val averageExecutionTimeMs: Double = 0.0,
    val lastExecutionTime: Long? = null,
    val totalExecutionTimeMs: Long = 0,
    val cacheHitCount: Long = 0,
    val cacheMissCount: Long = 0
) {

    /**
     * Success rate as percentage
     */
    val successRate: Double
        get() = if (totalEvaluations > 0) {
            (passedEvaluations.toDouble() / totalEvaluations) * 100
        } else 0.0

    /**
     * Block rate as percentage
     */
    val blockRate: Double
        get() = if (totalEvaluations > 0) {
            (blockedNavigations.toDouble() / totalEvaluations) * 100
        } else 0.0

    /**
     * Cache hit rate as percentage
     */
    val cacheHitRate: Double
        get() = if ((cacheHitCount + cacheMissCount) > 0) {
            (cacheHitCount.toDouble() / (cacheHitCount + cacheMissCount)) * 100
        } else 0.0
}

/**
 * State change notification for rules
 */
data class RuleStateChange(
    val ruleId: String,
    val oldState: Boolean,
    val newState: Boolean,
    val reason: String,
    val timestamp: Long
)

/**
 * Base class for navigation rule implementations
 */
abstract class BaseNavigationRule(
    override val id: String,
    override val name: String,
    override val description: String,
    override val priority: Int = 100,
    override val categories: Set<RuleCategory> = emptySet(),
    override val tags: Set<String> = emptySet(),
    override val config: RuleConfig = RuleConfig()
) : NavigationRule {

    private val _isEnabled = MutableStateFlow(config.enabledByDefault)
    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _stateChanges = MutableStateFlow<List<RuleStateChange>>(emptyList())
    val stateChanges: StateFlow<List<RuleStateChange>> = _stateChanges.asStateFlow()

    private var statistics = RuleStatistics()
    private var lastExecutionTime: Long = 0

    override suspend fun evaluate(context: NavigationContext): RuleEvaluationResult {
        if (!isEnabled.value) {
            return RuleEvaluationResult(
                passed = true,
                blocked = false,
                reason = "Rule is disabled",
                executionTimeMs = 0,
                metadata = mapOf("rule_disabled" to true)
            )
        }

        val startTime = System.currentTimeMillis()
        return try {
            val result = performEvaluation(context)
            lastExecutionTime = System.currentTimeMillis()

            // Update statistics
            statistics = statistics.copy(
                totalEvaluations = statistics.totalEvaluations + 1,
                passedEvaluations = statistics.passedEvaluations + (if (result.passed) 1 else 0),
                failedEvaluations = statistics.failedEvaluations + (if (result.passed) 0 else 1),
                blockedNavigations = statistics.blockedNavigations + (if (result.blocked) 1 else 0),
                lastExecutionTime = lastExecutionTime,
                totalExecutionTimeMs = statistics.totalExecutionTimeMs + result.executionTimeMs,
                averageExecutionTimeMs = calculateAverageExecutionTime(result.executionTimeMs)
            )

            result.copy(executionTimeMs = System.currentTimeMillis() - startTime)

        } catch (e: Exception) {
            lastExecutionTime = System.currentTimeMillis()
            statistics = statistics.copy(
                totalEvaluations = statistics.totalEvaluations + 1,
                failedEvaluations = statistics.failedEvaluations + 1,
                lastExecutionTime = lastExecutionTime
            )

            RuleEvaluationResult(
                passed = false,
                blocked = true,
                reason = "Rule evaluation failed: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime,
                metadata = mapOf("exception" to e.message.orEmpty())
            )
        }
    }

    override suspend fun shouldEvaluate(context: NavigationContext): Boolean {
        return isEnabled.value && isRuleApplicable(context)
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return getRuleAlternatives(context)
    }

    override fun enable() {
        val oldState = _isEnabled.value
        _isEnabled.value = true
        emitStateChange(oldState, true, "Rule manually enabled")
    }

    override fun disable() {
        val oldState = _isEnabled.value
        _isEnabled.value = false
        emitStateChange(oldState, false, "Rule manually disabled")
    }

    override fun updateConfig(newConfig: RuleConfig) {
        // Implementation would update configuration
        // For now, just store it
    }

    override fun validateConfig(): RuleValidationResult {
        return validateRuleConfig()
    }

    override fun reset() {
        statistics = RuleStatistics()
        lastExecutionTime = 0
        _stateChanges.value = emptyList()
    }

    override fun getStatistics(): RuleStatistics = statistics

    override fun observeState(): Flow<RuleStateChange> {
        // This would be implemented with proper flow observation
        return kotlinx.coroutines.flow.flow { }
    }

    /**
     * Perform the actual rule evaluation - to be implemented by subclasses
     */
    protected abstract suspend fun performEvaluation(context: NavigationContext): RuleEvaluationResult

    /**
     * Check if this rule is applicable to the given context - to be implemented by subclasses
     */
    protected abstract suspend fun isRuleApplicable(context: NavigationContext): Boolean

    /**
     * Get alternative destinations for this rule - to be implemented by subclasses
     */
    protected abstract suspend fun getRuleAlternatives(context: NavigationContext): List<String>

    /**
     * Validate rule-specific configuration - to be implemented by subclasses
     */
    protected abstract fun validateRuleConfig(): RuleValidationResult

    /**
     * Emit state change notification
     */
    private fun emitStateChange(oldState: Boolean, newState: Boolean, reason: String) {
        val change = RuleStateChange(
            ruleId = id,
            oldState = oldState,
            newState = newState,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        _stateChanges.value = _stateChanges.value + change
    }

    /**
     * Calculate average execution time
     */
    private fun calculateAverageExecutionTime(newExecutionTime: Long): Double {
        val totalTime = statistics.totalExecutionTimeMs + newExecutionTime
        return totalTime.toDouble() / statistics.totalEvaluations
    }
}