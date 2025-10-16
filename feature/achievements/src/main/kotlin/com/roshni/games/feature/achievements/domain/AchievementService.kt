package com.roshni.games.feature.achievements.domain

import com.roshni.games.feature.achievements.data.model.Achievement
import com.roshni.games.feature.achievements.data.model.AchievementCategory
import com.roshni.games.feature.achievements.data.model.AchievementFilter
import com.roshni.games.feature.achievements.data.model.AchievementNotification
import com.roshni.games.feature.achievements.data.model.AchievementProgressUpdate
import com.roshni.games.feature.achievements.data.model.AchievementRarity
import com.roshni.games.feature.achievements.data.model.AchievementSort
import com.roshni.games.feature.achievements.data.model.AchievementStats
import com.roshni.games.feature.achievements.data.model.AchievementType
import com.roshni.games.feature.achievements.data.model.NotificationType
import com.roshni.games.feature.achievements.data.model.PlayerAchievement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID

/**
 * Service for managing achievements and progress tracking
 */
class AchievementService {

    private val _allAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    private val _playerAchievements = MutableStateFlow<List<PlayerAchievement>>(emptyList())
    private val _achievementNotifications = MutableStateFlow<List<AchievementNotification>>(emptyList())
    private val _achievementStats = MutableStateFlow(AchievementStats())

    // Public flows
    val allAchievements: StateFlow<List<Achievement>> = _allAchievements.asStateFlow()
    val playerAchievements: StateFlow<List<PlayerAchievement>> = _playerAchievements.asStateFlow()
    val achievementNotifications: StateFlow<List<AchievementNotification>> = _achievementNotifications.asStateFlow()
    val achievementStats: StateFlow<AchievementStats> = _achievementStats.asStateFlow()

    /**
     * Initialize the achievement service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing AchievementService")

            // Load achievements and player progress
            loadAchievements()
            loadPlayerProgress("current_player") // In real implementation, get from auth

            // Calculate initial stats
            calculateAchievementStats()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AchievementService")
            Result.failure(e)
        }
    }

    /**
     * Update achievement progress
     */
    suspend fun updateProgress(
        playerId: String,
        achievementId: String,
        newValue: Float
    ): Result<Boolean> {
        return try {
            val achievement = _allAchievements.value.find { it.id == achievementId }
            if (achievement == null) {
                return Result.failure(IllegalArgumentException("Achievement not found: $achievementId"))
            }

            val currentProgress = _playerAchievements.value.find {
                it.playerId == playerId && it.achievementId == achievementId
            }

            val progress = minOf(newValue / achievement.targetValue, 1f)
            val isCompleted = progress >= 1f
            val wasCompleted = currentProgress?.isCompleted ?: false

            // Update or create player achievement
            val updatedPlayerAchievement = PlayerAchievement(
                playerId = playerId,
                achievementId = achievementId,
                progress = progress,
                currentValue = newValue,
                isCompleted = isCompleted,
                unlockedAt = if (isCompleted && !wasCompleted) System.currentTimeMillis() else currentProgress?.unlockedAt,
                lastUpdated = System.currentTimeMillis()
            )

            // Update in memory (in real implementation, save to database)
            val currentList = _playerAchievements.value.toMutableList()
            val existingIndex = currentList.indexOfFirst {
                it.playerId == playerId && it.achievementId == achievementId
            }

            if (existingIndex >= 0) {
                currentList[existingIndex] = updatedPlayerAchievement
            } else {
                currentList.add(updatedPlayerAchievement)
            }

            _playerAchievements.value = currentList

            // Create progress update record
            val progressUpdate = AchievementProgressUpdate(
                playerId = playerId,
                achievementId = achievementId,
                newValue = newValue
            )

            // Check if achievement was just completed
            if (isCompleted && !wasCompleted) {
                unlockAchievement(playerId, achievement)
            }

            // Recalculate stats
            calculateAchievementStats()

            Timber.d("Updated progress for achievement $achievementId: $newValue/$achievement.targetValue")
            Result.success(isCompleted && !wasCompleted) // Return true if newly completed

        } catch (e: Exception) {
            Timber.e(e, "Failed to update achievement progress")
            Result.failure(e)
        }
    }

