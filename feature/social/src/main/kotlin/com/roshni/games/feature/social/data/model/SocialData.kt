package com.roshni.games.feature.social.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for social features and friend system
 */

/**
 * User profile for social features
 */
@Serializable
data class SocialProfile(
    val id: String,
    val name: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val level: Int = 1,
    val experience: Long = 0,
    val totalScore: Long = 0,
    val gamesPlayed: Int = 0,
    val achievementsUnlocked: Int = 0,
    val friendsCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val joinedAt: Long = System.currentTimeMillis(),
    val preferences: SocialPreferences = SocialPreferences(),
    val stats: SocialStats = SocialStats()
)

/**
 * Social preferences
 */
@Serializable
data class SocialPreferences(
    val allowFriendRequests: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showGameActivity: Boolean = true,
    val allowMessages: Boolean = true,
    val allowChallenges: Boolean = true,
    val publicProfile: Boolean = true
)

/**
 * Social statistics
 */
@Serializable
data class SocialStats(
    val totalPlayTime: Long = 0, // in minutes
    val favoriteGame: String? = null,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val socialScore: Int = 0,
    val helpfulnessRating: Float = 0f
)

/**
 * Friend relationship
 */
@Serializable
data class Friendship(
    val id: String,
    val playerId: String,
    val friendId: String,
    val status: FriendshipStatus,
    val initiatedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Friendship status
 */
@Serializable
enum class FriendshipStatus {
    PENDING,    // Request sent, waiting for acceptance
    ACCEPTED,   // Friends
    BLOCKED,    // Blocked
    DECLINED    // Request declined
}

/**
 * Friend request
 */
@Serializable
data class FriendRequest(
    val id: String,
    val fromPlayerId: String,
    val toPlayerId: String,
    val message: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
)

/**
 * Social activity feed item
 */
@Serializable
data class SocialActivity(
    val id: String,
    val playerId: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val metadata: Map<String, Any> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val visibility: ActivityVisibility = ActivityVisibility.FRIENDS
)

/**
 * Types of social activities
 */
@Serializable
enum class ActivityType {
    ACHIEVEMENT_UNLOCKED,
    HIGH_SCORE,
    LEVEL_UP,
    GAME_COMPLETED,
    FRIEND_ADDED,
    CHALLENGE_SENT,
    CHALLENGE_COMPLETED,
    STREAK_MILESTONE
}

/**
 * Activity visibility levels
 */
@Serializable
enum class ActivityVisibility {
    PUBLIC,     // Visible to everyone
    FRIENDS,    // Visible to friends only
    PRIVATE     // Visible only to the player
}

/**
 * Challenge between players
 */
@Serializable
data class Challenge(
    val id: String,
    val challengerId: String,
    val challengedId: String,
    val gameId: String,
    val challengeType: ChallengeType,
    val targetScore: Long? = null,
    val targetTime: Long? = null,
    val customMessage: String? = null,
    val status: ChallengeStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val completedAt: Long? = null,
    val result: ChallengeResult? = null
)

/**
 * Types of challenges
 */
@Serializable
enum class ChallengeType {
    HIGH_SCORE,     // Beat my high score
    TIME_TRIAL,     // Complete faster than me
    LEVEL_RACE,     // Reach level faster
    CUSTOM          // Custom challenge
}

/**
 * Challenge status
 */
@Serializable
enum class ChallengeStatus {
    PENDING,        // Sent but not responded to
    ACCEPTED,       // Accepted, in progress
    DECLINED,       // Declined
    COMPLETED,      // Completed
    EXPIRED         // Expired without completion
}

/**
 * Challenge result
 */
@Serializable
data class ChallengeResult(
    val winnerId: String,
    val challengerScore: Long,
    val challengedScore: Long,
    val completedAt: Long
)

/**
 * Social group/clan
 */
@Serializable
data class SocialGroup(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val memberIds: List<String> = emptyList(),
    val maxMembers: Int = 50,
    val isPublic: Boolean = true,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val stats: GroupStats = GroupStats()
)

/**
 * Group statistics
 */
@Serializable
data class GroupStats(
    val totalScore: Long = 0,
    val averageLevel: Float = 0f,
    val activeMembers: Int = 0,
    val achievementsUnlocked: Int = 0
)

/**
 * Social configuration
 */
@Serializable
data class SocialConfig(
    val maxFriends: Int = 200,
    val maxFriendRequests: Int = 50,
    val maxChallenges: Int = 10,
    val activityFeedSize: Int = 100,
    val enableNotifications: Boolean = true,
    val autoAcceptFriendRequests: Boolean = false,
    val showRealNames: Boolean = false
)