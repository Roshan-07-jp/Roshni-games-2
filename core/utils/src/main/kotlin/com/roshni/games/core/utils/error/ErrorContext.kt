package com.roshni.games.core.utils.error

import com.roshni.games.core.utils.feature.FeatureContext

/**
 * Context information for error handling and recovery
 */
data class ErrorContext(
    /**
     * Unique identifier for this error context
     */
    val errorContextId: String = generateContextId(),

    /**
     * The operation or feature that was being executed when the error occurred
     */
    val operation: String,

    /**
     * The component or module where the error occurred
     */
    val component: String,

    /**
     * User ID if available
     */
    val userId: String? = null,

    /**
     * Session ID if available
     */
    val sessionId: String? = null,

    /**
     * Device information
     */
    val deviceInfo: DeviceInfo = DeviceInfo(),

    /**
     * Network state at the time of error
     */
    val networkState: NetworkState = NetworkState(),

    /**
     * Additional context data
     */
    val metadata: Map<String, Any> = emptyMap(),

    /**
     * Timestamp when the error context was created
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Feature context if the error occurred during feature execution
     */
    val featureContext: FeatureContext? = null,

    /**
     * Stack trace information
     */
    val stackTrace: List<String> = emptyList(),

    /**
     * Breadcrumb trail of operations leading to the error
     */
    val breadcrumbs: List<String> = emptyList()
) {

    /**
     * Device information for error context
     */
    data class DeviceInfo(
        val model: String = "unknown",
        val manufacturer: String = "unknown",
        val osVersion: String = "unknown",
        val appVersion: String = "unknown",
        val batteryLevel: Int? = null,
        val availableMemory: Long? = null,
        val availableStorage: Long? = null,
        val screenResolution: String? = null,
        val orientation: String? = null
    )

    /**
     * Network state information
     */
    data class NetworkState(
        val isConnected: Boolean = false,
        val connectionType: String? = null,
        val signalStrength: Int? = null,
        val isMetered: Boolean = false,
        val isRoaming: Boolean = false
    )

    companion object {
        private fun generateContextId(): String {
            return "ctx_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10000)}"
        }

        /**
         * Create error context from feature context
         */
        fun fromFeatureContext(
            operation: String,
            component: String,
            featureContext: FeatureContext
        ): ErrorContext {
            return ErrorContext(
                operation = operation,
                component = component,
                userId = featureContext.userId,
                sessionId = featureContext.sessionId,
                featureContext = featureContext,
                metadata = featureContext.metadata
            )
        }
    }
}

/**
 * Result of error handling operation
 */
data class ErrorHandlingResult(
    /**
     * Whether the error was successfully handled
     */
    val success: Boolean,

    /**
     * The original error that was handled
     */
    val originalError: AppError,

    /**
     * The final error state after handling (may be different from original if transformed)
     */
    val finalError: AppError? = null,

    /**
     * Recovery actions that were attempted
     */
    val recoveryActions: List<RecoveryAction> = emptyList(),

    /**
     * Whether the operation should be retried
     */
    val shouldRetry: Boolean = false,

    /**
     * Suggested delay before retry (in milliseconds)
     */
    val retryDelayMs: Long? = null,

    /**
     * User-facing message for the error
     */
    val userMessage: String? = null,

    /**
     * Whether user action is required to resolve the error
     */
    val requiresUserAction: Boolean = false,

    /**
     * Suggested user actions
     */
    val suggestedActions: List<UserAction> = emptyList(),

    /**
     * Additional data from error handling
     */
    val metadata: Map<String, Any> = emptyMap(),

    /**
     * Timestamp when error handling completed
     */
    val handledAt: Long = System.currentTimeMillis()
) {

    /**
     * Recovery action that was attempted
     */
    data class RecoveryAction(
        val action: String,
        val success: Boolean,
        val timestamp: Long,
        val details: Map<String, Any> = emptyMap()
    )

    /**
     * Suggested user action
     */
    data class UserAction(
        val action: String,
        val description: String,
        val priority: ActionPriority = ActionPriority.NORMAL,
        val data: Map<String, Any> = emptyMap()
    ) {

        enum class ActionPriority {
            LOW, NORMAL, HIGH, URGENT
        }
    }
}

