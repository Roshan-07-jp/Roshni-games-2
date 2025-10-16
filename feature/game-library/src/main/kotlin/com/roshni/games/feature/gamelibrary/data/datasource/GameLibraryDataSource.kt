package com.roshni.games.feature.gamelibrary.data.datasource

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.model.GameEntity
import com.roshni.games.feature.gamelibrary.data.model.GameCategory
import com.roshni.games.feature.gamelibrary.data.model.GameFilter
import com.roshni.games.feature.gamelibrary.data.model.GameLibraryItem
import com.roshni.games.feature.gamelibrary.data.model.GameLibrarySearchResult
import com.roshni.games.feature.gamelibrary.data.model.GameSortOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.random.Random

interface GameLibraryDataSource {
    fun getGames(
        filter: GameFilter = GameFilter(),
        sortOption: GameSortOption = GameSortOption("name", "Name"),
        searchQuery: String = "",
        page: Int = 0,
        pageSize: Int = 20
    ): Flow<List<GameLibraryItem>>

    fun searchGames(query: String): Flow<GameLibrarySearchResult>
    suspend fun getCategories(): List<GameCategory>
    suspend fun getGameById(gameId: String): GameLibraryItem?
    suspend fun toggleFavorite(gameId: String): Boolean
    suspend fun rateGame(gameId: String, rating: Float): Boolean
    suspend fun refreshGameLibrary(): Boolean
}

