package com.roshni.games.feature.profile.data.model

import kotlinx.datetime.LocalDateTime

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    val bio: String?,
    val joinDate: LocalDateTime,
    val lastActive: LocalDateTime,
    val isOnline: Boolean = false,
    val level: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 1000,
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val favoriteCategories: List<String> = emptyList(),
    val preferredDifficulty: String? = null,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeMode: String = "system",
    val language: String = "en"
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
    val progress: Float = 0f,
    val maxProgress: Float = 1f,
    val isSecret: Boolean = false,
    val requirements: List<AchievementRequirement> = emptyList()
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
    val totalGamesPlayed: Int = 0,
    val totalPlayTime: Long = 0, // in minutes
    val averageScore: Float = 0f,
    val highestScore: Int = 0,
    val gamesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val favoriteCategory: String? = null,
    val categoryStats: Map<String, CategoryStats> = emptyMap(),
    val difficultyStats: Map<String, DifficultyStats> = emptyMap(),
    val monthlyStats: List<MonthlyStats> = emptyList(),
    val lastUpdated: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0)
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
    val month: String, // e.g., "2024-01"
    val gamesPlayed: Int,
    val totalScore: Int,
    val playTime: Long,
    val achievementsUnlocked: Int
)

data class ProfileCustomization(
    val avatarOptions: List<AvatarOption> = defaultAvatarOptions(),
    val selectedAvatarId: String = "default",
    val backgroundColor: String = "#FFFFFF",
    val accentColor: String = "#6200EE",
    val showStats: Boolean = true,
    val showAchievements: Boolean = true,
    val showActivity: Boolean = true,
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC
)

enum class ProfileVisibility {
    PUBLIC, FRIENDS_ONLY, PRIVATE
}

data class AvatarOption(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val color: String,
    val isPremium: Boolean = false,
    val isUnlocked: Boolean = true
)

data class ProfileActivity(
    val id: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: LocalDateTime,
    val gameId: String? = null,
    val score: Int? = null,
    val achievementId: String? = null
)

enum class ActivityType {
    GAME_COMPLETED, ACHIEVEMENT_UNLOCKED, HIGH_SCORE, LEVEL_UP, STREAK_MILESTONE
}

data class ProfileState(
    val userProfile: UserProfile? = null,
    val achievements: List<Achievement> = emptyList(),
    val gameStatistics: GameStatistics = GameStatistics(),
    val customization: ProfileCustomization = ProfileCustomization(),
    val recentActivity: List<ProfileActivity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

private fun defaultAvatarOptions() = listOf(
    AvatarOption("default", "Default", null, "#6200EE", false, true),
    AvatarOption("gamer", "Gamer", null, "#FF5722", false, true),
    AvatarOption("pro", "Pro", null, "#FFC107", true, false),
    AvatarOption("legend", "Legend", null, "#4CAF50", true, false),
    AvatarOption("mystic", "Mystic", null, "#9C27B0", true, false),
    AvatarOption("cyber", "Cyber", null, "#00BCD4", true, false)
)