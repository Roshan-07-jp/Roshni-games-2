package com.roshni.games.core.utils.optimization

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Battery optimization utility for managing power consumption
 */
class BatteryOptimizer(private val context: Context) {

    private val _optimizationMode = MutableStateFlow(OptimizationMode.BALANCED)
    private val _batteryOptimizationEnabled = MutableStateFlow(false)
    private val _networkOptimizationEnabled = MutableStateFlow(true)
    private val _performanceOptimizations = MutableStateFlow(PerformanceOptimizations())

    // Public flows
    val optimizationMode: StateFlow<OptimizationMode> = _optimizationMode.asStateFlow()
    val batteryOptimizationEnabled: StateFlow<Boolean> = _batteryOptimizationEnabled.asStateFlow()
    val networkOptimizationEnabled: StateFlow<Boolean> = _networkOptimizationEnabled.asStateFlow()
    val performanceOptimizations: StateFlow<PerformanceOptimizations> = _performanceOptimizations.asStateFlow()

    /**
     * Optimization modes
     */
    enum class OptimizationMode {
        HIGH_PERFORMANCE,   // Maximum performance, higher battery drain
        BALANCED,          // Balanced performance and battery life
        BATTERY_SAVER,     // Maximum battery life, reduced performance
        ULTRA_SAVER        // Minimum battery usage, very limited features
    }

    /**
     * Performance optimization settings
     */
    data class PerformanceOptimizations(
        val reduceFrameRate: Boolean = false,
        val disableAnimations: Boolean = false,
        val reduceImageQuality: Boolean = false,
        val disableBackgroundSync: Boolean = false,
        val disableVibration: Boolean = false,
        val reduceSoundQuality: Boolean = false,
        val disableNotifications: Boolean = false,
        val limitNetworkRequests: Boolean = false
    )
    
    /**
     * Battery issue types
     */
    enum class BatteryIssueType {
        CRITICAL_BATTERY,
        LOW_BATTERY,
        OVERHEATING,
        HIGH_TEMPERATURE,
        NETWORK_USAGE,
        OPTIMIZATION_MODE
    }
    
    /**
     * Battery recommendation data class
     */
    data class BatteryRecommendation(
        val type: BatteryIssueType,
        val severity: SeverityLevel,
        val title: String,
        val description: String,
        val estimatedImpact: String,
        val suggestedActions: List<String>
    )
    
    /**
     * Severity levels for battery issues
     */
    enum class SeverityLevel {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Initialize battery optimizer
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing BatteryOptimizer")

            // Check if battery optimization is enabled for the app
            checkBatteryOptimizationStatus()

            // Detect current power saving mode
            detectCurrentOptimizationMode()

