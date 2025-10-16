package com.roshni.games.feature.social.domain

import com.roshni.games.feature.social.data.model.ActivityType
import com.roshni.games.feature.social.data.model.ActivityVisibility
import com.roshni.games.feature.social.data.model.Challenge
import com.roshni.games.feature.social.data.model.ChallengeResult
import com.roshni.games.feature.social.data.model.ChallengeStatus
import com.roshni.games.feature.social.data.model.ChallengeType
import com.roshni.games.feature.social.data.model.FriendRequest
import com.roshni.games.feature.social.data.model.Friendship
import com.roshni.games.feature.social.data.model.FriendshipStatus
import com.roshni.games.feature.social.data.model.SocialActivity
import com.roshni.games.feature.social.data.model.SocialConfig
import com.roshni.games.feature.social.data.model.SocialGroup
import com.roshni.games.feature.social.data.model.SocialProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID

/**
 * Service for managing social features and friend system
 */
class SocialService {

    private val _socialProfiles = MutableStateFlow<List<SocialProfile>>(emptyList())
    private val _friendships = MutableStateFlow<List<Friendship>>(emptyList())
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val _socialActivities = MutableStateFlow<List<SocialActivity>>(emptyList())
    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    private val _socialGroups = MutableStateFlow<List<SocialGroup>>(emptyList())
    private val _socialConfig = MutableStateFlow(
        SocialConfig(
            maxFriends = 200,
            maxFriendRequests = 50,
            maxChallenges = 10,
            activityFeedSize = 100,
            enableNotifications = true,
            autoAcceptFriendRequests = false,
            showRealNames = false
        )
    )

    // Public flows
    val socialProfiles: StateFlow<List<SocialProfile>> = _socialProfiles.asStateFlow()
    val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()
    val socialActivities: StateFlow<List<SocialActivity>> = _socialActivities.asStateFlow()
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()
    val socialGroups: StateFlow<List<SocialGroup>> = _socialGroups.asStateFlow()
    val socialConfig: StateFlow<SocialConfig> = _socialConfig.asStateFlow()

    /**
     * Initialize the social service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing SocialService")

            // Load social data
            loadSocialProfiles()
            loadFriendships()
            loadSocialActivities()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SocialService")
            Result.failure(e)
        }
    }

    /**
     * Send a friend request
     */
    suspend fun sendFriendRequest(
        fromPlayerId: String,
        toPlayerId: String,
        message: String? = null
    ): Result<String> {
        return try {
            // Check if request already exists
            val existingRequest = _friendRequests.value.find {
                it.fromPlayerId == fromPlayerId && it.toPlayerId == toPlayerId
            }

            if (existingRequest != null) {
                return Result.failure(IllegalStateException("Friend request already exists"))
            }

            // Check if already friends
            val existingFriendship = _friendships.value.find {
                (it.playerId == fromPlayerId && it.friendId == toPlayerId) ||
                (it.playerId == toPlayerId && it.friendId == fromPlayerId)
            }

            if (existingFriendship != null) {
                return Result.failure(IllegalStateException("Already friends or blocked"))
            }

            // Check friend limit
            val currentFriends = getFriendsCount(fromPlayerId)
            if (currentFriends >= _socialConfig.value.maxFriends) {
                return Result.failure(IllegalStateException("Maximum friends limit reached"))
            }

            val request = FriendRequest(
                id = UUID.randomUUID().toString(),
                fromPlayerId = fromPlayerId,
                toPlayerId = toPlayerId,
                message = message
            )

            val currentRequests = _friendRequests.value.toMutableList()
            currentRequests.add(request)
            _friendRequests.value = currentRequests

            // Create activity
            createSocialActivity(
                playerId = fromPlayerId,
                type = ActivityType.FRIEND_ADDED,
                title = "Friend Request Sent",
                description = "Sent a friend request",
                visibility = ActivityVisibility.FRIENDS
            )

            Timber.d("Sent friend request from $fromPlayerId to $toPlayerId")
            Result.success(request.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to send friend request")
            Result.failure(e)
        }
    }

    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<String> {
        return try {
            val request = _friendRequests.value.find { it.id == requestId }
            if (request == null) {
                return Result.failure(IllegalArgumentException("Friend request not found"))
            }

            // Create friendship
            val friendship = Friendship(
                id = UUID.randomUUID().toString(),
                playerId = request.toPlayerId,
                friendId = request.fromPlayerId,
                status = FriendshipStatus.ACCEPTED,
                initiatedBy = request.fromPlayerId,
                acceptedAt = System.currentTimeMillis()
            )

            val currentFriendships = _friendships.value.toMutableList()
            currentFriendships.add(friendship)
            _friendships.value = currentFriendships

            // Remove friend request
            val currentRequests = _friendRequests.value.toMutableList()
            currentRequests.remove(request)
            _friendRequests.value = currentRequests

            // Update friend counts
            updateFriendCounts(request.fromPlayerId, request.toPlayerId)

            // Create activities for both players
            createSocialActivity(
                playerId = request.toPlayerId,
                type = ActivityType.FRIEND_ADDED,
                title = "New Friend",
                description = "Accepted friend request",
                visibility = ActivityVisibility.FRIENDS
            )

            createSocialActivity(
                playerId = request.fromPlayerId,
                type = ActivityType.FRIEND_ADDED,
                title = "Friend Request Accepted",
                description = "Friend request was accepted",
                visibility = ActivityVisibility.FRIENDS
            )

            Timber.d("Accepted friend request: $requestId")
            Result.success(friendship.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to accept friend request")
            Result.failure(e)
        }
    }

    /**
     * Decline a friend request
     */
    suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            val request = _friendRequests.value.find { it.id == requestId }
            if (request == null) {
                return Result.failure(IllegalArgumentException("Friend request not found"))
            }

            val currentRequests = _friendRequests.value.toMutableList()
            currentRequests.remove(request)
            _friendRequests.value = currentRequests

            Timber.d("Declined friend request: $requestId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to decline friend request")
            Result.failure(e)
        }
    }

