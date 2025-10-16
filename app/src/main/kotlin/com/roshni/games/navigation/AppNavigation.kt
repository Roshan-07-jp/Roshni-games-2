package com.roshni.games.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roshni.games.core.navigation.NavigationDestinations
import com.roshni.games.core.navigation.toNavigationActions
import com.roshni.games.feature.gamelibrary.presentation.screens.GameLibraryScreen
import com.roshni.games.feature.home.presentation.screens.HomeScreen
import com.roshni.games.feature.profile.presentation.screens.ProfileScreen
import com.roshni.games.feature.settings.presentation.screens.SettingsScreen
import com.roshni.games.feature.splash.presentation.screens.SplashScreen
import com.roshni.games.feature.splash.presentation.viewmodel.SplashNavigationEvent
import com.roshni.games.feature.splash.presentation.viewmodel.SplashViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavigationDestinations.SPLASH
) {
    val navigationActions = navController.toNavigationActions()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(NavigationDestinations.SPLASH) {
            val splashViewModel = hiltViewModel<SplashViewModel>()
            val navigationEvent by splashViewModel.navigationEvent.collectAsState(initial = null)

            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToHome = { navigationActions.navigateToHome() },
                onNavigateToOnboarding = { /* Navigate to onboarding if needed */ },
                onNavigateToUpdate = { /* Handle update navigation */ },
                onShowError = { /* Handle error display */ }
            )

            // Handle navigation events from SplashViewModel
            LaunchedEffect(navigationEvent) {
                when (navigationEvent) {
                    SplashNavigationEvent.NavigateToHome -> {
                        navigationActions.navigateToHome()
                    }
                    SplashNavigationEvent.NavigateToOnboarding -> {
                        // Navigate to onboarding screen if needed
                    }
                    is SplashNavigationEvent.NavigateToUpdate -> {
                        // Handle update navigation
                    }
                    is SplashNavigationEvent.ShowError -> {
                        // Handle error display
                    }
                    null -> { /* No navigation event */ }
                }
            }
        }

        // Home Screen
        composable(NavigationDestinations.HOME) {
            HomeScreen(
                onNavigateToGame = { gameId ->
                    navigationActions.navigateToGame(gameId)
                },
                onNavigateToGameLibrary = {
                    navigationActions.navigateToGameLibrary()
                },
                onNavigateToProfile = {
                    navigationActions.navigateToProfile()
                },
                onNavigateToSettings = {
                    navigationActions.navigateToSettings()
                }
            )
        }

        // Game Library Screen
        composable(NavigationDestinations.GAME_LIBRARY) {
            GameLibraryScreen(
                onNavigateToGame = { gameId ->
                    navigationActions.navigateToGame(gameId)
                },
                onNavigateToCategory = { categoryId ->
                    // Navigate to category-specific view
                    navigationActions.navigateToGameLibrary()
                }
            )
        }

        // Profile Screen
        composable(NavigationDestinations.PROFILE) {
            ProfileScreen(
                onNavigateToSettings = {
                    navigationActions.navigateToSettings()
                },
                onNavigateToGame = { gameId ->
                    navigationActions.navigateToGame(gameId)
                }
            )
        }

        // Settings Screen
        composable(NavigationDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navigationActions.navigateBack() },
                onNavigateToProfile = { navigationActions.navigateToProfile() },
                onNavigateToParentalControls = { /* Navigate to parental controls */ },
                onNavigateToLanguageSelection = { /* Navigate to language selection */ }
            )
        }

        // Game Details Screen (placeholder)
        composable("game_details/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            // Placeholder for game details screen
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text(
                    text = "Game Details: $gameId",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Search Screen (placeholder)
        composable("search") {
            // Placeholder for search screen
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text(
                    text = "Search Screen",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Achievements Screen (placeholder)
        composable("achievements") {
            // Placeholder for achievements screen
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text(
                    text = "Achievements Screen",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Leaderboard Screen (placeholder)
        composable("leaderboard/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            // Placeholder for leaderboard screen
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text(
                    text = "Leaderboard: $type",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}