package com.roshni.games.multiplayer.leaderboard

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing leaderboards and player statistics
 */
class LeaderboardService {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Leaderboards by game and type
    private val leaderboards = ConcurrentHashMap<String, MutableMap<LeaderboardType, Leaderboard>>()

    // Player statistics
    private val playerStats = ConcurrentHashMap<String, PlayerStatistics>()

    // Leaderboard events
    private val _leaderboardEvents = MutableSharedFlow<LeaderboardEvent>(extraBufferCapacity = 100)
    val leaderboardEvents: SharedFlow<LeaderboardEvent> = _leaderboardEvents.asSharedFlow()

    /**
     * Submit game result for leaderboard update
     */
    fun submitGameResult(result: GameResult) {
        scope.launch {
            try {
                // Update player statistics
                updatePlayerStatistics(result)

                // Update leaderboards
                updateLeaderboards(result)

                // Emit event
                _leaderboardEvents.emit(LeaderboardEvent.GameResultSubmitted(result))

                Timber.d("Submitted game result for player ${result.playerId}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit game result")
            }
        }
    }

    /**
     * Get leaderboard for specific game and type
     */
    fun getLeaderboard(gameId: String, type: LeaderboardType, limit: Int = 100): Leaderboard {
        val gameLeaderboards = leaderboards.getOrPut(gameId) { mutableMapOf() }
        return gameLeaderboards.getOrPut(type) { createEmptyLeaderboard(gameId, type) }
    }

    /**
     * Get player rank in leaderboard
     */
    fun getPlayerRank(gameId: String, type: LeaderboardType, playerId: String): PlayerRank? {
        val leaderboard = getLeaderboard(gameId, type)

        return leaderboard.entries
            .sortedByDescending { it.score }
            .indexOfFirst { it.playerId == playerId }
            .takeIf { it >= 0 }
            ?.let { rank ->
                val entry = leaderboard.entries.find { it.playerId == playerId }
                PlayerRank(
                    rank = rank + 1,
                    playerId = playerId,
                    score = entry?.score ?: 0L,
                    totalPlayers = leaderboard.entries.size
                )
            }
    }

    /**
     * Get player statistics
     */
    fun getPlayerStatistics(playerId: String): PlayerStatistics {
        return playerStats.getOrPut(playerId) { PlayerStatistics(playerId) }
    }

    /**
     * Get top players across all games
     */
    fun getGlobalLeaderboard(limit: Int = 50): List<PlayerStatistics> {
        return playerStats.values
            .sortedByDescending { it.totalScore }
            .take(limit)
    }

    /**
     * Update player statistics after game result
     */
    private fun updatePlayerStatistics(result: GameResult) {
        val currentStats = playerStats.getOrPut(result.playerId) { PlayerStatistics(result.playerId) }
        val updatedStats = currentStats.copy(
            gamesPlayed = currentStats.gamesPlayed + 1,
            totalScore = currentStats.totalScore + result.score,
            averageScore = (currentStats.totalScore + result.score) / (currentStats.gamesPlayed + 1),
            lastPlayed = System.currentTimeMillis(),
            bestScore = maxOf(currentStats.bestScore, result.score),
            winRate = calculateWinRate(currentStats, result)
        )

        playerStats[result.playerId] = updatedStats
    }

    /**
     * Update leaderboards after game result
     */
    private fun updateLeaderboards(result: GameResult) {
        val gameLeaderboards = leaderboards.getOrPut(result.gameId) { mutableMapOf() }

        // Update different leaderboard types
        LeaderboardType.values().forEach { type ->
            val leaderboard = gameLeaderboards.getOrPut(type) { createEmptyLeaderboard(result.gameId, type) }
            val updatedLeaderboard = updateLeaderboard(leaderboard, result, type)
            gameLeaderboards[type] = updatedLeaderboard
        }
    }

