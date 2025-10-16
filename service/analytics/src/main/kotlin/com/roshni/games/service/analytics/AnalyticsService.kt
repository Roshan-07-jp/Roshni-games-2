package com.roshni.games.service.analytics

import com.roshni.games.service.analytics.data.model.AnalyticsConfig
import com.roshni.games.service.analytics.data.model.AnalyticsEvent
import com.roshni.games.service.analytics.data.model.AnalyticsSession
import com.roshni.games.service.analytics.data.model.AnalyticsStats
import com.roshni.games.service.analytics.data.model.CrashReport
import com.roshni.games.service.analytics.data.model.DeviceInfo
import com.roshni.games.service.analytics.data.model.PerformanceMetrics
import com.roshni.games.service.analytics.data.model.UserConsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

/**
 * Main service class for analytics and crash reporting
 */
class AnalyticsService {

    private val _userConsent = MutableStateFlow(UserConsent())
    private val _analyticsConfig = MutableStateFlow(AnalyticsConfig())
    private val _currentSession = MutableStateFlow<AnalyticsSession?>(null)
    private val _analyticsStats = MutableStateFlow(AnalyticsStats())
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)

    // Public flows
    val userConsent: StateFlow<UserConsent> = _userConsent.asStateFlow()
    val analyticsConfig: StateFlow<AnalyticsConfig> = _analyticsConfig.asStateFlow()
    val currentSession: StateFlow<AnalyticsSession?> = _currentSession.asStateFlow()
    val analyticsStats: StateFlow<AnalyticsStats> = _analyticsStats.asStateFlow()
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    /**
     * Initialize the analytics service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing AnalyticsService")

            // Collect device information
            collectDeviceInfo()

            // Load user consent from storage (in real implementation)
            loadUserConsent()

            // Load analytics configuration
            loadAnalyticsConfig()

            // Start new session if analytics is enabled
            if (_userConsent.value.analyticsEnabled) {
                startNewSession()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AnalyticsService")
            Result.failure(e)
        }
    }

    /**
     * Update user consent for analytics
     */
    suspend fun updateUserConsent(consent: UserConsent): Result<Unit> {
        return try {
            Timber.d("Updating user consent: $consent")

            _userConsent.value = consent

            // Save consent to storage (in real implementation)
            saveUserConsent(consent)

            // Update analytics configuration based on consent
            updateAnalyticsConfig(consent)

            // Start or stop analytics based on consent
            if (consent.analyticsEnabled && _currentSession.value == null) {
                startNewSession()
            } else if (!consent.analyticsEnabled) {
                endCurrentSession()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user consent")
            Result.failure(e)
        }
    }

    /**
     * Track an analytics event
     */
    fun trackEvent(
        eventName: String,
        properties: Map<String, Any> = emptyMap(),
        userId: String? = null
    ): Result<Unit> {
        return try {
            if (!isAnalyticsEnabled()) {
                Timber.d("Analytics disabled, skipping event: $eventName")
                return Result.success(Unit)
            }

            val session = _currentSession.value
            if (session == null) {
                Timber.w("No active session, cannot track event: $eventName")
                return Result.failure(IllegalStateException("No active analytics session"))
            }

            val event = AnalyticsEvent(
                name = eventName,
                properties = properties,
                sessionId = session.id,
                userId = userId
            )

            // Update session event count
            updateCurrentSession { it.copy(eventCount = it.eventCount + 1) }

            // Update stats
            updateStats { it.copy(totalEvents = it.totalEvents + 1) }

            // In real implementation, this would send to analytics backend
            Timber.d("Tracking event: $eventName with properties: $properties")

            // Simulate sending to analytics backend
            sendEventToBackend(event)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to track event: $eventName")
            Result.failure(e)
        }
    }

    /**
     * Track game-related events
     */
    fun trackGameEvent(
        gameId: String,
        event: String,
        properties: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        val gameProperties = properties.toMutableMap().apply {
            put("game_id", gameId)
            put("category", "game")
        }

        return trackEvent("game_$event", gameProperties)
    }

    /**
     * Track performance metrics
     */
    fun trackPerformanceMetrics(metrics: PerformanceMetrics): Result<Unit> {
        return try {
            if (!isAnalyticsEnabled()) {
                return Result.success(Unit)
            }

            Timber.d("Tracking performance metrics: $metrics")

            // In real implementation, this would send to performance monitoring backend
            sendPerformanceMetricsToBackend(metrics)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to track performance metrics")
            Result.failure(e)
        }
    }

    /**
     * Report a crash
     */
    fun reportCrash(
        throwable: Throwable,
        breadcrumbs: List<String> = emptyList(),
        customData: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        return try {
            if (!isCrashReportingEnabled()) {
                Timber.d("Crash reporting disabled, skipping crash report")
                return Result.success(Unit)
            }

            val deviceInfo = _deviceInfo.value
            if (deviceInfo == null) {
                Timber.w("Device info not available for crash report")
                return Result.failure(IllegalStateException("Device info not available"))
            }

            val session = _currentSession.value
            val crashReport = CrashReport(
                id = UUID.randomUUID().toString(),
                message = throwable.message ?: "Unknown error",
                stackTrace = throwable.stackTraceToString(),
                cause = throwable.cause?.message,
                deviceInfo = deviceInfo,
                appInfo = getAppInfo(),
                userId = null, // In real implementation, get from user preferences
                sessionId = session?.id ?: "unknown",
                timestamp = System.currentTimeMillis(),
                breadcrumbs = breadcrumbs,
                customData = customData
            )

            // Update crash stats
            updateStats { it.copy(crashesReported = it.crashesReported + 1) }

            // In real implementation, this would send to crash reporting backend
            Timber.d("Reporting crash: ${crashReport.message}")
            sendCrashReportToBackend(crashReport)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to report crash")
            Result.failure(e)
        }
    }

    /**
     * Add breadcrumb for crash reporting
     */
    fun addBreadcrumb(message: String): Result<Unit> {
        return try {
            if (!isAnalyticsEnabled()) {
                return Result.success(Unit)
            }

            Timber.d("Adding breadcrumb: $message")

            // In real implementation, this would store breadcrumbs for crash reports
            // For now, we'll just log it

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add breadcrumb")
            Result.failure(e)
        }
    }

    /**
     * Start a new analytics session
     */
    private fun startNewSession(): Result<Unit> {
        return try {
            val session = AnalyticsSession(
                id = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                isActive = true
            )

            _currentSession.value = session
            updateStats { it.copy(totalSessions = it.totalSessions + 1) }

            Timber.d("Started new analytics session: ${session.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start new session")
            Result.failure(e)
        }
    }

    /**
     * End current analytics session
     */
    private fun endCurrentSession(): Result<Unit> {
        return try {
            val currentSession = _currentSession.value
            if (currentSession != null) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - currentSession.startTime

                val endedSession = currentSession.copy(
                    endTime = endTime,
                    duration = duration,
                    isActive = false
                )

                _currentSession.value = null
                updateStats { it.copy(
                    averageSessionDuration = (it.averageSessionDuration + duration) / 2,
                    lastActivity = endTime
                )}

                Timber.d("Ended analytics session: ${currentSession.id}, duration: ${duration}ms")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to end current session")
            Result.failure(e)
        }
    }

    /**
     * Check if analytics is enabled
     */
    private fun isAnalyticsEnabled(): Boolean {
        return _userConsent.value.analyticsEnabled &&
               _analyticsConfig.value.enabled
    }

    /**
     * Check if crash reporting is enabled
     */
    private fun isCrashReportingEnabled(): Boolean {
        return _userConsent.value.crashReportingEnabled &&
               _analyticsConfig.value.crashReportingEnabled
    }

    /**
     * Collect device information
     */
    private fun collectDeviceInfo() {
        try {
            // In a real implementation, this would collect actual device information
            val deviceInfo = DeviceInfo(
                manufacturer = "Sample Manufacturer",
                model = "Sample Model",
                androidVersion = "13",
                apiLevel = 33,
                screenResolution = "1080x2400",
                density = "xxhdpi",
                locale = "en_US",
                timezone = "UTC",
                totalMemory = 8L * 1024 * 1024 * 1024, // 8GB
                availableMemory = 6L * 1024 * 1024 * 1024 // 6GB
            )

            _deviceInfo.value = deviceInfo
            Timber.d("Collected device info: $deviceInfo")
        } catch (e: Exception) {
            Timber.e(e, "Failed to collect device info")
        }
    }

    /**
     * Get application information
     */
    private fun getAppInfo(): com.roshni.games.service.analytics.data.model.AppInfo {
        return com.roshni.games.service.analytics.data.model.AppInfo(
            version = "1.0.0",
            buildNumber = "1",
            packageName = "com.roshni.games",
            installTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 7 days ago
            lastUpdateTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 1 day ago
        )
    }

    /**
     * Load user consent from storage
     */
    private suspend fun loadUserConsent() {
        try {
            // In real implementation, this would load from SharedPreferences or database
            // For now, we'll use default values
            Timber.d("Loaded user consent: ${_userConsent.value}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user consent")
        }
    }

    /**
     * Save user consent to storage
     */
    private suspend fun saveUserConsent(consent: UserConsent) {
        try {
            // In real implementation, this would save to SharedPreferences or database
            Timber.d("Saved user consent: $consent")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user consent")
        }
    }

    /**
     * Load analytics configuration
     */
    private suspend fun loadAnalyticsConfig() {
        try {
            // In real implementation, this would load from remote config or local storage
            val config = AnalyticsConfig(
                enabled = _userConsent.value.analyticsEnabled,
                crashReportingEnabled = _userConsent.value.crashReportingEnabled,
                performanceMonitoringEnabled = _userConsent.value.performanceMonitoringEnabled,
                userAnalyticsEnabled = _userConsent.value.dataCollectionEnabled
            )

            _analyticsConfig.value = config
            Timber.d("Loaded analytics config: $config")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load analytics config")
        }
    }

    /**
     * Update analytics configuration based on user consent
     */
    private fun updateAnalyticsConfig(consent: UserConsent) {
        val config = _analyticsConfig.value.copy(
            enabled = consent.analyticsEnabled,
            crashReportingEnabled = consent.crashReportingEnabled,
            performanceMonitoringEnabled = consent.performanceMonitoringEnabled,
            userAnalyticsEnabled = consent.dataCollectionEnabled
        )

        _analyticsConfig.value = config
    }

    /**
     * Update current session
     */
    private fun updateCurrentSession(update: (AnalyticsSession) -> AnalyticsSession) {
        val current = _currentSession.value
        if (current != null) {
            _currentSession.value = update(current)
        }
    }

    /**
     * Update analytics statistics
     */
    private fun updateStats(update: (AnalyticsStats) -> AnalyticsStats) {
        _analyticsStats.value = update(_analyticsStats.value)
    }

    /**
     * Send event to analytics backend (simulated)
     */
    private fun sendEventToBackend(event: AnalyticsEvent) {
        // In real implementation, this would send to Firebase Analytics, Mixpanel, etc.
        Timber.d("Sending event to backend: ${event.name}")
    }

    /**
     * Send performance metrics to backend (simulated)
     */
    private fun sendPerformanceMetricsToBackend(metrics: PerformanceMetrics) {
        // In real implementation, this would send to Firebase Performance Monitoring
        Timber.d("Sending performance metrics to backend")
    }

    /**
     * Send crash report to backend (simulated)
     */
    private fun sendCrashReportToBackend(crashReport: CrashReport) {
        // In real implementation, this would send to Firebase Crashlytics or Sentry
        Timber.d("Sending crash report to backend: ${crashReport.message}")
    }
}