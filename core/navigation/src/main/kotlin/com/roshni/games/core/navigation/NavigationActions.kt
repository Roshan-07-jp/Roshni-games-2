package com.roshni.games.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.roshni.games.core.navigation.NavigationDestinations.GAME_LIBRARY
import com.roshni.games.core.navigation.NavigationDestinations.HOME
import com.roshni.games.core.navigation.NavigationDestinations.PROFILE
import com.roshni.games.core.navigation.NavigationDestinations.SETTINGS

/**
 * Navigation actions for the main app navigation
 */
class NavigationActions(private val navController: NavController) {

    fun navigateToHome() {
        navController.navigate(HOME) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToGameLibrary() {
        navController.navigate(GAME_LIBRARY) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToProfile() {
        navController.navigate(PROFILE) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToSettings() {
        navController.navigate(SETTINGS) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToGame(gameId: String) {
        navController.navigate("game_details/$gameId") {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToPlayer(playerId: String) {
        navController.navigate("player_profile/$playerId") {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToLeaderboard(type: String) {
        navController.navigate("leaderboard/$type") {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToAchievements() {
        navController.navigate("achievements") {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToSearch(query: String? = null) {
        val route = if (query != null) "search?query=$query" else "search"
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateBackToHome() {
        navController.popBackStack(HOME, inclusive = false)
    }

    fun navigateUp() {
        navController.navigateUp()
    }
}

/**
 * Extension function to create NavigationActions
 */
fun NavController.toNavigationActions() = NavigationActions(this)

/**
 * Navigation options for different types of navigation
 */
object NavigationOptions {
    val defaultNavOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setRestoreState(true)
        .build()

    val popUpToHomeOptions = NavOptions.Builder()
        .setPopUpTo(HOME, inclusive = false)
        .setLaunchSingleTop(true)
        .setRestoreState(true)
        .build()

    val modalNavOptions = NavOptions.Builder()
        .setLaunchSingleTop(false)
        .setRestoreState(false)
        .build()
}