    /**
     * Update specific leaderboard
     */
    private fun updateLeaderboard(
        leaderboard: Leaderboard,
        result: GameResult,
        type: LeaderboardType
    ): Leaderboard {
        val score = when (type) {
            LeaderboardType.HIGH_SCORE -> result.score
            LeaderboardType.WIN_STREAK -> result.winStreak
            LeaderboardType.GAMES_WON -> if (result.isWin) 1L else 0L
            LeaderboardType.TOTAL_GAMES -> 1L
        }

        val updatedEntries = leaderboard.entries.toMutableList()

        // Find existing entry or create new one
        val existingIndex = updatedEntries.indexOfFirst { it.playerId == result.playerId }
        val entry = if (existingIndex >= 0) {
            val existing = updatedEntries[existingIndex]
            existing.copy(
                score = maxOf(existing.score, score),
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            LeaderboardEntry(
                playerId = result.playerId,
                score = score,
                lastUpdated = System.currentTimeMillis()
            )
        }

        if (existingIndex >= 0) {
            updatedEntries[existingIndex] = entry
        } else {
            updatedEntries.add(entry)
        }

        // Sort and limit entries
        val sortedEntries = updatedEntries
            .sortedByDescending { it.score }
            .take(1000) // Keep top 1000 players

        return leaderboard.copy(
            entries = sortedEntries,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Calculate win rate for player
     */
    private fun calculateWinRate(stats: PlayerStatistics, result: GameResult): Float {
        val totalGames = stats.gamesPlayed + 1
        val totalWins = if (result.isWin) stats.wins + 1 else stats.wins
        return totalWins.toFloat() / totalGames
    }

    /**
     * Create empty leaderboard
     */
    private fun createEmptyLeaderboard(gameId: String, type: LeaderboardType): Leaderboard {
        return Leaderboard(
            gameId = gameId,
            type = type,
            entries = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Reset all leaderboards (for testing or maintenance)
     */
    fun resetLeaderboards() {
        leaderboards.clear()
        Timber.d("All leaderboards reset")
    }

    /**
     * Get leaderboard statistics
     */
    fun getLeaderboardStats(): LeaderboardStats {
        val totalPlayers = playerStats.size
        val totalGames = playerStats.values.sumOf { it.gamesPlayed }
        val averageScore = if (totalGames > 0) {
            playerStats.values.sumOf { it.totalScore } / totalGames
        } else 0.0

        return LeaderboardStats(
            totalPlayers = totalPlayers,
            totalGamesPlayed = totalGames,
            averageScore = averageScore,
            activeLeaderboards = leaderboards.values.sumOf { it.size }
        )
    }

    /**
     * Cleanup old data
     */
    fun cleanupOldData() {
        scope.launch {
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days

            // Remove old player stats
            playerStats.entries.removeAll { (_, stats) ->
                stats.lastPlayed < cutoffTime
            }

            // Remove old leaderboard entries
            leaderboards.forEach { (gameId, gameLeaderboards) ->
                gameLeaderboards.forEach { (type, leaderboard) ->
                    val updatedEntries = leaderboard.entries.filter {
                        it.lastUpdated > cutoffTime
                    }
                    gameLeaderboards[type] = leaderboard.copy(entries = updatedEntries)
                }
            }

            Timber.d("Cleaned up old leaderboard data")
        }
    }
}

/**
 * Game result for leaderboard submission
 */
@Serializable
data class GameResult(
    val playerId: String,
    val gameId: String,
    val score: Long,
    val isWin: Boolean,
    val winStreak: Int,
    val duration: Long,
    val difficulty: String,
    val gameMode: String,
    val statistics: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Leaderboard entry
 */
@Serializable
data class LeaderboardEntry(
    val playerId: String,
    val score: Long,
    val lastUpdated: Long
)

/**
 * Leaderboard data class
 */
@Serializable
data class Leaderboard(
    val gameId: String,
    val type: LeaderboardType,
    val entries: List<LeaderboardEntry>,
    val lastUpdated: Long
)

/**
 * Leaderboard types
 */
enum class LeaderboardType(val displayName: String) {
    HIGH_SCORE("High Score"),
    WIN_STREAK("Win Streak"),
    GAMES_WON("Games Won"),
    TOTAL_GAMES("Total Games")
}

/**
 * Player statistics
 */
@Serializable
data class PlayerStatistics(
    val playerId: String,
    val gamesPlayed: Int = 0,
    val totalScore: Long = 0,
    val averageScore: Double = 0.0,
    val bestScore: Long = 0,
    val wins: Int = 0,
    val winRate: Float = 0f,
    val lastPlayed: Long = 0,
    val joinDate: Long = System.currentTimeMillis(),
    val favoriteGame: String? = null,
    val achievements: List<String> = emptyList()
)

/**
 * Player rank information
 */
data class PlayerRank(
    val rank: Int,
    val playerId: String,
    val score: Long,
    val totalPlayers: Int
)

/**
 * Leaderboard events
 */
sealed class LeaderboardEvent {
    data class GameResultSubmitted(val result: GameResult) : LeaderboardEvent()
    data class LeaderboardUpdated(val gameId: String, val type: LeaderboardType) : LeaderboardEvent()
    data class PlayerRankChanged(val playerId: String, val newRank: Int) : LeaderboardEvent()
}

/**
 * Leaderboard statistics
 */
data class LeaderboardStats(
    val totalPlayers: Int,
    val totalGamesPlayed: Int,
    val averageScore: Double,
    val activeLeaderboards: Int
)