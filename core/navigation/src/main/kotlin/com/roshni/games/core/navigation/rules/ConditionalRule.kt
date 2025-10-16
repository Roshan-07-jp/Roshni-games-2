package com.roshni.games.core.navigation.rules

import com.roshni.games.core.navigation.model.NavigationContext

/**
 * Rule that evaluates based on conditional logic and context parameters
 */
class ConditionalRule(
    override val id: String = "conditional_rule_${System.currentTimeMillis()}",
    override val name: String = "Conditional Navigation Rule",
    override val description: String = "Evaluates navigation based on conditional logic",
    override val priority: Int = 50,
    override val categories: Set<RuleCategory> = setOf(RuleCategory.CONDITIONAL, RuleCategory.BUSINESS_LOGIC),
    override val config: RuleConfig = RuleConfig()
) : BaseNavigationRule(id, name, description, priority, categories, config = config) {

    private val conditions = mutableListOf<NavigationCondition>()

    /**
     * Add a condition to this rule
     */
    fun addCondition(condition: NavigationCondition) {
        conditions.add(condition)
    }

    /**
     * Remove a condition from this rule
     */
    fun removeCondition(condition: NavigationCondition) {
        conditions.remove(condition)
    }

    /**
     * Clear all conditions
     */
    fun clearConditions() {
        conditions.clear()
    }

    /**
     * Get all conditions
     */
    fun getConditions(): List<NavigationCondition> = conditions.toList()

    override suspend fun performEvaluation(context: NavigationContext): RuleEvaluationResult {
        if (conditions.isEmpty()) {
            return RuleEvaluationResult(
                passed = true,
                blocked = false,
                reason = "No conditions defined",
                executionTimeMs = 0,
                metadata = mapOf("conditions_count" to 0)
            )
        }

        val startTime = System.currentTimeMillis()
        var allConditionsPassed = true
        var lastFailedCondition: NavigationCondition? = null
        val results = mutableListOf<ConditionResult>()

        for (condition in conditions) {
            val conditionResult = condition.evaluate(context)
            results.add(conditionResult)

            if (!conditionResult.passed) {
                allConditionsPassed = false
                lastFailedCondition = condition
                break // Stop at first failure if not explicitly configured otherwise
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        return RuleEvaluationResult(
            passed = allConditionsPassed,
            blocked = !allConditionsPassed,
            reason = if (allConditionsPassed) {
                "All conditions passed"
            } else {
                "Condition failed: ${lastFailedCondition?.description ?: "Unknown condition"}"
            },
            executionTimeMs = executionTime,
            metadata = mapOf(
                "conditions_evaluated" to conditions.size,
                "conditions_passed" to results.count { it.passed },
                "conditions_failed" to results.count { !it.passed },
                "condition_results" to results.map { mapOf(
                    "condition" to it.condition.description,
                    "passed" to it.passed,
                    "reason" to it.reason
                )}
            )
        )
    }

    override suspend fun isRuleApplicable(context: NavigationContext): Boolean {
        // This rule is applicable if it has conditions and the target destination is not empty
        return conditions.isNotEmpty() && context.targetDestination.isNotEmpty()
    }

    override suspend fun getRuleAlternatives(context: NavigationContext): List<String> {
        // Return alternatives based on failed conditions
        return conditions
            .filter { !it.evaluate(context).passed }
            .flatMap { it.getAlternativeDestinations(context) }
            .distinct()
    }

    override fun validateRuleConfig(): RuleValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (conditions.isEmpty()) {
            warnings.add("No conditions defined for conditional rule")
        }

        // Validate each condition
        conditions.forEach { condition ->
            when (val validation = condition.validate()) {
                is ConditionValidationResult.Invalid -> {
                    errors.add("Invalid condition '${condition.description}': ${validation.errors}")
                }
                is ConditionValidationResult.ValidWithWarnings -> {
                    warnings.addAll(validation.warnings.map { "Condition '${condition.description}': $it" })
                }
                else -> { /* Valid */ }
            }
        }

        return RuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}

/**
 * A single navigation condition that can be evaluated
 */
interface NavigationCondition {

    /**
     * Description of this condition
     */
    val description: String

    /**
     * Evaluate this condition against the navigation context
     */
    suspend fun evaluate(context: NavigationContext): ConditionResult

    /**
     * Get alternative destinations if this condition fails
     */
    suspend fun getAlternativeDestinations(context: NavigationContext): List<String>

    /**
     * Validate this condition
     */
    fun validate(): ConditionValidationResult
}

/**
 * Result of condition evaluation
 */
data class ConditionResult(
    val condition: NavigationCondition,
    val passed: Boolean,
    val reason: String,
    val executionTimeMs: Long = 0,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Validation result for conditions
 */
sealed class ConditionValidationResult {
    object Valid : ConditionValidationResult()
    data class ValidWithWarnings(val warnings: List<String>) : ConditionValidationResult()
    data class Invalid(val errors: List<String>) : ConditionValidationResult()
}

/**
 * Condition that checks if user has specific permission
 */
data class PermissionCondition(
    override val description: String = "Check user permission",
    val requiredPermission: String,
    val alternativeDestination: String? = null
) : NavigationCondition {

    override suspend fun evaluate(context: NavigationContext): ConditionResult {
        val startTime = System.currentTimeMillis()
        val hasPermission = context.hasPermission(requiredPermission)
        val executionTime = System.currentTimeMillis() - startTime

        return ConditionResult(
            condition = this,
            passed = hasPermission,
            reason = if (hasPermission) {
                "User has required permission: $requiredPermission"
            } else {
                "User missing required permission: $requiredPermission"
            },
            executionTimeMs = executionTime,
            metadata = mapOf("required_permission" to requiredPermission)
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): ConditionValidationResult {
        val errors = mutableListOf<String>()

        if (requiredPermission.isBlank()) {
            errors.add("Required permission cannot be blank")
        }

        return if (errors.isEmpty()) {
            ConditionValidationResult.Valid
        } else {
            ConditionValidationResult.Invalid(errors)
        }
    }
}

/**
 * Condition that checks if feature flag is enabled
 */
data class FeatureFlagCondition(
    override val description: String = "Check feature flag",
    val featureFlag: String,
    val alternativeDestination: String? = null
) : NavigationCondition {

    override suspend fun evaluate(context: NavigationContext): ConditionResult {
        val startTime = System.currentTimeMillis()
        val isEnabled = context.isFeatureEnabled(featureFlag)
        val executionTime = System.currentTimeMillis() - startTime

        return ConditionResult(
            condition = this,
            passed = isEnabled,
            reason = if (isEnabled) {
                "Feature flag is enabled: $featureFlag"
            } else {
                "Feature flag is disabled: $featureFlag"
            },
            executionTimeMs = executionTime,
            metadata = mapOf("feature_flag" to featureFlag)
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): ConditionValidationResult {
        val errors = mutableListOf<String>()

        if (featureFlag.isBlank()) {
            errors.add("Feature flag cannot be blank")
        }

        return if (errors.isEmpty()) {
            ConditionValidationResult.Valid
        } else {
            ConditionValidationResult.Invalid(errors)
        }
    }
}

/**
 * Condition that checks device context
 */
data class DeviceCondition(
    override val description: String = "Check device context",
    val minScreenWidth: Int? = null,
    val minScreenHeight: Int? = null,
    val requiresTablet: Boolean? = null,
    val requiresLandscape: Boolean? = null,
    val maxMemoryUsage: Long? = null,
    val alternativeDestination: String? = null
) : NavigationCondition {

    override suspend fun evaluate(context: NavigationContext): ConditionResult {
        val startTime = System.currentTimeMillis()
        val device = context.deviceContext

        var passed = true
        val reasons = mutableListOf<String>()

        minScreenWidth?.let {
            if (device.screenWidth < it) {
                passed = false
                reasons.add("Screen width ${device.screenWidth} < required $it")
            }
        }

        minScreenHeight?.let {
            if (device.screenHeight < it) {
                passed = false
                reasons.add("Screen height ${device.screenHeight} < required $it")
            }
        }

        requiresTablet?.let {
            if (device.isTablet != it) {
                passed = false
                reasons.add("Tablet requirement not met: required $it, actual ${device.isTablet}")
            }
        }

        requiresLandscape?.let {
            if (device.isLandscape != it) {
                passed = false
                reasons.add("Orientation requirement not met: required $it, actual ${device.isLandscape}")
            }
        }

        maxMemoryUsage?.let {
            if (context.appState.memoryUsage > it) {
                passed = false
                reasons.add("Memory usage ${context.appState.memoryUsage} > max $it")
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        return ConditionResult(
            condition = this,
            passed = passed,
            reason = if (passed) {
                "All device conditions met"
            } else {
                "Device conditions not met: ${reasons.joinToString(", ")}"
            },
            executionTimeMs = executionTime,
            metadata = mapOf(
                "screen_width" to device.screenWidth,
                "screen_height" to device.screenHeight,
                "is_tablet" to device.isTablet,
                "is_landscape" to device.isLandscape,
                "memory_usage" to context.appState.memoryUsage
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): ConditionValidationResult {
        val warnings = mutableListOf<String>()

        if (minScreenWidth == null && minScreenHeight == null &&
            requiresTablet == null && requiresLandscape == null && maxMemoryUsage == null) {
            warnings.add("No device conditions specified")
        }

        return if (warnings.isEmpty()) {
            ConditionValidationResult.Valid
        } else {
            ConditionValidationResult.ValidWithWarnings(warnings)
        }
    }
}

/**
 * Condition that checks application state
 */
data class AppStateCondition(
    override val description: String = "Check application state",
    val requiresNetwork: Boolean? = null,
    val requiresAuthentication: Boolean? = null,
    val maxBatteryLevel: Int? = null,
    val minBatteryLevel: Int? = null,
    val alternativeDestination: String? = null
) : NavigationCondition {

    override suspend fun evaluate(context: NavigationContext): ConditionResult {
        val startTime = System.currentTimeMillis()
        val appState = context.appState

        var passed = true
        val reasons = mutableListOf<String>()

        requiresNetwork?.let {
            if (appState.isNetworkAvailable != it) {
                passed = false
                reasons.add("Network requirement not met: required $it, actual ${appState.isNetworkAvailable}")
            }
        }

        requiresAuthentication?.let {
            if (appState.isUserAuthenticated != it) {
                passed = false
                reasons.add("Authentication requirement not met: required $it, actual ${appState.isUserAuthenticated}")
            }
        }

        minBatteryLevel?.let {
            if (appState.batteryLevel < it) {
                passed = false
                reasons.add("Battery level ${appState.batteryLevel}% < required $it%")
            }
        }

        maxBatteryLevel?.let {
            if (appState.batteryLevel > it) {
                passed = false
                reasons.add("Battery level ${appState.batteryLevel}% > max $it%")
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        return ConditionResult(
            condition = this,
            passed = passed,
            reason = if (passed) {
                "All app state conditions met"
            } else {
                "App state conditions not met: ${reasons.joinToString(", ")}"
            },
            executionTimeMs = executionTime,
            metadata = mapOf(
                "network_available" to appState.isNetworkAvailable,
                "user_authenticated" to appState.isUserAuthenticated,
                "battery_level" to appState.batteryLevel,
                "foreground" to appState.isAppInForeground
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): ConditionValidationResult {
        val warnings = mutableListOf<String>()

        if (requiresNetwork == null && requiresAuthentication == null &&
            minBatteryLevel == null && maxBatteryLevel == null) {
            warnings.add("No app state conditions specified")
        }

        return if (warnings.isEmpty()) {
            ConditionValidationResult.Valid
        } else {
            ConditionValidationResult.ValidWithWarnings(warnings)
        }
    }
}