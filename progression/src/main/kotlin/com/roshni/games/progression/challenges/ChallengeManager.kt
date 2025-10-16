package com.roshni.games.progression.challenges

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages daily/weekly challenges and seasonal events
 */
class ChallengeManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Challenge definitions
    private val challengeDefinitions = ConcurrentHashMap<String, ChallengeDefinition>()

    // Player challenges
    private val playerChallenges = ConcurrentHashMap<String, MutableMap<String, PlayerChallenge>>()

    // Seasonal events
    private val seasonalEvents = ConcurrentHashMap<String, SeasonalEvent>()

    // Challenge events
    private val _challengeEvents = MutableSharedFlow<ChallengeEvent>(extraBufferCapacity = 100)
    val challengeEvents: SharedFlow<ChallengeEvent> = _challengeEvents.asSharedFlow()

    init {
        initializeChallenges()
        initializeSeasonalEvents()
        startDailyReset()
        startWeeklyReset()
    }

    /**
     * Get daily challenges for player
     */
    fun getDailyChallenges(playerId: String): List<PlayerChallenge> {
        return getPlayerChallenges(playerId).filter { it.definition.type == ChallengeType.DAILY }
    }

    /**
     * Get weekly challenges for player
     */
    fun getWeeklyChallenges(playerId: String): List<PlayerChallenge> {
        return getPlayerChallenges(playerId).filter { it.definition.type == ChallengeType.WEEKLY }
    }

    /**
     * Get seasonal challenges for player
     */
    fun getSeasonalChallenges(playerId: String): List<PlayerChallenge> {
        return getPlayerChallenges(playerId).filter { it.definition.type == ChallengeType.SEASONAL }
    }

    /**
     * Update challenge progress
     */
    fun updateChallengeProgress(playerId: String, challengeId: String, progress: Int) {
        scope.launch {
            try {
                val playerChallengeMap = playerChallenges.getOrPut(playerId) { mutableMapOf() }
                val playerChallenge = playerChallengeMap[challengeId]

                if (playerChallenge != null && !playerChallenge.isCompleted) {
                    val newProgress = (playerChallenge.progress + progress).coerceAtMost(playerChallenge.definition.targetValue)
                    val updatedChallenge = playerChallenge.copy(progress = newProgress)

                    playerChallengeMap[challengeId] = updatedChallenge

                    // Check if challenge is completed
                    if (newProgress >= playerChallenge.definition.targetValue && !playerChallenge.isCompleted) {
                        completeChallenge(playerId, updatedChallenge)
                    }

                    _challengeEvents.emit(ChallengeEvent.ChallengeProgressUpdated(playerId, challengeId, newProgress))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update challenge progress")
            }
        }
    }

    /**
     * Get current seasonal event
     */
    fun getCurrentSeasonalEvent(): SeasonalEvent? {
        val now = System.currentTimeMillis()
        return seasonalEvents.values.find { event ->
            now >= event.startTime && now <= event.endTime
        }
    }

    /**
     * Get player challenge progress
     */
    fun getPlayerChallenge(playerId: String, challengeId: String): PlayerChallenge? {
        return playerChallenges[playerId]?.get(challengeId)
    }

    /**
     * Claim challenge reward
     */
    fun claimChallengeReward(playerId: String, challengeId: String): Result<ChallengeReward> {
        return try {
            val playerChallenge = getPlayerChallenge(playerId, challengeId)
            if (playerChallenge == null) {
                return Result.failure(IllegalArgumentException("Challenge not found"))
            }

            if (!playerChallenge.isCompleted) {
                return Result.failure(IllegalStateException("Challenge not completed"))
            }

            if (playerChallenge.rewardClaimed) {
                return Result.failure(IllegalStateException("Reward already claimed"))
            }

            // Mark reward as claimed
            val playerChallengeMap = playerChallenges.getOrPut(playerId) { mutableMapOf() }
            val updatedChallenge = playerChallenge.copy(rewardClaimed = true)
            playerChallengeMap[challengeId] = updatedChallenge

            val reward = playerChallenge.definition.reward

            scope.launch {
                _challengeEvents.emit(ChallengeEvent.ChallengeRewardClaimed(playerId, challengeId, reward))
            }

            Result.success(reward)
        } catch (e: Exception) {
            Timber.e(e, "Failed to claim challenge reward")
            Result.failure(e)
        }
    }

    private fun initializeChallenges() {
        // Daily Challenges
        challengeDefinitions["daily_score_5000"] = ChallengeDefinition(
            id = "daily_score_5000",
            name = "Daily High Scorer",
            description = "Score 5000 points today",
            type = ChallengeType.DAILY,
            targetValue = 5000,
            reward = ChallengeReward(xp = 100, coins = 50, items = emptyList())
        )

        challengeDefinitions["daily_games_5"] = ChallengeDefinition(
            id = "daily_games_5",
            name = "Game Explorer",
            description = "Play 5 different games today",
            type = ChallengeType.DAILY,
            targetValue = 5,
            reward = ChallengeReward(xp = 150, coins = 75, items = emptyList())
        )

        challengeDefinitions["daily_puzzle_master"] = ChallengeDefinition(
            id = "daily_puzzle_master",
            name = "Puzzle Master",
            description = "Complete 3 puzzle games today",
            type = ChallengeType.DAILY,
            targetValue = 3,
            reward = ChallengeReward(xp = 200, coins = 100, items = emptyList())
        )

        // Weekly Challenges
        challengeDefinitions["weekly_score_50000"] = ChallengeDefinition(
            id = "weekly_score_50000",
            name = "Weekly Champion",
            description = "Score 50,000 points this week",
            type = ChallengeType.WEEKLY,
            targetValue = 50000,
            reward = ChallengeReward(xp = 500, coins = 250, items = listOf("golden_skin"))
        )

        challengeDefinitions["weekly_achievements_10"] = ChallengeDefinition(
            id = "weekly_achievements_10",
            name = "Achievement Hunter",
            description = "Unlock 10 achievements this week",
            type = ChallengeType.WEEKLY,
            targetValue = 10,
            reward = ChallengeReward(xp = 400, coins = 200, items = emptyList())
        )

        // Seasonal Challenges
        challengeDefinitions["seasonal_winter_wins"] = ChallengeDefinition(
            id = "seasonal_winter_wins",
            name = "Winter Champion",
            description = "Win 100 games during Winter season",
            type = ChallengeType.SEASONAL,
            targetValue = 100,
            reward = ChallengeReward(xp = 1000, coins = 500, items = listOf("winter_theme"))
        )

        Timber.d("Initialized ${challengeDefinitions.size} challenges")
    }

    private fun initializeSeasonalEvents() {
        val now = System.currentTimeMillis()
        val weekInMillis = 7 * 24 * 60 * 60 * 1000L

        // Winter Event
        seasonalEvents["winter_2024"] = SeasonalEvent(
            id = "winter_2024",
            name = "Winter Wonderland",
            description = "Special winter-themed challenges and rewards",
            startTime = now,
            endTime = now + (30 * weekInMillis), // 30 weeks
            theme = "winter",
            specialRewards = listOf("winter_avatar", "snow_theme", "holiday_music")
        )

        Timber.d("Initialized ${seasonalEvents.size} seasonal events")
    }

    private fun getPlayerChallenges(playerId: String): List<PlayerChallenge> {
        return playerChallenges.getOrPut(playerId) { mutableMapOf() }.values.toList()
    }

    private fun completeChallenge(playerId: String, challenge: PlayerChallenge) {
        val completedChallenge = challenge.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )

        playerChallenges[playerId]?.set(challenge.definition.id, completedChallenge)

        scope.launch {
            _challengeEvents.emit(ChallengeEvent.ChallengeCompleted(playerId, challenge.definition))
        }

        Timber.d("Challenge completed: ${challenge.definition.name} for player $playerId")
    }

    private fun startDailyReset() {
        scope.launch {
            while (isActive) {
                try {
                    // Calculate time until next midnight
                    val now = System.currentTimeMillis()
                    val nextMidnight = getNextMidnight()
                    val delay = nextMidnight - now

                    delay(delay)

                    // Reset daily challenges
                    resetDailyChallenges()

                    _challengeEvents.emit(ChallengeEvent.DailyChallengesReset)
                } catch (e: Exception) {
                    Timber.e(e, "Error in daily reset")
                }
            }
        }
    }

    private fun startWeeklyReset() {
        scope.launch {
            while (isActive) {
                try {
                    // Calculate time until next Monday
                    val now = System.currentTimeMillis()
                    val nextMonday = getNextMonday()
                    val delay = nextMonday - now

                    delay(delay)

                    // Reset weekly challenges
                    resetWeeklyChallenges()

                    _challengeEvents.emit(ChallengeEvent.WeeklyChallengesReset)
                } catch (e: Exception) {
                    Timber.e(e, "Error in weekly reset")
                }
            }
        }
    }

    private fun resetDailyChallenges() {
        playerChallenges.values.forEach { playerChallengeMap ->
            playerChallengeMap.values.filter { it.definition.type == ChallengeType.DAILY }
                .forEach { challenge ->
                    playerChallengeMap[challenge.definition.id] = challenge.copy(
                        progress = 0,
                        isCompleted = false,
                        completedAt = null,
                        rewardClaimed = false
                    )
                }
        }

        Timber.d("Reset daily challenges for all players")
    }

    private fun resetWeeklyChallenges() {
        playerChallenges.values.forEach { playerChallengeMap ->
            playerChallengeMap.values.filter { it.definition.type == ChallengeType.WEEKLY }
                .forEach { challenge ->
                    playerChallengeMap[challenge.definition.id] = challenge.copy(
                        progress = 0,
                        isCompleted = false,
                        completedAt = null,
                        rewardClaimed = false
                    )
                }
        }

        Timber.d("Reset weekly challenges for all players")
    }

    private fun getNextMidnight(): Long {
        val now = System.currentTimeMillis()
        val midnight = kotlinx.datetime.LocalDateTime(
            year = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).year,
            month = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).month,
            dayOfMonth = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).dayOfMonth + 1,
            hour = 0,
            minute = 0,
            second = 0
        )
        return midnight.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    private fun getNextMonday(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val daysUntilMonday = (8 - calendar.get(java.util.Calendar.DAY_OF_WEEK)) % 7
        calendar.add(java.util.Calendar.DAY_OF_MONTH, if (daysUntilMonday == 0) 7 else daysUntilMonday)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        challengeDefinitions.clear()
        playerChallenges.clear()
        seasonalEvents.clear()
        scope.cancel()
    }
}

