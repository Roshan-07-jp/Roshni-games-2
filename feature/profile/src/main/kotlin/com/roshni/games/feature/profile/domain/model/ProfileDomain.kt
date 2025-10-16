package com.roshni.games.feature.profile.domain.model

import kotlinx.datetime.LocalDateTime

data class ProfileState(
    val userProfile: UserProfile?,
    val gameStatistics: GameStatistics,
    val achievements: List<Achievement>,
    val recentActivity: List<ProfileActivity>,
    val customization: ProfileCustomization,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val showAchievementDialog: Boolean = false,
    val selectedAchievement: Achievement? = null
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    val bio: String?,
    val joinDate: LocalDateTime,
    val lastActive: LocalDateTime,
    val isOnline: Boolean,
    val level: Int,
    val experience: Long,
    val experienceToNextLevel: Long,
    val preferences: UserPreferences
)

data class UserPreferences(
    val favoriteCategories: List<String>,
    val preferredDifficulty: String?,
    val notificationsEnabled: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val themeMode: String,
    val language: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    val points: Int,
    val unlockedAt: LocalDateTime?,
    val progress: Float,
    val maxProgress: Float,
    val isSecret: Boolean,
    val requirements: List<AchievementRequirement>
)

enum class AchievementCategory {
    GAMEPLAY, SOCIAL, PROGRESSION, SPECIAL, HIDDEN
}

enum class AchievementRarity {
    COMMON, RARE, EPIC, LEGENDARY
}

data class AchievementRequirement(
    val type: RequirementType,
    val target: String,
    val currentValue: Float,
    val requiredValue: Float,
    val description: String
)

enum class RequirementType {
    GAMES_PLAYED, SCORE_ACHIEVED, TIME_PLAYED, STREAK_DAYS, GAMES_COMPLETED
}

data class GameStatistics(
    val totalGamesPlayed: Int,
    val totalPlayTime: Long,
    val averageScore: Float,
    val highestScore: Int,
    val gamesCompleted: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val favoriteCategory: String?,
    val categoryStats: Map<String, CategoryStats>,
    val difficultyStats: Map<String, DifficultyStats>,
    val monthlyStats: List<MonthlyStats>,
    val lastUpdated: LocalDateTime
)

data class CategoryStats(
    val category: String,
    val gamesPlayed: Int,
    val bestScore: Int,
    val totalPlayTime: Long,
    val averageScore: Float
)

data class DifficultyStats(
    val difficulty: String,
    val gamesPlayed: Int,
    val winRate: Float,
    val averageScore: Float
)

data class MonthlyStats(
    val month: String,
    val gamesPlayed: Int,
    val totalScore: Int,
    val playTime: Long,
    val achievementsUnlocked: Int
)

data class ProfileCustomization(
    val avatarOptions: List<AvatarOption>,
    val selectedAvatarId: String,
    val backgroundColor: String,
    val accentColor: String,
    val showStats: Boolean,
    val showAchievements: Boolean,
    val showActivity: Boolean,
    val profileVisibility: ProfileVisibility
)

enum class ProfileVisibility {
    PUBLIC, FRIENDS_ONLY, PRIVATE
}

data class AvatarOption(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val color: String,
    val isPremium: Boolean,
    val isUnlocked: Boolean
)

data class ProfileActivity(
    val id: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: LocalDateTime,
    val gameId: String?,
    val score: Int?,
    val achievementId: String?
)

enum class ActivityType {
    GAME_COMPLETED, ACHIEVEMENT_UNLOCKED, HIGH_SCORE, LEVEL_UP, STREAK_MILESTONE
}

sealed class ProfileNavigationEvent {
    object NavigateToSettings : ProfileNavigationEvent()
    object NavigateToAchievements : ProfileNavigationEvent()
    object NavigateToStatistics : ProfileNavigationEvent()
    data class NavigateToGame(val gameId: String) : ProfileNavigationEvent()
    object ShowEditProfile : ProfileNavigationEvent()
    object ShowCustomization : ProfileNavigationEvent()
}