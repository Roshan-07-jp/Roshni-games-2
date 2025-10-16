package com.roshni.games.opensource.assets

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Manages downloading, caching, and serving of open source game assets
 */
interface AssetManager {

    /**
     * Initialize asset manager
     */
    suspend fun initialize(context: Context): Result<Unit>

    /**
     * Download assets for a specific game
     */
    suspend fun downloadGameAssets(gameId: String, metadata: com.roshni.games.opensource.metadata.GameMetadata): Result<Unit>

    /**
     * Check if game assets are cached locally
     */
    suspend fun areAssetsCached(gameId: String): Boolean

    /**
     * Get local path for game asset
     */
    suspend fun getAssetPath(gameId: String, assetName: String): File?

    /**
     * Update assets for a game
     */
    suspend fun updateGameAssets(gameId: String): Result<Unit>

    /**
     * Clean up unused assets
     */
    suspend fun cleanupUnusedAssets(): Result<Unit>

    /**
     * Get asset download progress
     */
    fun getDownloadProgress(gameId: String): StateFlow<DownloadProgress>

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats

    /**
     * Preload assets for frequently used games
     */
    suspend fun preloadAssets(gameIds: List<String>): Result<Unit>
}

/**
 * Download progress information
 */
data class DownloadProgress(
    val gameId: String,
    val totalFiles: Int,
    val downloadedFiles: Int,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val currentFile: String? = null,
    val isComplete: Boolean = false,
    val error: String? = null
)

/**
 * Cache statistics
 */
data class CacheStats(
    val totalSize: Long,
    val usedSize: Long,
    val freeSize: Long,
    val gameCount: Int,
    val assetCount: Int,
    val lastCleanup: Long
)

/**
 * Asset types
 */
enum class AssetType(val extension: String) {
    IMAGE_PNG(".png"),
    IMAGE_JPG(".jpg"),
    IMAGE_JPEG(".jpeg"),
    IMAGE_WEBP(".webp"),
    IMAGE_SVG(".svg"),
    AUDIO_MP3(".mp3"),
    AUDIO_WAV(".wav"),
    AUDIO_OGG(".ogg"),
    FONT_TTF(".ttf"),
    FONT_OTF(".otf"),
    DATA_JSON(".json"),
    DATA_XML(".xml"),
    DATA_BIN(".bin"),
    ARCHIVE_ZIP(".zip"),
    ARCHIVE_TAR(".tar.gz")
}

/**
 * Asset validation result
 */
data class AssetValidationResult(
    val isValid: Boolean,
    val checksum: String?,
    val size: Long,
    val lastModified: Long,
    val error: String? = null
)