    /**
     * Get achievements with filtering and sorting
     */
    fun getAchievements(
        playerId: String,
        filter: AchievementFilter = AchievementFilter(),
        sortBy: AchievementSort = AchievementSort.PROGRESS
    ): Flow<List<Pair<Achievement, PlayerAchievement?>>> {
        return combine(_allAchievements, _playerAchievements) { achievements, playerProgress ->
            // Filter achievements
            var filteredAchievements = achievements.filter { achievement ->
                // Category filter
                if (filter.categories.isNotEmpty() && achievement.category !in filter.categories) {
                    return@filter false
                }

                // Type filter
                if (filter.types.isNotEmpty() && achievement.type !in filter.types) {
                    return@filter false
                }

                // Rarity filter
                if (filter.rarities.isNotEmpty() && achievement.rarity !in filter.rarities) {
                    return@filter false
                }

                // Game filter
                if (filter.gameId != null && achievement.gameId != filter.gameId) {
                    return@filter false
                }

                // Search query
                if (filter.searchQuery != null) {
                    val query = filter.searchQuery.lowercase()
                    if (!achievement.name.lowercase().contains(query) &&
                        !achievement.description.lowercase().contains(query)) {
                        return@filter false
                    }
                }

                true
            }

            // Map to pairs with player progress
            filteredAchievements.map { achievement ->
                val playerProgress = playerProgress.find {
                    it.playerId == playerId && it.achievementId == achievement.id
                }
                achievement to playerProgress
            }
        }.map { achievementPairs ->
            // Apply completion filter
            var filteredPairs = when (filter.completed) {
                true -> achievementPairs.filter { it.second?.isCompleted == true }
                false -> achievementPairs.filter { it.second?.isCompleted != true }
                null -> achievementPairs
            }

            // Sort results
            filteredPairs = when (sortBy) {
                AchievementSort.NAME -> filteredPairs.sortedBy { it.first.name }
                AchievementSort.PROGRESS -> filteredPairs.sortedByDescending { it.second?.progress ?: 0f }
                AchievementSort.RARITY -> filteredPairs.sortedByDescending {
                    rarityToInt(it.first.rarity)
                }
                AchievementSort.POINTS -> filteredPairs.sortedByDescending { it.first.points }
                AchievementSort.RECENTLY_UNLOCKED -> filteredPairs.sortedByDescending {
                    it.second?.unlockedAt ?: 0
                }
                AchievementSort.CATEGORY -> filteredPairs.sortedBy { it.first.category.name }
            }

            filteredPairs
        }
    }

