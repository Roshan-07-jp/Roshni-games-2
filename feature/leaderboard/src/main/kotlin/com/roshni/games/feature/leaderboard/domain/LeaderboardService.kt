package com.roshni.games.feature.leaderboard.domain

import com.roshni.games.feature.leaderboard.data.model.Leaderboard
import com.roshni.games.feature.leaderboard.data.model.LeaderboardConfig
import com.roshni.games.feature.leaderboard.data.model.LeaderboardEntry
import com.roshni.games.feature.leaderboard.data.model.LeaderboardFilter
import com.roshni.games.feature.leaderboard.data.model.LeaderboardScope
import com.roshni.games.feature.leaderboard.data.model.LeaderboardSort
import com.roshni.games.feature.leaderboard.data.model.LeaderboardStats
import com.roshni.games.feature.leaderboard.data.model.LeaderboardType
import com.roshni.games.feature.leaderboard.data.model.PlayerLeaderboardPosition
import com.roshni.games.feature.leaderboard.data.model.TimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID

/**
 * Service for managing leaderboards with real-time updates
 */
class LeaderboardService {

    private val _leaderboards = MutableStateFlow<List<Leaderboard>>(emptyList())
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    private val _leaderboardConfig = MutableStateFlow(
        LeaderboardConfig(
            realTimeUpdates = true,
            updateIntervalSeconds = 30,
            cacheDurationMinutes = 5,
            maxCachedEntries = 1000,
            enableNotifications = true,
            notificationThreshold = 10
        )
    )

    private val _leaderboardStats = MutableStateFlow<Map<String, LeaderboardStats>>(emptyMap())
    private val _playerPositions = MutableStateFlow<Map<String, PlayerLeaderboardPosition>>(emptyMap())
    private val _realTimeUpdates = MutableStateFlow<List<com.roshni.games.feature.leaderboard.data.model.LeaderboardUpdate>>(emptyList())

    // Public flows
    val leaderboards: StateFlow<List<Leaderboard>> = _leaderboards.asStateFlow()
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()
    val leaderboardConfig: StateFlow<LeaderboardConfig> = _leaderboardConfig.asStateFlow()
    val leaderboardStats: StateFlow<Map<String, LeaderboardStats>> = _leaderboardStats.asStateFlow()
    val playerPositions: StateFlow<Map<String, PlayerLeaderboardPosition>> = _playerPositions.asStateFlow()
    val realTimeUpdates: StateFlow<List<com.roshni.games.feature.leaderboard.data.model.LeaderboardUpdate>> = _realTimeUpdates.asStateFlow()