/**
 * Challenge definition
 */
@Serializable
data class ChallengeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val type: ChallengeType,
    val targetValue: Int,
    val reward: ChallengeReward,
    val icon: String = "🎯",
    val category: ChallengeCategory = ChallengeCategory.GENERAL
)

/**
 * Player challenge progress
 */
@Serializable
data class PlayerChallenge(
    val playerId: String,
    val definition: ChallengeDefinition,
    val progress: Int,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val rewardClaimed: Boolean,
    val startedAt: Long = System.currentTimeMillis()
)

/**
 * Challenge types
 */
enum class ChallengeType {
    DAILY, WEEKLY, SEASONAL
}

/**
 * Challenge categories
 */
enum class ChallengeCategory {
    GENERAL, PUZZLE, ACTION, STRATEGY, ARCADE, CARD, TRIVIA, SIMULATION, CASUAL
}

/**
 * Challenge reward
 */
@Serializable
data class ChallengeReward(
    val xp: Int,
    val coins: Int,
    val items: List<String>
)

/**
 * Seasonal event
 */
@Serializable
data class SeasonalEvent(
    val id: String,
    val name: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val theme: String,
    val specialRewards: List<String>,
    val isActive: Boolean = true
)

/**
 * Challenge events
 */
sealed class ChallengeEvent {
    data class ChallengeProgressUpdated(val playerId: String, val challengeId: String, val progress: Int) : ChallengeEvent()
    data class ChallengeCompleted(val playerId: String, val definition: ChallengeDefinition) : ChallengeEvent()
    data class ChallengeRewardClaimed(val playerId: String, val challengeId: String, val reward: ChallengeReward) : ChallengeEvent()
    data object DailyChallengesReset : ChallengeEvent()
    data object WeeklyChallengesReset : ChallengeEvent()
}