package com.roshni.games.feature.gamelibrary.domain.repository

import com.roshni.games.feature.gamelibrary.domain.model.GameCategory
import com.roshni.games.feature.gamelibrary.domain.model.GameFilter
import com.roshni.games.feature.gamelibrary.domain.model.GameItem
import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryState
import com.roshni.games.feature.gamelibrary.domain.model.SearchResults
import com.roshni.games.feature.gamelibrary.domain.model.SortOption
import com.roshni.games.feature.gamelibrary.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow

interface GameLibraryDomainRepository {
    fun getGameLibraryState(
        filter: GameFilter = GameFilter(),
        sortOption: SortOption = SortOption("name", "Name"),
        searchQuery: String = "",
        viewMode: ViewMode = ViewMode.GRID
    ): Flow<GameLibraryState>

    suspend fun searchGames(query: String): SearchResults?
    suspend fun getCategories(): List<GameCategory>
    suspend fun getGameById(gameId: String): GameItem?
    suspend fun toggleFavorite(gameId: String): Boolean
    suspend fun rateGame(gameId: String, rating: Float): Boolean
    suspend fun refreshLibrary(): Boolean
}