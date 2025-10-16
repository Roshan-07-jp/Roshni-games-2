package com.roshni.games.core.utils.optimization

import com.roshni.games.core.utils.optimization.BatteryOptimizer.OptimizationMode

/**
 * Context information for performance optimization decisions
 */
data class OptimizationContext(
    val memoryMetrics: com.roshni.games.core.utils.performance.MemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(),
    val batteryMetrics: com.roshni.games.core.utils.performance.BatteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(),
    val batteryOptimizationMode: OptimizationMode = OptimizationMode.BALANCED,
    val resourceConstraints: ResourceConstraints = ResourceConstraints(),
    val systemLoad: SystemLoad = SystemLoad(),
    val networkConditions: NetworkConditions = NetworkConditions(),
    val userPreferences: UserPreferences = UserPreferences(),
    val lastUpdated: Long = System.currentTimeMillis()
) {

    /**
     * Check if optimization should be triggered based on current context
     */
    fun shouldTriggerOptimization(): Boolean {
        return when {
            // Critical memory usage
            memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat() > 0.9f -> true

            // Critical battery level
            batteryMetrics.level < 15f && !batteryMetrics.isCharging -> true

            // High temperature
            batteryMetrics.temperature > 42f -> true

            // High system load
            systemLoad.cpuUsage > 85f -> true

            // Poor network conditions with high usage
            networkConditions.isPoorConnection && networkConditions.bandwidthUsage > 80f -> true

            // User preferences indicate need for optimization
            userPreferences.performanceMode == PerformanceMode.BATTERY_SAVER &&
            batteryOptimizationMode != OptimizationMode.BATTERY_SAVER -> true

            // Time since last update is too long
            System.currentTimeMillis() - lastUpdated > 60000 -> true // 1 minute

            else -> false
        }
    }

    /**
     * Get overall performance score (0-100, higher is better)
     */
    fun getPerformanceScore(): Int {
        var score = 100

        // Memory impact
        val memoryUsage = memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat()
        score -= (memoryUsage * 30).toInt() // Up to 30 points for memory

        // Battery impact
        if (batteryMetrics.level < 20f) {
            score -= 25
        } else if (batteryMetrics.level < 50f) {
            score -= 10
        }

        // Temperature impact
        if (batteryMetrics.temperature > 40f) {
            score -= 20
        } else if (batteryMetrics.temperature > 35f) {
            score -= 10
        }

        // System load impact
        score -= (systemLoad.cpuUsage / 5).toInt() // Up to 20 points for CPU

        // Network impact
        if (networkConditions.isPoorConnection) {
            score -= 15
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Get optimization priority based on current context
     */
    fun getOptimizationPriority(): OptimizationPriority {
        return when {
            // Critical conditions
            memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat() > 0.95f ||
            batteryMetrics.level < 10f ||
            batteryMetrics.temperature > 45f -> OptimizationPriority.CRITICAL

            // High priority conditions
            memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat() > 0.85f ||
            batteryMetrics.level < 20f ||
            batteryMetrics.temperature > 40f ||
            systemLoad.cpuUsage > 80f -> OptimizationPriority.HIGH

            // Medium priority conditions
            memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat() > 0.75f ||
            batteryMetrics.level < 50f ||
            systemLoad.cpuUsage > 60f -> OptimizationPriority.MEDIUM

            // Low priority
            else -> OptimizationPriority.LOW
        }
    }
}

/**
 * Resource constraints for optimization
 */
data class ResourceConstraints(
    val maxMemoryUsage: Float = 0.8f, // Maximum memory usage percentage
    val maxBatteryDrain: Float = 0.7f, // Maximum battery drain rate
    val maxNetworkUsage: Float = 0.6f, // Maximum network usage percentage
    val maxCpuUsage: Float = 0.8f, // Maximum CPU usage percentage
    val maxTemperature: Float = 40f, // Maximum device temperature in Celsius
    val minBatteryLevel: Float = 15f // Minimum battery level percentage
)

/**
 * System load information
 */
data class SystemLoad(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val ioUsage: Float = 0f,
    val threadCount: Int = 0,
    val gcCount: Int = 0,
    val gcTime: Long = 0
)

/**
 * Network conditions
 */
data class NetworkConditions(
    val isConnected: Boolean = true,
    val isWifi: Boolean = true,
    val isMobile: Boolean = false,
    val bandwidthUsage: Float = 0f,
    val latency: Long = 0,
    val isPoorConnection: Boolean = false,
    val signalStrength: Int = 100
)

/**
 * User preferences for optimization
 */
data class UserPreferences(
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val enableAdaptiveOptimization: Boolean = true,
    val enableBatteryOptimization: Boolean = true,
    val enableMemoryOptimization: Boolean = true,
    val enableNetworkOptimization: Boolean = true,
    val optimizationAggressiveness: OptimizationAggressiveness = OptimizationAggressiveness.MODERATE
)

/**
 * Performance modes
 */
enum class PerformanceMode {
    HIGH_PERFORMANCE,
    BALANCED,
    BATTERY_SAVER,
    ADAPTIVE
}

/**
 * Optimization aggressiveness levels
 */
enum class OptimizationAggressiveness {
    CONSERVATIVE, // Minimal optimizations
    MODERATE,     // Balanced optimizations
    AGGRESSIVE,   // Strong optimizations
    EXTREME       // Maximum optimizations
}

/**
 * Optimization priority levels
 */
enum class OptimizationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}