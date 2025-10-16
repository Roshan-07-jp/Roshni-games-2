package com.roshni.games.gamecatalog.data.repository

import com.roshni.games.gamecatalog.data.GameCatalogData
import com.roshni.games.gamecatalog.data.model.GameCatalog
import com.roshni.games.gamecatalog.data.model.GameCategory
import com.roshni.games.gamecatalog.data.model.GameCategoryType
import com.roshni.games.gamecatalog.data.model.GameDefinition
import com.roshni.games.gamecatalog.domain.repository.GameCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Implementation of GameCatalogRepository using local data
 */
class GameCatalogRepositoryImpl : GameCatalogRepository {

    private var cachedCatalog: GameCatalog? = null
    private var lastUpdateTime: Long = 0
    private val cacheValidityDuration = 24 * 60 * 60 * 1000L // 24 hours

    override suspend fun getGameCatalog(): Result<GameCatalog> {
        return try {
            if (cachedCatalog == null || isCacheExpired()) {
                cachedCatalog = GameCatalogData.completeCatalog
                lastUpdateTime = System.currentTimeMillis()
                Timber.d("Loaded fresh game catalog with ${cachedCatalog?.totalGames} games")
            } else {
                Timber.d("Using cached game catalog")
            }

            Result.success(cachedCatalog!!)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load game catalog")
            Result.failure(e)
        }
    }

    override suspend fun getGameCategories(): Result<List<GameCategory>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            Result.success(catalog.categories)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get game categories")
            Result.failure(e)
        }
    }

    override suspend fun getGamesByCategory(category: GameCategoryType): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { it.category == category }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get games by category: $category")
            Result.failure(e)
        }
    }

    override suspend fun getFeaturedGames(): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val featuredGames = catalog.games.filter { game ->
                catalog.featuredGames.contains(game.id)
            }
            Result.success(featuredGames)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get featured games")
            Result.failure(e)
        }
    }

    override suspend fun getNewGames(): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val newGames = catalog.games.filter { game ->
                catalog.newGames.contains(game.id)
            }
            Result.success(newGames)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get new games")
            Result.failure(e)
        }
    }

    override suspend fun searchGames(query: String): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val searchQuery = query.lowercase().trim()
            if (searchQuery.isEmpty()) {
                return Result.success(emptyList())
            }

            val results = catalog.games.filter { game ->
                game.name.contains(searchQuery, ignoreCase = true) ||
                game.displayName.contains(searchQuery, ignoreCase = true) ||
                game.description.contains(searchQuery, ignoreCase = true) ||
                game.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                game.subcategory.contains(searchQuery, ignoreCase = true) ||
                game.developer.contains(searchQuery, ignoreCase = true)
            }

            // Sort by relevance (featured first, then by rating)
            val sortedResults = results.sortedWith(
                compareByDescending<GameDefinition> { catalog.featuredGames.contains(it.id) }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.downloadCount }
            )

            Result.success(sortedResults)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search games with query: $query")
            Result.failure(e)
        }
    }

    override suspend fun getGameById(gameId: String): Result<GameDefinition?> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val game = catalog.games.find { it.id == gameId }
            Result.success(game)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get game by ID: $gameId")
            Result.failure(e)
        }
    }

    override suspend fun getGamesByDifficulty(difficulty: com.roshni.games.gamecatalog.data.model.GameDifficulty): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { it.difficulty == difficulty }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get games by difficulty: $difficulty")
            Result.failure(e)
        }
    }

    override suspend fun getGamesByPlayerCount(minPlayers: Int, maxPlayers: Int): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { game ->
                game.minPlayers <= maxPlayers && game.maxPlayers >= minPlayers
            }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get games by player count: $minPlayers-$maxPlayers")
            Result.failure(e)
        }
    }

    override suspend fun getGamesByDuration(maxDuration: Int): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { it.estimatedDuration <= maxDuration }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get games by duration: $maxDuration")
            Result.failure(e)
        }
    }

    override suspend fun getOnlineGames(): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { it.isOnline }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get online games")
            Result.failure(e)
        }
    }

    override suspend fun getOfflineGames(): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))
            val games = catalog.games.filter { it.isOffline }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get offline games")
            Result.failure(e)
        }
    }

    override suspend fun getGamesByTags(tags: List<String>): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val games = catalog.games.filter { game ->
                tags.any { tag -> game.tags.contains(tag) }
            }
            Result.success(games)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get games by tags: $tags")
            Result.failure(e)
        }
    }

    override suspend fun getRandomGames(count: Int): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val shuffled = catalog.games.shuffled()
            val randomGames = shuffled.take(count)
            Result.success(randomGames)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get random games: $count")
            Result.failure(e)
        }
    }

    override suspend fun getTopRatedGames(limit: Int): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val topRated = catalog.games
                .filter { it.rating > 0 }
                .sortedByDescending { it.rating }
                .take(limit)

            Result.success(topRated)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get top rated games: $limit")
            Result.failure(e)
        }
    }

    override suspend fun getMostDownloadedGames(limit: Int): Result<List<GameDefinition>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val mostDownloaded = catalog.games
                .sortedByDescending { it.downloadCount }
                .take(limit)

            Result.success(mostDownloaded)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get most downloaded games: $limit")
            Result.failure(e)
        }
    }

    override suspend fun refreshCatalog(): Result<Unit> {
        return try {
            cachedCatalog = null
            lastUpdateTime = 0
            getGameCatalog() // This will reload the catalog
            Timber.d("Catalog refreshed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh catalog")
            Result.failure(e)
        }
    }

    override suspend fun isUpdateAvailable(): Result<Boolean> {
        return try {
            // In a real implementation, this would check against a remote server
            // For now, we'll simulate that updates are available weekly
            val weekInMillis = 7 * 24 * 60 * 60 * 1000L
            val isExpired = System.currentTimeMillis() - lastUpdateTime > weekInMillis
            Result.success(isExpired)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            Result.failure(e)
        }
    }

    override suspend fun getCatalogStatistics(): Result<Map<String, Any>> {
        return try {
            val catalog = getGameCatalog().getOrNull() ?: return Result.failure(IllegalStateException("Failed to load catalog"))

            val stats = mapOf(
                "totalGames" to catalog.totalGames,
                "totalCategories" to catalog.categories.size,
                "featuredGames" to catalog.featuredGames.size,
                "newGames" to catalog.newGames.size,
                "onlineGames" to catalog.games.count { it.isOnline },
                "offlineGames" to catalog.games.count { it.isOffline },
                "averageRating" to catalog.games.map { it.rating }.average(),
                "totalDownloads" to catalog.games.sumOf { it.downloadCount },
                "gamesByCategory" to catalog.categories.associate { it.id to it.gameCount },
                "gamesByDifficulty" to catalog.games.groupBy { it.difficulty }.mapValues { it.value.size },
                "gamesByType" to catalog.games.groupBy { it.gameType }.mapValues { it.value.size }
            )

            Result.success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get catalog statistics")
            Result.failure(e)
        }
    }

    private fun isCacheExpired(): Boolean {
        return System.currentTimeMillis() - lastUpdateTime > cacheValidityDuration
    }
}