            // Apply initial optimizations
            applyOptimizations()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize BatteryOptimizer")
            Result.failure(e)
        }
    }

    /**
     * Set optimization mode
     */
    suspend fun setOptimizationMode(mode: OptimizationMode): Result<Unit> {
        return try {
            _optimizationMode.value = mode

            // Apply optimizations for the new mode
            applyOptimizations()

            Timber.d("Set optimization mode to: $mode")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set optimization mode")
            Result.failure(e)
        }
    }

    /**
     * Enable or disable battery optimization
     */
    suspend fun setBatteryOptimizationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            _batteryOptimizationEnabled.value = enabled

            if (enabled) {
                enableBatteryOptimizations()
            } else {
                disableBatteryOptimizations()
            }

            Timber.d("Battery optimization ${if (enabled) "enabled" else "disabled"}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set battery optimization")
            Result.failure(e)
        }
    }

    /**
     * Get battery optimization recommendations
     */
    fun getOptimizationRecommendations(): Flow<List<String>> = flow {
        val recommendations = mutableListOf<String>()

        try {
            // Check battery level
            val batteryLevel = getBatteryLevel()
            if (batteryLevel < 20) {
                recommendations.add("Battery level is low (${batteryLevel}%). Consider enabling battery saver mode.")
            }

            // Check if charging
            if (!isCharging() && batteryLevel < 50) {
                recommendations.add("Device is not charging and battery is below 50%. Consider reducing performance settings.")
            }

            // Check network status
            if (isOnMobileNetwork() && _networkOptimizationEnabled.value) {
                recommendations.add("Using mobile data. Consider enabling network optimizations to save data.")
            }

            // Check thermal status
            if (isDeviceHot()) {
                recommendations.add("Device temperature is high. Consider reducing performance to prevent overheating.")
            }

            // Check if power saving mode is available but not enabled
            if (batteryLevel < 30 && _optimizationMode.value != OptimizationMode.BATTERY_SAVER) {
                recommendations.add("Consider enabling battery saver mode to extend battery life.")
            }

            emit(recommendations)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get optimization recommendations")
            emit(emptyList())
        }
    }

    /**
     * Get detailed battery optimization recommendations
     */
    fun getDetailedOptimizationRecommendations(): Flow<List<BatteryRecommendation>> = flow {
        val recommendations = mutableListOf<BatteryRecommendation>()

        try {
            val batteryLevel = getBatteryLevel()
            val isCharging = isCharging()
            val isOnMobileData = isOnMobileNetwork()
            val deviceTemp = getDeviceTemperature()
            val currentMode = _optimizationMode.value

            // Battery level recommendations
            when {
                batteryLevel < 10 && !isCharging -> {
                    recommendations.add(BatteryRecommendation(
                        type = BatteryIssueType.CRITICAL_BATTERY,
                        severity = SeverityLevel.CRITICAL,
                        title = "Critical Battery Level",
                        description = "Battery level is critically low (${batteryLevel.toInt()}%). Connect charger immediately.",
                        estimatedImpact = "Device will shut down soon",
                        suggestedActions = listOf(
                            "Connect charger immediately",
                            "Save all game progress",
                            "Close unnecessary applications"
                        )
                    ))
                }
                batteryLevel < 20 && !isCharging -> {
                    recommendations.add(BatteryRecommendation(
                        type = BatteryIssueType.LOW_BATTERY,
                        severity = SeverityLevel.WARNING,
                        title = "Low Battery Level",
                        description = "Battery level is low (${batteryLevel.toInt()}%). Consider enabling battery saver mode.",
                        estimatedImpact = "Approximately ${getEstimatedBatteryLifeMinutes()} minutes remaining",
                        suggestedActions = listOf(
                            "Enable battery saver mode",
                            "Reduce screen brightness",
                            "Close background apps"
                        )
                    ))
                }
            }

            // Thermal recommendations
            when {
                deviceTemp > 45 -> {
                    recommendations.add(BatteryRecommendation(
                        type = BatteryIssueType.OVERHEATING,
                        severity = SeverityLevel.CRITICAL,
                        title = "Device Overheating",
                        description = "Device temperature is critically high (${deviceTemp.toInt()}°C). Performance and battery life are affected.",
                        estimatedImpact = "Battery drain increased by ~30%",
                        suggestedActions = listOf(
                            "Stop gaming and let device cool",
                            "Remove device case if possible",
                            "Move to cooler environment"
                        )
                    ))
                }
                deviceTemp > 40 -> {
                    recommendations.add(BatteryRecommendation(
                        type = BatteryIssueType.HIGH_TEMPERATURE,
                        severity = SeverityLevel.WARNING,
                        title = "High Device Temperature",
                        description = "Device temperature is high (${deviceTemp.toInt()}°C). Consider taking a break.",
                        estimatedImpact = "Battery drain increased by ~15%",
                        suggestedActions = listOf(
                            "Take a break from gaming",
                            "Enable thermal management",
                            "Check ventilation"
                        )
                    ))
                }
            }

            // Network optimization recommendations
            if (isOnMobileData && _networkOptimizationEnabled.value) {
                recommendations.add(BatteryRecommendation(
                    type = BatteryIssueType.NETWORK_USAGE,
                    severity = SeverityLevel.INFO,
                    title = "Mobile Data Usage",
                    description = "Using mobile data consumes more battery. Consider connecting to WiFi.",
                    estimatedImpact = "Battery drain increased by ~20%",
                    suggestedActions = listOf(
                        "Connect to WiFi network",
                        "Enable data saver mode",
                        "Limit background sync"
                    )
                ))
            }

            // Optimization mode recommendations
            if (currentMode == OptimizationMode.HIGH_PERFORMANCE && batteryLevel < 50) {
                recommendations.add(BatteryRecommendation(
                    type = BatteryIssueType.OPTIMIZATION_MODE,
                    severity = SeverityLevel.INFO,
                    title = "High Performance Mode",
                    description = "High performance mode drains battery faster. Consider switching to balanced mode.",
                    estimatedImpact = "Battery life reduced by ~25%",
                    suggestedActions = listOf(
                        "Switch to balanced mode",
                        "Enable battery saver when needed",
                        "Monitor battery level"
                    )
                ))
            }

            emit(recommendations)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get detailed optimization recommendations")
            emit(emptyList())
        }
    }

    /**
     * Get device temperature
     */
    private fun getDeviceTemperature(): Float {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val temperature = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)
                temperature / 10f // Convert to Celsius
            } else 0f
        } catch (e: Exception) {
            Timber.e(e, "Failed to get device temperature")
            0f
        }
    }

    /**
     * Get estimated battery life in minutes
     */
    private fun getEstimatedBatteryLifeMinutes(): Int {
        return try {
            val batteryLevel = getBatteryLevel()
            val isCharging = isCharging()

            if (isCharging) return -1

            // Base estimation on optimization mode and battery level
            val baseMinutes = when (_optimizationMode.value) {
                OptimizationMode.HIGH_PERFORMANCE -> 120 // 2 hours
                OptimizationMode.BALANCED -> 240 // 4 hours
                OptimizationMode.BATTERY_SAVER -> 360 // 6 hours
                OptimizationMode.ULTRA_SAVER -> 480 // 8 hours
            }

            (baseMinutes * (batteryLevel / 100f)).toInt()

        } catch (e: Exception) {
            Timber.e(e, "Failed to estimate battery life")
            0
        }
    }

    /**
     * Get estimated battery life based on current usage
     */
    fun getEstimatedBatteryLife(): Flow<Int> = flow {
        try {
            val batteryLevel = getBatteryLevel()
            val isCharging = isCharging()

            if (isCharging) {
                emit(-1) // Charging, no estimation needed
                return@flow
            }

            // Simple estimation based on optimization mode and battery level
            val baseHours = when (_optimizationMode.value) {
                OptimizationMode.HIGH_PERFORMANCE -> 2
                OptimizationMode.BALANCED -> 4
                OptimizationMode.BATTERY_SAVER -> 6
                OptimizationMode.ULTRA_SAVER -> 8
            }

            val estimatedMinutes = (baseHours * 60 * (batteryLevel / 100f)).toInt()
            emit(estimatedMinutes)

        } catch (e: Exception) {
            Timber.e(e, "Failed to estimate battery life")
            emit(0)
        }
    }

    /**
     * Optimize for current network conditions
     */
    suspend fun optimizeForNetwork(): Result<Unit> {
        return try {
            val isWifi = isOnWifi()
            val isMobile = isOnMobileNetwork()

            when {
                isWifi -> {
                    // Enable full features on WiFi
                    _networkOptimizationEnabled.value = false
                    Timber.d("On WiFi, enabling full features")
                }
                isMobile -> {
                    // Enable network optimizations on mobile data
                    _networkOptimizationEnabled.value = true
                    Timber.d("On mobile data, enabling network optimizations")
                }
                else -> {
                    // Offline or unknown network
                    _networkOptimizationEnabled.value = true
                    Timber.d("Offline or unknown network, enabling optimizations")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to optimize for network")
            Result.failure(e)
        }
    }

    /**
     * Check if device is in power saving mode
     */
    fun isInPowerSavingMode(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                powerManager.isPowerSaveMode
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check power saving mode")
            false
        }
    }

    /**
     * Get battery level
     */
    private fun getBatteryLevel(): Float {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    (level.toFloat() / scale.toFloat()) * 100
                } else 0f
            } else 0f
        } catch (e: Exception) {
            Timber.e(e, "Failed to get battery level")
            0f
        }
    }

    /**
     * Check if device is charging
     */
    private fun isCharging(): Boolean {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
            } else false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check charging status")
            false
        }
    }

    /**
     * Check if device is on WiFi
     */
    private fun isOnWifi(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            Timber.e(e, "Failed to check WiFi status")
            false
        }
    }

    /**
     * Check if device is on mobile network
     */
    private fun isOnMobileNetwork(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (e: Exception) {
            Timber.e(e, "Failed to check mobile network status")
            false
        }
    }

    /**
     * Check if device is hot
     */
    private fun isDeviceHot(): Boolean {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val temperature = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)
                temperature / 10f > 40f // Temperature in Celsius
            } else false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check device temperature")
            false
        }
    }

    /**
     * Check battery optimization status for the app
     */
    private fun checkBatteryOptimizationStatus() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
                _batteryOptimizationEnabled.value = !isIgnoringOptimizations
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check battery optimization status")
        }
    }

    /**
     * Detect current optimization mode based on system settings
     */
    private fun detectCurrentOptimizationMode() {
        try {
            val batteryLevel = getBatteryLevel()
            val isCharging = isCharging()
            val isInPowerSaveMode = isInPowerSavingMode()

            _optimizationMode.value = when {
                isInPowerSaveMode -> OptimizationMode.BATTERY_SAVER
                batteryLevel < 15 && !isCharging -> OptimizationMode.ULTRA_SAVER
                batteryLevel < 30 && !isCharging -> OptimizationMode.BATTERY_SAVER
                else -> OptimizationMode.BALANCED
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to detect optimization mode")
        }
    }

    /**
     * Apply optimizations based on current mode
     */
    private suspend fun applyOptimizations() {
        try {
            val optimizations = when (_optimizationMode.value) {
                OptimizationMode.HIGH_PERFORMANCE -> PerformanceOptimizations(
                    reduceFrameRate = false,
                    disableAnimations = false,
                    reduceImageQuality = false,
                    disableBackgroundSync = false,
                    disableVibration = false,
                    reduceSoundQuality = false,
                    disableNotifications = false,
                    limitNetworkRequests = false
                )

                OptimizationMode.BALANCED -> PerformanceOptimizations(
                    reduceFrameRate = false,
                    disableAnimations = false,
                    reduceImageQuality = false,
                    disableBackgroundSync = false,
                    disableVibration = false,
                    reduceSoundQuality = false,
                    disableNotifications = false,
                    limitNetworkRequests = _networkOptimizationEnabled.value
                )

                OptimizationMode.BATTERY_SAVER -> PerformanceOptimizations(
                    reduceFrameRate = true,
                    disableAnimations = true,
                    reduceImageQuality = true,
                    disableBackgroundSync = true,
                    disableVibration = true,
                    reduceSoundQuality = true,
                    disableNotifications = false,
                    limitNetworkRequests = true
                )

                OptimizationMode.ULTRA_SAVER -> PerformanceOptimizations(
                    reduceFrameRate = true,
                    disableAnimations = true,
                    reduceImageQuality = true,
                    disableBackgroundSync = true,
                    disableVibration = true,
                    reduceSoundQuality = true,
                    disableNotifications = true,
                    limitNetworkRequests = true
                )
            }

            _performanceOptimizations.value = optimizations
            Timber.d("Applied optimizations for mode: ${_optimizationMode.value}")

        } catch (e: Exception) {
            Timber.e(e, "Failed to apply optimizations")
        }
    }

    /**
     * Enable battery optimizations
     */
    private suspend fun enableBatteryOptimizations() {
        try {
            // Request to disable battery optimization for the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // Note: This would need to be called from an Activity in a real implementation
                Timber.d("Would request to ignore battery optimizations")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable battery optimizations")
        }
    }

    /**
     * Disable battery optimizations
     */
    private suspend fun disableBatteryOptimizations() {
        try {
            // Allow battery optimization for the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // Note: This would need to be called from an Activity in a real implementation
                Timber.d("Would open battery optimization settings")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable battery optimizations")
        }
    }
}