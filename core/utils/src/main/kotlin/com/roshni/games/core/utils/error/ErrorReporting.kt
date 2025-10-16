package com.roshni.games.core.utils.error

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Comprehensive error reporting system for analytics, monitoring, and debugging
 */
class ErrorReporting(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val _reportingStatus = MutableStateFlow(ReportingStatus())
    private val _errorReports = Channel<ErrorReport>(Channel.UNLIMITED)

    val reportingStatus: StateFlow<ReportingStatus> = _reportingStatus.asStateFlow()
    val errorReports: Flow<ErrorReport> = _errorReports.receiveAsFlow()

    init {
        startReportingProcessor()
    }

    /**
     * Report an error with full context and metadata
     */
    suspend fun reportError(
        error: AppError,
        context: ErrorContext? = null,
        tags: Map<String, String> = emptyMap(),
        userId: String? = context?.userId,
        sessionId: String? = context?.sessionId,
        includeStackTrace: Boolean = true,
        includeDeviceInfo: Boolean = true,
        includeAppInfo: Boolean = true
    ): ErrorReport {
        val report = createErrorReport(
            error = error,
            context = context,
            tags = tags,
            userId = userId,
            sessionId = sessionId,
            includeStackTrace = includeStackTrace,
            includeDeviceInfo = includeDeviceInfo,
            includeAppInfo = includeAppInfo
        )

        _errorReports.send(report)

        // Update reporting status
        updateReportingStatus()

        Timber.d("Error report created: ${report.reportId}")
        return report
    }

    /**
     * Report multiple errors in batch
     */
    suspend fun reportErrors(
        errors: List<Pair<AppError, ErrorContext?>>,
        batchId: String = generateBatchId()
    ): BatchErrorReport {
        val reports = errors.map { (error, context) ->
            createErrorReport(error, context, batchId = batchId)
        }

        val batchReport = BatchErrorReport(
            batchId = batchId,
            reports = reports,
            totalCount = reports.size,
            createdAt = System.currentTimeMillis()
        )

        reports.forEach { _errorReports.send(it) }

        updateReportingStatus()
        return batchReport
    }

    /**
     * Create error report without sending (for local storage or manual processing)
     */
    fun createErrorReport(
        error: AppError,
        context: ErrorContext? = null,
        tags: Map<String, String> = emptyMap(),
        userId: String? = context?.userId,
        sessionId: String? = context?.sessionId,
        batchId: String? = null,
        includeStackTrace: Boolean = true,
        includeDeviceInfo: Boolean = true,
        includeAppInfo: Boolean = true
    ): ErrorReport {
        return ErrorReport(
            reportId = generateReportId(),
            batchId = batchId,
            error = error,
            context = context,
            tags = tags,
            userId = userId,
            sessionId = sessionId,
            stackTrace = if (includeStackTrace) error.cause?.stackTraceToString() else null,
            deviceInfo = if (includeDeviceInfo) getDeviceInfo() else null,
            appInfo = if (includeAppInfo) getAppInfo() else null,
            networkInfo = getNetworkInfo(),
            breadcrumbs = context?.breadcrumbs ?: emptyList(),
            customData = context?.metadata ?: emptyMap(),
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Export error reports for external analysis
     */
    suspend fun exportErrorReports(
        timeRange: TimeRange? = null,
        format: ReportFormat = ReportFormat.JSON,
        destination: File? = null
    ): ExportResult {
        // In a real implementation, this would collect reports from storage
        // and export them in the specified format

        return ExportResult(
            success = true,
            exportedCount = 0, // Would be actual count
            format = format,
            destination = destination,
            exportedAt = System.currentTimeMillis()
        )
    }

    /**
     * Get error reporting statistics
     */
    suspend fun getReportingStatistics(timeRange: TimeRange? = null): ReportingStatistics {
        // In a real implementation, this would query stored reports
        return ReportingStatistics(
            totalReports = 0,
            reportsByType = emptyMap(),
            reportsBySeverity = emptyMap(),
            averageReportsPerDay = 0.0,
            timeRange = timeRange ?: TimeRange.last24Hours()
        )
    }

    private fun startReportingProcessor() {
        scope.launch {
            for (report in errorReports) {
                try {
                    processErrorReport(report)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process error report: ${report.reportId}")
                }
            }
        }
    }

    private suspend fun processErrorReport(report: ErrorReport) {
        // Send to external services (Crashlytics, Sentry, etc.)
        sendToExternalServices(report)

        // Store locally if needed
        storeLocally(report)

        // Update analytics
        updateAnalytics(report)

        Timber.d("Error report processed: ${report.reportId}")
    }

    private suspend fun sendToExternalServices(report: ErrorReport) {
        // Integration with external error reporting services
        // This would be implemented based on the specific services used

        try {
            // Example: Send to analytics system
            // analytics.track("error_occurred", report.toAnalyticsData())

            // Example: Send to crash reporting service
            // crashReporter.report(report.toCrashData())

            Timber.d("Error report sent to external services: ${report.reportId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send error report to external services")
        }
    }

    private suspend fun storeLocally(report: ErrorReport) {
        // Store report locally for offline analysis
        // In a real implementation, this might use local database or file storage

        try {
            // Example: Store in local database
            // errorReportDao.insert(report)

            Timber.d("Error report stored locally: ${report.reportId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to store error report locally")
        }
    }

    private suspend fun updateAnalytics(report: ErrorReport) {
        // Update internal analytics and metrics
        // This would integrate with the existing analytics system

        try {
            // Example: Update error metrics
            // metrics.increment("error.${report.error.errorType.name}")

            Timber.d("Error analytics updated: ${report.reportId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update error analytics")
        }
    }

    private fun updateReportingStatus() {
        _reportingStatus.value = _reportingStatus.value.copy(
            lastReportTime = System.currentTimeMillis(),
            totalReportsProcessed = _reportingStatus.value.totalReportsProcessed + 1
        )
    }

    // Device and environment information collection
    private fun getDeviceInfo(): DeviceInfo {
        return ErrorContext.DeviceInfo(
            model = getSystemProperty("ro.product.model") ?: "unknown",
            manufacturer = getSystemProperty("ro.product.manufacturer") ?: "unknown",
            osVersion = "Android ${android.os.Build.VERSION.SDK_INT}",
            appVersion = getAppVersion(),
            batteryLevel = getBatteryLevel(),
            availableMemory = getAvailableMemory(),
            availableStorage = getAvailableStorage(),
            screenResolution = getScreenResolution(),
            orientation = getOrientation()
        )
    }

    private fun getAppInfo(): AppInfo {
        return AppInfo(
            version = getAppVersion(),
            buildNumber = getBuildNumber(),
            packageName = getPackageName(),
            installTime = getInstallTime(),
            lastUpdateTime = getLastUpdateTime()
        )
    }

    private fun getNetworkInfo(): NetworkInfo {
        return ErrorContext.NetworkState(
            isConnected = isNetworkConnected(),
            connectionType = getConnectionType(),
            signalStrength = getSignalStrength(),
            isMetered = isMeteredConnection(),
            isRoaming = isRoaming()
        )
    }

    // Platform-specific implementations would go here
    private fun getSystemProperty(key: String): String? = System.getProperty(key)
    private fun getAppVersion(): String = "1.0.0" // Would get from package manager
    private fun getBuildNumber(): String = "1" // Would get from build config
    private fun getPackageName(): String = "com.roshni.games" // Would get from context
    private fun getInstallTime(): Long = 0L // Would get from package manager
    private fun getLastUpdateTime(): Long = 0L // Would get from package manager
    private fun getBatteryLevel(): Int? = null // Would get from battery manager
    private fun getAvailableMemory(): Long? = null // Would get from activity manager
    private fun getAvailableStorage(): Long? = null // Would get from storage stats
    private fun getScreenResolution(): String? = null // Would get from display metrics
    private fun getOrientation(): String? = null // Would get from display
    private fun isNetworkConnected(): Boolean = true // Would check connectivity manager
    private fun getConnectionType(): String? = null // Would get from connectivity manager
    private fun getSignalStrength(): Int? = null // Would get from telephony manager
    private fun isMeteredConnection(): Boolean = false // Would check connectivity manager
    private fun isRoaming(): Boolean = false // Would check telephony manager

    companion object {
        private fun generateReportId(): String {
            return "report_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10000)}"
        }

        private fun generateBatchId(): String {
            return "batch_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10000)}"
        }
    }
}

/**
 * Individual error report
 */
data class ErrorReport(
    val reportId: String,
    val batchId: String?,
    val error: AppError,
    val context: ErrorContext?,
    val tags: Map<String, String>,
    val userId: String?,
    val sessionId: String?,
    val stackTrace: String?,
    val deviceInfo: ErrorContext.DeviceInfo?,
    val appInfo: AppInfo?,
    val networkInfo: ErrorContext.NetworkState?,
    val breadcrumbs: List<String>,
    val customData: Map<String, Any>,
    val createdAt: Long
) {

    /**
     * Convert to analytics data format
     */
    fun toAnalyticsData(): Map<String, Any> {
        return mapOf(
            "reportId" to reportId,
            "errorType" to error.errorType.name,
            "errorSeverity" to error.severity.name,
            "errorMessage" to error.message,
            "component" to (context?.component ?: "unknown"),
            "operation" to (context?.operation ?: "unknown"),
            "userId" to (userId ?: "anonymous"),
            "sessionId" to (sessionId ?: "unknown"),
            "timestamp" to createdAt,
            "tags" to tags,
            "customData" to customData
        )
    }

    /**
     * Convert to crash reporting format
     */
    fun toCrashData(): Map<String, Any> {
        return mapOf(
            "id" to reportId,
            "type" to error.errorType.name,
            "message" to error.message,
            "stacktrace" to (stackTrace ?: ""),
            "timestamp" to createdAt,
            "device" to (deviceInfo?.toString() ?: ""),
            "app" to (appInfo?.toString() ?: ""),
            "breadcrumbs" to breadcrumbs,
            "customData" to customData
        )
    }
}

/**
 * Batch error report for multiple errors
 */
data class BatchErrorReport(
    val batchId: String,
    val reports: List<ErrorReport>,
    val totalCount: Int,
    val createdAt: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Application information for error reporting
 */
data class AppInfo(
    val version: String,
    val buildNumber: String,
    val packageName: String,
    val installTime: Long,
    val lastUpdateTime: Long
)

/**
 * Network information for error reporting
 */
data class NetworkInfo(
    val isConnected: Boolean,
    val connectionType: String?,
    val signalStrength: Int?,
    val isMetered: Boolean,
    val isRoaming: Boolean
)

/**
 * Error reporting status
 */
data class ReportingStatus(
    val isEnabled: Boolean = true,
    val totalReportsProcessed: Long = 0,
    val lastReportTime: Long? = null,
    val pendingReports: Int = 0,
    val failedReports: Long = 0,
    val externalServices: List<String> = emptyList()
)

/**
 * Error reporting statistics
 */
data class ReportingStatistics(
    val totalReports: Long,
    val reportsByType: Map<ErrorType, Long>,
    val reportsBySeverity: Map<ErrorSeverity, Long>,
    val averageReportsPerDay: Double,
    val timeRange: TimeRange
)

/**
 * Export result for error reports
 */
data class ExportResult(
    val success: Boolean,
    val exportedCount: Int,
    val format: ReportFormat,
    val destination: File?,
    val exportedAt: Long,
    val error: String? = null
)

/**
 * Format for exporting error reports
 */
enum class ReportFormat {
    JSON, XML, CSV, PROTOCOL_BUFFER
}

/**
 * Error reporting configuration
 */
data class ErrorReportingConfig(
    val enableExternalReporting: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val enableAnalytics: Boolean = true,
    val maxLocalReports: Int = 1000,
    val reportRetentionDays: Int = 30,
    val externalServices: List<ExternalService> = emptyList(),
    val sampleRate: Double = 1.0, // 1.0 = 100% of errors reported
    val enableBreadcrumbs: Boolean = true,
    val maxBreadcrumbEntries: Int = 50,
    val enableDeviceInfoCollection: Boolean = true,
    val enableAppInfoCollection: Boolean = true,
    val enableNetworkInfoCollection: Boolean = true
)

/**
 * External service configuration
 */
data class ExternalService(
    val name: String,
    val endpoint: String,
    val apiKey: String?,
    val enabled: Boolean = true,
    val batchSize: Int = 10,
    val flushIntervalMs: Long = 30000
)

/**
 * Error reporting manager for centralized error reporting
 */
class ErrorReportingManager(
    private val errorReporting: ErrorReporting,
    private val config: ErrorReportingConfig = ErrorReportingConfig()
) {

    /**
     * Report error with automatic configuration-based processing
     */
    suspend fun reportError(
        error: AppError,
        context: ErrorContext? = null,
        tags: Map<String, String> = emptyMap()
    ) {
        // Apply sampling rate
        if (kotlin.random.Random.nextDouble() > config.sampleRate) {
            return // Skip reporting based on sample rate
        }

        errorReporting.reportError(
            error = error,
            context = context,
            tags = tags,
            includeStackTrace = true,
            includeDeviceInfo = config.enableDeviceInfoCollection,
            includeAppInfo = config.enableAppInfoCollection
        )
    }

    /**
     * Report error with breadcrumbs for better debugging context
     */
    suspend fun reportErrorWithBreadcrumbs(
        error: AppError,
        breadcrumbs: List<String>,
        context: ErrorContext? = null
    ) {
        val contextWithBreadcrumbs = context?.copy(breadcrumbs = breadcrumbs) ?: ErrorContext(
            operation = "unknown",
            component = "unknown",
            breadcrumbs = breadcrumbs
        )

        reportError(error, contextWithBreadcrumbs)
    }

    /**
     * Add breadcrumb for current operation context
     */
    fun addBreadcrumb(message: String, data: Map<String, Any> = emptyMap()) {
        // In a real implementation, this would maintain a breadcrumb trail
        // that gets included in error reports
        Timber.d("Breadcrumb: $message - $data")
    }

    /**
     * Configure error reporting settings
     */
    fun configure(config: ErrorReportingConfig) {
        // Update configuration
        Timber.d("Error reporting configuration updated")
    }
}