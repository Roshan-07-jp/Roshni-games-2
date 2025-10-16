package com.roshni.games.core.utils.error

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for the comprehensive error handling framework
 */
interface ErrorHandlingFramework {

    /**
     * Current status of the error handling framework
     */
    val status: StateFlow<ErrorHandlingStatus>

    /**
     * Flow of all error events for observation
     */
    val errorEvents: Flow<ErrorEvent>

    /**
     * Handle an error with automatic recovery strategies
     */
    suspend fun handleError(
        error: AppError,
        context: ErrorContext? = null,
        recoveryStrategies: List<ErrorRecoveryStrategy>? = null
    ): ErrorHandlingResult

    /**
     * Handle a throwable by converting it to an AppError first
     */
    suspend fun handleThrowable(
        throwable: Throwable,
        operation: String,
        component: String,
        context: ErrorContext? = null,
        recoveryStrategies: List<ErrorRecoveryStrategy>? = null
    ): ErrorHandlingResult

    /**
     * Register a custom error recovery strategy
     */
    suspend fun registerRecoveryStrategy(strategy: ErrorRecoveryStrategy): Boolean

    /**
     * Unregister a recovery strategy
     */
    suspend fun unregisterRecoveryStrategy(strategyName: String): Boolean

    /**
     * Get all registered recovery strategies
     */
    suspend fun getRecoveryStrategies(): List<ErrorRecoveryStrategy>

    /**
     * Get recovery strategies applicable to a specific error type
     */
    suspend fun getRecoveryStrategiesForError(error: AppError): List<ErrorRecoveryStrategy>

    /**
     * Report an error for analytics and monitoring
     */
    suspend fun reportError(
        error: AppError,
        context: ErrorContext? = null,
        includeStackTrace: Boolean = true
    ): ErrorReportingResult

    /**
     * Get error statistics and metrics
     */
    suspend fun getErrorStatistics(
        timeRange: TimeRange? = null,
        errorType: ErrorType? = null,
        severity: ErrorSeverity? = null
    ): ErrorStatistics

    /**
     * Clear error statistics
     */
    suspend fun clearErrorStatistics()

    /**
     * Configure error handling behavior
     */
    suspend fun configureErrorHandling(config: ErrorHandlingConfig): Boolean

    /**
     * Get current error handling configuration
     */
    suspend fun getErrorHandlingConfig(): ErrorHandlingConfig

    /**
     * Execute an operation with error handling
     */
    suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        componentName: String,
        context: ErrorContext? = null,
        recoveryStrategies: List<ErrorRecoveryStrategy>? = null
    ): ErrorHandlingExecutionResult<T>

    /**
     * Observe errors for a specific component
     */
    fun observeErrors(component: String): Flow<ErrorEvent>

    /**
     * Observe errors for a specific error type
     */
    fun observeErrorsByType(errorType: ErrorType): Flow<ErrorEvent>

    /**
     * Observe errors by severity level
     */
    fun observeErrorsBySeverity(severity: ErrorSeverity): Flow<ErrorEvent>

    /**
     * Create error context for an operation
     */
    fun createErrorContext(
        operation: String,
        component: String,
        userId: String? = null,
        sessionId: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): ErrorContext

    /**
     * Validate error recovery strategies
     */
    suspend fun validateRecoveryStrategies(): ValidationResult

    /**
     * Get error trends and patterns
     */
    suspend fun getErrorTrends(
        timeRange: TimeRange,
        groupBy: ErrorGrouping = ErrorGrouping.BY_TYPE
    ): ErrorTrends

    /**
     * Export error data for analysis
     */
    suspend fun exportErrorData(
        timeRange: TimeRange? = null,
        format: ExportFormat = ExportFormat.JSON
    ): ExportedErrorData

    /**
     * Import error data (for testing or migration)
     */
    suspend fun importErrorData(data: ExportedErrorData): Boolean

    /**
     * Initialize the error handling framework
     */
    suspend fun initialize(): Boolean

    /**
     * Shutdown the error handling framework
     */
    suspend fun shutdown()
}

/**
 * Status of the error handling framework
 */
