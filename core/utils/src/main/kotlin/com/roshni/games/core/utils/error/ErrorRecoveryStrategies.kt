package com.roshni.games.core.utils.error

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Collection of specialized error recovery strategies for different scenarios
 */

/**
 * Network-specific recovery strategies
 */
object NetworkRecoveryStrategies {

    /**
     * Strategy for handling network connectivity issues
     */
    class NetworkConnectivityStrategy : ErrorRecoveryStrategy.RetryStrategy(
        strategyName = "network_connectivity",
        maxRetryAttempts = 5,
        baseDelayMs = 2000,
        maxDelayMs = 60000,
        backoffMultiplier = 1.5
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.NetworkError && when (error) {
                is AppError.NetworkError.ConnectionError -> true
                is AppError.NetworkError.TimeoutError -> true
                is AppError.NetworkError.UnavailableError -> true
                else -> false
            }
        }
    }

    /**
     * Strategy for handling authentication errors
     */
    class AuthenticationRecoveryStrategy : ErrorRecoveryStrategy.UserInterventionStrategy(
        strategyName = "authentication_recovery",
        requiredAction = "reauthenticate",
        actionDescription = "Please sign in again to continue"
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.NetworkError.AuthenticationError
        }
    }

    /**
     * Strategy for handling rate limiting
     */
    class RateLimitRecoveryStrategy : ErrorRecoveryStrategy.RetryStrategy(
        strategyName = "rate_limit_recovery",
        maxRetryAttempts = 3,
        baseDelayMs = 5000,
        maxDelayMs = 30000,
        backoffMultiplier = 2.0
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.NetworkError.RateLimitedError
        }

        override fun calculateRetryDelay(attemptNumber: Int): Long {
            val baseError = error as? AppError.NetworkError.RateLimitedError
            val retryAfter = baseError?.retryAfterMs ?: super.calculateRetryDelay(attemptNumber)
            return retryAfter.coerceAtLeast(super.calculateRetryDelay(attemptNumber))
        }
    }
}

/**
 * Gameplay-specific recovery strategies
 */
object GameplayRecoveryStrategies {

    /**
     * Strategy for handling invalid game moves
     */
    class InvalidMoveRecoveryStrategy : ErrorRecoveryStrategy.FallbackStrategy(
        strategyName = "invalid_move_recovery",
        fallbackOperation = "suggest_valid_moves",
        fallbackData = mapOf("provide_hints" to true)
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.GameplayError.InvalidMoveError
        }
    }

    /**
     * Strategy for handling game state issues
     */
    class GameStateRecoveryStrategy : ErrorRecoveryStrategy.RetryStrategy(
        strategyName = "game_state_recovery",
        maxRetryAttempts = 2,
        baseDelayMs = 1000,
        maxDelayMs = 5000
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.GameplayError.InvalidStateError
        }
    }

    /**
     * Strategy for handling save/load failures
     */
    class SaveLoadRecoveryStrategy : ErrorRecoveryStrategy.CacheStrategy(
        strategyName = "save_load_recovery",
        cacheKey = "game_backup",
        maxCacheAgeMs = 300000 // 5 minutes
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.GameplayError.SaveFailedError ||
                   error is AppError.GameplayError.LoadFailedError
        }
    }
}

/**
 * Permission-specific recovery strategies
 */
object PermissionRecoveryStrategies {

    /**
     * Strategy for handling permission denials
     */
    class PermissionDeniedRecoveryStrategy : ErrorRecoveryStrategy.UserInterventionStrategy(
        strategyName = "permission_denied_recovery",
        requiredAction = "grant_permission",
        actionDescription = "Please grant the required permission in settings"
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.PermissionError.DeniedError ||
                   error is AppError.PermissionError.NotGrantedError
        }
    }

    /**
     * Strategy for handling expired permissions
     */
    class PermissionExpiredRecoveryStrategy : ErrorRecoveryStrategy.UserInterventionStrategy(
        strategyName = "permission_expired_recovery",
        requiredAction = "refresh_permission",
        actionDescription = "Please grant permission again as it has expired"
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.PermissionError.ExpiredError
        }
    }
}

