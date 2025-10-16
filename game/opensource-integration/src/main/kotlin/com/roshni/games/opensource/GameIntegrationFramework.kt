package com.roshni.games.opensource

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the Open Source Game Integration Framework
 * Provides unified access to all open source games and their management
 */
interface GameIntegrationFramework {

    /**
     * Initialize the framework with application context
     */
    suspend fun initialize(context: Context): Result<Unit>

    /**
     * Get all available games
     */
    fun getAllGames(): StateFlow<List<OpenSourceGame>>

    /**
     * Get games by category
     */
    fun getGamesByCategory(category: GameCategory): StateFlow<List<OpenSourceGame>>

    /**
     * Search games by name or description
     */
    fun searchGames(query: String): StateFlow<List<OpenSourceGame>>

    /**
     * Get game by ID
     */
    suspend fun getGameById(gameId: String): OpenSourceGame?

    /**
     * Launch a game
     */
    suspend fun launchGame(gameId: String, context: Context): Result<GameSession>

    /**
     * Get game metadata
     */
    suspend fun getGameMetadata(gameId: String): GameMetadata?

    /**
     * Update game assets/cache
     */
    suspend fun updateGameAssets(gameId: String): Result<Unit>

    /**
     * Get framework statistics
     */
    fun getFrameworkStats(): FrameworkStats
}

/**
 * Represents an open source game in the framework
 */
data class OpenSourceGame(
    val id: String,
    val name: String,
    val description: String,
    val category: GameCategory,
    val metadata: GameMetadata,
    val adapter: GameAdapter,
    val isAvailable: Boolean,
    val installationStatus: InstallationStatus
)

/**
 * Game categories for organization
 */
enum class GameCategory {
    PUZZLE, CARD, ARCADE, STRATEGY, TRIVIA,
    ACTION, BOARD, CASUAL, WORD, MATH,
    MEMORY, LOGIC, ADVENTURE, SIMULATION
}

/**
 * Installation status of a game
 */
enum class InstallationStatus {
    NOT_INSTALLED, INSTALLING, INSTALLED, UPDATE_AVAILABLE, ERROR
}

/**
 * Framework statistics
 */
data class FrameworkStats(
    val totalGames: Int,
    val installedGames: Int,
    val totalDownloads: Long,
    val cacheSize: Long,
    val lastUpdate: Long
)