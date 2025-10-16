package com.roshni.games.feature.home.data.model

import com.roshni.games.core.database.model.GameEntity
import kotlinx.datetime.LocalDateTime

data class RecentlyPlayedGame(
    val game: GameEntity,
    val lastPlayed: LocalDateTime,
    val playTime: Long, // in minutes
    val score: Int?,
    val level: Int?
)

data class GameRecommendation(
    val game: GameEntity,
    val reason: RecommendationReason,
    val confidence: Float, // 0.0 to 1.0
    val personalizedScore: Float
)

enum class RecommendationReason {
    BASED_ON_PREFERENCES,
    SIMILAR_TO_RECENTLY_PLAYED,
    POPULAR_IN_CATEGORY,
    TRENDING_NOW,
    BASED_ON_SKILL_LEVEL,
    FRIENDS_PLAYING,
    NEW_RELEASE
}

data class GameCategory(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val gameCount: Int,
    val isPopular: Boolean = false
)

data class AchievementHighlight(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val rarity: AchievementRarity,
    val unlockedAt: LocalDateTime,
    val progress: Float = 1.0f
)

enum class AchievementRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class UserStats(
    val gamesPlayed: Int,
    val totalPlayTime: Long, // in minutes
    val averageScore: Float,
    val achievementsUnlocked: Int,
    val currentStreak: Int,
    val favoriteCategory: String?
)

data class HomeScreenData(
    val recentlyPlayedGames: List<RecentlyPlayedGame>,
    val recommendedGames: List<GameRecommendation>,
    val popularCategories: List<GameCategory>,
    val achievementHighlights: List<AchievementHighlight>,
    val userStats: UserStats,
    val lastUpdated: LocalDateTime
)