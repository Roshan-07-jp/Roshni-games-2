package com.roshni.games.gamecatalog.domain.repository

import com.roshni.games.gamecatalog.data.model.GameCatalog
import com.roshni.games.gamecatalog.data.model.GameCategory
import com.roshni.games.gamecatalog.data.model.GameCategoryType
import com.roshni.games.gamecatalog.data.model.GameDefinition
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for game catalog operations
 */
interface GameCatalogRepository {

    /**
     * Get the complete game catalog
     */
    suspend fun getGameCatalog(): Result<GameCatalog>

    /**
     * Get all game categories
     */
    suspend fun getGameCategories(): Result<List<GameCategory>>

    /**
     * Get games by category
     */
    suspend fun getGamesByCategory(category: GameCategoryType): Result<List<GameDefinition>>

    /**
     * Get featured games
     */
    suspend fun getFeaturedGames(): Result<List<GameDefinition>>

    /**
     * Get new games
     */
    suspend fun getNewGames(): Result<List<GameDefinition>>

    /**
     * Search games by query
     */
    suspend fun searchGames(query: String): Result<List<GameDefinition>>

    /**
     * Get game by ID
     */
    suspend fun getGameById(gameId: String): Result<GameDefinition?>

    /**
     * Get games by difficulty
     */
    suspend fun getGamesByDifficulty(difficulty: com.roshni.games.gamecatalog.data.model.GameDifficulty): Result<List<GameDefinition>>

    /**
     * Get games by player count
     */
    suspend fun getGamesByPlayerCount(minPlayers: Int, maxPlayers: Int): Result<List<GameDefinition>>

    /**
     * Get games by estimated duration
     */
    suspend fun getGamesByDuration(maxDuration: Int): Result<List<GameDefinition>>

    /**
     * Get online multiplayer games
     */
    suspend fun getOnlineGames(): Result<List<GameDefinition>>

    /**
     * Get offline games
     */
    suspend fun getOfflineGames(): Result<List<GameDefinition>>

    /**
     * Get games by tags
     */
    suspend fun getGamesByTags(tags: List<String>): Result<List<GameDefinition>>

    /**
     * Get random games for discovery
     */
    suspend fun getRandomGames(count: Int): Result<List<GameDefinition>>

    /**
     * Get games sorted by rating
     */
    suspend fun getTopRatedGames(limit: Int): Result<List<GameDefinition>>

    /**
     * Get most downloaded games
     */
    suspend fun getMostDownloadedGames(limit: Int): Result<List<GameDefinition>>

    /**
     * Refresh catalog data (for updates)
     */
    suspend fun refreshCatalog(): Result<Unit>

    /**
     * Check if catalog needs update
     */
    suspend fun isUpdateAvailable(): Result<Boolean>

    /**
     * Get catalog statistics
     */
    suspend fun getCatalogStatistics(): Result<Map<String, Any>>
}