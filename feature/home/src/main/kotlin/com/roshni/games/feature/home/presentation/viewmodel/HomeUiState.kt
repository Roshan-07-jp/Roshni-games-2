package com.roshni.games.feature.home.presentation.viewmodel

import com.roshni.games.feature.home.domain.model.HomeScreenState

data class HomeUiState(
    val homeState: HomeScreenState = HomeScreenState(
        recentlyPlayedGames = emptyList(),
        recommendedGames = emptyList(),
        popularCategories = emptyList(),
        achievementHighlights = emptyList(),
        userStats = com.roshni.games.feature.home.domain.model.UserStats(
            gamesPlayed = 0,
            totalPlayTime = 0,
            averageScore = 0f,
            achievementsUnlocked = 0,
            currentStreak = 0,
            favoriteCategory = null
        ),
        lastUpdated = kotlinx.datetime.Clock.System.now().toLocalDateTime(
            kotlinx.datetime.TimeZone.currentSystemDefault()
        )
    ),
    val isRefreshing: Boolean = false,
    val showWelcomeMessage: Boolean = true
)

sealed class HomeAction {
    object RefreshData : HomeAction()
    object DismissWelcomeMessage : HomeAction()
    data class NavigateToGame(val gameId: String) : HomeAction()
    data class NavigateToCategory(val categoryId: String) : HomeAction()
    object NavigateToGameLibrary : HomeAction()
    object NavigateToProfile : HomeAction()
    object NavigateToSettings : HomeAction()
}