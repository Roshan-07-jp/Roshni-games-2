package com.roshni.games.core.utils.performance

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Process
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * Performance monitoring utility for tracking memory, battery, and system performance
 */
class PerformanceMonitor(private val context: Context) {

    private val _memoryMetrics = MutableStateFlow(MemoryMetrics())
    private val _batteryMetrics = MutableStateFlow(BatteryMetrics())
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())

    // Public flows
    val memoryMetrics: StateFlow<MemoryMetrics> = _memoryMetrics.asStateFlow()
    val batteryMetrics: StateFlow<BatteryMetrics> = _batteryMetrics.asStateFlow()
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private var monitoringEnabled = false
    private val monitoringInterval = 5000L // 5 seconds

    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (monitoringEnabled) return

        monitoringEnabled = true
        Timber.d("Starting performance monitoring")

        // Start periodic monitoring
        kotlinx.coroutines.GlobalScope.launch {
            while (monitoringEnabled) {
                try {
                    updateMemoryMetrics()
                    updateBatteryMetrics()
                    updatePerformanceMetrics()

                    kotlinx.coroutines.delay(monitoringInterval)
                } catch (e: Exception) {
                    Timber.e(e, "Error during performance monitoring")
                }
            }
        }
    }

    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        monitoringEnabled = false
        Timber.d("Stopped performance monitoring")
    }

    /**
     * Get current memory metrics
     */
    fun getCurrentMemoryMetrics(): MemoryMetrics {
        return try {
            val runtime = Runtime.getRuntime()

            MemoryMetrics(
                usedMemory = runtime.totalMemory() - runtime.freeMemory(),
                totalMemory = runtime.totalMemory(),
                maxMemory = runtime.maxMemory(),
                availableMemory = runtime.freeMemory(),
                nativeHeapSize = Debug.getNativeHeapSize(),
                nativeHeapFreeSize = Debug.getNativeHeapFreeSize(),
                nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize(),
                processId = Process.myPid()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get memory metrics")
            MemoryMetrics()
        }
    }

    /**
     * Get current battery metrics
     */
    fun getCurrentBatteryMetrics(): BatteryMetrics {
        return try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

                val batteryPercentage = if (level != -1 && scale != -1) {
                    (level.toFloat() / scale.toFloat()) * 100
                } else 0f

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL

                BatteryMetrics(
                    level = batteryPercentage,
                    temperature = temperature / 10f, // Convert to Celsius
                    voltage = voltage / 1000f, // Convert to volts
                    isCharging = isCharging,
                    status = status,
                    health = health,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                BatteryMetrics()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get battery metrics")
            BatteryMetrics()
        }
    }

    /**
     * Monitor FPS for a specific view
     */
    fun monitorFPS(viewTag: String): Flow<Float> = flow {
        var lastFrameTime = System.nanoTime()
        var frameCount = 0

        while (monitoringEnabled) {
            val currentTime = System.nanoTime()
            frameCount++

            // Calculate FPS every second
            if (currentTime - lastFrameTime >= 1_000_000_000) { // 1 second in nanoseconds
                val fps = frameCount.toFloat()
                emit(fps)

                frameCount = 0
                lastFrameTime = currentTime
            }

            kotlinx.coroutines.delay(16) // ~60 FPS target
        }
    }

    /**
     * Monitor memory usage patterns
     */
    fun monitorMemoryUsage(): Flow<MemoryUsagePattern> = flow {
        val usageHistory = mutableListOf<Long>()
        val maxHistorySize = 100

        while (monitoringEnabled) {
            val currentMemory = getCurrentMemoryMetrics()
            usageHistory.add(currentMemory.usedMemory)

            if (usageHistory.size > maxHistorySize) {
                usageHistory.removeAt(0)
            }

            if (usageHistory.size >= 10) { // Need minimum data points
                val pattern = analyzeMemoryPattern(usageHistory)
                emit(pattern)
            }

            kotlinx.coroutines.delay(5000) // Check every 5 seconds
        }
    }

    /**
     * Monitor thermal throttling
     */
    fun monitorThermalThrottling(): Flow<ThermalState> = flow {
        var previousCpuUsage = 0f
        var throttlingDetected = false

        while (monitoringEnabled) {
            val currentCpuUsage = getCpuUsage()
            val batteryMetrics = getCurrentBatteryMetrics()

            // Detect thermal throttling by sudden CPU usage drop with high temperature
            if (previousCpuUsage > 70f && currentCpuUsage < 30f && batteryMetrics.temperature > 35f) {
                if (!throttlingDetected) {
                    throttlingDetected = true
                    emit(ThermalState.THROTTLING_ACTIVE)
                }
            } else if (throttlingDetected && currentCpuUsage > 50f) {
                throttlingDetected = false
                emit(ThermalState.NORMAL)
            } else if (batteryMetrics.temperature > 40f) {
                emit(ThermalState.HIGH_TEMPERATURE)
            } else {
                emit(ThermalState.NORMAL)
            }

            previousCpuUsage = currentCpuUsage
            kotlinx.coroutines.delay(10000) // Check every 10 seconds
        }
    }

    /**
     * Get performance recommendations based on current metrics
     */
    fun getDetailedPerformanceRecommendations(): Flow<List<PerformanceRecommendation>> = flow {
        while (monitoringEnabled) {
            val recommendations = mutableListOf<PerformanceRecommendation>()

            val memory = getCurrentMemoryMetrics()
            val battery = getCurrentBatteryMetrics()
            val performance = getCurrentPerformanceMetrics()

            // Memory recommendations
            val memoryUsagePercentage = memory.usedMemory.toFloat() / memory.maxMemory.toFloat()
            when {
                memoryUsagePercentage > 0.95f -> {
                    recommendations.add(PerformanceRecommendation(
                        type = RecommendationType.MEMORY_CRITICAL,
                        severity = SeverityLevel.CRITICAL,
                        title = "Critical Memory Usage",
                        description = "Memory usage is critically high (${(memoryUsagePercentage * 100).toInt()}%). Close background apps immediately.",
                        action = "Close background applications"
                    ))
                }
                memoryUsagePercentage > 0.85f -> {
                    recommendations.add(PerformanceRecommendation(
                        type = RecommendationType.MEMORY_HIGH,
                        severity = SeverityLevel.WARNING,
                        title = "High Memory Usage",
                        description = "Memory usage is high (${(memoryUsagePercentage * 100).toInt()}%). Monitor performance.",
                        action = "Monitor app performance"
                    ))
                }
            }

            // Battery recommendations
            if (battery.level < 10f && !battery.isCharging) {
                recommendations.add(PerformanceRecommendation(
                    type = RecommendationType.BATTERY_CRITICAL,
                    severity = SeverityLevel.CRITICAL,
                    title = "Critical Battery Level",
                    description = "Battery level is critically low (${battery.level.toInt()}%). Connect charger immediately.",
                    action = "Connect charger"
                ))
            } else if (battery.level < 20f && !battery.isCharging) {
                recommendations.add(PerformanceRecommendation(
                    type = RecommendationType.BATTERY_LOW,
                    severity = SeverityLevel.WARNING,
                    title = "Low Battery Level",
                    description = "Battery level is low (${battery.level.toInt()}%). Consider enabling battery saver.",
                    action = "Enable battery saver mode"
                ))
            }

            // Thermal recommendations
            if (battery.temperature > 45f) {
                recommendations.add(PerformanceRecommendation(
                    type = RecommendationType.THERMAL_CRITICAL,
                    severity = SeverityLevel.CRITICAL,
                    title = "Device Overheating",
                    description = "Device temperature is critically high (${battery.temperature.toInt()}°C). Stop gaming and let device cool.",
                    action = "Stop gaming and cool device"
                ))
            } else if (battery.temperature > 40f) {
                recommendations.add(PerformanceRecommendation(
                    type = RecommendationType.THERMAL_HIGH,
                    severity = SeverityLevel.WARNING,
                    title = "High Device Temperature",
                    description = "Device temperature is high (${battery.temperature.toInt()}°C). Consider taking a break.",
                    action = "Take a break from gaming"
                ))
            }

            // Performance recommendations
            if (performance.cpuUsage > 90f) {
                recommendations.add(PerformanceRecommendation(
                    type = RecommendationType.CPU_HIGH,
                    severity = SeverityLevel.INFO,
                    title = "High CPU Usage",
                    description = "CPU usage is high (${performance.cpuUsage.toInt()}%). Performance may be affected.",
                    action = "Monitor performance"
                ))
            }

            emit(recommendations)
            kotlinx.coroutines.delay(15000) // Update every 15 seconds
        }
    }

    /**
     * Get current performance metrics
     */
    private fun getCurrentPerformanceMetrics(): PerformanceMetrics {
        return _performanceMetrics.value
    }

    /**
     * Analyze memory usage patterns
     */
    private fun analyzeMemoryPattern(history: List<Long>): MemoryUsagePattern {
        if (history.size < 10) {
            return MemoryUsagePattern.STABLE
        }

        val recent = history.takeLast(10)
        val older = history.take(history.size - 10)

        val recentAvg = recent.average()
        val olderAvg = older.average()

        val trend = recentAvg - olderAvg
        val trendPercentage = (trend / olderAvg) * 100

        return when {
            trendPercentage > 20 -> MemoryUsagePattern.INCREASING_RAPIDLY
            trendPercentage > 5 -> MemoryUsagePattern.INCREASING
            trendPercentage < -20 -> MemoryUsagePattern.DECREASING_RAPIDLY
            trendPercentage < -5 -> MemoryUsagePattern.DECREASING
            else -> MemoryUsagePattern.STABLE
        }
    }

    /**
     * Get CPU usage (simplified implementation)
     */
    fun getCpuUsage(): Float {
        return try {
            val pid = Process.myPid()
            val reader = BufferedReader(FileReader("/proc/$pid/stat"))
            val line = reader.readLine()
            reader.close()

            val parts = line.split(" ")
            if (parts.size >= 15) {
                val utime = parts[13].toLongOrNull() ?: 0
                val stime = parts[14].toLongOrNull() ?: 0
                val totalTime = utime + stime

                // This is a simplified calculation
                // In a real implementation, you'd need to track over time
                val cpuUsage = (totalTime / 1000f) / 100f // Simplified percentage
                cpuUsage.coerceIn(0f, 100f)
            } else 0f

        } catch (e: Exception) {
            Timber.e(e, "Failed to get CPU usage")
            0f
        }
    }

    /**
     * Check if device performance is degraded
     */
    fun isPerformanceDegraded(): Boolean {
        val memory = getCurrentMemoryMetrics()
        val battery = getCurrentBatteryMetrics()

        // Check memory usage
        val memoryUsagePercentage = memory.usedMemory.toFloat() / memory.maxMemory.toFloat()
        val highMemoryUsage = memoryUsagePercentage > 0.85f // More than 85% memory usage

        // Check battery level
        val lowBattery = battery.level < 20f && !battery.isCharging

        // Check if too many GC events (simplified)
        val frequentGC = false // Would need to track GC events

        return highMemoryUsage || lowBattery || frequentGC
    }

    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val memory = getCurrentMemoryMetrics()
        val battery = getCurrentBatteryMetrics()

        // Memory recommendations
        val memoryUsagePercentage = memory.usedMemory.toFloat() / memory.maxMemory.toFloat()
        when {
            memoryUsagePercentage > 0.9f -> {
                recommendations.add("Critical memory usage detected. Consider closing background apps.")
            }
            memoryUsagePercentage > 0.8f -> {
                recommendations.add("High memory usage. Monitor app performance.")
            }
        }

        // Battery recommendations
        if (battery.level < 15f && !battery.isCharging) {
            recommendations.add("Low battery level. Consider enabling battery saver mode.")
        }

        if (battery.temperature > 40f) {
            recommendations.add("Device is getting hot. Consider taking a break from gaming.")
        }

        // Performance recommendations
        if (isPerformanceDegraded()) {
            recommendations.add("Performance degraded. Consider restarting the app.")
        }

        return recommendations
    }

    /**
     * Update memory metrics
     */
    private fun updateMemoryMetrics() {
        try {
            _memoryMetrics.value = getCurrentMemoryMetrics()
        } catch (e: Exception) {
            Timber.e(e, "Failed to update memory metrics")
        }
    }

    /**
     * Update battery metrics
     */
    private fun updateBatteryMetrics() {
        try {
            _batteryMetrics.value = getCurrentBatteryMetrics()
        } catch (e: Exception) {
            Timber.e(e, "Failed to update battery metrics")
        }
    }

    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        try {
            val memory = getCurrentMemoryMetrics()
            val battery = getCurrentBatteryMetrics()

            val metrics = PerformanceMetrics(
                memoryUsage = memory.usedMemory,
                memoryUsagePercentage = memory.usedMemory.toFloat() / memory.maxMemory.toFloat(),
                batteryLevel = battery.level,
                batteryTemperature = battery.temperature,
                isCharging = battery.isCharging,
                cpuUsage = getCpuUsage(),
                timestamp = System.currentTimeMillis()
            )

            _performanceMetrics.value = metrics
        } catch (e: Exception) {
            Timber.e(e, "Failed to update performance metrics")
        }
    }
}

