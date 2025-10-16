package com.roshni.games.feature.gamelibrary.domain.repository

import com.roshni.games.core.database.model.GameDifficulty
import com.roshni.games.core.utils.Result
import com.roshni.games.feature.gamelibrary.data.model.GameCategory
import com.roshni.games.feature.gamelibrary.data.model.GameFilter
import com.roshni.games.feature.gamelibrary.data.model.GameLibraryItem
import com.roshni.games.feature.gamelibrary.data.model.GameLibrarySearchResult
import com.roshni.games.feature.gamelibrary.data.model.GameSortOption
import com.roshni.games.feature.gamelibrary.data.repository.GameLibraryRepository
import com.roshni.games.feature.gamelibrary.domain.model.GameCategory as DomainGameCategory
import com.roshni.games.feature.gamelibrary.domain.model.GameFilter as DomainGameFilter
import com.roshni.games.feature.gamelibrary.domain.model.GameItem
import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryState
import com.roshni.games.feature.gamelibrary.domain.model.SearchResults
import com.roshni.games.feature.gamelibrary.domain.model.SortOption
import com.roshni.games.feature.gamelibrary.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GameLibraryDomainRepositoryImpl @Inject constructor(
    private val gameLibraryRepository: GameLibraryRepository
) : GameLibraryDomainRepository {

    override fun getGameLibraryState(
        filter: DomainGameFilter,
        sortOption: SortOption,
        searchQuery: String,
        viewMode: ViewMode
    ): Flow<GameLibraryState> {
        return gameLibraryRepository.getGames(
            filter = filter.toDataFilter(),
            sortOption = sortOption.toDataSortOption(),
            searchQuery = searchQuery,
            page = 0,
            pageSize = 50
        ).map { result ->
            when (result) {
                is Result.Success -> {
                    val games = result.data
                    GameLibraryState(
                        games = games.map { it.toGameItem() },
                        categories = emptyList(), // Will be loaded separately
                        filter = filter,
                        sortOptions = defaultSortOptions().map { option ->
                            option.copy(isSelected = option.id == sortOption.id)
                        },
                        selectedSortOption = sortOption,
                        viewMode = viewMode,
                        searchQuery = searchQuery,
                        isLoading = false,
                        error = null,
                        hasMorePages = games.size >= 50,
                        currentPage = 0,
                        searchResults = null
                    )
                }
                is Result.Error -> {
                    GameLibraryState(
                        games = emptyList(),
                        categories = emptyList(),
                        filter = filter,
                        sortOptions = defaultSortOptions().map { option ->
                            option.copy(isSelected = option.id == sortOption.id)
                        },
                        selectedSortOption = sortOption,
                        viewMode = viewMode,
                        searchQuery = searchQuery,
                        isLoading = false,
                        error = result.exception.message,
                        hasMorePages = false,
                        currentPage = 0,
                        searchResults = null
                    )
                }
            }
        }
    }

    override suspend fun searchGames(query: String): SearchResults? {
        return when (val result = gameLibraryRepository.searchGames(query)) {
            is Result.Success -> {
                SearchResults(
                    games = result.data.games.map { it.toGameItem() },
                    totalCount = result.data.totalCount,
                    hasMoreResults = result.data.hasMoreResults,
                    searchTime = result.data.searchTime
                )
            }
            is Result.Error -> null
        }
    }

    override suspend fun getCategories(): List<DomainGameCategory> {
        return when (val result = gameLibraryRepository.getCategories()) {
            is Result.Success -> {
                result.data.map { it.toDomainCategory() }
            }
            is Result.Error -> emptyList()
        }
    }

    override suspend fun getGameById(gameId: String): GameItem? {
        return when (val result = gameLibraryRepository.getGameById(gameId)) {
            is Result.Success -> result.data?.toGameItem()
            is Result.Error -> null
        }
    }

    override suspend fun toggleFavorite(gameId: String): Boolean {
        return when (val result = gameLibraryRepository.toggleFavorite(gameId)) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun rateGame(gameId: String, rating: Float): Boolean {
        return when (val result = gameLibraryRepository.rateGame(gameId, rating)) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun refreshLibrary(): Boolean {
        return when (val result = gameLibraryRepository.refreshGameLibrary()) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    private fun DomainGameFilter.toDataFilter(): GameFilter {
        return GameFilter(
            categories = categories,
            difficulties = difficulties,
            minRating = minRating,
            maxRating = maxRating,
            installedOnly = installedOnly,
            favoritesOnly = favoritesOnly,
            multiplayerOnly = multiplayerOnly
        )
    }

    private fun SortOption.toDataSortOption(): GameSortOption {
        return GameSortOption(id = id, name = name, isSelected = isSelected)
    }

    private fun GameLibraryItem.toGameItem(): GameItem {
        return GameItem(
            id = game.id,
            name = game.name,
            description = game.description,
            category = game.category,
            difficulty = game.difficulty.name,
            iconUrl = game.iconUrl,
            thumbnailUrl = game.thumbnailUrl,
            isInstalled = isInstalled,
            isFavorite = isFavorite,
            userRating = userRating,
            playCount = playCount,
            lastPlayed = lastPlayed,
            totalPlayTime = totalPlayTime,
            highScore = highScore,
            achievementsUnlocked = achievementsUnlocked,
            totalAchievements = totalAchievements
        )
    }

    private fun GameCategory.toDomainCategory(): DomainGameCategory {
        return DomainGameCategory(
            id = id,
            name = name,
            description = description,
            iconUrl = iconUrl,
            gameCount = gameCount,
            isPopular = isPopular,
            color = color
        )
    }

    private fun defaultSortOptions() = listOf(
        SortOption("name", "Name"),
        SortOption("popularity", "Popularity"),
        SortOption("rating", "Rating"),
        SortOption("recently_added", "Recently Added"),
        SortOption("last_played", "Last Played")
    )
}