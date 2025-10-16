package com.roshni.games.core.utils.integration

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized error handling for integration operations
 */
object IntegrationErrorHandler {

    private val errorCounts = ConcurrentHashMap<String, Int>()
    private val errorHistory = ConcurrentHashMap<String, MutableList<IntegrationError>>()

    /**
     * Handle an integration error
     */
    fun handleError(error: Throwable, context: ErrorContext): IntegrationError {
        val integrationError = IntegrationError(
            id = "error_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}",
            type = determineErrorType(error),
            message = error.message ?: "Unknown error",
            cause = error.cause?.message,
            context = context,
            timestamp = System.currentTimeMillis(),
            stackTrace = error.stackTraceToString()
        )

        // Log the error
        logError(integrationError)

        // Record error statistics
        recordError(integrationError)

        // Execute error recovery if applicable
        executeErrorRecovery(integrationError)

        return integrationError
    }

    /**
     * Handle integration error with custom recovery
     */
    suspend fun handleErrorWithRecovery(
        error: Throwable,
        context: ErrorContext,
        recoveryStrategy: ErrorRecoveryStrategy
    ): ErrorRecoveryResult {
        val integrationError = handleError(error, context)

        return try {
            val recoveryResult = recoveryStrategy.recover(integrationError, context)

            if (recoveryResult.success) {
                Timber.i("Error recovery successful for ${integrationError.type}: ${integrationError.id}")
                ErrorRecoveryResult.success(recoveryResult.data, recoveryResult.message)
            } else {
                Timber.w("Error recovery failed for ${integrationError.type}: ${integrationError.id}")
                ErrorRecoveryResult.failure(recoveryResult.message)
            }

        } catch (recoveryError: Throwable) {
            Timber.e(recoveryError, "Error recovery itself failed for ${integrationError.type}: ${integrationError.id}")
            ErrorRecoveryResult.failure("Recovery failed: ${recoveryError.message}")
        }
    }

    /**
     * Get error statistics
     */
    fun getErrorStatistics(): ErrorStatistics {
        val totalErrors = errorCounts.values.sum()
        val errorTypes = errorCounts.keys.groupBy { determineErrorType(it) }

        return ErrorStatistics(
            totalErrors = totalErrors,
            errorsByType = errorCounts.toMap(),
            errorRate = if (totalErrors > 0) {
                errorCounts.values.average()
            } else 0.0,
            recentErrors = getRecentErrors(50),
            errorTrends = calculateErrorTrends()
        )
    }

    /**
     * Clear error history
     */
    fun clearErrorHistory() {
        errorCounts.clear()
        errorHistory.clear()
        Timber.d("Cleared integration error history")
    }

    /**
     * Determine error type from throwable
     */
    private fun determineErrorType(error: Throwable): IntegrationErrorType {
        return when (error) {
            is TimeoutCancellationException -> IntegrationErrorType.TIMEOUT
            is CancellationException -> IntegrationErrorType.CANCELLATION
            is IllegalArgumentException -> IntegrationErrorType.VALIDATION
            is IllegalStateException -> IntegrationErrorType.STATE
            is SecurityException -> IntegrationErrorType.SECURITY
            is UnsupportedOperationException -> IntegrationErrorType.UNSUPPORTED_OPERATION
            is kotlinx.coroutines.flow.internal.AbortFlowException -> IntegrationErrorType.FLOW_ABORT
            else -> IntegrationErrorType.UNKNOWN
        }
    }

    /**
     * Log error based on type and severity
     */
    private fun logError(error: IntegrationError) {
        val logMessage = "Integration Error [${error.type}]: ${error.message} in ${error.context.componentId}"

        when (error.type) {
            IntegrationErrorType.CRITICAL -> Timber.e(error.cause?.let { Throwable(logMessage, it) } ?: Throwable(logMessage))
            IntegrationErrorType.TIMEOUT -> Timber.w(logMessage)
            IntegrationErrorType.VALIDATION -> Timber.w(logMessage)
            IntegrationErrorType.STATE -> Timber.w(logMessage)
            IntegrationErrorType.SECURITY -> Timber.w(logMessage)
            else -> Timber.i(logMessage)
        }
    }

    /**
     * Record error for statistics
     */
    private fun recordError(error: IntegrationError) {
        // Increment error count for this component
        errorCounts[error.context.componentId] = errorCounts.getOrDefault(error.context.componentId, 0) + 1

        // Add to error history
        val history = errorHistory.getOrPut(error.context.componentId) { mutableListOf() }
        history.add(error)

        // Keep only last 100 errors per component
        if (history.size > 100) {
            history.removeAt(0)
        }
    }