data class ErrorHandlingStatus(
    val isInitialized: Boolean = false,
    val totalErrorsHandled: Long = 0,
    val activeRecoveryStrategies: Int = 0,
    val lastErrorTime: Long? = null,
    val errorRatePerMinute: Double = 0.0,
    val isShuttingDown: Boolean = false
)

/**
 * Error event for observation
 */
data class ErrorEvent(
    val error: AppError,
    val context: ErrorContext?,
    val handlingResult: ErrorHandlingResult?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration for error handling behavior
 */
data class ErrorHandlingConfig(
    val enableAutoRecovery: Boolean = true,
    val enableErrorReporting: Boolean = true,
    val enableErrorAnalytics: Boolean = true,
    val maxRetryAttempts: Int = 3,
    val baseRetryDelayMs: Long = 1000,
    val maxRetryDelayMs: Long = 30000,
    val enableCircuitBreaker: Boolean = true,
    val circuitBreakerThreshold: Int = 5,
    val circuitBreakerRecoveryTimeoutMs: Long = 60000,
    val enableErrorCaching: Boolean = true,
    val maxErrorCacheSize: Int = 1000,
    val errorCacheRetentionMs: Long = 3600000, // 1 hour
    val enableUserFriendlyMessages: Boolean = true,
    val enableDetailedLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.ERROR,
    val enableStackTraceCollection: Boolean = true,
    val maxStackTraceDepth: Int = 10
) {

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR, FATAL
    }
}

/**
 * Result of error reporting
 */
data class ErrorReportingResult(
    val success: Boolean,
    val errorId: String? = null,
    val reportedAt: Long = System.currentTimeMillis(),
    val message: String? = null
)

/**
 * Error statistics and metrics
 */
data class ErrorStatistics(
    val totalErrors: Long = 0,
    val errorsByType: Map<ErrorType, Long> = emptyMap(),
    val errorsBySeverity: Map<ErrorSeverity, Long> = emptyMap(),
    val errorsByComponent: Map<String, Long> = emptyMap(),
    val averageHandlingTimeMs: Double = 0.0,
    val recoverySuccessRate: Double = 0.0,
    val timeRange: TimeRange,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Time range for error statistics
 */
data class TimeRange(
    val startTime: Long,
    val endTime: Long
) {
    companion object {
        fun lastHour(): TimeRange {
            val now = System.currentTimeMillis()
            return TimeRange(now - 3600000, now)
        }

        fun last24Hours(): TimeRange {
            val now = System.currentTimeMillis()
            return TimeRange(now - 86400000, now)
        }

        fun last7Days(): TimeRange {
            val now = System.currentTimeMillis()
            return TimeRange(now - 604800000, now)
        }

        fun custom(startTime: Long, endTime: Long): TimeRange {
            return TimeRange(startTime, endTime)
        }
    }
}

/**
 * Result of error handling execution
 */
data class ErrorHandlingExecutionResult<T>(
    val success: Boolean,
    val result: T? = null,
    val error: AppError? = null,
    val handlingResult: ErrorHandlingResult? = null,
    val executionTimeMs: Long,
    val attempts: Int = 1
)

/**
 * Validation result for recovery strategies
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Error trends and patterns
 */
data class ErrorTrends(
    val timeRange: TimeRange,
    val dataPoints: List<ErrorDataPoint>,
    val trend: TrendDirection,
    val peakErrorTime: Long? = null,
    val mostCommonErrorType: ErrorType? = null,
    val generatedAt: Long = System.currentTimeMillis()
) {

    data class ErrorDataPoint(
        val timestamp: Long,
        val errorCount: Long,
        val errorTypes: Map<ErrorType, Long> = emptyMap()
    )

    enum class TrendDirection {
        INCREASING, DECREASING, STABLE, VOLATILE
    }
}

/**
 * Format for error data export
 */
enum class ExportFormat {
    JSON, XML, CSV
}

/**
 * Exported error data
 */
data class ExportedErrorData(
    val format: ExportFormat,
    val data: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val timeRange: TimeRange? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Grouping options for error trends
 */
enum class ErrorGrouping {
    BY_TYPE, BY_SEVERITY, BY_COMPONENT, BY_HOUR, BY_DAY
}