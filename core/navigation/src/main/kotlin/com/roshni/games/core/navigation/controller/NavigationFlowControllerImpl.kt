package com.roshni.games.core.navigation.controller

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.roshni.games.core.navigation.NavigationDestinations
import com.roshni.games.core.navigation.model.NavigationContext
import com.roshni.games.core.navigation.model.NavigationEvent
import com.roshni.games.core.navigation.model.NavigationEventBus
import com.roshni.games.core.navigation.model.NavigationResult
import com.roshni.games.core.navigation.model.NavigationState
import com.roshni.games.core.navigation.rules.NavigationRule
import com.roshni.games.core.navigation.rules.RuleCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the NavigationFlowController
 */
@Singleton
class NavigationFlowControllerImpl @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val eventBus: NavigationEventBus
) : NavigationFlowController {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val mutex = Mutex()

    private val _navigationState = MutableStateFlow(NavigationControllerState())
    override val navigationState: StateFlow<NavigationControllerState> = _navigationState.asStateFlow()

    private val _registeredRules = MutableStateFlow<List<NavigationRule>>(emptyList())
    override val registeredRules: StateFlow<List<NavigationRule>> = _registeredRules.asStateFlow()

    private val _performanceStats = MutableStateFlow(NavigationPerformanceStats())
    override val performanceStats: StateFlow<NavigationPerformanceStats> = _performanceStats.asStateFlow()

    override val eventBus: NavigationEventBus get() = this@NavigationFlowControllerImpl.eventBus

    private var navController: NavController? = null
    private var currentContext: NavigationContext? = null

    // Navigation graph representation for route optimization
    private val navigationGraph = mutableMapOf<String, MutableSet<String>>()
    private val navigationWeights = mutableMapOf<Pair<String, String>, Double>()

    // Cache for rule evaluation results
    private val ruleCache = mutableMapOf<String, Pair<com.roshni.games.core.navigation.rules.RuleEvaluationResult, Long>>()
    private val routeCache = mutableMapOf<String, Pair<OptimalRoute, Long>>()

    init {
        // Observe navigation events for statistics
        scope.launch {
            eventBus.events.collect { event ->
                handleNavigationEvent(event)
            }
        }

        // Build initial navigation graph
        buildNavigationGraph()
    }

    override suspend fun initialize(
        navController: NavController,
        initialContext: NavigationContext?
    ): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing NavigationFlowController")

            this.navController = navController
            this.currentContext = initialContext ?: createDefaultContext()

            // Update state
            _navigationState.value = _navigationState.value.copy(
                isInitialized = true,
                currentDestination = currentContext?.currentDestination,
                lastActivityTime = System.currentTimeMillis()
            )

            // Emit initialization event
            eventBus.emitNavigationStarted(currentContext!!)

            Timber.d("NavigationFlowController initialized successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NavigationFlowController")
            _navigationState.value = _navigationState.value.copy(
                errorCount = _navigationState.value.errorCount + 1
            )
            false
        }
    }

    override suspend fun navigate(
        destination: String,
        arguments: Map<String, Any>,
        navOptions: NavOptions?,
        context: NavigationContext?
    ): NavigationResult = mutex.withLock {
        val startTime = System.currentTimeMillis()

        try {
            // Use provided context or merge with current context
            val navigationContext = context?.let { mergeContexts(currentContext, it) }
                ?: currentContext?.copy(targetDestination = destination, arguments = arguments)
                ?: createContextForDestination(destination, arguments)

            // Emit navigation started event
            eventBus.emitNavigationStarted(navigationContext)

            // Update state
            _navigationState.value = _navigationState.value.copy(
                isNavigating = true,
                currentDestination = navigationContext.currentDestination,
                lastActivityTime = System.currentTimeMillis()
            )

            // Evaluate navigation rules
            val ruleResults = evaluateNavigationRules(navigationContext)

            // Check if any rules block navigation
            val blockingRules = ruleResults.filter { !it.passed }
            if (blockingRules.isNotEmpty()) {
                val failureResult = NavigationResult.Failure(
                    attemptedDestination = destination,
                    reason = NavigationResult.NavigationFailureReason.RULE_VIOLATION,
                    errorMessage = "Navigation blocked by rules: ${blockingRules.joinToString { "Rule ${it.executionTimeMs}ms" }}",
                    blockingRules = blockingRules.map { "Rule ${it.executionTimeMs}ms" },
                    suggestedAlternatives = getSuggestedAlternatives(navigationContext, blockingRules),
                    timeToFailureMs = System.currentTimeMillis() - startTime
                )

                // Emit failure event
                eventBus.emitNavigationFailed(failureResult)

                // Update statistics
                updatePerformanceStats(false, System.currentTimeMillis() - startTime)

                return failureResult
            }

            // Calculate optimal route
            val optimalRoute = calculateOptimalRouteInternal(destination, navigationContext)

            // Perform navigation
            val navigationResult = performNavigation(optimalRoute, navOptions, navigationContext)

            // Update current context
            currentContext = navigationContext.copy(
                currentDestination = navigationResult.destination,
                timestamp = System.currentTimeMillis()
            )

            // Update state
            _navigationState.value = _navigationState.value.copy(
                isNavigating = false,
                currentDestination = navigationResult.destination,
                lastActivityTime = System.currentTimeMillis()
            )

            // Update performance statistics
            updatePerformanceStats(navigationResult.isSuccess(), System.currentTimeMillis() - startTime)

            return navigationResult

        } catch (e: Exception) {
            Timber.e(e, "Navigation failed for destination: $destination")

            val failureResult = NavigationResult.Failure(
                attemptedDestination = destination,
                reason = NavigationResult.NavigationFailureReason.UNKNOWN_ERROR,
                errorMessage = "Navigation failed: ${e.message}",
                exception = e,
                timeToFailureMs = System.currentTimeMillis() - startTime
            )

            // Emit failure event
            eventBus.emitNavigationFailed(failureResult)

            // Update statistics
            updatePerformanceStats(false, System.currentTimeMillis() - startTime)

            failureResult
        }
    }

    override suspend fun navigateWithContext(
        context: NavigationContext,
        navOptions: NavOptions?
    ): NavigationResult {
        return navigate(
            destination = context.targetDestination,
            arguments = context.arguments,
            navOptions = navOptions,
            context = context
        )
    }

    override suspend fun calculateOptimalRoute(
        destination: String,
        context: NavigationContext
    ): OptimalRoute = withContext(dispatcher) {
        val cacheKey = "route_${context.currentDestination}_$destination"

        // Check cache first
        val cached = routeCache[cacheKey]
        if (cached != null && isCacheValid(cached.second)) {
            return@withContext cached.first
        }

        val startTime = System.currentTimeMillis()

        // Use A* algorithm for optimal route calculation
        val route = calculateOptimalRouteAStar(context.currentDestination, destination, context)

        val optimalRoute = OptimalRoute(
            destination = destination,
            route = route,
            estimatedTimeMs = estimateRouteTime(route),
            confidence = calculateRouteConfidence(route, context),
            appliedOptimizations = getAppliedOptimizations(route, context),
            metadata = mapOf(
                "calculation_time_ms" to (System.currentTimeMillis() - startTime),
                "algorithm" to "astar",
                "nodes_visited" to route.size
            )
        )

        // Cache the result
        routeCache[cacheKey] = Pair(optimalRoute, System.currentTimeMillis())

        optimalRoute
    }

    override suspend fun registerRule(rule: NavigationRule): Boolean = mutex.withLock {
        try {
            // Check if rule is already registered
            if (_registeredRules.value.any { it.id == rule.id }) {
                Timber.w("Rule ${rule.id} is already registered")
                return false
            }

            // Validate rule
            val validation = rule.validateConfig()
            if (!validation.isValid) {
                Timber.e("Rule ${rule.id} validation failed: ${validation.errors}")
                return false
            }

            // Add to registered rules
            val updatedRules = _registeredRules.value + rule
            _registeredRules.value = updatedRules

            // Update state
            _navigationState.value = _navigationState.value.copy(
                ruleCount = updatedRules.size,
                enabledRuleCount = updatedRules.count { it.isEnabled.value },
                lastActivityTime = System.currentTimeMillis()
            )

            Timber.d("Rule ${rule.id} registered successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to register rule ${rule.id}")
            false
        }
    }

    override suspend fun unregisterRule(ruleId: String): Boolean = mutex.withLock {
        try {
            val rule = getRule(ruleId) ?: return false

            // Remove from registered rules
            val updatedRules = _registeredRules.value.filter { it.id != ruleId }
            _registeredRules.value = updatedRules

            // Cleanup rule
            rule.reset()

            // Update state
            _navigationState.value = _navigationState.value.copy(
                ruleCount = updatedRules.size,
                enabledRuleCount = updatedRules.count { it.isEnabled.value },
                lastActivityTime = System.currentTimeMillis()
            )

            Timber.d("Rule $ruleId unregistered successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister rule $ruleId")
            false
        }
    }

    override suspend fun getRule(ruleId: String): NavigationRule? {
        return _registeredRules.value.find { it.id == ruleId }
    }

    override suspend fun enableRule(ruleId: String): Boolean = mutex.withLock {
        val rule = getRule(ruleId) ?: return false
        rule.enable()

        // Update state
        _navigationState.value = _navigationState.value.copy(
            enabledRuleCount = _registeredRules.value.count { it.isEnabled.value },
            lastActivityTime = System.currentTimeMillis()
        )

        Timber.d("Rule $ruleId enabled successfully")
        true
    }

    override suspend fun disableRule(ruleId: String): Boolean = mutex.withLock {
        val rule = getRule(ruleId) ?: return false
        rule.disable()

        // Update state
        _navigationState.value = _navigationState.value.copy(
            enabledRuleCount = _registeredRules.value.count { it.isEnabled.value },
            lastActivityTime = System.currentTimeMillis()
        )

        Timber.d("Rule $ruleId disabled successfully")
        true
    }

    override suspend fun updateContext(contextUpdater: (NavigationContext) -> NavigationContext): NavigationContext = mutex.withLock {
        currentContext?.let { current ->
            val updated = contextUpdater(current)
            currentContext = updated
            updated
        } ?: throw IllegalStateException("NavigationFlowController not initialized")
    }

    override suspend fun getCurrentContext(): NavigationContext? = currentContext

    override suspend fun validateRules(): NavigationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val ruleValidationResults = mutableMapOf<String, com.roshni.games.core.navigation.rules.RuleValidationResult>()

        for (rule in _registeredRules.value) {
            try {
                val validation = rule.validateConfig()
                ruleValidationResults[rule.id] = validation

                if (!validation.isValid) {
                    errors.add("Rule ${rule.id} validation failed: ${validation.errors}")
                }
                warnings.addAll(validation.warnings)

            } catch (e: Exception) {
                errors.add("Rule ${rule.id} validation threw exception: ${e.message}")
            }
        }

        return NavigationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            ruleValidationResults = ruleValidationResults
        )
    }

    override suspend fun getStatistics(): NavigationStatistics {
        val ruleStats = _registeredRules.value.associate { rule ->
            rule.id to rule.getStatistics()
        }

        // Calculate destination statistics
        val destinationStats = mutableMapOf<String, NavigationStatistics.DestinationStats>()

        // This would be populated from actual navigation data in a real implementation

        return NavigationStatistics(
            totalNavigations = _performanceStats.value.totalNavigations,
            successfulNavigations = _performanceStats.value.successfulNavigations,
            failedNavigations = _performanceStats.value.failedNavigations,
            ruleStatistics = ruleStats,
            destinationStatistics = destinationStats
        )
    }

    override suspend fun clearStatistics() {
        _performanceStats.value = NavigationPerformanceStats()
        _registeredRules.value.forEach { it.reset() }
        ruleCache.clear()
        routeCache.clear()
    }

    override fun observeEvents(): Flow<NavigationEvent> = eventBus.events

    override fun observeStateChanges(): Flow<NavigationStateChange> = flow {
        // This would be implemented with proper state observation
        // For now, return empty flow
    }

    override suspend fun shutdown() = mutex.withLock {
        try {
            Timber.d("Shutting down NavigationFlowController")

            _navigationState.value = _navigationState.value.copy(isShuttingDown = true)

            // Cleanup all rules
            _registeredRules.value.forEach { rule ->
                try {
                    rule.reset()
                } catch (e: Exception) {
                    Timber.e(e, "Error cleaning up rule ${rule.id}")
                }
            }

            _registeredRules.value = emptyList()
            navController = null
            currentContext = null
            ruleCache.clear()
            routeCache.clear()

            _navigationState.value = NavigationControllerState()

            Timber.d("NavigationFlowController shutdown complete")

        } catch (e: Exception) {
            Timber.e(e, "Error during NavigationFlowController shutdown")
        }
    }

    override suspend fun canNavigateTo(destination: String, context: NavigationContext?): Boolean {
        val navigationContext = context ?: currentContext ?: return false

        val ruleResults = evaluateNavigationRules(
            navigationContext.copy(targetDestination = destination)
        )

        return ruleResults.all { it.passed }
    }

    override suspend fun getAlternativeDestinations(destination: String, context: NavigationContext?): List<String> {
        val navigationContext = context ?: currentContext ?: return emptyList()

        val ruleResults = evaluateNavigationRules(
            navigationContext.copy(targetDestination = destination)
        )

        val blockingRules = ruleResults.filter { !it.passed }
        return getSuggestedAlternatives(navigationContext, blockingRules)
    }

    override suspend fun preloadNavigationData(destinations: List<String>) {
        // Pre-calculate routes for better performance
        destinations.forEach { destination ->
            currentContext?.let { context ->
                try {
                    calculateOptimalRoute(destination, context)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to preload route for destination: $destination")
                }
            }
        }
    }

    override suspend fun optimizeNavigationGraph(): NavigationOptimizationResult {
        val startTime = System.currentTimeMillis()

        // Analyze navigation patterns and optimize graph
        val optimizations = mutableListOf<String>()
        var performanceImprovement = 0L
        var memoryReduction = 0L

        // Remove unused navigation paths
        val usedPaths = navigationWeights.filter { it.value > 0.1 }
        val removedPaths = navigationWeights.size - usedPaths.size
        if (removedPaths > 0) {
            navigationWeights.clear()
            navigationWeights.putAll(usedPaths)
            optimizations.add("Removed $removedPaths unused navigation paths")
            memoryReduction += removedPaths * 16L // Approximate memory per entry
        }

        // Clear old cache entries
        val cacheSizeBefore = ruleCache.size + routeCache.size
        ruleCache.entries.removeIf { !isCacheValid(it.value.second) }
        routeCache.entries.removeIf { !isCacheValid(it.value.second) }
        val cacheSizeAfter = ruleCache.size + routeCache.size
        val cacheEntriesRemoved = cacheSizeBefore - cacheSizeAfter

        if (cacheEntriesRemoved > 0) {
            optimizations.add("Cleaned up $cacheEntriesRemoved cache entries")
            memoryReduction += cacheEntriesRemoved * 32L // Approximate memory per cache entry
        }

        return NavigationOptimizationResult(
            optimizationsApplied = optimizations,
            performanceImprovementMs = performanceImprovement,
            memoryReductionKb = memoryReduction / 1024,
            cacheHitRateImprovement = 0.0, // Would be calculated from actual metrics
            timestamp = System.currentTimeMillis()
        )
    }

    // Private helper methods

    private suspend fun evaluateNavigationRules(context: NavigationContext): List<com.roshni.games.core.navigation.rules.RuleEvaluationResult> {
        val applicableRules = _registeredRules.value.filter { rule ->
            scope.launch { rule.shouldEvaluate(context) }
            true // Simplified for now
        }

        return applicableRules.map { rule ->
            val cacheKey = "rule_${rule.id}_${context.targetDestination}_${context.userId}"

            // Check cache first
            val cached = ruleCache[cacheKey]
            if (cached != null && isCacheValid(cached.second)) {
                cached.first
            } else {
                val result = rule.evaluate(context)
                ruleCache[cacheKey] = Pair(result, System.currentTimeMillis())
                result
            }
        }
    }

    private fun calculateOptimalRouteAStar(
        start: String,
        goal: String,
        context: NavigationContext
    ): List<String> {
        // Simplified A* implementation for navigation routing
        val frontier = mutableListOf(start)
        val cameFrom = mutableMapOf<String, String>()
        val costSoFar = mutableMapOf(start to 0.0)

        while (frontier.isNotEmpty()) {
            val current = frontier.minByOrNull { costSoFar[it] ?: Double.MAX_VALUE } ?: break

            if (current == goal) {
                break
            }

            frontier.remove(current)

            // Get neighbors from navigation graph
            val neighbors = navigationGraph[current] ?: emptySet()

            for (neighbor in neighbors) {
                val newCost = (costSoFar[current] ?: 0.0) + (navigationWeights[current to neighbor] ?: 1.0)

                if (newCost < (costSoFar[neighbor] ?: Double.MAX_VALUE)) {
                    costSoFar[neighbor] = newCost
                    frontier.add(neighbor)
                    cameFrom[neighbor] = current
                }
            }
        }

        // Reconstruct path
        return reconstructPath(cameFrom, start, goal)
    }

    private fun reconstructPath(cameFrom: Map<String, String>, start: String, goal: String): List<String> {
        val path = mutableListOf<String>()
        var current = goal

        while (current != start) {
            path.add(0, current)
            current = cameFrom[current] ?: break
        }

        if (current == start) {
            path.add(0, start)
        }

        return path
    }

    private fun estimateRouteTime(route: List<String>): Long {
        // Simple time estimation based on route length
        return route.size * 100L // 100ms per hop
    }

    private fun calculateRouteConfidence(route: List<String>, context: NavigationContext): Double {
        // Calculate confidence based on various factors
        var confidence = 1.0

        // Reduce confidence for longer routes
        if (route.size > 5) {
            confidence *= 0.8
        }

        // Reduce confidence if user lacks permissions for any destination
        route.forEach { destination ->
            if (!context.hasPermission("access_$destination")) {
                confidence *= 0.9
            }
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    private fun getAppliedOptimizations(route: List<String>, context: NavigationContext): List<String> {
        val optimizations = mutableListOf<String>()

        if (route.size > 2) {
            optimizations.add("Multi-hop route optimization")
        }

        if (context.isFeatureEnabled("navigation_shortcuts")) {
            optimizations.add("Shortcut optimization applied")
        }

        return optimizations
    }

    private suspend fun performNavigation(
        optimalRoute: OptimalRoute,
        navOptions: NavOptions?,
        context: NavigationContext
    ): NavigationResult {
        val navController = navController ?: throw IllegalStateException("NavController not initialized")

        val startTime = System.currentTimeMillis()

        try {
            // Navigate to final destination
            navController.navigate(optimalRoute.destination, navOptions)

            val navigationTime = System.currentTimeMillis() - startTime

            return NavigationResult.Success(
                destination = optimalRoute.destination,
                navOptions = navOptions,
                arguments = context.arguments,
                navigationTimeMs = navigationTime,
                actualRoute = optimalRoute.route.joinToString(" -> "),
                appliedRules = emptyList(), // Would be populated from rule evaluation
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            val navigationTime = System.currentTimeMillis() - startTime

            return NavigationResult.Failure(
                attemptedDestination = optimalRoute.destination,
                reason = NavigationResult.NavigationFailureReason.UNKNOWN_ERROR,
                errorMessage = "Navigation execution failed: ${e.message}",
                exception = e,
                timeToFailureMs = navigationTime
            )
        }
    }

    private fun getSuggestedAlternatives(
        context: NavigationContext,
        blockingRules: List<com.roshni.games.core.navigation.rules.RuleEvaluationResult>
    ): List<String> {
        return blockingRules.flatMap { ruleResult ->
            _registeredRules.value
                .find { it.id == "rule_${ruleResult.executionTimeMs}" }
                ?.let { rule ->
                    // This would be implemented to get alternatives from the specific rule
                    emptyList<String>()
                } ?: emptyList()
        }.distinct()
    }

    private fun updatePerformanceStats(success: Boolean, executionTimeMs: Long) {
        val current = _performanceStats.value
        val newTotal = current.totalNavigations + 1
        val newSuccessful = if (success) current.successfulNavigations + 1 else current.successfulNavigations
        val newFailed = if (success) current.failedNavigations else current.failedNavigations + 1

        val newAverageTime = if (newTotal > 1) {
            (current.averageNavigationTimeMs * current.totalNavigations + executionTimeMs) / newTotal
        } else {
            executionTimeMs.toDouble()
        }

        _performanceStats.value = current.copy(
            totalNavigations = newTotal,
            successfulNavigations = newSuccessful,
            failedNavigations = newFailed,
            averageNavigationTimeMs = newAverageTime,
            totalNavigationTimeMs = current.totalNavigationTimeMs + executionTimeMs,
            lastNavigationTime = System.currentTimeMillis()
        )
    }

    private fun handleNavigationEvent(event: NavigationEvent) {
        // Update statistics based on navigation events
        when (event) {
            is NavigationEvent.NavigationCompleted -> {
                updatePerformanceStats(true, event.result.navigationTimeMs)
            }
            is NavigationEvent.NavigationFailed -> {
                updatePerformanceStats(false, event.result.timeToFailureMs)
            }
            else -> { /* Other events don't affect performance stats */ }
        }
    }

    private fun buildNavigationGraph() {
        // Build a basic navigation graph based on known destinations
        val destinations = listOf(
            NavigationDestinations.HOME,
            NavigationDestinations.GAME_LIBRARY,
            NavigationDestinations.PROFILE,
            NavigationDestinations.SETTINGS,
            NavigationDestinations.ACHIEVEMENTS,
            NavigationDestinations.LEADERBOARD,
            NavigationDestinations.SEARCH
        )

        // Create bidirectional connections between main destinations
        destinations.forEach { dest ->
            navigationGraph[dest] = mutableSetOf()
        }

        // Connect main destinations to home
        destinations.filter { it != NavigationDestinations.HOME }.forEach { dest ->
            navigationGraph[NavigationDestinations.HOME]?.add(dest)
            navigationGraph[dest]?.add(NavigationDestinations.HOME)
        }

        // Add some cross-connections based on logical relationships
        navigationGraph[NavigationDestinations.PROFILE]?.add(NavigationDestinations.ACHIEVEMENTS)
        navigationGraph[NavigationDestinations.ACHIEVEMENTS]?.add(NavigationDestinations.PROFILE)

        navigationGraph[NavigationDestinations.PROFILE]?.add(NavigationDestinations.SETTINGS)
        navigationGraph[NavigationDestinations.SETTINGS]?.add(NavigationDestinations.PROFILE)

        // Set default weights
        navigationGraph.forEach { (from, destinations) ->
            destinations.forEach { to ->
                navigationWeights[from to to] = 1.0
            }
        }
    }

    private fun createDefaultContext(): NavigationContext {
        return NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.HOME
        )
    }

    private fun createContextForDestination(destination: String, arguments: Map<String, Any>): NavigationContext {
        return NavigationContext(
            currentDestination = currentContext?.currentDestination ?: NavigationDestinations.HOME,
            targetDestination = destination,
            arguments = arguments
        )
    }

    private fun mergeContexts(base: NavigationContext?, override: NavigationContext): NavigationContext {
        return base?.copy(
            targetDestination = override.targetDestination,
            arguments = override.arguments,
            permissions = override.permissions,
            featureFlags = override.featureFlags,
            userPreferences = override.userPreferences,
            deviceContext = override.deviceContext,
            appState = override.appState
        ) ?: override
    }

    private fun isCacheValid(cacheTime: Long): Boolean {
        val cacheDuration = 30000L // 30 seconds
        return (System.currentTimeMillis() - cacheTime) < cacheDuration
    }
}