    /**
     * Execute error recovery
     */
    private suspend fun executeErrorRecovery(error: IntegrationError) {
        // Default recovery strategies based on error type
        when (error.type) {
            IntegrationErrorType.TIMEOUT -> {
                // Could implement retry logic or circuit breaker
                Timber.d("Executing timeout recovery for error: ${error.id}")
            }
            IntegrationErrorType.STATE -> {
                // Could implement state reset or rollback
                Timber.d("Executing state recovery for error: ${error.id}")
            }
            IntegrationErrorType.VALIDATION -> {
                // Could implement validation retry with corrected data
                Timber.d("Executing validation recovery for error: ${error.id}")
            }
            else -> {
                // No specific recovery for other error types
            }
        }
    }

    /**
     * Get recent errors
     */
    private fun getRecentErrors(limit: Int): List<IntegrationError> {
        return errorHistory.values.flatten()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    /**
     * Calculate error trends
     */
    private fun calculateErrorTrends(): ErrorTrends {
        val allErrors = errorHistory.values.flatten()
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000
        val oneDayAgo = now - 86400000

        val recentErrors = allErrors.filter { it.timestamp > oneHourAgo }
        val dailyErrors = allErrors.filter { it.timestamp > oneDayAgo }

        return ErrorTrends(
            errorsLastHour = recentErrors.size,
            errorsLastDay = dailyErrors.size,
            errorRateLastHour = if (recentErrors.isNotEmpty()) {
                recentErrors.size.toDouble() / 60 // per minute
            } else 0.0,
            errorRateLastDay = if (dailyErrors.isNotEmpty()) {
                dailyErrors.size.toDouble() / 1440 // per minute
            } else 0.0
        )
    }
}

/**
 * Integration error types
 */
enum class IntegrationErrorType {
    CRITICAL,
    TIMEOUT,
    CANCELLATION,
    VALIDATION,
    STATE,
    SECURITY,
    UNSUPPORTED_OPERATION,
    FLOW_ABORT,
    UNKNOWN
}

/**
 * Integration error information
 */
data class IntegrationError(
    val id: String,
    val type: IntegrationErrorType,
    val message: String,
    val cause: String?,
    val context: ErrorContext,
    val timestamp: Long,
    val stackTrace: String,
    val resolved: Boolean = false,
    val resolution: String? = null
)

/**
 * Error context information
 */
data class ErrorContext(
    val componentId: String,
    val operation: String,
    val integrationId: String? = null,
    val featureId: String? = null,
    val workflowId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Error recovery strategy interface
 */
interface ErrorRecoveryStrategy {
    suspend fun recover(error: IntegrationError, context: ErrorContext): ErrorRecoveryResult
    fun getStrategyInfo(): RecoveryStrategyInfo
}

/**
 * Error recovery result
 */
data class ErrorRecoveryResult(
    val success: Boolean,
    val data: Any? = null,
    val message: String,
    val retryable: Boolean = false,
    val retryAfterMs: Long = 0
) {
    companion object {
        fun success(data: Any? = null, message: String = "Recovery successful"): ErrorRecoveryResult {
            return ErrorRecoveryResult(
                success = true,
                data = data,
                message = message
            )
        }

        fun failure(message: String, retryable: Boolean = false, retryAfterMs: Long = 0): ErrorRecoveryResult {
            return ErrorRecoveryResult(
                success = false,
                message = message,
                retryable = retryable,
                retryAfterMs = retryAfterMs
            )
        }
    }
}

/**
 * Recovery strategy information
 */
data class RecoveryStrategyInfo(
    val name: String,
    val description: String,
    val maxRetries: Int,
    val backoffStrategy: BackoffStrategy
)

/**
 * Backoff strategy for retries
 */
enum class BackoffStrategy {
    NONE,
    LINEAR,
    EXPONENTIAL,
    FIXED
}

/**
 * Error statistics
 */
data class ErrorStatistics(
    val totalErrors: Int,
    val errorsByType: Map<String, Int>,
    val errorRate: Double,
    val recentErrors: List<IntegrationError>,
    val errorTrends: ErrorTrends
)

/**
 * Error trends over time
 */
data class ErrorTrends(
    val errorsLastHour: Int,
    val errorsLastDay: Int,
    val errorRateLastHour: Double,
    val errorRateLastDay: Double
)

/**
 * Default error recovery strategies
 */
object DefaultErrorRecoveryStrategies {

    /**
     * Retry strategy with exponential backoff
     */
    class RetryWithBackoffStrategy(
        private val maxRetries: Int = 3,
        private val initialDelayMs: Long = 1000,
        private val backoffMultiplier: Double = 2.0
    ) : ErrorRecoveryStrategy {

        override suspend fun recover(error: IntegrationError, context: ErrorContext): ErrorRecoveryResult {
            val currentRetries = context.metadata["retryCount"] as? Int ?: 0

            return if (currentRetries < maxRetries) {
                val delayMs = (initialDelayMs * Math.pow(backoffMultiplier, currentRetries.toDouble())).toLong()

                ErrorRecoveryResult(
                    success = false,
                    message = "Retry scheduled after ${delayMs}ms",
                    retryable = true,
                    retryAfterMs = delayMs
                )
            } else {
                ErrorRecoveryResult.failure("Max retries exceeded", retryable = false)
            }
        }

        override fun getStrategyInfo(): RecoveryStrategyInfo {
            return RecoveryStrategyInfo(
                name = "RetryWithBackoff",
                description = "Retries operation with exponential backoff",
                maxRetries = maxRetries,
                backoffStrategy = BackoffStrategy.EXPONENTIAL
            )
        }
    }

    /**
     * Circuit breaker strategy
     */
    class CircuitBreakerStrategy(
        private val failureThreshold: Int = 5,
        private val recoveryTimeoutMs: Long = 60000
    ) : ErrorRecoveryStrategy {

        private val failureCounts = ConcurrentHashMap<String, Int>()
        private val lastFailureTimes = ConcurrentHashMap<String, Long>()

        override suspend fun recover(error: IntegrationError, context: ErrorContext): ErrorRecoveryResult {
            val key = "${context.componentId}_${context.operation}"
            val failureCount = failureCounts.getOrDefault(key, 0) + 1
            val lastFailureTime = lastFailureTimes.getOrDefault(key, 0L)
            val now = System.currentTimeMillis()

            failureCounts[key] = failureCount
            lastFailureTimes[key] = now

            return if (failureCount >= failureThreshold) {
                val timeSinceLastFailure = now - lastFailureTime
                if (timeSinceLastFailure < recoveryTimeoutMs) {
                    ErrorRecoveryResult.failure(
                        message = "Circuit breaker open - too many failures",
                        retryable = false
                    )
                } else {
                    // Reset circuit breaker
                    failureCounts[key] = 0
                    ErrorRecoveryResult.failure(
                        message = "Circuit breaker reset - retrying",
                        retryable = true,
                        retryAfterMs = 1000
                    )
                }
            } else {
                ErrorRecoveryResult.failure(
                    message = "Failure recorded - will retry",
                    retryable = true,
                    retryAfterMs = 1000
                )
            }
        }

        override fun getStrategyInfo(): RecoveryStrategyInfo {
            return RecoveryStrategyInfo(
                name = "CircuitBreaker",
                description = "Circuit breaker pattern to prevent cascade failures",
                maxRetries = failureThreshold,
                backoffStrategy = BackoffStrategy.FIXED
            )
        }
    }

    /**
     * Fallback strategy
     */
    class FallbackStrategy(
        private val fallbackData: Any,
        private val fallbackMessage: String = "Using fallback data"
    ) : ErrorRecoveryStrategy {

        override suspend fun recover(error: IntegrationError, context: ErrorContext): ErrorRecoveryResult {
            return ErrorRecoveryResult.success(
                data = fallbackData,
                message = fallbackMessage
            )
        }

        override fun getStrategyInfo(): RecoveryStrategyInfo {
            return RecoveryStrategyInfo(
                name = "Fallback",
                description = "Returns fallback data when operation fails",
                maxRetries = 0,
                backoffStrategy = BackoffStrategy.NONE
            )
        }
    }
}

/**
 * Integration operation wrapper with error handling
 */
suspend fun <T> withIntegrationErrorHandling(
    context: ErrorContext,
    recoveryStrategy: ErrorRecoveryStrategy? = null,
    operation: suspend () -> T
): Result<T> {
    return try {
        val result = operation()
        Result.success(result)

    } catch (e: Throwable) {
        val integrationError = IntegrationErrorHandler.handleError(e, context)

        if (recoveryStrategy != null) {
            val recoveryResult = IntegrationErrorHandler.handleErrorWithRecovery(e, context, recoveryStrategy)

            if (recoveryResult.success) {
                @Suppress("UNCHECKED_CAST")
                Result.success(recoveryResult.data as T)
            } else {
                Result.failure(e)
            }
        } else {
            Result.failure(e)
        }
    }
}

/**
 * Extension function for safe integration operations
 */
suspend fun <T> SystemIntegrationHub.safeExecute(
    operation: String,
    componentId: String,
    integrationId: String? = null,
    recoveryStrategy: ErrorRecoveryStrategy? = null,
    block: suspend () -> T
): Result<T> {
    val errorContext = ErrorContext(
        componentId = componentId,
        operation = operation,
        integrationId = integrationId
    )

    return withIntegrationErrorHandling(errorContext, recoveryStrategy, block)
}