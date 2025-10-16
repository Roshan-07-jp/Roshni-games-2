package com.roshni.games.core.navigation.controller

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.roshni.games.core.navigation.model.NavigationContext
import com.roshni.games.core.navigation.model.NavigationEvent
import com.roshni.games.core.navigation.model.NavigationResult
import com.roshni.games.core.navigation.rules.NavigationRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for the intelligent navigation flow controller
 * Manages navigation rules, context, and optimal routing
 */
interface NavigationFlowController {

    /**
     * Current navigation state
     */
    val navigationState: StateFlow<NavigationControllerState>

    /**
     * All registered navigation rules
     */
    val registeredRules: StateFlow<List<NavigationRule>>

    /**
     * Navigation event bus for observing navigation events
     */
    val eventBus: com.roshni.games.core.navigation.model.NavigationEventBus

    /**
     * Performance statistics for navigation operations
     */
    val performanceStats: StateFlow<NavigationPerformanceStats>

    /**
     * Initialize the navigation flow controller
     *
     * @param navController The NavController instance to use for navigation
     * @param initialContext Initial navigation context
     * @return true if initialization was successful
     */
    suspend fun initialize(
        navController: NavController,
        initialContext: NavigationContext? = null
    ): Boolean

    /**
     * Navigate to a destination with intelligent routing
     *
     * @param destination The target destination
     * @param arguments Navigation arguments
     * @param navOptions Navigation options (optional)
     * @param context Additional navigation context (optional)
     * @return Navigation result
     */
    suspend fun navigate(
        destination: String,
        arguments: Map<String, Any> = emptyMap(),
        navOptions: NavOptions? = null,
        context: NavigationContext? = null
    ): NavigationResult

    /**
     * Navigate with a pre-built navigation context
     *
     * @param context Complete navigation context
     * @param navOptions Navigation options (optional)
     * @return Navigation result
     */
    suspend fun navigateWithContext(
        context: NavigationContext,
        navOptions: NavOptions? = null
    ): NavigationResult

    /**
     * Calculate the optimal route to a destination
     *
     * @param destination Target destination
     * @param context Navigation context
     * @return Optimal route information
     */
    suspend fun calculateOptimalRoute(
        destination: String,
        context: NavigationContext
    ): OptimalRoute

    /**
     * Register a navigation rule
     *
     * @param rule The rule to register
     * @return true if registration was successful
     */
    suspend fun registerRule(rule: NavigationRule): Boolean

    /**
     * Unregister a navigation rule
     *
     * @param ruleId ID of the rule to unregister
     * @return true if unregistration was successful
     */
    suspend fun unregisterRule(ruleId: String): Boolean

    /**
     * Get a registered rule by ID
     *
     * @param ruleId The rule ID to look for
     * @return The rule if found, null otherwise
     */
    suspend fun getRule(ruleId: String): NavigationRule?

    /**
     * Enable a navigation rule
     *
     * @param ruleId ID of the rule to enable
     * @return true if the rule was enabled successfully
     */
    suspend fun enableRule(ruleId: String): Boolean

    /**
     * Disable a navigation rule
     *
     * @param ruleId ID of the rule to disable
     * @return true if the rule was disabled successfully
     */
    suspend fun disableRule(ruleId: String): Boolean

    /**
     * Update navigation context
     *
     * @param contextUpdater Function to update the current context
     * @return Updated navigation context
     */
    suspend fun updateContext(contextUpdater: (NavigationContext) -> NavigationContext): NavigationContext

    /**
     * Get current navigation context
     *
     * @return Current navigation context, null if not initialized
     */
    suspend fun getCurrentContext(): NavigationContext?

    /**
     * Validate all registered rules
     *
     * @return Validation result
     */
    suspend fun validateRules(): NavigationValidationResult

    /**
     * Get navigation statistics
     *
     * @return Current navigation statistics
     */
    suspend fun getStatistics(): NavigationStatistics

    /**
     * Clear navigation statistics
     */
    suspend fun clearStatistics()