/**
 * Memory metrics data class
 */
data class MemoryMetrics(
    val usedMemory: Long = 0,
    val totalMemory: Long = 0,
    val maxMemory: Long = 0,
    val availableMemory: Long = 0,
    val nativeHeapSize: Long = 0,
    val nativeHeapFreeSize: Long = 0,
    val nativeHeapAllocatedSize: Long = 0,
    val processId: Int = 0
)

/**
 * Battery metrics data class
 */
data class BatteryMetrics(
    val level: Float = 0f,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val isCharging: Boolean = false,
    val status: Int = 0,
    val health: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val memoryUsage: Long = 0,
    val memoryUsagePercentage: Float = 0f,
    val batteryLevel: Float = 0f,
    val batteryTemperature: Float = 0f,
    val isCharging: Boolean = false,
    val cpuUsage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Memory usage pattern analysis
 */
enum class MemoryUsagePattern {
    STABLE,
    INCREASING,
    INCREASING_RAPIDLY,
    DECREASING,
    DECREASING_RAPIDLY
}

/**
 * Thermal state of the device
 */
enum class ThermalState {
    NORMAL,
    HIGH_TEMPERATURE,
    THROTTLING_ACTIVE
}

/**
 * Performance recommendation types
 */
enum class RecommendationType {
    MEMORY_CRITICAL,
    MEMORY_HIGH,
    BATTERY_CRITICAL,
    BATTERY_LOW,
    THERMAL_CRITICAL,
    THERMAL_HIGH,
    CPU_HIGH
}

/**
 * Severity levels for recommendations
 */
enum class SeverityLevel {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Performance recommendation data class
 */
data class PerformanceRecommendation(
    val type: RecommendationType,
    val severity: SeverityLevel,
    val title: String,
    val description: String,
    val action: String
)