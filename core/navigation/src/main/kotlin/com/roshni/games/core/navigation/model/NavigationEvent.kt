package com.roshni.games.core.navigation.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Events that can occur during navigation
 */
sealed class NavigationEvent {

    /**
     * Event fired when navigation starts
     */
    data class NavigationStarted(
        val context: NavigationContext,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "nav_start_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when navigation completes successfully
     */
    data class NavigationCompleted(
        val result: NavigationResult.Success,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "nav_complete_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when navigation fails
     */
    data class NavigationFailed(
        val result: NavigationResult.Failure,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "nav_failed_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when navigation is cancelled
     */
    data class NavigationCancelled(
        val result: NavigationResult.Cancelled,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "nav_cancelled_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when destination changes
     */
    data class DestinationChanged(
        val previousDestination: String,
        val newDestination: String,
        val reason: DestinationChangeReason,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "dest_change_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when navigation state changes
     */
    data class NavigationStateChanged(
        val oldState: NavigationState,
        val newState: NavigationState,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "nav_state_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when a rule is evaluated during navigation
     */
    data class RuleEvaluated(
        val ruleName: String,
        val rulePassed: Boolean,
        val context: NavigationContext,
        val executionTimeMs: Long,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "rule_eval_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when route optimization occurs
     */
    data class RouteOptimized(
        val originalRoute: String,
        val optimizedRoute: String,
        val optimizationReason: String,
        val context: NavigationContext,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "route_opt_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()

    /**
     * Event fired when navigation performance is measured
     */
    data class PerformanceMeasured(
        val navigationTimeMs: Long,
        val memoryUsageKb: Long,
        val cpuUsagePercent: Float,
        val context: NavigationContext,
        val timestamp: Long = System.currentTimeMillis(),
        val eventId: String = "perf_measure_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
    ) : NavigationEvent()
}

/**
 * Reasons why destination might change
 */
enum class DestinationChangeReason {
    USER_ACTION,
    RULE_REDIRECT,
    FEATURE_GATE,
    PERMISSION_CHANGE,
    OPTIMIZATION,
    ERROR_RECOVERY,
    DEEP_LINK,
    NOTIFICATION
}

/**
 * Navigation states for state machine
 */
enum class NavigationState {
    IDLE,
    NAVIGATING,
    PROCESSING_RULES,
    OPTIMIZING_ROUTE,
    EXECUTING_NAVIGATION,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Navigation event bus for observing navigation events across the app
 */
class NavigationEventBus {

    private val _events = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<NavigationEvent> = _events.asSharedFlow()

    /**
     * Emit a navigation event
     */
    suspend fun emit(event: NavigationEvent) {
        _events.emit(event)
    }

    /**
     * Emit navigation started event
     */
    suspend fun emitNavigationStarted(context: NavigationContext) {
        emit(NavigationEvent.NavigationStarted(context))
    }

    /**
     * Emit navigation completed event
     */
    suspend fun emitNavigationCompleted(result: NavigationResult.Success) {
        emit(NavigationEvent.NavigationCompleted(result))
    }

    /**
     * Emit navigation failed event
     */
    suspend fun emitNavigationFailed(result: NavigationResult.Failure) {
        emit(NavigationEvent.NavigationFailed(result))
    }

    /**
     * Emit navigation cancelled event
     */
    suspend fun emitNavigationCancelled(result: NavigationResult.Cancelled) {
        emit(NavigationEvent.NavigationCancelled(result))
    }

    /**
     * Emit destination changed event
     */
    suspend fun emitDestinationChanged(
        previousDestination: String,
        newDestination: String,
        reason: DestinationChangeReason
    ) {
        emit(NavigationEvent.DestinationChanged(previousDestination, newDestination, reason))
    }

    /**
     * Emit navigation state changed event
     */
    suspend fun emitNavigationStateChanged(
        oldState: NavigationState,
        newState: NavigationState,
        reason: String? = null
    ) {
        emit(NavigationEvent.NavigationStateChanged(oldState, newState, reason))
    }

    /**
     * Emit rule evaluated event
     */
    suspend fun emitRuleEvaluated(
        ruleName: String,
        rulePassed: Boolean,
        context: NavigationContext,
        executionTimeMs: Long
    ) {
        emit(NavigationEvent.RuleEvaluated(ruleName, rulePassed, context, executionTimeMs))
    }

    /**
     * Emit route optimized event
     */
    suspend fun emitRouteOptimized(
        originalRoute: String,
        optimizedRoute: String,
        optimizationReason: String,
        context: NavigationContext
    ) {
        emit(NavigationEvent.RouteOptimized(originalRoute, optimizedRoute, optimizationReason, context))
    }

    /**
     * Emit performance measured event
     */
    suspend fun emitPerformanceMeasured(
        navigationTimeMs: Long,
        memoryUsageKb: Long,
        cpuUsagePercent: Float,
        context: NavigationContext
    ) {
        emit(NavigationEvent.PerformanceMeasured(navigationTimeMs, memoryUsageKb, cpuUsagePercent, context))
    }
}

/**
 * Utility functions for working with NavigationEvent
 */
object NavigationEventUtils {

    /**
     * Get event type as string for logging
     */
    fun NavigationEvent.getEventType(): String = when (this) {
        is NavigationEvent.NavigationStarted -> "NAVIGATION_STARTED"
        is NavigationEvent.NavigationCompleted -> "NAVIGATION_COMPLETED"
        is NavigationEvent.NavigationFailed -> "NAVIGATION_FAILED"
        is NavigationEvent.NavigationCancelled -> "NAVIGATION_CANCELLED"
        is NavigationEvent.DestinationChanged -> "DESTINATION_CHANGED"
        is NavigationEvent.NavigationStateChanged -> "NAVIGATION_STATE_CHANGED"
        is NavigationEvent.RuleEvaluated -> "RULE_EVALUATED"
        is NavigationEvent.RouteOptimized -> "ROUTE_OPTIMIZED"
        is NavigationEvent.PerformanceMeasured -> "PERFORMANCE_MEASURED"
    }

    /**
     * Check if event indicates navigation success
     */
    fun NavigationEvent.isSuccessEvent(): Boolean = this is NavigationEvent.NavigationCompleted

    /**
     * Check if event indicates navigation failure
     */
    fun NavigationEvent.isFailureEvent(): Boolean = this is NavigationEvent.NavigationFailed

    /**
     * Check if event indicates navigation cancellation
     */
    fun NavigationEvent.isCancellationEvent(): Boolean = this is NavigationEvent.NavigationCancelled

    /**
     * Get associated context from event if available
     */
    fun NavigationEvent.getContext(): NavigationContext? = when (this) {
        is NavigationEvent.NavigationStarted -> this.context
        is NavigationEvent.NavigationCompleted -> null // Would need to be stored in result
        is NavigationEvent.NavigationFailed -> null // Would need to be stored in result
        is NavigationEvent.NavigationCancelled -> null // Would need to be stored in result
        is NavigationEvent.DestinationChanged -> null
        is NavigationEvent.NavigationStateChanged -> null
        is NavigationEvent.RuleEvaluated -> this.context
        is NavigationEvent.RouteOptimized -> this.context
        is NavigationEvent.PerformanceMeasured -> this.context
    }

    /**
     * Get timestamp from event
     */
    fun NavigationEvent.getTimestamp(): Long = when (this) {
        is NavigationEvent.NavigationStarted -> this.timestamp
        is NavigationEvent.NavigationCompleted -> this.timestamp
        is NavigationEvent.NavigationFailed -> this.timestamp
        is NavigationEvent.NavigationCancelled -> this.timestamp
        is NavigationEvent.DestinationChanged -> this.timestamp
        is NavigationEvent.NavigationStateChanged -> this.timestamp
        is NavigationEvent.RuleEvaluated -> this.timestamp
        is NavigationEvent.RouteOptimized -> this.timestamp
        is NavigationEvent.PerformanceMeasured -> this.timestamp
    }
}