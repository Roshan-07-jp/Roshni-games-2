package com.roshni.games.feature.gamelibrary.data.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.gamelibrary.data.datasource.GameLibraryDataSource
import com.roshni.games.feature.gamelibrary.data.model.GameCategory
import com.roshni.games.feature.gamelibrary.data.model.GameFilter
import com.roshni.games.feature.gamelibrary.data.model.GameLibraryItem
import com.roshni.games.feature.gamelibrary.data.model.GameLibrarySearchResult
import com.roshni.games.feature.gamelibrary.data.model.GameSortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GameLibraryRepository {
    fun getGames(
        filter: GameFilter = GameFilter(),
        sortOption: GameSortOption = GameSortOption("name", "Name"),
        searchQuery: String = "",
        page: Int = 0,
        pageSize: Int = 20
    ): Flow<Result<List<GameLibraryItem>>>

    fun searchGames(query: String): Flow<Result<GameLibrarySearchResult>>
    suspend fun getCategories(): Result<List<GameCategory>>
    suspend fun getGameById(gameId: String): Result<GameLibraryItem?>
    suspend fun toggleFavorite(gameId: String): Result<Boolean>
    suspend fun rateGame(gameId: String, rating: Float): Result<Boolean>
    suspend fun refreshGameLibrary(): Result<Boolean>
}

class GameLibraryRepositoryImpl @Inject constructor(
    private val gameLibraryDataSource: GameLibraryDataSource
) : GameLibraryRepository {

    override fun getGames(
        filter: GameFilter,
        sortOption: GameSortOption,
        searchQuery: String,
        page: Int,
        pageSize: Int
    ): Flow<Result<List<GameLibraryItem>>> {
        return gameLibraryDataSource.getGames(filter, sortOption, searchQuery, page, pageSize)
            .map { games -> Result.Success(games) }
            .catch { error -> Result.Error(error) }
    }

    override fun searchGames(query: String): Flow<Result<GameLibrarySearchResult>> {
        return gameLibraryDataSource.searchGames(query)
            .map { result -> Result.Success(result) }
            .catch { error -> Result.Error(error) }
    }

    override suspend fun getCategories(): Result<List<GameCategory>> {
        return try {
            val categories = gameLibraryDataSource.getCategories()
            Result.Success(categories)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGameById(gameId: String): Result<GameLibraryItem?> {
        return try {
            val game = gameLibraryDataSource.getGameById(gameId)
            Result.Success(game)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun toggleFavorite(gameId: String): Result<Boolean> {
        return try {
            val success = gameLibraryDataSource.toggleFavorite(gameId)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun rateGame(gameId: String, rating: Float): Result<Boolean> {
        return try {
            val success = gameLibraryDataSource.rateGame(gameId, rating)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshGameLibrary(): Result<Boolean> {
        return try {
            val success = gameLibraryDataSource.refreshGameLibrary()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}