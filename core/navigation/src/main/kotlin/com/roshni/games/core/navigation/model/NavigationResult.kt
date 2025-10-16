package com.roshni.games.core.navigation.model

import androidx.navigation.NavController
import androidx.navigation.NavOptions

/**
 * Result of a navigation operation
 */
sealed class NavigationResult {
    /**
     * Successful navigation result
     */
    data class Success(
        /**
         * The destination that was navigated to
         */
        val destination: String,

        /**
         * Navigation options that were used
         */
        val navOptions: NavOptions? = null,

        /**
         * Arguments that were passed during navigation
         */
        val arguments: Map<String, Any> = emptyMap(),

        /**
         * Time taken for navigation in milliseconds
         */
        val navigationTimeMs: Long,

        /**
         * Route that was actually taken (may differ from requested due to rules/optimization)
         */
        val actualRoute: String,

        /**
         * Rules that were applied during navigation
         */
        val appliedRules: List<String> = emptyList(),

        /**
         * Timestamp when navigation completed
         */
        val timestamp: Long = System.currentTimeMillis(),

        /**
         * Unique identifier for this navigation result
         */
        val resultId: String = "nav_result_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationResult() {

        /**
         * Check if navigation was instantaneous (cached or immediate)
         */
        val isInstantaneous: Boolean get() = navigationTimeMs < 10

        /**
         * Check if any rules were applied
         */
        val hasAppliedRules: Boolean get() = appliedRules.isNotEmpty()

        /**
         * Get the navigation efficiency score (lower time is better)
         */
        fun getEfficiencyScore(): Double {
            return when {
                navigationTimeMs < 50 -> 1.0 // Excellent
                navigationTimeMs < 100 -> 0.8 // Good
                navigationTimeMs < 200 -> 0.6 // Fair
                navigationTimeMs < 500 -> 0.4 // Poor
                else -> 0.2 // Very poor
            }
        }
    }

    /**
     * Failed navigation result
     */
    data class Failure(
        /**
         * The destination that was attempted
         */
        val attemptedDestination: String,

        /**
         * Reason for navigation failure
         */
        val reason: NavigationFailureReason,

        /**
         * Detailed error message
         */
        val errorMessage: String,

        /**
         * Exception that caused the failure (if any)
         */
        val exception: Exception? = null,

        /**
         * Suggested alternative destinations
         */
        val suggestedAlternatives: List<String> = emptyList(),

        /**
         * Rules that blocked the navigation
         */
        val blockingRules: List<String> = emptyList(),

        /**
         * Time taken before failure in milliseconds
         */
        val timeToFailureMs: Long,

        /**
         * Timestamp when navigation failed
         */
        val timestamp: Long = System.currentTimeMillis(),

        /**
         * Unique identifier for this navigation result
         */
        val resultId: String = "nav_failure_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationResult() {

        /**
         * Check if failure was due to rule blocking
         */
        val isBlockedByRules: Boolean get() = blockingRules.isNotEmpty()

        /**
         * Check if there are suggested alternatives
         */
        val hasAlternatives: Boolean get() = suggestedAlternatives.isNotEmpty()

        /**
         * Get severity level of the failure
         */
        fun getSeverityLevel(): FailureSeverity {
            return when (reason) {
                NavigationFailureReason.PERMISSION_DENIED,
                NavigationFailureReason.FEATURE_NOT_ENABLED -> FailureSeverity.HIGH
                NavigationFailureReason.INVALID_DESTINATION -> FailureSeverity.MEDIUM
                NavigationFailureReason.NETWORK_ERROR,
                NavigationFailureReason.DEVICE_INCOMPATIBLE -> FailureSeverity.LOW
                else -> FailureSeverity.MEDIUM
            }
        }
    }

    /**
     * Cancelled navigation result
     */
    data class Cancelled(
        /**
         * The destination that was cancelled
         */
        val destination: String,

        /**
         * Reason for cancellation
         */
        val reason: NavigationCancellationReason,

        /**
         * Time spent before cancellation in milliseconds
         */
        val timeSpentMs: Long,

        /**
         * Timestamp when navigation was cancelled
         */
        val timestamp: Long = System.currentTimeMillis(),

        /**
         * Unique identifier for this navigation result
         */
        val resultId: String = "nav_cancelled_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationResult()
}

/**
 * Reasons why navigation might fail
 */
enum class NavigationFailureReason {
    PERMISSION_DENIED,
    FEATURE_NOT_ENABLED,
    INVALID_DESTINATION,
    NETWORK_ERROR,
    DEVICE_INCOMPATIBLE,
    USER_NOT_AUTHENTICATED,
    PARENTAL_CONTROLS_BLOCKED,
    MAINTENANCE_MODE,
    NAVIGATION_NOT_ALLOWED,
    DEPENDENCY_MISSING,
    RULE_VIOLATION,
    UNKNOWN_ERROR
}

/**
 * Reasons why navigation might be cancelled
 */
enum class NavigationCancellationReason {
    USER_CANCELLED,
    HIGHER_PRIORITY_NAVIGATION,
    APP_STATE_CHANGED,
    NETWORK_LOST,
    BACKGROUND_NAVIGATION,
    TIMEOUT
}

/**
 * Severity levels for navigation failures
 */
enum class FailureSeverity {
    LOW,    // Minor issues, user can likely continue
    MEDIUM, // Moderate issues, may need user intervention
    HIGH    // Critical issues, navigation completely blocked
}

/**
 * Utility functions for working with NavigationResult
 */
object NavigationResultUtils {

    /**
     * Check if result is successful
     */
    fun NavigationResult.isSuccess(): Boolean = this is NavigationResult.Success

    /**
     * Check if result is a failure
     */
    fun NavigationResult.isFailure(): Boolean = this is NavigationResult.Failure

    /**
     * Check if result is cancelled
     */
    fun NavigationResult.isCancelled(): Boolean = this is NavigationResult.Cancelled

    /**
     * Get success result or null
     */
    fun NavigationResult.getSuccessOrNull(): NavigationResult.Success? = this as? NavigationResult.Success

    /**
     * Get failure result or null
     */
    fun NavigationResult.getFailureOrNull(): NavigationResult.Failure? = this as? NavigationResult.Failure

    /**
     * Get cancelled result or null
     */
    fun NavigationResult.getCancelledOrNull(): NavigationResult.Cancelled? = this as? NavigationResult.Cancelled

    /**
     * Execute action based on result type
     */
    inline fun NavigationResult.onSuccess(action: (NavigationResult.Success) -> Unit): NavigationResult {
        if (this is NavigationResult.Success) {
            action(this)
        }
        return this
    }

    inline fun NavigationResult.onFailure(action: (NavigationResult.Failure) -> Unit): NavigationResult {
        if (this is NavigationResult.Failure) {
            action(this)
        }
        return this
    }

    inline fun NavigationResult.onCancelled(action: (NavigationResult.Cancelled) -> Unit): NavigationResult {
        if (this is NavigationResult.Cancelled) {
            action(this)
        }
        return this
    }
}