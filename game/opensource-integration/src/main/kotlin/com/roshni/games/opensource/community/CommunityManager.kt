package com.roshni.games.opensource.community

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages community features for open source games
 */
interface CommunityManager {

    /**
     * Initialize community manager
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Submit game rating
     */
    suspend fun submitRating(gameId: String, userId: String, rating: Int, review: String? = null): Result<Unit>

    /**
     * Get game ratings
     */
    fun getGameRatings(gameId: String): StateFlow<GameRatings>

    /**
     * Add game to favorites
     */
    suspend fun addToFavorites(userId: String, gameId: String): Result<Unit>

    /**
     * Remove game from favorites
     */
    suspend fun removeFromFavorites(userId: String, gameId: String): Result<Unit>

    /**
     * Get user favorites
     */
    fun getUserFavorites(userId: String): StateFlow<List<String>>

    /**
     * Submit game request
     */
    suspend fun submitGameRequest(userId: String, request: GameRequest): Result<String>

    /**
     * Get game requests
     */
    fun getGameRequests(status: RequestStatus? = null): StateFlow<List<GameRequest>>

    /**
     * Vote on game request
     */
    suspend fun voteOnRequest(userId: String, requestId: String): Result<Unit>

    /**
     * Submit high score
     */
    suspend fun submitHighScore(gameId: String, userId: String, score: Long, metadata: Map<String, Any> = emptyMap()): Result<Unit>

    /**
     * Get game leaderboards
     */
    fun getGameLeaderboard(gameId: String, period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME): StateFlow<GameLeaderboard>

    /**
     * Get community statistics
     */
    fun getCommunityStats(): CommunityStatistics
}

/**
 * Game ratings data
 */
data class GameRatings(
    val gameId: String,
    val averageRating: Float,
    val totalRatings: Int,
    val ratingDistribution: Map<Int, Int>, // rating -> count
    val recentReviews: List<GameReview>,
    val lastUpdated: Long
)

/**
 * Game review
 */
data class GameReview(
    val id: String,
    val userId: String,
    val username: String,
    val rating: Int,
    val review: String?,
    val timestamp: Long,
    val helpful: Int,
    val notHelpful: Int
)

/**
 * Game request
 */
data class GameRequest(
    val id: String,
    val userId: String,
    val username: String,
    val gameName: String,
    val description: String,
    val category: com.roshni.games.opensource.metadata.GameCategory,
    val sourceUrl: String?,
    val status: RequestStatus,
    val votes: Int,
    val comments: List<RequestComment>,
    val submittedDate: Long,
    val lastUpdated: Long
)

/**
 * Request comment
 */
data class RequestComment(
    val id: String,
    val userId: String,
    val username: String,
    val comment: String,
    val timestamp: Long
)

/**
 * Request status
 */
enum class RequestStatus {
    PENDING, UNDER_REVIEW, APPROVED, REJECTED, IMPLEMENTED, DUPLICATE
}

/**
 * Game leaderboard
 */
data class GameLeaderboard(
    val gameId: String,
    val period: LeaderboardPeriod,
    val entries: List<LeaderboardEntry>,
    val userRank: Int?,
    val totalEntries: Int,
    val lastUpdated: Long
)

/**
 * Leaderboard entry
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Long,
    val metadata: Map<String, Any>,
    val achievedDate: Long
)

/**
 * Leaderboard periods
 */
enum class LeaderboardPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY, ALL_TIME
}

/**
 * Community statistics
 */
data class CommunityStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalRatings: Int,
    val totalReviews: Int,
    val totalFavorites: Int,
    val totalGameRequests: Int,
    val implementedRequests: Int,
    val averageRating: Float,
    val topCategories: List<CategoryStats>,
    val recentActivity: List<CommunityActivity>
)

/**
 * Category statistics
 */
data class CategoryStats(
    val category: com.roshni.games.opensource.metadata.GameCategory,
    val gameCount: Int,
    val averageRating: Float,
    val totalRatings: Int,
    val popularGames: List<String>
)

/**
 * Community activity
 */
data class CommunityActivity(
    val id: String,
    val type: ActivityType,
    val userId: String,
    val username: String,
    val gameId: String?,
    val gameName: String?,
    val description: String,
    val timestamp: Long
)

/**
 * Activity types
 */
enum class ActivityType {
    RATING_SUBMITTED,
    REVIEW_SUBMITTED,
    GAME_FAVORITED,
    GAME_REQUEST_SUBMITTED,
    HIGH_SCORE_ACHIEVED,
    REQUEST_VOTED,
    GAME_COMPLETED
}

/**
 * User profile for community features
 */
data class CommunityUserProfile(
    val userId: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val joinDate: Long,
    val gamesPlayed: Int,
    val totalPlayTime: Long,
    val achievements: List<UserAchievement>,
    val favoriteCategories: List<com.roshni.games.opensource.metadata.GameCategory>,
    val isActive: Boolean
)

/**
 * User achievement
 */
data class UserAchievement(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val unlockedDate: Long,
    val rarity: AchievementRarity
)

/**
 * Achievement rarity
 */
enum class AchievementRarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
}

/**
 * Community settings
 */
data class CommunitySettings(
    val enableRatings: Boolean = true,
    val enableReviews: Boolean = true,
    val enableGameRequests: Boolean = true,
    val enableLeaderboards: Boolean = true,
    val moderateContent: Boolean = true,
    val requireEmailVerification: Boolean = false,
    val maxReviewsPerDay: Int = 10,
    val minPlayTimeForReview: Long = 300000 // 5 minutes in milliseconds
)