/**
 * Validation-specific recovery strategies
 */
object ValidationRecoveryStrategies {

    /**
     * Strategy for handling validation errors
     */
    class ValidationErrorRecoveryStrategy : ErrorRecoveryStrategy.FallbackStrategy(
        strategyName = "validation_recovery",
        fallbackOperation = "provide_validation_hints",
        fallbackData = mapOf("show_field_hints" to true)
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error is AppError.ValidationError
        }
    }
}

/**
 * System-specific recovery strategies
 */
object SystemRecoveryStrategies {

    /**
     * Strategy for handling out of memory errors
     */
    class OutOfMemoryRecoveryStrategy : ErrorRecoveryStrategy.CircuitBreakerStrategy(
        strategyName = "out_of_memory_recovery",
        failureThreshold = 1, // Immediate circuit breaker for OOM
        recoveryTimeoutMs = 120000, // 2 minutes
        monitoringPeriodMs = 600000 // 10 minutes
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error.errorType == ErrorType.SYSTEM_OUT_OF_MEMORY
        }
    }

    /**
     * Strategy for handling storage issues
     */
    class StorageFullRecoveryStrategy : ErrorRecoveryStrategy.UserInterventionStrategy(
        strategyName = "storage_full_recovery",
        requiredAction = "free_storage",
        actionDescription = "Please free up storage space and try again"
    ) {
        override fun canHandle(error: AppError): Boolean {
            return error.errorType == ErrorType.SYSTEM_STORAGE_FULL
        }
    }
}

/**
 * Composite recovery strategy that combines multiple strategies
 */
class CompositeRecoveryStrategy(
    override val strategyName: String,
    private val strategies: List<ErrorRecoveryStrategy>
) : ErrorRecoveryStrategy(strategyName, 1) {

    override fun canHandle(error: AppError): Boolean {
        return strategies.any { it.canHandle(error) }
    }

    override fun calculateRetryDelay(attemptNumber: Int): Long {
        val applicableStrategy = strategies.firstOrNull { it.canHandle(error) }
        return applicableStrategy?.calculateRetryDelay(attemptNumber) ?: super.calculateRetryDelay(attemptNumber)
    }
}

/**
 * Adaptive recovery strategy that learns from past errors
 */
class AdaptiveRecoveryStrategy(
    override val strategyName: String = "adaptive_recovery",
    private val learningRate: Double = 0.1,
    private val minConfidence: Double = 0.5
) : ErrorRecoveryStrategy(strategyName, 3) {

    private val strategyPerformance = mutableMapOf<String, StrategyPerformance>()

    override fun canHandle(error: AppError): Boolean {
        return true // Can handle any error type
    }

    override fun calculateRetryDelay(attemptNumber: Int): Long {
        // Adaptive delay based on past performance
        val key = "${error.errorType.name}_${error.severity.name}"
        val performance = strategyPerformance[key]

        return if (performance != null && performance.confidence >= minConfidence) {
            (performance.averageDelayMs * (1.0 + learningRate * (1.0 - performance.successRate))).toLong()
        } else {
            super.calculateRetryDelay(attemptNumber)
        }
    }

    /**
     * Update strategy performance based on results
     */
    fun updatePerformance(error: AppError, success: Boolean, delayMs: Long) {
        val key = "${error.errorType.name}_${error.severity.name}"
        val current = strategyPerformance[key] ?: StrategyPerformance()

        val newCount = current.executionCount + 1
        val newSuccessRate = (current.successRate * current.executionCount + if (success) 1.0 else 0.0) / newCount
        val newAverageDelay = (current.averageDelayMs * current.executionCount + delayMs) / newCount
        val newConfidence = minOf(1.0, current.confidence + learningRate)

        strategyPerformance[key] = StrategyPerformance(
            successRate = newSuccessRate,
            averageDelayMs = newAverageDelay,
            executionCount = newCount,
            confidence = newConfidence
        )
    }

    private data class StrategyPerformance(
        val successRate: Double = 0.0,
        val averageDelayMs: Double = 1000.0,
        val executionCount: Int = 0,
        val confidence: Double = 0.0
    )
}