    /**
     * Initialize the leaderboard service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing LeaderboardService")

            // Load leaderboards and entries
            loadLeaderboards()
            loadLeaderboardEntries()

            // Start real-time updates if enabled
            if (_leaderboardConfig.value.realTimeUpdates) {
                startRealTimeUpdates()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize LeaderboardService")
            Result.failure(e)
        }
    }

    /**
     * Get leaderboard entries with filtering and sorting
     */
    fun getLeaderboardEntries(
        leaderboardId: String,
        filter: LeaderboardFilter = LeaderboardFilter(),
        sortBy: LeaderboardSort = LeaderboardSort.RANK_ASC,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<LeaderboardEntry>> {
        return _leaderboardEntries.map { entries ->
            // Filter entries
            var filteredEntries = entries.filter { it.id.startsWith(leaderboardId) }

            // Apply additional filters
            if (filter.gameId != null) {
                filteredEntries = filteredEntries.filter { it.gameId == filter.gameId }
            }

            if (filter.gameMode != null) {
                filteredEntries = filteredEntries.filter { it.gameMode == filter.gameMode }
            }

            if (filter.difficulty != null) {
                filteredEntries = filteredEntries.filter { it.difficulty == filter.difficulty }
            }

            if (filter.timeRange != null) {
                filteredEntries = filteredEntries.filter { entry ->
                    entry.achievedAt in filter.timeRange.startTime..filter.timeRange.endTime
                }
            }

            if (filter.playerIds.isNotEmpty()) {
                filteredEntries = filteredEntries.filter { it.playerId in filter.playerIds }
            }

            // Sort entries
            val sortedEntries = when (sortBy) {
                LeaderboardSort.RANK_ASC -> filteredEntries.sortedBy { it.rank }
                LeaderboardSort.RANK_DESC -> filteredEntries.sortedByDescending { it.rank }
                LeaderboardSort.SCORE_ASC -> filteredEntries.sortedBy { it.score }
                LeaderboardSort.SCORE_DESC -> filteredEntries.sortedByDescending { it.score }
                LeaderboardSort.NAME_ASC -> filteredEntries.sortedBy { it.playerName }
                LeaderboardSort.NAME_DESC -> filteredEntries.sortedByDescending { it.playerName }
                LeaderboardSort.RECENT_ASC -> filteredEntries.sortedBy { it.achievedAt }
                LeaderboardSort.RECENT_DESC -> filteredEntries.sortedByDescending { it.achievedAt }
            }

            // Apply pagination
            sortedEntries.drop(offset).take(limit)
        }
    }

    /**
     * Submit a score to a leaderboard
     */
    suspend fun submitScore(
        playerId: String,
        playerName: String,
        score: Long,
        gameId: String? = null,
        gameMode: String? = null,
        difficulty: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): Result<String> {
        return try {
            // Find appropriate leaderboards for this score
            val applicableLeaderboards = findApplicableLeaderboards(gameId, gameMode, difficulty)

            if (applicableLeaderboards.isEmpty()) {
                return Result.failure(IllegalArgumentException("No applicable leaderboards found"))
            }

            val entryId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            // Create entries for each applicable leaderboard
            val newEntries = applicableLeaderboards.map { leaderboard ->
                LeaderboardEntry(
                    id = "${leaderboard.id}_$entryId",
                    playerId = playerId,
                    playerName = playerName,
                    score = score,
                    rank = 0, // Will be calculated
                    gameId = gameId,
                    gameMode = gameMode,
                    difficulty = difficulty,
                    metadata = metadata,
                    achievedAt = currentTime
                )
            }

            // Add entries to the leaderboard
            val currentEntries = _leaderboardEntries.value.toMutableList()
            currentEntries.addAll(newEntries)
            _leaderboardEntries.value = currentEntries

            // Recalculate ranks for affected leaderboards
            applicableLeaderboards.forEach { leaderboard ->
                recalculateRanks(leaderboard.id)
            }

            // Create real-time updates
            createRealTimeUpdates(newEntries)

            // Update player positions
            updatePlayerPositions(playerId)

            // Update leaderboard stats
            updateLeaderboardStats(applicableLeaderboards)

            Timber.d("Submitted score $score for player $playerName to ${applicableLeaderboards.size} leaderboards")
            Result.success(entryId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to submit score")
            Result.failure(e)
        }
    }

    /**
     * Get player's rank in a specific leaderboard
     */
    fun getPlayerRank(leaderboardId: String, playerId: String): Flow<Int?> {
        return _leaderboardEntries.map { entries ->
            entries.filter { it.id.startsWith(leaderboardId) }
                .find { it.playerId == playerId }
                ?.rank
        }
    }

    /**
     * Get player's position across all leaderboards
     */
    fun getPlayerLeaderboardPosition(playerId: String): Flow<PlayerLeaderboardPosition?> {
        return _playerPositions.map { positions ->
            positions[playerId]
        }
    }

    /**
     * Get top players in a leaderboard
     */
    fun getTopPlayers(
        leaderboardId: String,
        limit: Int = 10
    ): Flow<List<LeaderboardEntry>> {
        return getLeaderboardEntries(
            leaderboardId = leaderboardId,
            sortBy = LeaderboardSort.RANK_ASC,
            limit = limit
        )
    }

    /**
     * Get players around a specific rank
     */
    fun getPlayersAroundRank(
        leaderboardId: String,
        targetRank: Int,
        range: Int = 5
    ): Flow<List<LeaderboardEntry>> {
        return getLeaderboardEntries(
            leaderboardId = leaderboardId,
            sortBy = LeaderboardSort.RANK_ASC,
            limit = range * 2 + 1,
            offset = maxOf(0, targetRank - range - 1)
        )
    }

    /**
     * Get leaderboard statistics
     */
    fun getLeaderboardStats(leaderboardId: String): Flow<LeaderboardStats?> {
        return _leaderboardStats.map { stats ->
            stats[leaderboardId]
        }
    }

    /**
     * Update leaderboard configuration
     */
    suspend fun updateLeaderboardConfig(config: LeaderboardConfig): Result<Unit> {
        return try {
            _leaderboardConfig.value = config

            if (config.realTimeUpdates) {
                startRealTimeUpdates()
            } else {
                stopRealTimeUpdates()
            }

            Timber.d("Updated leaderboard config: $config")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update leaderboard config")
            Result.failure(e)
        }
    }

    /**
     * Get real-time updates for leaderboards
     */
    fun getRealTimeUpdates(playerId: String? = null): Flow<List<com.roshni.games.feature.leaderboard.data.model.LeaderboardUpdate>> {
        return _realTimeUpdates.map { updates ->
            if (playerId != null) {
                updates.filter { update ->
                    update.playerId == playerId || update.updateType == com.roshni.games.feature.leaderboard.data.model.UpdateType.RANK_CHANGE
                }
            } else {
                updates
            }
        }
    }

    /**
     * Find applicable leaderboards for a score
     */
    private fun findApplicableLeaderboards(
        gameId: String?,
        gameMode: String?,
        difficulty: String?
    ): List<Leaderboard> {
        return _leaderboards.value.filter { leaderboard ->
            (leaderboard.gameId == null || leaderboard.gameId == gameId) &&
            (leaderboard.gameMode == null || leaderboard.gameMode == gameMode) &&
            leaderboard.isActive
        }
    }

    /**
     * Recalculate ranks for a leaderboard
     */
    private fun recalculateRanks(leaderboardId: String) {
        val entries = _leaderboardEntries.value
            .filter { it.id.startsWith(leaderboardId) }
            .sortedByDescending { it.score }

        entries.forEachIndexed { index, entry ->
            val entryIndex = _leaderboardEntries.value.indexOfFirst { it.id == entry.id }
            if (entryIndex >= 0) {
                val currentEntries = _leaderboardEntries.value.toMutableList()
                currentEntries[entryIndex] = entry.copy(rank = index + 1)
                _leaderboardEntries.value = currentEntries
            }
        }
    }

    /**
     * Create real-time updates for new entries
     */
    private fun createRealTimeUpdates(newEntries: List<LeaderboardEntry>) {
        val updates = newEntries.map { entry ->
            com.roshni.games.feature.leaderboard.data.model.LeaderboardUpdate(
                leaderboardId = entry.id.substringBefore("_"),
                entryId = entry.id,
                oldRank = 0,
                newRank = entry.rank,
                score = entry.score,
                playerId = entry.playerId,
                updateType = com.roshni.games.feature.leaderboard.data.model.UpdateType.NEW_ENTRY
            )
        }

        val currentUpdates = _realTimeUpdates.value.toMutableList()
        currentUpdates.addAll(updates)
        _realTimeUpdates.value = currentUpdates.takeLast(100) // Keep only recent updates
    }

    /**
     * Update player positions across all leaderboards
     */
    private fun updatePlayerPositions(playerId: String) {
        val positions = mutableMapOf<String, Int>()
        var bestRank = Int.MAX_VALUE
        var totalRank = 0
        var leaderboardCount = 0

        _leaderboards.value.forEach { leaderboard ->
            val rank = _leaderboardEntries.value
                .filter { it.id.startsWith(leaderboard.id) }
                .find { it.playerId == playerId }
                ?.rank

            if (rank != null) {
                positions[leaderboard.id] = rank
                bestRank = minOf(bestRank, rank)
                totalRank += rank
                leaderboardCount++
            }
        }

        val averageRank = if (leaderboardCount > 0) {
            totalRank.toFloat() / leaderboardCount
        } else 0f

        val playerPosition = PlayerLeaderboardPosition(
            playerId = playerId,
            positions = positions,
            bestRank = bestRank,
            averageRank = averageRank,
            totalLeaderboards = leaderboardCount
        )

        val currentPositions = _playerPositions.value.toMutableMap()
        currentPositions[playerId] = playerPosition
        _playerPositions.value = currentPositions
    }

    /**
     * Update statistics for leaderboards
     */
    private fun updateLeaderboardStats(leaderboards: List<Leaderboard>) {
        leaderboards.forEach { leaderboard ->
            val entries = _leaderboardEntries.value.filter { it.id.startsWith(leaderboard.id) }

            if (entries.isNotEmpty()) {
                val stats = LeaderboardStats(
                    totalEntries = entries.size,
                    activePlayers = entries.distinctBy { it.playerId }.size,
                    averageScore = entries.map { it.score }.average().toLong(),
                    highestScore = entries.maxOf { it.score },
                    lowestScore = entries.minOf { it.score },
                    lastUpdate = System.currentTimeMillis(),
                    updateFrequency = calculateUpdateFrequency(leaderboard.id)
                )

                val currentStats = _leaderboardStats.value.toMutableMap()
                currentStats[leaderboard.id] = stats
                _leaderboardStats.value = currentStats
            }
        }
    }

    /**
     * Calculate update frequency for a leaderboard
     */
    private fun calculateUpdateFrequency(leaderboardId: String): Long {
        // In real implementation, this would track actual update frequency
        return 12 // 12 updates per hour as example
    }

    /**
     * Start real-time updates
     */
    private fun startRealTimeUpdates() {
        Timber.d("Starting real-time leaderboard updates")
        // In real implementation, this would start WebSocket or periodic polling
    }

    /**
     * Stop real-time updates
     */
    private fun stopRealTimeUpdates() {
        Timber.d("Stopping real-time leaderboard updates")
        // In real implementation, this would stop WebSocket or periodic polling
    }

    /**
     * Load leaderboards from data source
     */
    private suspend fun loadLeaderboards() {
        // In real implementation, this would load from database or API
        val sampleLeaderboards = listOf(
            Leaderboard(
                id = "global_high_score",
                name = "Global High Scores",
                description = "All-time highest scores across all games",
                type = LeaderboardType.HIGH_SCORE,
                scope = LeaderboardScope.GLOBAL,
                resetFrequency = ResetFrequency.NEVER,
                maxEntries = 1000
            ),
            Leaderboard(
                id = "weekly_puzzle",
                name = "Weekly Puzzle Champions",
                description = "Top puzzle scores this week",
                type = LeaderboardType.HIGH_SCORE,
                scope = LeaderboardScope.GLOBAL,
                resetFrequency = ResetFrequency.WEEKLY,
                maxEntries = 100
            )
        )

        _leaderboards.value = sampleLeaderboards
        Timber.d("Loaded ${sampleLeaderboards.size} leaderboards")
    }

    /**
     * Load leaderboard entries from data source
     */
    private suspend fun loadLeaderboardEntries() {
        // In real implementation, this would load from database or API
        val sampleEntries = listOf(
            LeaderboardEntry(
                id = "global_high_score_entry_1",
                playerId = "player_1",
                playerName = "GameMaster2024",
                score = 50000,
                rank = 1,
                achievedAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000) // 2 hours ago
            ),
            LeaderboardEntry(
                id = "global_high_score_entry_2",
                playerId = "player_2",
                playerName = "PuzzleQueen",
                score = 45000,
                rank = 2,
                achievedAt = System.currentTimeMillis() - (4 * 60 * 60 * 1000) // 4 hours ago
            )
        )

        _leaderboardEntries.value = sampleEntries
        Timber.d("Loaded ${sampleEntries.size} leaderboard entries")
    }
}