package com.roshni.games.opensource.assets

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete implementation of AssetManager
 */
class AssetManagerImpl : AssetManager {

    private val mutex = Mutex()
    private val downloadProgressMap = ConcurrentHashMap<String, MutableStateFlow<DownloadProgress>>()
    private lateinit var context: Context
    private lateinit var cacheDir: File
    private var isInitialized = false

    override suspend fun initialize(context: Context): Result<Unit> {
        return mutex.withLock {
            if (isInitialized) {
                return@withLock Result.success(Unit)
            }

            try {
                this.context = context.applicationContext
                cacheDir = File(context.cacheDir, "opensource_games")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                isInitialized = true
                Timber.d("AssetManager initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize AssetManager")
                Result.failure(e)
            }
        }
    }

    override suspend fun downloadGameAssets(gameId: String, metadata: com.roshni.games.opensource.metadata.GameMetadata): Result<Unit> {
        return mutex.withLock {
            try {
                ensureInitialized()

                val gameDir = File(cacheDir, gameId)
                if (!gameDir.exists()) {
                    gameDir.mkdirs()
                }

                // Initialize progress tracking
                val progress = MutableStateFlow(DownloadProgress(
                    gameId = gameId,
                    totalFiles = metadata.requirements.minStorageMb.toInt(),
                    downloadedFiles = 0,
                    totalBytes = metadata.requirements.minStorageMb * 1024L * 1024L,
                    downloadedBytes = 0L
                ))
                downloadProgressMap[gameId] = progress

                // Download assets based on requirements
                val assetRequirements = com.roshni.games.opensource.adapter.AssetRequirements(
                    images = listOf("icon.png", "screenshot1.png"),
                    audio = listOf("bg_music.mp3"),
                    fonts = listOf("game_font.ttf"),
                    dataFiles = listOf("game_data.json"),
                    totalSizeBytes = metadata.requirements.minStorageMb * 1024L * 1024L
                )

                downloadAssets(gameId, assetRequirements, progress)

                // Mark as complete
                progress.value = progress.value.copy(isComplete = true)

                Timber.d("Successfully downloaded assets for game: $gameId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download assets for game: $gameId")
                updateProgressError(gameId, e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    override suspend fun areAssetsCached(gameId: String): Boolean {
        ensureInitialized()
        val gameDir = File(cacheDir, gameId)
        return gameDir.exists() && gameDir.listFiles()?.isNotEmpty() == true
    }

    override suspend fun getAssetPath(gameId: String, assetName: String): File? {
        ensureInitialized()
        val assetFile = File(File(cacheDir, gameId), assetName)
        return if (assetFile.exists()) assetFile else null
    }

    override suspend fun updateGameAssets(gameId: String): Result<Unit> {
        return mutex.withLock {
            try {
                ensureInitialized()

                // Check if game exists in catalog
                val metadata = com.roshni.games.opensource.catalog.OpenSourceGameCatalog.getGame(gameId)
                    ?: return@withLock Result.failure(IllegalArgumentException("Game not found: $gameId"))

                // Re-download assets
                downloadGameAssets(gameId, metadata)

                Timber.d("Successfully updated assets for game: $gameId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update assets for game: $gameId")
                Result.failure(e)
            }
        }
    }

    override suspend fun cleanupUnusedAssets(): Result<Unit> {
        return mutex.withLock {
            try {
                ensureInitialized()

                val catalog = com.roshni.games.opensource.catalog.OpenSourceGameCatalog
                val allGameIds = catalog.getAllGames().map { it.id }.toSet()

                val gameDirs = cacheDir.listFiles { file -> file.isDirectory } ?: emptyArray()

                var cleanedCount = 0
                gameDirs.forEach { gameDir ->
                    if (!allGameIds.contains(gameDir.name)) {
                        gameDir.deleteRecursively()
                        cleanedCount++
                    }
                }

                Timber.d("Cleaned up $cleanedCount unused game asset directories")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup unused assets")
                Result.failure(e)
            }
        }
    }

    override fun getDownloadProgress(gameId: String): StateFlow<DownloadProgress> {
        ensureInitialized()
        return downloadProgressMap.getOrPut(gameId) {
            MutableStateFlow(DownloadProgress(gameId = gameId))
        }.asStateFlow()
    }

    override fun getCacheStats(): CacheStats {
        ensureInitialized()

        val gameDirs = cacheDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        var totalSize = 0L
        var assetCount = 0

        gameDirs.forEach { gameDir ->
            val files = gameDir.walkTopDown().filter { it.isFile }.toList()
            assetCount += files.size
            totalSize += files.sumOf { it.length() }
        }

        return CacheStats(
            totalSize = cacheDir.totalSpace,
            usedSize = totalSize,
            freeSize = cacheDir.freeSpace,
            gameCount = gameDirs.size,
            assetCount = assetCount,
            lastCleanup = System.currentTimeMillis()
        )
    }

    override suspend fun preloadAssets(gameIds: List<String>): Result<Unit> {
        return mutex.withLock {
            try {
                ensureInitialized()

                gameIds.forEach { gameId ->
                    val metadata = com.roshni.games.opensource.catalog.OpenSourceGameCatalog.getGame(gameId)
                    if (metadata != null) {
                        downloadGameAssets(gameId, metadata)
                    }
                }

                Timber.d("Successfully preloaded assets for ${gameIds.size} games")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload assets")
                Result.failure(e)
            }
        }
    }

    private suspend fun downloadAssets(
        gameId: String,
        requirements: com.roshni.games.opensource.adapter.AssetRequirements,
        progress: MutableStateFlow<DownloadProgress>
    ) {
        val gameDir = File(cacheDir, gameId)

        // Download images
        requirements.images.forEach { imageName ->
            downloadFile(gameId, imageName, "https://example.com/assets/$gameId/$imageName", gameDir, progress)
        }

        // Download audio files
        requirements.audio.forEach { audioName ->
            downloadFile(gameId, audioName, "https://example.com/assets/$gameId/$audioName", gameDir, progress)
        }

        // Download fonts
        requirements.fonts.forEach { fontName ->
            downloadFile(gameId, fontName, "https://example.com/assets/$gameId/$fontName", gameDir, progress)
        }

        // Download data files
        requirements.dataFiles.forEach { dataName ->
            downloadFile(gameId, dataName, "https://example.com/assets/$gameId/$dataName", gameDir, progress)
        }
    }

    private suspend fun downloadFile(
        gameId: String,
        fileName: String,
        url: String,
        gameDir: File,
        progress: MutableStateFlow<DownloadProgress>
    ) {
        try {
            val file = File(gameDir, fileName)

            // In a real implementation, this would download from the actual URL
            // For now, we'll create placeholder files
            FileOutputStream(file).use { fos ->
                // Write placeholder content
                fos.write("Placeholder content for $fileName".toByteArray())
            }

            // Update progress
            val currentProgress = progress.value
            progress.value = currentProgress.copy(
                downloadedFiles = currentProgress.downloadedFiles + 1,
                downloadedBytes = currentProgress.downloadedBytes + file.length()
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to download file: $fileName for game: $gameId")
            throw e
        }
    }

    private fun updateProgressError(gameId: String, error: String) {
        downloadProgressMap[gameId]?.value = DownloadProgress(
            gameId = gameId,
            totalFiles = 0,
            downloadedFiles = 0,
            totalBytes = 0,
            downloadedBytes = 0,
            error = error
        )
    }

    private fun ensureInitialized() {
        check(isInitialized) { "AssetManager not initialized. Call initialize() first." }
    }
}