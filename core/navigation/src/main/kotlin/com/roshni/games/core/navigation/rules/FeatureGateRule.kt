package com.roshni.games.core.navigation.rules

import com.roshni.games.core.navigation.model.NavigationContext

/**
 * Rule that controls navigation based on feature flags and feature availability
 */
class FeatureGateRule(
    override val id: String = "feature_gate_rule_${System.currentTimeMillis()}",
    override val name: String = "Feature Gate Navigation Rule",
    override val description: String = "Controls navigation based on feature availability and flags",
    override val priority: Int = 80,
    override val categories: Set<RuleCategory> = setOf(RuleCategory.FEATURE_GATE, RuleCategory.BUSINESS_LOGIC),
    override val config: RuleConfig = RuleConfig()
) : BaseNavigationRule(id, name, description, priority, categories, config = config) {

    private val featureGates = mutableMapOf<String, FeatureGate>()

    /**
     * Add a feature gate to this rule
     */
    fun addFeatureGate(featureFlag: String, gate: FeatureGate) {
        featureGates[featureFlag] = gate
    }

    /**
     * Remove a feature gate
     */
    fun removeFeatureGate(featureFlag: String) {
        featureGates.remove(featureFlag)
    }

    /**
     * Get all feature gates
     */
    fun getFeatureGates(): Map<String, FeatureGate> = featureGates.toMap()

    /**
     * Check if feature gate exists
     */
    fun hasFeatureGate(featureFlag: String): Boolean = featureGates.containsKey(featureFlag)

    override suspend fun performEvaluation(context: NavigationContext): RuleEvaluationResult {
        if (featureGates.isEmpty()) {
            return RuleEvaluationResult(
                passed = true,
                blocked = false,
                reason = "No feature gates defined",
                executionTimeMs = 0,
                metadata = mapOf("feature_gates_count" to 0)
            )
        }

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<FeatureGateResult>()
        var allGatesPassed = true
        var lastFailedGate: FeatureGate? = null

        for ((featureFlag, gate) in featureGates) {
            val isFeatureEnabled = context.isFeatureEnabled(featureFlag)
            val gateResult = gate.evaluate(isFeatureEnabled, context)
            results.add(gateResult)

            if (!gateResult.passed) {
                allGatesPassed = false
                lastFailedGate = gate
                break // Stop at first failed gate
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        return RuleEvaluationResult(
            passed = allGatesPassed,
            blocked = !allGatesPassed,
            reason = if (allGatesPassed) {
                "All feature gates passed"
            } else {
                "Feature gate blocked: ${lastFailedGate?.description ?: "Unknown gate"}"
            },
            executionTimeMs = executionTime,
            metadata = mapOf(
                "feature_gates_evaluated" to featureGates.size,
                "feature_gates_passed" to results.count { it.passed },
                "feature_gates_failed" to results.count { !it.passed },
                "feature_gate_results" to results.map { mapOf(
                    "feature_flag" to it.featureFlag,
                    "passed" to it.passed,
                    "reason" to it.reason,
                    "gate_type" to it.gate::class.simpleName
                )}
            )
        )
    }

    override suspend fun isRuleApplicable(context: NavigationContext): Boolean {
        // This rule is applicable if there are feature gates and target destination is not empty
        return featureGates.isNotEmpty() && context.targetDestination.isNotEmpty()
    }

    override suspend fun getRuleAlternatives(context: NavigationContext): List<String> {
        return featureGates.values
            .filter { !it.evaluate(context.isFeatureEnabled(it.featureFlag), context).passed }
            .flatMap { it.getAlternativeDestinations(context) }
            .distinct()
    }

    override fun validateRuleConfig(): RuleValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (featureGates.isEmpty()) {
            warnings.add("No feature gates defined")
        }

        // Validate each feature gate
        featureGates.forEach { (featureFlag, gate) ->
            when (val validation = gate.validate()) {
                is FeatureGateValidationResult.Invalid -> {
                    errors.add("Invalid feature gate for '$featureFlag': ${validation.errors}")
                }
                is FeatureGateValidationResult.ValidWithWarnings -> {
                    warnings.addAll(validation.warnings.map { "Feature gate '$featureFlag': $it" })
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
 * Represents a feature gate that controls access based on feature flags
 */
interface FeatureGate {

    /**
     * The feature flag this gate controls
     */
    val featureFlag: String

    /**
     * Description of this feature gate
     */
    val description: String

    /**
     * Evaluate the feature gate
     *
     * @param isFeatureEnabled Whether the feature flag is currently enabled
     * @param context Navigation context for additional evaluation
     * @return Gate evaluation result
     */
    suspend fun evaluate(isFeatureEnabled: Boolean, context: NavigationContext): FeatureGateResult

    /**
     * Get alternative destinations if this gate blocks access
     */
    suspend fun getAlternativeDestinations(context: NavigationContext): List<String>

    /**
     * Validate this feature gate configuration
     */
    fun validate(): FeatureGateValidationResult
}

/**
 * Result of feature gate evaluation
 */
data class FeatureGateResult(
    val featureFlag: String,
    val gate: FeatureGate,
    val passed: Boolean,
    val reason: String,
    val executionTimeMs: Long = 0,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Validation result for feature gates
 */
sealed class FeatureGateValidationResult {
    object Valid : FeatureGateValidationResult()
    data class ValidWithWarnings(val warnings: List<String>) : FeatureGateValidationResult()
    data class Invalid(val errors: List<String>) : FeatureGateValidationResult()
}

/**
 * Simple feature gate that allows access when feature is enabled
 */
data class SimpleFeatureGate(
    override val featureFlag: String,
    override val description: String = "Simple feature gate for $featureFlag",
    val alternativeDestination: String? = null
) : FeatureGate {

    override suspend fun evaluate(isFeatureEnabled: Boolean, context: NavigationContext): FeatureGateResult {
        val startTime = System.currentTimeMillis()

        return FeatureGateResult(
            featureFlag = featureFlag,
            gate = this,
            passed = isFeatureEnabled,
            reason = if (isFeatureEnabled) {
                "Feature is enabled"
            } else {
                "Feature is disabled"
            },
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf("feature_enabled" to isFeatureEnabled)
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): FeatureGateValidationResult {
        val errors = mutableListOf<String>()

        if (featureFlag.isBlank()) {
            errors.add("Feature flag cannot be blank")
        }

        return if (errors.isEmpty()) {
            FeatureGateValidationResult.Valid
        } else {
            FeatureGateValidationResult.Invalid(errors)
        }
    }
}

/**
 * Feature gate that allows access when feature is disabled (inverse logic)
 */
data class InverseFeatureGate(
    override val featureFlag: String,
    override val description: String = "Inverse feature gate for $featureFlag",
    val alternativeDestination: String? = null
) : FeatureGate {

    override suspend fun evaluate(isFeatureEnabled: Boolean, context: NavigationContext): FeatureGateResult {
        val startTime = System.currentTimeMillis()

        return FeatureGateResult(
            featureFlag = featureFlag,
            gate = this,
            passed = !isFeatureEnabled,
            reason = if (!isFeatureEnabled) {
                "Feature is disabled (required for access)"
            } else {
                "Feature is enabled (blocks access)"
            },
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf("feature_enabled" to isFeatureEnabled)
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): FeatureGateValidationResult {
        val errors = mutableListOf<String>()

        if (featureFlag.isBlank()) {
            errors.add("Feature flag cannot be blank")
        }

        return if (errors.isEmpty()) {
            FeatureGateValidationResult.Valid
        } else {
            FeatureGateValidationResult.Invalid(errors)
        }
    }
}

/**
 * Feature gate that allows access based on percentage rollout
 */
data class PercentageFeatureGate(
    override val featureFlag: String,
    override val description: String = "Percentage-based feature gate for $featureFlag",
    val percentage: Double,
    val userIdBased: Boolean = true,
    val alternativeDestination: String? = null
) : FeatureGate {

    override suspend fun evaluate(isFeatureEnabled: Boolean, context: NavigationContext): FeatureGateResult {
        val startTime = System.currentTimeMillis()

        // If feature is not enabled at all, check percentage
        if (!isFeatureEnabled) {
            val userHash = if (userIdBased) {
                context.userId?.hashCode() ?: 0
            } else {
                context.sessionId?.hashCode() ?: System.currentTimeMillis().hashCode()
            }

            val percentageAllowed = (userHash % 100).toDouble() / 100.0
            val passed = percentageAllowed < (percentage / 100.0)

            return FeatureGateResult(
                featureFlag = featureFlag,
                gate = this,
                passed = passed,
                reason = if (passed) {
                    "User in ${percentage}% rollout"
                } else {
                    "User not in ${percentage}% rollout"
                },
                executionTimeMs = System.currentTimeMillis() - startTime,
                metadata = mapOf(
                    "feature_enabled" to isFeatureEnabled,
                    "rollout_percentage" to percentage,
                    "user_hash" to userHash,
                    "percentage_allowed" to percentageAllowed
                )
            )
        }

        // Feature is enabled, allow access
        return FeatureGateResult(
            featureFlag = featureFlag,
            gate = this,
            passed = true,
            reason = "Feature is enabled",
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf("feature_enabled" to isFeatureEnabled)
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): FeatureGateValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (featureFlag.isBlank()) {
            errors.add("Feature flag cannot be blank")
        }

        if (percentage < 0 || percentage > 100) {
            errors.add("Percentage must be between 0 and 100")
        }

        if (percentage < 50) {
            warnings.add("Low rollout percentage ($percentage%) may limit feature access")
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                FeatureGateValidationResult.Valid
            } else {
                FeatureGateValidationResult.ValidWithWarnings(warnings)
            }
        } else {
            FeatureGateValidationResult.Invalid(errors)
        }
    }
}

/**
 * Feature gate that allows access based on user groups or segments
 */
data class UserSegmentFeatureGate(
    override val featureFlag: String,
    override val description: String = "User segment feature gate for $featureFlag",
    val allowedSegments: Set<String>,
    val userSegmentProvider: (NavigationContext) -> String?,
    val alternativeDestination: String? = null
) : FeatureGate {

    override suspend fun evaluate(isFeatureEnabled: Boolean, context: NavigationContext): FeatureGateResult {
        val startTime = System.currentTimeMillis()

        if (!isFeatureEnabled) {
            return FeatureGateResult(
                featureFlag = featureFlag,
                gate = this,
                passed = false,
                reason = "Feature is disabled",
                executionTimeMs = System.currentTimeMillis() - startTime,
                metadata = mapOf("feature_enabled" to false)
            )
        }

        val userSegment = userSegmentProvider(context)
        val passed = userSegment != null && allowedSegments.contains(userSegment)

        return FeatureGateResult(
            featureFlag = featureFlag,
            gate = this,
            passed = passed,
            reason = if (passed) {
                "User in allowed segment: $userSegment"
            } else {
                "User not in allowed segments. User segment: $userSegment, Allowed: $allowedSegments"
            },
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "feature_enabled" to isFeatureEnabled,
                "user_segment" to (userSegment ?: "unknown"),
                "allowed_segments" to allowedSegments
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): FeatureGateValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (featureFlag.isBlank()) {
            errors.add("Feature flag cannot be blank")
        }

        if (allowedSegments.isEmpty()) {
            errors.add("Allowed segments cannot be empty")
        }

        if (allowedSegments.any { it.isBlank() }) {
            errors.add("Allowed segments cannot contain blank entries")
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                FeatureGateValidationResult.Valid
            } else {
                FeatureGateValidationResult.ValidWithWarnings(warnings)
            }
        } else {
            FeatureGateValidationResult.Invalid(errors)
        }
    }
}