    /**
     * Send a challenge to another player
     */
    suspend fun sendChallenge(
        challengerId: String,
        challengedId: String,
        gameId: String,
        challengeType: ChallengeType,
        targetScore: Long? = null,
        targetTime: Long? = null,
        customMessage: String? = null
    ): Result<String> {
        return try {
            // Check if players are friends
            val friendship = _friendships.value.find {
                (it.playerId == challengerId && it.friendId == challengedId) &&
                it.status == FriendshipStatus.ACCEPTED
            }

            if (friendship == null) {
                return Result.failure(IllegalStateException("Players must be friends to send challenges"))
            }

            // Check challenge limit
            val activeChallenges = _challenges.value.count {
                (it.challengerId == challengerId || it.challengedId == challengerId) &&
                it.status == ChallengeStatus.PENDING
            }

            if (activeChallenges >= _socialConfig.value.maxChallenges) {
                return Result.failure(IllegalStateException("Maximum active challenges limit reached"))
            }

            val challenge = Challenge(
                id = UUID.randomUUID().toString(),
                challengerId = challengerId,
                challengedId = challengedId,
                gameId = gameId,
                challengeType = challengeType,
                targetScore = targetScore,
                targetTime = targetTime,
                customMessage = customMessage,
                status = ChallengeStatus.PENDING,
                expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
            )

            val currentChallenges = _challenges.value.toMutableList()
            currentChallenges.add(challenge)
            _challenges.value = currentChallenges

            // Create activity
            createSocialActivity(
                playerId = challengerId,
                type = ActivityType.CHALLENGE_SENT,
                title = "Challenge Sent",
                description = "Sent a challenge",
                visibility = ActivityVisibility.FRIENDS,
                metadata = mapOf("gameId" to gameId, "challengeType" to challengeType.name)
            )

            Timber.d("Sent challenge from $challengerId to $challengedId")
            Result.success(challenge.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to send challenge")
            Result.failure(e)
        }
    }

    /**
     * Accept a challenge
     */
    suspend fun acceptChallenge(challengeId: String): Result<Unit> {
        return try {
            val challenge = _challenges.value.find { it.id == challengeId }
            if (challenge == null) {
                return Result.failure(IllegalArgumentException("Challenge not found"))
            }

            if (challenge.status != ChallengeStatus.PENDING) {
                return Result.failure(IllegalStateException("Challenge is not pending"))
            }

            val currentChallenges = _challenges.value.toMutableList()
            val index = currentChallenges.indexOf(challenge)
            currentChallenges[index] = challenge.copy(status = ChallengeStatus.ACCEPTED)
            _challenges.value = currentChallenges

            // Create activity
            createSocialActivity(
                playerId = challenge.challengedId,
                type = ActivityType.CHALLENGE_SENT,
                title = "Challenge Accepted",
                description = "Accepted a challenge",
                visibility = ActivityVisibility.FRIENDS
            )

            Timber.d("Accepted challenge: $challengeId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to accept challenge")
            Result.failure(e)
        }
    }

    /**
     * Complete a challenge with results
     */
    suspend fun completeChallenge(
        challengeId: String,
        challengerScore: Long,
        challengedScore: Long
    ): Result<Unit> {
        return try {
            val challenge = _challenges.value.find { it.id == challengeId }
            if (challenge == null) {
                return Result.failure(IllegalArgumentException("Challenge not found"))
            }

            val winnerId = when {
                challengerScore > challengedScore -> challenge.challengerId
                challengedScore > challengerScore -> challenge.challengedId
                else -> challenge.challengerId // Tie goes to challenger
            }

            val result = ChallengeResult(
                winnerId = winnerId,
                challengerScore = challengerScore,
                challengedScore = challengedScore,
                completedAt = System.currentTimeMillis()
            )

            val currentChallenges = _challenges.value.toMutableList()
            val index = currentChallenges.indexOf(challenge)
            currentChallenges[index] = challenge.copy(
                status = ChallengeStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                result = result
            )
            _challenges.value = currentChallenges

            // Create activities for both players
            createSocialActivity(
                playerId = challenge.challengerId,
                type = ActivityType.CHALLENGE_COMPLETED,
                title = "Challenge Completed",
                description = "Challenge completed with score: $challengerScore",
                visibility = ActivityVisibility.FRIENDS
            )

            createSocialActivity(
                playerId = challenge.challengedId,
                type = ActivityType.CHALLENGE_COMPLETED,
                description = "Challenge completed with score: $challengedScore",
                visibility = ActivityVisibility.FRIENDS
            )

            Timber.d("Completed challenge: $challengeId, winner: $winnerId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to complete challenge")
            Result.failure(e)
        }
    }

    /**
     * Get friends list for a player
     */
    fun getFriends(playerId: String): Flow<List<SocialProfile>> {
        return combine(_friendships, _socialProfiles) { friendships, profiles ->
            val friendIds = friendships.filter {
                it.playerId == playerId && it.status == FriendshipStatus.ACCEPTED
            }.map { it.friendId }

            profiles.filter { it.id in friendIds }
        }
    }

    /**
     * Get online friends
     */
    fun getOnlineFriends(playerId: String): Flow<List<SocialProfile>> {
        return getFriends(playerId).map { friends ->
            friends.filter { it.isOnline }
        }
    }

    /**
     * Get friend requests for a player
     */
    fun getFriendRequests(playerId: String): Flow<List<FriendRequest>> {
        return _friendRequests.map { requests ->
            requests.filter { it.toPlayerId == playerId }
        }
    }

    /**
     * Get active challenges for a player
     */
    fun getActiveChallenges(playerId: String): Flow<List<Challenge>> {
        return _challenges.map { challenges ->
            challenges.filter {
                (it.challengerId == playerId || it.challengedId == playerId) &&
                it.status in listOf(ChallengeStatus.PENDING, ChallengeStatus.ACCEPTED)
            }
        }
    }

    /**
     * Get social activity feed for a player
     */
    fun getActivityFeed(playerId: String, limit: Int = 50): Flow<List<SocialActivity>> {
        return combine(_socialActivities, getFriends(playerId)) { activities, friends ->
            val friendIds = friends.map { it.id }.toSet() + playerId

            activities
                .filter { activity ->
                    when (activity.visibility) {
                        ActivityVisibility.PUBLIC -> true
                        ActivityVisibility.FRIENDS -> activity.playerId in friendIds
                        ActivityVisibility.PRIVATE -> activity.playerId == playerId
                    }
                }
                .sortedByDescending { it.createdAt }
                .take(limit)
        }
    }

    /**
     * Create a social activity
     */
    private suspend fun createSocialActivity(
        playerId: String,
        type: ActivityType,
        title: String,
        description: String,
        visibility: ActivityVisibility = ActivityVisibility.FRIENDS,
        metadata: Map<String, Any> = emptyMap()
    ) {
        try {
            val activity = SocialActivity(
                id = UUID.randomUUID().toString(),
                playerId = playerId,
                type = type,
                title = title,
                description = description,
                metadata = metadata,
                visibility = visibility
            )

            val currentActivities = _socialActivities.value.toMutableList()
            currentActivities.add(0, activity) // Add to beginning

            // Keep only recent activities
            if (currentActivities.size > _socialConfig.value.activityFeedSize) {
                currentActivities.removeAt(currentActivities.lastIndex)
            }

            _socialActivities.value = currentActivities

        } catch (e: Exception) {
            Timber.e(e, "Failed to create social activity")
        }
    }

    /**
     * Get friends count for a player
     */
    private fun getFriendsCount(playerId: String): Int {
        return _friendships.value.count {
            it.playerId == playerId && it.status == FriendshipStatus.ACCEPTED
        }
    }

    /**
     * Update friend counts for both players
     */
    private fun updateFriendCounts(playerId1: String, playerId2: String) {
        // Update social profiles with new friend counts
        val currentProfiles = _socialProfiles.value.toMutableList()

        currentProfiles.forEachIndexed { index, profile ->
            when (profile.id) {
                playerId1, playerId2 -> {
                    val friendCount = getFriendsCount(profile.id)
                    currentProfiles[index] = profile.copy(friendsCount = friendCount)
                }
            }
        }

        _socialProfiles.value = currentProfiles
    }

    /**
     * Load social profiles (simulated)
     */
    private suspend fun loadSocialProfiles() {
        val sampleProfiles = listOf(
            SocialProfile(
                id = "player_1",
                name = "GameMaster2024",
                displayName = "Game Master",
                level = 45,
                experience = 125000,
                totalScore = 500000,
                gamesPlayed = 150,
                achievementsUnlocked = 89,
                friendsCount = 25,
                isOnline = true
            ),
            SocialProfile(
                id = "player_2",
                name = "PuzzleQueen",
                displayName = "Puzzle Queen",
                level = 38,
                experience = 95000,
                totalScore = 380000,
                gamesPlayed = 120,
                achievementsUnlocked = 76,
                friendsCount = 18,
                isOnline = false,
                lastSeenAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000) // 2 hours ago
            )
        )

        _socialProfiles.value = sampleProfiles
        Timber.d("Loaded ${sampleProfiles.size} social profiles")
    }

