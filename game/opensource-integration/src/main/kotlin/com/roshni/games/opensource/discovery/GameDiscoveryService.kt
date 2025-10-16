package com.roshni.games.opensource.discovery

import kotlinx.coroutines.flow.StateFlow
import com.roshni.games.opensource.metadata.GameMetadata
import com.roshni.games.opensource.metadata.GameCategory

/**
 * Discovers and registers new open source games
 */
interface GameDiscoveryService {

    /**
     * Initialize discovery service
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Scan for new games
     */
    suspend fun scanForGames(categories: List<GameCategory>? = null): ScanResult

    /**
     * Register discovered game
     */
    suspend fun registerGame(metadata: GameMetadata): RegistrationResult

    /**
     * Update existing game registration
     */
    suspend fun updateGameRegistration(gameId: String, metadata: GameMetadata): UpdateResult

    /**
     * Unregister game
     */
    suspend fun unregisterGame(gameId: String): UnregistrationResult

    /**
     * Get discovery sources
     */
    fun getDiscoverySources(): List<DiscoverySource>

    /**
     * Add custom discovery source
     */
    suspend fun addDiscoverySource(source: DiscoverySource): Result<Unit>

    /**
     * Remove discovery source
     */
    suspend fun removeDiscoverySource(sourceId: String): Result<Unit>

    /**
     * Get discovery progress
     */
    fun getDiscoveryProgress(): StateFlow<DiscoveryProgress>

    /**
     * Get discovery statistics
     */
    fun getDiscoveryStats(): DiscoveryStatistics
}

/**
 * Discovery source configuration
 */
data class DiscoverySource(
    val id: String,
    val name: String,
    val type: DiscoverySourceType,
    val url: String,
    val enabled: Boolean = true,
    val lastScan: Long? = null,
    val scanInterval: Long = 24 * 60 * 60 * 1000, // 24 hours in milliseconds
    val priority: Int = 0,
    val credentials: Map<String, String> = emptyMap()
)

/**
 * Discovery source types
 */
enum class DiscoverySourceType {
    GITHUB_REPOSITORIES,
    GITLAB_REPOSITORIES,
    SOURCEFORGE_PROJECTS,
    OPEN_SOURCE_DIRECTORIES,
    GAME_DEVELOPMENT_FORUMS,
    CUSTOM_API,
    RSS_FEEDS,
    WEB_SCRAPING
}

/**
 * Scan result
 */
data class ScanResult(
    val totalScanned: Int,
    val newGamesFound: Int,
    val updatedGames: Int,
    val errors: Int,
    val scanDuration: Long,
    val discoveredGames: List<GameMetadata>,
    val errors: List<ScanError>
)

/**
 * Scan error
 */
data class ScanError(
    val source: String,
    val errorType: ScanErrorType,
    val description: String,
    val timestamp: Long
)

/**
 * Scan error types
 */
enum class ScanErrorType {
    NETWORK_ERROR,
    PARSING_ERROR,
    LICENSE_ERROR,
    METADATA_ERROR,
    DUPLICATE_GAME,
    INVALID_FORMAT
}

/**
 * Registration result
 */
data class RegistrationResult(
    val success: Boolean,
    val gameId: String?,
    val registrationId: String?,
    val errors: List<RegistrationError>,
    val warnings: List<String>
)

/**
 * Registration error
 */
data class RegistrationError(
    val field: String,
    val errorType: RegistrationErrorType,
    val description: String
)

/**
 * Registration error types
 */
enum class RegistrationErrorType {
    INVALID_METADATA,
    DUPLICATE_ID,
    LICENSE_ERROR,
    ASSET_ERROR,
    VALIDATION_ERROR
}

/**
 * Update result
 */
data class UpdateResult(
    val success: Boolean,
    val gameId: String,
    val changes: List<String>,
    val errors: List<String>,
    val requiresRestart: Boolean
)

/**
 * Unregistration result
 */
data class UnregistrationResult(
    val success: Boolean,
    val gameId: String,
    val removedAssets: Boolean,
    val errors: List<String>
)

/**
 * Discovery progress
 */
data class DiscoveryProgress(
    val isScanning: Boolean,
    val currentSource: String?,
    val sourcesProcessed: Int,
    val totalSources: Int,
    val gamesFound: Int,
    val currentProgress: Float,
    val estimatedTimeRemaining: Long
)

/**
 * Discovery statistics
 */
data class DiscoveryStatistics(
    val totalGamesRegistered: Int,
    val gamesByCategory: Map<GameCategory, Int>,
    val gamesByLicense: Map<String, Int>,
    val lastScanDate: Long?,
    val averageScanTime: Long,
    val successRate: Float,
    val sourcesConfigured: Int,
    val activeSources: Int
)

/**
 * Game registry for managing registered games
 */
interface GameRegistry {

    /**
     * Register a game
     */
    suspend fun registerGame(metadata: GameMetadata): Result<String>

    /**
     * Unregister a game
     */
    suspend fun unregisterGame(gameId: String): Result<Unit>

    /**
     * Update game registration
     */
    suspend fun updateGame(gameId: String, metadata: GameMetadata): Result<Unit>

    /**
     * Get all registered games
     */
    fun getAllGames(): StateFlow<List<GameMetadata>>

    /**
     * Get game by ID
     */
    suspend fun getGame(gameId: String): GameMetadata?

    /**
     * Search games
     */
    fun searchGames(query: String): StateFlow<List<GameMetadata>>

    /**
     * Get games by category
     */
    fun getGamesByCategory(category: GameCategory): StateFlow<List<GameMetadata>>

    /**
     * Check if game is registered
     */
    suspend fun isGameRegistered(gameId: String): Boolean

    /**
     * Get registry statistics
     */
    fun getRegistryStats(): RegistryStatistics
}

/**
 * Registry statistics
 */
data class RegistryStatistics(
    val totalGames: Int,
    val gamesByCategory: Map<GameCategory, Int>,
    val gamesByLicense: Map<String, Int>,
    val lastUpdate: Long,
    val registrySize: Long,
    val averageMetadataSize: Long
)