/**
 * Strategy for recovering from errors
 */
sealed class ErrorRecoveryStrategy(
    open val strategyName: String,
    open val maxRetryAttempts: Int = 3,
    open val baseDelayMs: Long = 1000,
    open val maxDelayMs: Long = 30000,
    open val backoffMultiplier: Double = 2.0
) {

    /**
     * Retry strategy with exponential backoff
     */
    data class RetryStrategy(
        override val strategyName: String = "retry_with_backoff",
        override val maxRetryAttempts: Int = 3,
        override val baseDelayMs: Long = 1000,
        override val maxDelayMs: Long = 30000,
        override val backoffMultiplier: Double = 2.0,
        val jitterMs: Long = 100
    ) : ErrorRecoveryStrategy(strategyName, maxRetryAttempts, baseDelayMs, maxDelayMs, backoffMultiplier)

    /**
     * Fallback strategy that uses alternative implementation
     */
    data class FallbackStrategy(
        override val strategyName: String = "fallback",
        val fallbackOperation: String,
        val fallbackData: Map<String, Any> = emptyMap()
    ) : ErrorRecoveryStrategy(strategyName, 1)

    /**
     * Cache strategy that uses cached data when fresh data is unavailable
     */
    data class CacheStrategy(
        override val strategyName: String = "use_cache",
        val cacheKey: String,
        val maxCacheAgeMs: Long = 300000 // 5 minutes
    ) : ErrorRecoveryStrategy(strategyName, 1)

    /**
     * Offline strategy that queues operations for later execution
     */
    data class OfflineStrategy(
        override val strategyName: String = "queue_offline",
        val queueKey: String,
        val operationData: Map<String, Any> = emptyMap()
    ) : ErrorRecoveryStrategy(strategyName, 1)

    /**
     * User intervention strategy that requires user action
     */
    data class UserInterventionStrategy(
        override val strategyName: String = "user_intervention",
        val requiredAction: String,
        val actionDescription: String,
        val canRetryAfterAction: Boolean = true
    ) : ErrorRecoveryStrategy(strategyName, 0)

    /**
     * Circuit breaker strategy that temporarily disables functionality
     */
    data class CircuitBreakerStrategy(
        override val strategyName: String = "circuit_breaker",
        val failureThreshold: Int = 5,
        val recoveryTimeoutMs: Long = 60000, // 1 minute
        val monitoringPeriodMs: Long = 300000 // 5 minutes
    ) : ErrorRecoveryStrategy(strategyName, 0)

    /**
     * Calculate delay for retry attempt
     */
    fun calculateRetryDelay(attemptNumber: Int): Long {
        return when (this) {
            is RetryStrategy -> {
                val exponentialDelay = baseDelayMs * kotlin.math.pow(backoffMultiplier, attemptNumber.toDouble())
                val cappedDelay = exponentialDelay.toLong().coerceAtMost(maxDelayMs)
                val jitter = kotlin.random.Random.nextLong(-jitterMs, jitterMs)
                (cappedDelay + jitter).coerceAtLeast(0)
            }
            else -> baseDelayMs
        }
    }

    /**
     * Check if strategy can handle the given error
     */
    open fun canHandle(error: AppError): Boolean {
        return when (this) {
            is RetryStrategy -> error.isRecoverable()
            is FallbackStrategy -> true // Fallback can handle most errors
            is CacheStrategy -> error.errorType in listOf(
                ErrorType.NETWORK_ERROR,
                ErrorType.NETWORK_TIMEOUT,
                ErrorType.NETWORK_UNAVAILABLE
            )
            is OfflineStrategy -> error.errorType in listOf(
                ErrorType.NETWORK_ERROR,
                ErrorType.NETWORK_TIMEOUT,
                ErrorType.NETWORK_UNAVAILABLE
            )
            is UserInterventionStrategy -> error.severity >= ErrorSeverity.HIGH
            is CircuitBreakerStrategy -> error.severity >= ErrorSeverity.MEDIUM
        }
    }
}