    /**
     * Load friendships (simulated)
     */
    private suspend fun loadFriendships() {
        val sampleFriendships = listOf(
            Friendship(
                id = "friendship_1",
                playerId = "player_1",
                friendId = "player_2",
                status = FriendshipStatus.ACCEPTED,
                initiatedBy = "player_1",
                acceptedAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
            )
        )

        _friendships.value = sampleFriendships
        Timber.d("Loaded ${sampleFriendships.size} friendships")
    }

    /**
     * Load social activities (simulated)
     */
    private suspend fun loadSocialActivities() {
        val sampleActivities = listOf(
            SocialActivity(
                id = "activity_1",
                playerId = "player_1",
                type = ActivityType.ACHIEVEMENT_UNLOCKED,
                title = "Achievement Unlocked!",
                description = "Unlocked 'Puzzle Master' achievement",
                createdAt = System.currentTimeMillis() - (30 * 60 * 1000) // 30 minutes ago
            ),
            SocialActivity(
                id = "activity_2",
                playerId = "player_2",
                type = ActivityType.HIGH_SCORE,
                title = "New High Score!",
                description = "Achieved a new personal best in Mind Bender",
                createdAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000) // 2 hours ago
            )
        )

        _socialActivities.value = sampleActivities
        Timber.d("Loaded ${sampleActivities.size} social activities")
    }
}