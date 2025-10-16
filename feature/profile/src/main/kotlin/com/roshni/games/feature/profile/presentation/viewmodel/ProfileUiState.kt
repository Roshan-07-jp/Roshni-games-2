package com.roshni.games.feature.profile.presentation.viewmodel

import com.roshni.games.feature.profile.domain.model.ProfileState

data class ProfileUiState(
    val profileState: ProfileState = ProfileState(
        userProfile = null,
        gameStatistics = com.roshni.games.feature.profile.domain.model.GameStatistics(
            totalGamesPlayed = 0,
            totalPlayTime = 0,
            averageScore = 0f,
            highestScore = 0,
            gamesCompleted = 0,
            currentStreak = 0,
            longestStreak = 0,
            favoriteCategory = null,
            categoryStats = emptyMap(),
            difficultyStats = emptyMap(),
            monthlyStats = emptyList(),
            lastUpdated = kotlinx.datetime.Clock.System.now().toLocalDateTime(
                kotlinx.datetime.TimeZone.currentSystemDefault()
            )
        ),
        achievements = emptyList(),
        recentActivity = emptyList(),
        customization = com.roshni.games.feature.profile.domain.model.ProfileCustomization(
            avatarOptions = emptyList(),
            selectedAvatarId = "default",
            backgroundColor = "#FFFFFF",
            accentColor = "#6200EE",
            showStats = true,
            showAchievements = true,
            showActivity = true,
            profileVisibility = com.roshni.games.feature.profile.domain.model.ProfileVisibility.PUBLIC
        )
    ),
    val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
    val showEditProfileDialog: Boolean = false,
    val showCustomizationDialog: Boolean = false,
    val showAchievementDetailsDialog: Boolean = false,
    val selectedAchievement: com.roshni.games.feature.profile.domain.model.Achievement? = null
)

enum class ProfileTab {
    OVERVIEW, ACHIEVEMENTS, STATISTICS, ACTIVITY
}

sealed class ProfileAction {
    data class SelectTab(val tab: ProfileTab) : ProfileAction()
    object EditProfile : ProfileAction()
    object CustomizeProfile : ProfileAction()
    data class ViewAchievement(val achievement: com.roshni.games.feature.profile.domain.model.Achievement) : ProfileAction()
    data class NavigateToGame(val gameId: String) : ProfileAction()
    object RefreshProfile : ProfileAction()
    object DismissDialogs : ProfileAction()
}