    /**
     * Observe navigation events
     *
     * @return Flow of navigation events
     */
    fun observeEvents(): Flow<NavigationEvent>

    /**
     * Observe navigation state changes
     *
     * @return Flow of navigation state changes
     */
    fun observeStateChanges(): Flow<NavigationStateChange>

    /**
     * Shutdown the navigation flow controller
     */
    suspend fun shutdown()

    /**
     * Check if navigation is allowed to a destination
     *
     * @param destination Target destination
     * @param context Navigation context
     * @return true if navigation is allowed
     */
    suspend fun canNavigateTo(destination: String, context: NavigationContext? = null): Boolean

    /**
     * Get suggested alternative destinations if navigation is blocked
     *
     * @param destination Blocked destination
     * @param context Navigation context
     * @return List of suggested alternatives
     */
    suspend fun getAlternativeDestinations(destination: String, context: NavigationContext? = null): List<String>

    /**
     * Preload navigation data for better performance
     *
     * @param destinations List of destinations to preload
     */
    suspend fun preloadNavigationData(destinations: List<String>)

    /**
     * Optimize navigation graph based on usage patterns
     *
     * @return Optimization result
     */
    suspend fun optimizeNavigationGraph(): NavigationOptimizationResult
}

/**
 * State of the navigation controller
 */
data class NavigationControllerState(
    val isInitialized: Boolean = false,
    val isNavigating: Boolean = false,
    val currentDestination: String? = null,
    val ruleCount: Int = 0,
    val enabledRuleCount: Int = 0,
    val lastActivityTime: Long? = null,
    val errorCount: Long = 0,
    val isShuttingDown: Boolean = false
)

/**
 * Performance statistics for navigation operations
 */
data class NavigationPerformanceStats(
    val totalNavigations: Long = 0,
    val successfulNavigations: Long = 0,
    val failedNavigations: Long = 0,
    val averageNavigationTimeMs: Double = 0.0,
    val totalNavigationTimeMs: Long = 0,
    val ruleEvaluationTimeMs: Long = 0,
    val routeOptimizationTimeMs: Long = 0,
    val cacheHitRate: Double = 0.0,
    val lastNavigationTime: Long? = null
)

/**
 * Optimal route information
 */
data class OptimalRoute(
    val destination: String,
    val route: List<String>, // Path of destinations
    val estimatedTimeMs: Long,
    val confidence: Double, // 0.0 to 1.0
    val appliedOptimizations: List<String>,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Validation result for navigation rules
 */
data class NavigationValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val ruleValidationResults: Map<String, RuleValidationResult> = emptyMap()
)

/**
 * Comprehensive navigation statistics
 */
data class NavigationStatistics(
    val totalNavigations: Long = 0,
    val successfulNavigations: Long = 0,
    val failedNavigations: Long = 0,
    val cancelledNavigations: Long = 0,
    val averageNavigationTimeMs: Double = 0.0,
    val ruleStatistics: Map<String, RuleStatistics> = emptyMap(),
    val destinationStatistics: Map<String, DestinationStats> = emptyMap(),
    val timeRangeStatistics: Map<String, TimeRangeStats> = emptyMap(),
    val errorStatistics: Map<String, Long> = emptyMap()
) {

    data class DestinationStats(
        val navigationCount: Long = 0,
        val successRate: Double = 0.0,
        val averageTimeMs: Double = 0.0,
        val blockCount: Long = 0
    )

    data class TimeRangeStats(
        val navigationCount: Long = 0,
        val successRate: Double = 0.0,
        val averageTimeMs: Double = 0.0
    )
}

/**
 * Navigation state change event
 */
data class NavigationStateChange(
    val oldState: NavigationControllerState,
    val newState: NavigationControllerState,
    val reason: String,
    val timestamp: Long
)

/**
 * Result of navigation optimization
 */
data class NavigationOptimizationResult(
    val optimizationsApplied: List<String>,
    val performanceImprovementMs: Long,
    val memoryReductionKb: Long,
    val cacheHitRateImprovement: Double,
    val timestamp: Long
)
