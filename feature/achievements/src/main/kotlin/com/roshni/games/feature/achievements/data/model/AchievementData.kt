package com.roshni.games.feature.achievements.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for achievements system
 */

/**
 * Achievement definition
 */
@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val category: AchievementCategory,
    val type: AchievementType,
    val targetValue: Float,
    val currentValue: Float = 0f,
    val isCompleted: Boolean = false,
    val isSecret: Boolean = false,
    val isHidden: Boolean = false,
    val points: Int = 0,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val prerequisites: List<String> = emptyList(), // Other achievement IDs
    val gameId: String? = null, // null for global achievements
    val metadata: Map<String, Any> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Achievement categories
 */
@Serializable
enum class AchievementCategory {
    GAMEPLAY,
    SOCIAL,
    PROGRESSION,
    COLLECTION,
    SPECIAL,
    SEASONAL,
    SECRET
}

/**
 * Achievement types
 */
@Serializable
enum class AchievementType {
    SCORE_BASED,        // Reach a specific score
    LEVEL_BASED,        // Reach a specific level
    TIME_BASED,         // Complete within time limit
    STREAK_BASED,       // Maintain a streak
    COLLECTION_BASED,   // Collect items
    SOCIAL_BASED,       // Social interactions
    COUNT_BASED,        // Perform action X times
    SPECIAL_BASED       // Special conditions
}

/**
 * Achievement rarity levels
 */
@Serializable
enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Player achievement progress
 */
@Serializable
data class PlayerAchievement(
    val playerId: String,
    val achievementId: String,
    val progress: Float, // 0.0 to 1.0
    val currentValue: Float,
    val isCompleted: Boolean = false,
    val isNotified: Boolean = false,
    val unlockedAt: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Achievement progress update
 */
@Serializable
data class AchievementProgressUpdate(
    val playerId: String,
    val achievementId: String,
    val newValue: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Achievement notification
 */
@Serializable
data class AchievementNotification(
    val id: String,
    val playerId: String,
    val achievementId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * Types of achievement notifications
 */
@Serializable
enum class NotificationType {
    UNLOCKED,
    PROGRESS_MILESTONE,
    RARITY_UPGRADE,
    NEAR_COMPLETION
}

/**
 * Achievement statistics
 */
@Serializable
data class AchievementStats(
    val totalAchievements: Int = 0,
    val completedAchievements: Int = 0,
    val totalPoints: Int = 0,
    val completionRate: Float = 0f,
    val rareAchievementsUnlocked: Int = 0,
    val recentUnlocks: Int = 0, // Last 7 days
    val favoriteCategory: AchievementCategory? = null
)

/**
 * Achievement filter criteria
 */
@Serializable
data class AchievementFilter(
    val categories: List<AchievementCategory> = emptyList(),
    val types: List<AchievementType> = emptyList(),
    val rarities: List<AchievementRarity> = emptyList(),
    val completed: Boolean? = null, // null = all, true = completed only, false = incomplete only
    val gameId: String? = null,
    val searchQuery: String? = null
)

/**
 * Achievement sort options
 */
@Serializable
enum class AchievementSort {
    NAME,
    PROGRESS,
    RARITY,
    POINTS,
    RECENTLY_UNLOCKED,
    CATEGORY
}