/**
 * Contextual recovery strategy that considers user context
 */
class ContextualRecoveryStrategy(
    override val strategyName: String = "contextual_recovery",
    private val userContext: Map<String, Any>
) : ErrorRecoveryStrategy(strategyName, 3) {

    override fun canHandle(error: AppError): Boolean {
        return when (error.errorType) {
            ErrorType.USER_NOT_AUTHENTICATED -> {
                // Check if user was recently active
                val lastActivity = userContext["lastActivityTime"] as? Long ?: 0
                val timeSinceActivity = System.currentTimeMillis() - lastActivity
                timeSinceActivity < 300000 // 5 minutes
            }
            ErrorType.USER_ACCOUNT_SUSPENDED -> {
                // Check if user has admin override
                userContext["userRole"] == "admin"
            }
            else -> true
        }
    }

    override fun calculateRetryDelay(attemptNumber: Int): Long {
        // Adjust delay based on user context
        val baseDelay = super.calculateRetryDelay(attemptNumber)
        val userPatience = userContext["patienceLevel"] as? Double ?: 1.0
        return (baseDelay * userPatience).toLong()
    }
}

/**
 * Factory for creating common recovery strategies
 */
object ErrorRecoveryStrategyFactory {

    /**
     * Create a comprehensive set of recovery strategies for gaming applications
     */
    fun createGamingStrategies(): List<ErrorRecoveryStrategy> {
        return listOf(
            // Network strategies
            NetworkRecoveryStrategies.NetworkConnectivityStrategy(),
            NetworkRecoveryStrategies.AuthenticationRecoveryStrategy(),
            NetworkRecoveryStrategies.RateLimitRecoveryStrategy(),

            // Gameplay strategies
            GameplayRecoveryStrategies.InvalidMoveRecoveryStrategy(),
            GameplayRecoveryStrategies.GameStateRecoveryStrategy(),
            GameplayRecoveryStrategies.SaveLoadRecoveryStrategy(),

            // Permission strategies
            PermissionRecoveryStrategies.PermissionDeniedRecoveryStrategy(),
            PermissionRecoveryStrategies.PermissionExpiredRecoveryStrategy(),

            // Validation strategies
            ValidationRecoveryStrategies.ValidationErrorRecoveryStrategy(),

            // System strategies
            SystemRecoveryStrategies.OutOfMemoryRecoveryStrategy(),
            SystemRecoveryStrategies.StorageFullRecoveryStrategy(),

            // Advanced strategies
            AdaptiveRecoveryStrategy(),
            CompositeRecoveryStrategy(
                "comprehensive_fallback",
                listOf(
                    ErrorRecoveryStrategy.CacheStrategy("emergency_cache"),
                    ErrorRecoveryStrategy.OfflineStrategy("emergency_offline")
                )
            )
        )
    }

    /**
     * Create minimal recovery strategies for lightweight applications
     */
    fun createMinimalStrategies(): List<ErrorRecoveryStrategy> {
        return listOf(
            ErrorRecoveryStrategy.RetryStrategy(maxRetryAttempts = 2),
            ErrorRecoveryStrategy.FallbackStrategy("minimal_fallback"),
            ErrorRecoveryStrategy.CacheStrategy("minimal_cache")
        )
    }

    /**
     * Create network-focused recovery strategies
     */
    fun createNetworkStrategies(): List<ErrorRecoveryStrategy> {
        return listOf(
            NetworkRecoveryStrategies.NetworkConnectivityStrategy(),
            NetworkRecoveryStrategies.AuthenticationRecoveryStrategy(),
            NetworkRecoveryStrategies.RateLimitRecoveryStrategy(),
            ErrorRecoveryStrategy.CacheStrategy("network_cache"),
            ErrorRecoveryStrategy.OfflineStrategy("network_offline")
        )
    }
}