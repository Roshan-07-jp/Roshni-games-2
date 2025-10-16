package com.roshni.games.gameengine.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Asset management system for loading and caching game assets
 */
class AssetManager(
    private val context: Context,
    private val gameId: String
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Asset caches
    private val bitmapCache: LruCache<String, Bitmap>
    private val textCache = ConcurrentHashMap<String, String>()
    private val binaryCache = ConcurrentHashMap<String, ByteArray>()

    // Asset loading states
    private val _assetLoadStates = MutableSharedFlow<AssetLoadState>(extraBufferCapacity = 100)
    val assetLoadStates: SharedFlow<AssetLoadState> = _assetLoadStates.asSharedFlow()

    // Asset bundles for grouping related assets
    private val assetBundles = mutableMapOf<String, AssetBundle>()

    init {
        // Initialize bitmap cache with 25% of available memory
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val cacheSize = (maxMemory / 4).toInt() // 25% of available memory

        bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024 // Size in KB
            }
        }

        Timber.d("Asset manager initialized with cache size: ${cacheSize}KB")
    }

    /**
     * Load bitmap from assets folder
     */
    suspend fun loadBitmap(assetPath: String): Result<Bitmap> {
        return try {
            // Check cache first
            val cached = bitmapCache.get(assetPath)
            if (cached != null) {
                Timber.d("Loaded bitmap from cache: $assetPath")
                return Result.success(cached)
            }

            // Load from assets
            val inputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                // Add to cache
                bitmapCache.put(assetPath, bitmap)

                _assetLoadStates.emit(AssetLoadState.Loaded(assetPath, AssetType.BITMAP))
                Timber.d("Loaded bitmap: $assetPath")
                Result.success(bitmap)
            } else {
                val error = IOException("Failed to decode bitmap: $assetPath")
                _assetLoadStates.emit(AssetLoadState.Failed(assetPath, error.message ?: "Unknown error"))
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap: $assetPath")
            _assetLoadStates.emit(AssetLoadState.Failed(assetPath, e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Load text file from assets folder
     */
    suspend fun loadText(assetPath: String): Result<String> {
        return try {
            // Check cache first
            val cached = textCache[assetPath]
            if (cached != null) {
                Timber.d("Loaded text from cache: $assetPath")
                return Result.success(cached)
            }

            // Load from assets
            val inputStream = context.assets.open(assetPath)
            val text = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            // Add to cache
            textCache[assetPath] = text

            _assetLoadStates.emit(AssetLoadState.Loaded(assetPath, AssetType.TEXT))
            Timber.d("Loaded text: $assetPath")
            Result.success(text)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load text: $assetPath")
            _assetLoadStates.emit(AssetLoadState.Failed(assetPath, e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Load binary data from assets folder
     */
    suspend fun loadBinary(assetPath: String): Result<ByteArray> {
        return try {
            // Check cache first
            val cached = binaryCache[assetPath]
            if (cached != null) {
                Timber.d("Loaded binary from cache: $assetPath")
                return Result.success(cached)
            }

            // Load from assets
            val inputStream = context.assets.open(assetPath)
            val data = inputStream.readBytes()
            inputStream.close()

            // Add to cache
            binaryCache[assetPath] = data

            _assetLoadStates.emit(AssetLoadState.Loaded(assetPath, AssetType.BINARY))
            Timber.d("Loaded binary: $assetPath")
            Result.success(data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load binary: $assetPath")
            _assetLoadStates.emit(AssetLoadState.Failed(assetPath, e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Load bitmap from internal storage
     */
    suspend fun loadBitmapFromStorage(fileName: String): Result<Bitmap> {
        return try {
            val file = File(context.filesDir, "games/$gameId/$fileName")
            if (!file.exists()) {
                return Result.failure(IOException("File not found: $fileName"))
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                _assetLoadStates.emit(AssetLoadState.Loaded(fileName, AssetType.BITMAP))
                Result.success(bitmap)
            } else {
                val error = IOException("Failed to decode bitmap: $fileName")
                _assetLoadStates.emit(AssetLoadState.Failed(fileName, error.message ?: "Unknown error"))
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from storage: $fileName")
            _assetLoadStates.emit(AssetLoadState.Failed(fileName, e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Save bitmap to internal storage
     */
    suspend fun saveBitmapToStorage(fileName: String, bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): Result<Unit> {
        return try {
            val gameDir = File(context.filesDir, "games/$gameId")
            gameDir.mkdirs()

            val file = File(gameDir, fileName)
            val outputStream = file.outputStream()

            bitmap.compress(format, 100, outputStream)
            outputStream.close()

            _assetLoadStates.emit(AssetLoadState.Saved(fileName, AssetType.BITMAP))
            Timber.d("Saved bitmap to storage: $fileName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save bitmap to storage: $fileName")
            _assetLoadStates.emit(AssetLoadState.Failed(fileName, e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Create asset bundle for loading multiple assets together
     */
    fun createAssetBundle(bundleId: String, assetPaths: List<String>): AssetBundle {
        val bundle = AssetBundle(bundleId, assetPaths)
        assetBundles[bundleId] = bundle
        return bundle
    }

    /**
     * Load asset bundle
     */
    suspend fun loadAssetBundle(bundleId: String): Result<Unit> {
        return try {
            val bundle = assetBundles[bundleId] ?: return Result.failure(IllegalArgumentException("Bundle not found: $bundleId"))

            bundle.status = AssetBundle.Status.LOADING

            // Load all assets in bundle
            bundle.assetPaths.forEach { assetPath ->
                when {
                    assetPath.endsWith(".png") || assetPath.endsWith(".jpg") || assetPath.endsWith(".jpeg") -> {
                        loadBitmap(assetPath)
                    }
                    assetPath.endsWith(".txt") || assetPath.endsWith(".json") || assetPath.endsWith(".xml") -> {
                        loadText(assetPath)
                    }
                    else -> {
                        loadBinary(assetPath)
                    }
                }
            }

            bundle.status = AssetBundle.Status.LOADED
            _assetLoadStates.emit(AssetLoadState.BundleLoaded(bundleId))
            Timber.d("Loaded asset bundle: $bundleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load asset bundle: $bundleId")
            assetBundles[bundleId]?.status = AssetBundle.Status.FAILED
            Result.failure(e)
        }
    }

    /**
     * Preload critical assets
     */
    suspend fun preloadAssets(assetPaths: List<String>): Result<Int> {
        var loadedCount = 0

        assetPaths.forEach { assetPath ->
            try {
                when {
                    assetPath.endsWith(".png") || assetPath.endsWith(".jpg") || assetPath.endsWith(".jpeg") -> {
                        loadBitmap(assetPath)
                        loadedCount++
                    }
                    assetPath.endsWith(".txt") || assetPath.endsWith(".json") || assetPath.endsWith(".xml") -> {
                        loadText(assetPath)
                        loadedCount++
                    }
                    else -> {
                        loadBinary(assetPath)
                        loadedCount++
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to preload asset: $assetPath")
            }
        }

        Timber.d("Preloaded $loadedCount/${assetPaths.size} assets")
        return Result.success(loadedCount)
    }

    /**
     * Get cached bitmap
     */
    fun getCachedBitmap(assetPath: String): Bitmap? = bitmapCache.get(assetPath)

    /**
     * Get cached text
     */
    fun getCachedText(assetPath: String): String? = textCache[assetPath]

    /**
     * Get cached binary data
     */
    fun getCachedBinary(assetPath: String): ByteArray? = binaryCache[assetPath]

    /**
     * Check if asset is cached
     */
    fun isAssetCached(assetPath: String): Boolean {
        return when {
            assetPath.endsWith(".png") || assetPath.endsWith(".jpg") || assetPath.endsWith(".jpeg") -> {
                bitmapCache.get(assetPath) != null
            }
            assetPath.endsWith(".txt") || assetPath.endsWith(".json") || assetPath.endsWith(".xml") -> {
                textCache.containsKey(assetPath)
            }
            else -> {
                binaryCache.containsKey(assetPath)
            }
        }
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        bitmapCache.evictAll()
        textCache.clear()
        binaryCache.clear()
        Timber.d("Cleared all asset caches")
    }

    /**
     * Clear specific asset from cache
     */
    fun clearAssetFromCache(assetPath: String) {
        bitmapCache.remove(assetPath)
        textCache.remove(assetPath)
        binaryCache.remove(assetPath)
        Timber.d("Cleared asset from cache: $assetPath")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            bitmapCacheCount = bitmapCache.size(),
            bitmapCacheSize = bitmapCache.size(),
            textCacheCount = textCache.size,
            binaryCacheCount = binaryCache.size,
            maxBitmapCacheSize = bitmapCache.maxSize()
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        clearCache()
        assetBundles.clear()
        scope.cancel()
        Timber.d("Asset manager cleaned up")
    }
}

/**
 * Asset types
 */
enum class AssetType {
    BITMAP, TEXT, BINARY, AUDIO, VIDEO
}

/**
 * Asset load state
 */
sealed class AssetLoadState {
    data class Loaded(val assetPath: String, val type: AssetType) : AssetLoadState()
    data class Failed(val assetPath: String, val error: String) : AssetLoadState()
    data class Saved(val assetPath: String, val type: AssetType) : AssetLoadState()
    data class BundleLoaded(val bundleId: String) : AssetLoadState()
}

/**
 * Asset bundle for grouping related assets
 */
class AssetBundle(
    val id: String,
    val assetPaths: List<String>
) {
    var status: Status = Status.IDLE

    enum class Status {
        IDLE, LOADING, LOADED, FAILED
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val bitmapCacheCount: Int,
    val bitmapCacheSize: Int,
    val textCacheCount: Int,
    val binaryCacheCount: Int,
    val maxBitmapCacheSize: Int
)