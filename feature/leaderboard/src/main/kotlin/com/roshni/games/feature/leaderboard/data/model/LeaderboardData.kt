package com.roshni.games.feature.leaderboard.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for leaderboard functionality
 */

/**
 * Leaderboard entry
 */
@Serializable
data class LeaderboardEntry(
    val id: String,
    val playerId: String,
    val playerName: String,
    val playerAvatar: String? = null,
    val score: Long,
    val rank: Int,
    val gameId: String? = null, // null for global leaderboards
    val gameMode: String? = null,
    val difficulty: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val achievedAt: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Leaderboard definition
 */
@Serializable
data class Leaderboard(
    val id: String,
    val name: String,
    val description: String,
    val gameId: String? = null,
    val gameMode: String? = null,
    val type: LeaderboardType,
    val scope: LeaderboardScope,
    val resetFrequency: ResetFrequency,
    val maxEntries: Int = 100,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReset: Long? = null
)

/**
 * Types of leaderboards
 */
@Serializable
enum class LeaderboardType {
    HIGH_SCORE,     // Highest score wins
    LOW_SCORE,      // Lowest score wins (for time-based games)
    ACCUMULATIVE,   // Sum of multiple scores
    AVERAGE,        // Average score
    STREAK,         // Longest streak
    CUSTOM          // Custom scoring logic
}

/**
 * Scope of leaderboards
 */
@Serializable
enum class LeaderboardScope {
    GLOBAL,         // All players worldwide
    REGIONAL,       // Players in same region
    FRIENDS,        // Friends only
    LOCAL,          // Local multiplayer
    GAME_SPECIFIC   // Specific to a game
}

/**
 * Reset frequency for periodic leaderboards
 */
@Serializable
enum class ResetFrequency {
    NEVER,          // Never reset
    DAILY,          // Reset daily
    WEEKLY,         // Reset weekly
    MONTHLY,        // Reset monthly
    SEASONAL        // Reset seasonally
}

/**
 * Leaderboard filter criteria
 */
@Serializable
data class LeaderboardFilter(
    val gameId: String? = null,
    val gameMode: String? = null,
    val difficulty: String? = null,
    val timeRange: TimeRange? = null,
    val scope: LeaderboardScope? = null,
    val playerIds: List<String> = emptyList()
)

/**
 * Time range for filtering
 */
@Serializable
data class TimeRange(
    val startTime: Long,
    val endTime: Long
)

/**
 * Leaderboard sort options
 */
@Serializable
enum class LeaderboardSort {
    RANK_ASC,
    RANK_DESC,
    SCORE_ASC,
    SCORE_DESC,
    NAME_ASC,
    NAME_DESC,
    RECENT_ASC,
    RECENT_DESC
}

/**
 * Real-time update for leaderboards
 */
@Serializable
data class LeaderboardUpdate(
    val leaderboardId: String,
    val entryId: String,
    val oldRank: Int,
    val newRank: Int,
    val score: Long,
    val playerId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val updateType: UpdateType
)

/**
 * Types of leaderboard updates
 */
@Serializable
enum class UpdateType {
    NEW_ENTRY,      // New entry added
    SCORE_UPDATE,   // Existing entry score updated
    RANK_CHANGE,    // Rank changed due to other updates
    ENTRY_REMOVED   // Entry removed
}

/**
 * Leaderboard statistics
 */
@Serializable
data class LeaderboardStats(
    val totalEntries: Int = 0,
    val activePlayers: Int = 0,
    val averageScore: Long = 0,
    val highestScore: Long = 0,
    val lowestScore: Long = 0,
    val lastUpdate: Long? = null,
    val updateFrequency: Long = 0 // updates per hour
)

/**
 * Player's position in multiple leaderboards
 */
@Serializable
data class PlayerLeaderboardPosition(
    val playerId: String,
    val positions: Map<String, Int> = emptyMap(), // leaderboardId -> rank
    val bestRank: Int = Int.MAX_VALUE,
    val averageRank: Float = 0f,
    val totalLeaderboards: Int = 0
)

/**
 * Leaderboard configuration
 */
@Serializable
data class LeaderboardConfig(
    val realTimeUpdates: Boolean = true,
    val updateIntervalSeconds: Long = 30,
    val cacheDurationMinutes: Long = 5,
    val maxCachedEntries: Int = 1000,
    val enableNotifications: Boolean = true,
    val notificationThreshold: Int = 10 // Notify if rank changes by this amount
)