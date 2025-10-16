package com.roshni.games.feature.home.domain.model

import kotlinx.datetime.LocalDateTime

data class HomeScreenState(
    val recentlyPlayedGames: List<GameSummary>,
    val recommendedGames: List<GameRecommendation>,
    val popularCategories: List<CategorySummary>,
    val achievementHighlights: List<AchievementSummary>,
    val userStats: UserStats,
    val lastUpdated: LocalDateTime,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class GameSummary(
    val id: String,
    val name: String,
    val category: String,
    val difficulty: String,
    val iconUrl: String?,
    val thumbnailUrl: String?,
    val lastPlayed: LocalDateTime?,
    val playTime: Long,
    val score: Int?,
    val level: Int?
)

data class GameRecommendation(
    val game: GameSummary,
    val reason: String,
    val confidence: Float,
    val personalizedScore: Float
)

data class CategorySummary(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val gameCount: Int,
    val isPopular: Boolean
)

data class AchievementSummary(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val rarity: AchievementRarity,
    val unlockedAt: LocalDateTime,
    val progress: Float
)

enum class AchievementRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class UserStats(
    val gamesPlayed: Int,
    val totalPlayTime: Long,
    val averageScore: Float,
    val achievementsUnlocked: Int,
    val currentStreak: Int,
    val favoriteCategory: String?
)

sealed class HomeNavigationEvent {
    object NavigateToGameLibrary : HomeNavigationEvent()
    object NavigateToProfile : HomeNavigationEvent()
    object NavigateToSettings : HomeNavigationEvent()
    data class NavigateToGame(val gameId: String) : HomeNavigationEvent()
    data class NavigateToCategory(val categoryId: String) : HomeNavigationEvent()
}