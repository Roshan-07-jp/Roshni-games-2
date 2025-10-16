package com.roshni.games.core.navigation.integration

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.roshni.games.core.navigation.NavigationActions
import com.roshni.games.core.navigation.NavigationDestinations
import com.roshni.games.core.navigation.controller.NavigationFlowController
import com.roshni.games.core.navigation.model.NavigationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Integration layer that bridges the existing NavigationActions with the new NavigationFlowController
 */
class NavigationIntegration(
    private val navigationFlowController: NavigationFlowController,
    private val dispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Enhanced NavigationActions that uses the NavigationFlowController
     */
    inner class EnhancedNavigationActions(
        private val navController: NavController,
        private val baseActions: NavigationActions = NavigationActions(navController)
    ) : NavigationActions(navController) {

        override fun navigateToHome() {
            scope.launch {
                try {
                    val context = createContextForDestination(NavigationDestinations.HOME)
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to home failed, falling back to base navigation")
                    baseActions.navigateToHome()
                }
            }
        }

        override fun navigateToGameLibrary() {
            scope.launch {
                try {
                    val context = createContextForDestination(NavigationDestinations.GAME_LIBRARY)
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to game library failed, falling back to base navigation")
                    baseActions.navigateToGameLibrary()
                }
            }
        }

        override fun navigateToProfile() {
            scope.launch {
                try {
                    val context = createContextForDestination(NavigationDestinations.PROFILE)
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to profile failed, falling back to base navigation")
                    baseActions.navigateToProfile()
                }
            }
        }

        override fun navigateToSettings() {
            scope.launch {
                try {
                    val context = createContextForDestination(NavigationDestinations.SETTINGS)
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to settings failed, falling back to base navigation")
                    baseActions.navigateToSettings()
                }
            }
        }

        override fun navigateToGame(gameId: String) {
            scope.launch {
                try {
                    val context = createContextForDestination(
                        NavigationDestinations.GAME_DETAILS,
                        mapOf("gameId" to gameId)
                    )
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to game failed, falling back to base navigation")
                    baseActions.navigateToGame(gameId)
                }
            }
        }

        override fun navigateToPlayer(playerId: String) {
            scope.launch {
                try {
                    val context = createContextForDestination(
                        "player_profile/$playerId",
                        mapOf("playerId" to playerId)
                    )
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to player failed, falling back to base navigation")
                    baseActions.navigateToPlayer(playerId)
                }
            }
        }

        override fun navigateToLeaderboard(type: String) {
            scope.launch {
                try {
                    val context = createContextForDestination(
                        "leaderboard/$type",
                        mapOf("leaderboardType" to type)
                    )
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to leaderboard failed, falling back to base navigation")
                    baseActions.navigateToLeaderboard(type)
                }
            }
        }

        override fun navigateToAchievements() {
            scope.launch {
                try {
                    val context = createContextForDestination(NavigationDestinations.ACHIEVEMENTS)
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to achievements failed, falling back to base navigation")
                    baseActions.navigateToAchievements()
                }
            }
        }

        override fun navigateToSearch(query: String?) {
            scope.launch {
                try {
                    val context = if (query != null) {
                        createContextForDestination(
                            NavigationDestinations.SEARCH,
                            mapOf("searchQuery" to query)
                        )
                    } else {
                        createContextForDestination(NavigationDestinations.SEARCH)
                    }
                    navigationFlowController.navigateWithContext(context)
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced navigation to search failed, falling back to base navigation")
                    baseActions.navigateToSearch(query)
                }
            }
        }

        override fun navigateBack() {
            scope.launch {
                try {
                    // For back navigation, we'll use the base implementation
                    // as it requires direct NavController access
                    baseActions.navigateBack()
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced back navigation failed")
                }
            }
        }

        override fun navigateBackToHome() {
            scope.launch {
                try {
                    // For back to home navigation, we'll use the base implementation
                    baseActions.navigateBackToHome()
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced back to home navigation failed")
                }
            }
        }

        override fun navigateUp() {
            scope.launch {
                try {
                    // For up navigation, we'll use the base implementation
                    baseActions.navigateUp()
                } catch (e: Exception) {
                    Timber.e(e, "Enhanced up navigation failed")
                }
            }
        }
    }

    /**
     * Create enhanced NavigationActions that integrate with NavigationFlowController
     */
    fun createEnhancedNavigationActions(navController: NavController): EnhancedNavigationActions {
        return EnhancedNavigationActions(navController)
    }

    /**
     * Initialize the integration with NavController
     */
    suspend fun initialize(navController: NavController): Boolean {
        return navigationFlowController.initialize(navController)
    }

    /**
     * Check if navigation to destination is allowed
     */
    suspend fun canNavigateTo(destination: String): Boolean {
        val context = createContextForDestination(destination)
        return navigationFlowController.canNavigateTo(destination, context)
    }

    /**
     * Get alternative destinations if navigation is blocked
     */
    suspend fun getAlternativeDestinations(destination: String): List<String> {
        val context = createContextForDestination(destination)
        return navigationFlowController.getAlternativeDestinations(destination, context)
    }

    /**
     * Preload navigation data for better performance
     */
    suspend fun preloadNavigationData() {
        val destinations = listOf(
            NavigationDestinations.HOME,
            NavigationDestinations.GAME_LIBRARY,
            NavigationDestinations.PROFILE,
            NavigationDestinations.SETTINGS,
            NavigationDestinations.ACHIEVEMENTS,
            NavigationDestinations.LEADERBOARD,
            NavigationDestinations.SEARCH
        )
        navigationFlowController.preloadNavigationData(destinations)
    }

    /**
     * Get current navigation context
     */
    suspend fun getCurrentContext(): NavigationContext? {
        return navigationFlowController.getCurrentContext()
    }

    /**
     * Update current navigation context
     */
    suspend fun updateContext(
        contextUpdater: (NavigationContext) -> NavigationContext
    ): NavigationContext {
        return navigationFlowController.updateContext(contextUpdater)
    }

    /**
     * Register a navigation rule
     */
    suspend fun registerRule(rule: com.roshni.games.core.navigation.rules.NavigationRule): Boolean {
        return navigationFlowController.registerRule(rule)
    }

    /**
     * Enable a navigation rule
     */
    suspend fun enableRule(ruleId: String): Boolean {
        return navigationFlowController.enableRule(ruleId)
    }

    /**
     * Disable a navigation rule
     */
    suspend fun disableRule(ruleId: String): Boolean {
        return navigationFlowController.disableRule(ruleId)
    }

    /**
     * Shutdown the integration
     */
    suspend fun shutdown() {
        navigationFlowController.shutdown()
    }

    /**
     * Create navigation context for a destination
     */
    private fun createContextForDestination(
        destination: String,
        arguments: Map<String, Any> = emptyMap()
    ): NavigationContext {
        return NavigationContext(
            currentDestination = navigationFlowController.getCurrentContext()?.currentDestination
                ?: NavigationDestinations.HOME,
            targetDestination = destination,
            arguments = arguments,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Factory for creating NavigationIntegration instances
 */
object NavigationIntegrationFactory {

    /**
     * Create a new NavigationIntegration instance
     */
    fun create(
        navigationFlowController: NavigationFlowController,
        dispatcher: CoroutineDispatcher
    ): NavigationIntegration {
        return NavigationIntegration(navigationFlowController, dispatcher)
    }
}

/**
 * Extension function to easily integrate NavigationFlowController with existing NavController
 */
suspend fun NavController.integrateWithNavigationFlowController(
    navigationFlowController: NavigationFlowController,
    dispatcher: CoroutineDispatcher
): NavigationIntegration.EnhancedNavigationActions {
    val integration = NavigationIntegration(navigationFlowController, dispatcher)
    integration.initialize(this)
    return integration.createEnhancedNavigationActions(this)
}

/**
 * Extension function to create NavigationIntegration from NavigationFlowController
 */
fun NavigationFlowController.withIntegration(
    dispatcher: CoroutineDispatcher
): NavigationIntegration {
    return NavigationIntegration(this, dispatcher)
}