    /**
     * Get achievement progress for a specific achievement
     */
    fun getAchievementProgress(playerId: String, achievementId: String): Flow<PlayerAchievement?> {
        return _playerAchievements.map { playerAchievements ->
            playerAchievements.find {
                it.playerId == playerId && it.achievementId == achievementId
            }
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            val currentNotifications = _achievementNotifications.value.toMutableList()
            val index = currentNotifications.indexOfFirst { it.id == notificationId }

            if (index >= 0) {
                currentNotifications[index] = currentNotifications[index].copy(isRead = true)
                _achievementNotifications.value = currentNotifications
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
            Result.failure(e)
        }
    }

    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            _achievementNotifications.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear notifications")
            Result.failure(e)
        }
    }

    /**
     * Get recent achievements (unlocked in last 7 days)
     */
    fun getRecentAchievements(playerId: String): Flow<List<Pair<Achievement, PlayerAchievement>>> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        return getAchievements(playerId).map { achievements ->
            achievements.filter { (_, playerProgress) ->
                playerProgress?.isCompleted == true &&
                (playerProgress.unlockedAt ?: 0) > sevenDaysAgo
            }
        }
    }

    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(
        playerId: String,
        category: AchievementCategory
    ): Flow<List<Pair<Achievement, PlayerAchievement?>>> {
        return getAchievements(
            playerId = playerId,
            filter = AchievementFilter(categories = listOf(category))
        )
    }

    /**
     * Get achievements by rarity
     */
    fun getAchievementsByRarity(
        playerId: String,
        rarity: AchievementRarity
    ): Flow<List<Pair<Achievement, PlayerAchievement?>>> {
        return getAchievements(
            playerId = playerId,
            filter = AchievementFilter(rarities = listOf(rarity))
        )
    }

    /**
     * Unlock an achievement (internal method)
     */
    private suspend fun unlockAchievement(playerId: String, achievement: Achievement) {
        try {
            // Create notification
            val notification = AchievementNotification(
                id = UUID.randomUUID().toString(),
                playerId = playerId,
                achievementId = achievement.id,
                type = NotificationType.UNLOCKED,
                title = "Achievement Unlocked!",
                message = "${achievement.name}: ${achievement.description}"
            )

            val currentNotifications = _achievementNotifications.value.toMutableList()
            currentNotifications.add(0, notification) // Add to beginning
            _achievementNotifications.value = currentNotifications

            // In real implementation, trigger analytics event
            Timber.d("Achievement unlocked: ${achievement.name} for player: $playerId")

        } catch (e: Exception) {
            Timber.e(e, "Failed to unlock achievement: ${achievement.name}")
        }
    }

    /**
     * Load achievements from data source
     */
    private suspend fun loadAchievements() {
        // In real implementation, this would load from database or API
        val sampleAchievements = listOf(
            Achievement(
                id = "first_puzzle",
                name = "Puzzle Beginner",
                description = "Complete your first puzzle",
                category = AchievementCategory.GAMEPLAY,
                type = AchievementType.COUNT_BASED,
                targetValue = 1f,
                points = 10,
                rarity = AchievementRarity.COMMON
            ),
            Achievement(
                id = "score_master",
                name = "Score Master",
                description = "Achieve a score of 10,000 points",
                category = AchievementCategory.GAMEPLAY,
                type = AchievementType.SCORE_BASED,
                targetValue = 10000f,
                points = 50,
                rarity = AchievementRarity.RARE
            ),
            Achievement(
                id = "social_butterfly",
                name = "Social Butterfly",
                description = "Play multiplayer games with 10 different players",
                category = AchievementCategory.SOCIAL,
                type = AchievementType.SOCIAL_BASED,
                targetValue = 10f,
                points = 25,
                rarity = AchievementRarity.UNCOMMON
            )
        )

        _allAchievements.value = sampleAchievements
        Timber.d("Loaded ${sampleAchievements.size} achievements")
    }

    /**
     * Load player progress from data source
     */
    private suspend fun loadPlayerProgress(playerId: String) {
        // In real implementation, this would load from database
        // For now, start with empty progress
        Timber.d("Loaded progress for player: $playerId")
    }

    /**
     * Calculate achievement statistics
     */
    private suspend fun calculateAchievementStats() {
        val achievements = _allAchievements.value
        val playerProgress = _playerAchievements.value

        val totalAchievements = achievements.size
        val completedAchievements = playerProgress.count { it.isCompleted }
        val totalPoints = playerProgress.filter { it.isCompleted }
            .sumOf { achievement ->
                achievements.find { it.id == achievement.achievementId }?.points ?: 0
            }

        val completionRate = if (totalAchievements > 0) {
            completedAchievements.toFloat() / totalAchievements
        } else 0f

        val rareAchievementsUnlocked = playerProgress.count { progress ->
            progress.isCompleted && achievements.find { it.id == progress.achievementId }?.rarity in
                listOf(AchievementRarity.RARE, AchievementRarity.EPIC, AchievementRarity.LEGENDARY)
        }

        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val recentUnlocks = playerProgress.count { progress ->
            progress.isCompleted && (progress.unlockedAt ?: 0) > sevenDaysAgo
        }

        val categoryCounts = achievements.groupBy { it.category }
        val favoriteCategory = categoryCounts.maxByOrNull { it.value.size }?.key

        val stats = AchievementStats(
            totalAchievements = totalAchievements,
            completedAchievements = completedAchievements,
            totalPoints = totalPoints,
            completionRate = completionRate,
            rareAchievementsUnlocked = rareAchievementsUnlocked,
            recentUnlocks = recentUnlocks,
            favoriteCategory = favoriteCategory
        )

        _achievementStats.value = stats
        Timber.d("Calculated achievement stats: $stats")
    }

    /**
     * Convert rarity enum to int for sorting
     */
    private fun rarityToInt(rarity: AchievementRarity): Int {
        return when (rarity) {
            AchievementRarity.COMMON -> 1
            AchievementRarity.UNCOMMON -> 2
            AchievementRarity.RARE -> 3
            AchievementRarity.EPIC -> 4
            AchievementRarity.LEGENDARY -> 5
        }
    }
}