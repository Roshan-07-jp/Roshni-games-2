package com.roshni.games.core.utils.optimization

import com.roshni.games.core.utils.optimization.BatteryOptimizer.OptimizationMode

/**
 * Result of an optimization operation
 */
data class OptimizationResult(
    val strategyId: String,
    val success: Boolean,
    val message: String,
    val timestamp: Long,
    val executionTime: Long = 0,
    val optimizationsApplied: List<AppliedOptimization> = emptyList(),
    val metricsBefore: OptimizationMetrics = OptimizationMetrics(),
    val metricsAfter: OptimizationMetrics = OptimizationMetrics(),
    val error: String? = null
) {

    /**
     * Get improvement metrics
     */
    fun getImprovement(): OptimizationImprovement {
        return OptimizationImprovement(
            memoryImprovement = metricsAfter.memoryUsage - metricsBefore.memoryUsage,
            batteryImprovement = metricsAfter.batteryDrain - metricsBefore.batteryDrain,
            performanceImprovement = metricsAfter.performanceScore - metricsBefore.performanceScore,
            networkImprovement = metricsAfter.networkUsage - metricsBefore.networkUsage
        )
    }

    /**
     * Check if this optimization was effective
     */
    fun isEffective(): Boolean {
        val improvement = getImprovement()
        return improvement.performanceImprovement > 0 ||
               improvement.memoryImprovement < 0 || // Lower memory usage is better
               improvement.batteryImprovement < 0 || // Lower battery drain is better
               improvement.networkImprovement < 0   // Lower network usage is better
    }
}

/**
 * Applied optimization details
 */
data class AppliedOptimization(
    val type: OptimizationType,
    val description: String,
    val impact: OptimizationImpact,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * Optimization types
 */
enum class OptimizationType {
    MEMORY_CLEANUP,
    BATTERY_SAVING,
    NETWORK_THROTTLING,
    CPU_THROTTLING,
    CACHE_CLEARING,
    BACKGROUND_LIMITATION,
    QUALITY_REDUCTION,
    FRAME_RATE_LIMITING
}

/**
 * Impact of an optimization
 */
data class OptimizationImpact(
    val memoryReduction: Long = 0, // Bytes saved
    val batterySavings: Float = 0f, // Percentage saved per hour
    val performanceCost: Int = 0, // Performance score reduction (0-100)
    val networkSavings: Long = 0 // Bytes saved per minute
)

/**
 * Optimization metrics before and after
 */
data class OptimizationMetrics(
    val memoryUsage: Long = 0,
    val batteryDrain: Float = 0f,
    val performanceScore: Int = 100,
    val networkUsage: Long = 0,
    val cpuUsage: Float = 0f,
    val frameRate: Float = 60f,
    val temperature: Float = 0f
)

/**
 * Improvement metrics from optimization
 */
data class OptimizationImprovement(
    val memoryImprovement: Long = 0, // Negative means reduction (good)
    val batteryImprovement: Float = 0f, // Negative means reduction (good)
    val performanceImprovement: Int = 0, // Positive means improvement (good)
    val networkImprovement: Long = 0 // Negative means reduction (good)
)

/**
 * Result of adaptation to resource constraints
 */
data class AdaptationResult(
    val originalConstraints: ResourceConstraints,
    val newConstraints: ResourceConstraints,
    val adaptationActions: List<AdaptationAction>,
    val timestamp: Long,
    val adaptationTime: Long = 0,
    val success: Boolean = true,
    val error: String? = null
) {

    /**
     * Check if adaptation was necessary
     */
    fun isAdaptationNeeded(): Boolean {
        return originalConstraints != newConstraints
    }

    /**
     * Get adaptation severity
     */
    fun getAdaptationSeverity(): AdaptationSeverity {
        val constraintChanges = countConstraintChanges()
        return when {
            constraintChanges >= 3 -> AdaptationSeverity.MAJOR
            constraintChanges >= 2 -> AdaptationSeverity.MODERATE
            constraintChanges >= 1 -> AdaptationSeverity.MINOR
            else -> AdaptationSeverity.NONE
        }
    }

    /**
     * Count how many constraints changed
     */
    private fun countConstraintChanges(): Int {
        var changes = 0
        if (originalConstraints.maxMemoryUsage != newConstraints.maxMemoryUsage) changes++
        if (originalConstraints.maxBatteryDrain != newConstraints.maxBatteryDrain) changes++
        if (originalConstraints.maxNetworkUsage != newConstraints.maxNetworkUsage) changes++
        if (originalConstraints.maxCpuUsage != newConstraints.maxCpuUsage) changes++
        if (originalConstraints.maxTemperature != newConstraints.maxTemperature) changes++
        if (originalConstraints.minBatteryLevel != newConstraints.minBatteryLevel) changes++
        return changes
    }
}

/**
 * Individual adaptation action
 */
data class AdaptationAction(
    val type: AdaptationActionType,
    val description: String,
    val priority: AdaptationPriority,
    val parameters: Map<String, Any> = emptyMap(),
    val estimatedImpact: String = ""
)

/**
 * Types of adaptation actions
 */
enum class AdaptationActionType {
    MEMORY_OPTIMIZATION,
    BATTERY_OPTIMIZATION,
    NETWORK_OPTIMIZATION,
    CPU_OPTIMIZATION,
    QUALITY_ADJUSTMENT,
    FEATURE_TOGGLE,
    CACHE_MANAGEMENT,
    BACKGROUND_PROCESS_LIMITATION
}

/**
 * Priority levels for adaptation actions
 */
enum class AdaptationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Severity of adaptation changes
 */
enum class AdaptationSeverity {
    NONE,
    MINOR,
    MODERATE,
    MAJOR
}

/**
 * Performance recommendation with detailed information
 */
data class PerformanceRecommendation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val suggestedActions: List<String>,
    val estimatedImpact: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = timestamp + 300000, // 5 minutes default
    val context: Map<String, Any> = emptyMap()
)

/**
 * Types of performance recommendations
 */
enum class RecommendationType {
    MEMORY_OPTIMIZATION,
    BATTERY_OPTIMIZATION,
    NETWORK_OPTIMIZATION,
    THERMAL_MANAGEMENT,
    PERFORMANCE_IMPROVEMENT,
    RESOURCE_CLEANUP,
    SETTINGS_ADJUSTMENT,
    BEHAVIOR_CHANGE
}

/**
 * Priority levels for recommendations
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}