class GameLibraryDataSourceImpl @Inject constructor(
    private val gameDao: GameDao
) : GameLibraryDataSource {

    override fun getGames(
        filter: GameFilter,
        sortOption: GameSortOption,
        searchQuery: String,
        page: Int,
        pageSize: Int
    ): Flow<List<GameLibraryItem>> = flow {
        delay(500) // Simulate network delay

        try {
            // Get all games from database
            val allGames = gameDao.getActiveGames()

            // Apply search filter
            val filteredGames = if (searchQuery.isNotEmpty()) {
                allGames.filter { game ->
                    game.name.contains(searchQuery, ignoreCase = true) ||
                    game.description.contains(searchQuery, ignoreCase = true) ||
                    game.category.contains(searchQuery, ignoreCase = true) ||
                    game.tags.any { it.contains(searchQuery, ignoreCase = true) }
                }
            } else {
                allGames
            }

            // Apply category filter
            val categoryFilteredGames = if (filter.categories.isNotEmpty()) {
                filteredGames.filter { game ->
                    filter.categories.contains(game.category)
                }
            } else {
                filteredGames
            }

            // Apply difficulty filter
            val difficultyFilteredGames = if (filter.difficulties.isNotEmpty()) {
                categoryFilteredGames.filter { game ->
                    filter.difficulties.contains(game.difficulty.name)
                }
            } else {
                categoryFilteredGames
            }

            // Apply multiplayer filter
            val multiplayerFilteredGames = if (filter.multiplayerOnly) {
                difficultyFilteredGames.filter { it.isMultiplayer }
            } else {
                difficultyFilteredGames
            }

            // Apply installed filter
            val installedFilteredGames = if (filter.installedOnly) {
                multiplayerFilteredGames.filter { Random.nextBoolean() } // Simulate installed status
            } else {
                multiplayerFilteredGames
            }

            // Apply sorting
            val sortedGames = when (sortOption.id) {
                "name" -> installedFilteredGames.sortedBy { it.name }
                "popularity" -> installedFilteredGames.sortedByDescending { Random.nextInt(1000) }
                "rating" -> installedFilteredGames.sortedByDescending { Random.nextFloat() * 5 }
                "recently_added" -> installedFilteredGames.sortedByDescending { it.createdAt }
                "last_played" -> installedFilteredGames.sortedByDescending { Random.nextLong() }
                else -> installedFilteredGames
            }

            // Apply pagination
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, sortedGames.size)
            val pagedGames = if (startIndex < sortedGames.size) {
                sortedGames.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            // Convert to GameLibraryItem
            val gameLibraryItems = pagedGames.map { game ->
                GameLibraryItem(
                    game = game,
                    isInstalled = !filter.installedOnly || Random.nextBoolean(),
                    isFavorite = Random.nextBoolean(),
                    userRating = if (Random.nextBoolean()) Random.nextFloat() * 5 else null,
                    playCount = Random.nextInt(50),
                    lastPlayed = if (Random.nextBoolean()) {
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    } else null,
                    totalPlayTime = Random.nextLong(300),
                    highScore = if (Random.nextBoolean()) Random.nextInt(10000) else null,
                    achievementsUnlocked = Random.nextInt(10),
                    totalAchievements = Random.nextInt(15)
                )
            }

            emit(gameLibraryItems)

        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun searchGames(query: String): Flow<GameLibrarySearchResult> = flow {
        delay(300) // Simulate search delay

        val startTime = System.currentTimeMillis()

        try {
            val allGames = gameDao.getActiveGames()
            val searchResults = allGames.filter { game ->
                game.name.contains(query, ignoreCase = true) ||
                game.description.contains(query, ignoreCase = true) ||
                game.category.contains(query, ignoreCase = true)
            }

            val gameLibraryItems = searchResults.take(50).map { game ->
                GameLibraryItem(
                    game = game,
                    isFavorite = Random.nextBoolean(),
                    playCount = Random.nextInt(30)
                )
            }

            val searchTime = System.currentTimeMillis() - startTime

            val result = GameLibrarySearchResult(
                games = gameLibraryItems,
                totalCount = searchResults.size,
                hasMoreResults = searchResults.size > 50,
                searchTime = searchTime
            )

            emit(result)

        } catch (e: Exception) {
            val result = GameLibrarySearchResult(
                games = emptyList(),
                totalCount = 0,
                hasMoreResults = false,
                searchTime = System.currentTimeMillis() - startTime
            )
            emit(result)
        }
    }

    override suspend fun getCategories(): List<GameCategory> {
        delay(200)

        return listOf(
            GameCategory(
                id = "puzzle",
                name = "Puzzle",
                description = "Brain teasers and logic games",
                iconUrl = null,
                gameCount = 25,
                isPopular = true,
                color = "#FF9800"
            ),
            GameCategory(
                id = "action",
                name = "Action",
                description = "Fast-paced adrenaline games",
                iconUrl = null,
                gameCount = 18,
                isPopular = true,
                color = "#F44336"
            ),
            GameCategory(
                id = "strategy",
                name = "Strategy",
                description = "Think ahead and plan your moves",
                iconUrl = null,
                gameCount = 12,
                isPopular = false,
                color = "#2196F3"
            ),
            GameCategory(
                id = "casual",
                name = "Casual",
                description = "Easy to pick up and play",
                iconUrl = null,
                gameCount = 30,
                isPopular = true,
                color = "#4CAF50"
            ),
            GameCategory(
                id = "adventure",
                name = "Adventure",
                description = "Epic quests and stories",
                iconUrl = null,
                gameCount = 15,
                isPopular = false,
                color = "#9C27B0"
            ),
            GameCategory(
                id = "racing",
                name = "Racing",
                description = "Speed through tracks and circuits",
                iconUrl = null,
                gameCount = 8,
                isPopular = false,
                color = "#FFC107"
            )
        )
    }

    override suspend fun getGameById(gameId: String): GameLibraryItem? {
        delay(100)

        return try {
            val game = gameDao.getGameById(gameId) ?: return null

            GameLibraryItem(
                game = game,
                isFavorite = Random.nextBoolean(),
                playCount = Random.nextInt(100),
                totalPlayTime = Random.nextLong(1000),
                highScore = Random.nextInt(50000),
                achievementsUnlocked = Random.nextInt(20),
                totalAchievements = Random.nextInt(25)
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleFavorite(gameId: String): Boolean {
        delay(200)
        return try {
            // In real implementation, this would update the database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun rateGame(gameId: String, rating: Float): Boolean {
        delay(200)
        return try {
            // In real implementation, this would save the rating to database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refreshGameLibrary(): Boolean {
        delay(1000)
        return try {
            // In real implementation, this would sync with remote server
            true
        } catch (e: Exception) {
            false
        }
    }
}