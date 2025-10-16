package com.roshni.games.feature.gamelibrary.domain.model

import kotlinx.datetime.LocalDateTime

data class GameLibraryState(
    val games: List<GameItem>,
    val categories: List<GameCategory>,
    val filter: GameFilter,
    val sortOptions: List<SortOption>,
    val selectedSortOption: SortOption,
    val viewMode: ViewMode,
    val searchQuery: String,
    val isLoading: Boolean,
    val error: String?,
    val hasMorePages: Boolean,
    val currentPage: Int,
    val searchResults: SearchResults?
)

data class GameItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val iconUrl: String?,
    val thumbnailUrl: String?,
    val isInstalled: Boolean,
    val isFavorite: Boolean,
    val userRating: Float?,
    val playCount: Int,
    val lastPlayed: LocalDateTime?,
    val totalPlayTime: Long,
    val highScore: Int?,
    val achievementsUnlocked: Int,
    val totalAchievements: Int
)

data class GameCategory(
    val id: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val gameCount: Int,
    val isPopular: Boolean,
    val color: String?
)

data class GameFilter(
    val categories: List<String>,
    val difficulties: List<String>,
    val minRating: Float?,
    val maxRating: Float?,
    val installedOnly: Boolean,
    val favoritesOnly: Boolean,
    val multiplayerOnly: Boolean
)

data class SortOption(
    val id: String,
    val name: String,
    val isSelected: Boolean
)

enum class ViewMode {
    GRID, LIST
}

data class SearchResults(
    val games: List<GameItem>,
    val totalCount: Int,
    val hasMoreResults: Boolean,
    val searchTime: Long
)

sealed class GameLibraryNavigationEvent {
    object NavigateToGameDetails : GameLibraryNavigationEvent()
    data class NavigateToGame(val gameId: String) : GameLibraryNavigationEvent()
    data class NavigateToCategory(val categoryId: String) : GameLibraryNavigationEvent()
    object NavigateToFilters : GameLibraryNavigationEvent()
    object NavigateToSearch : GameLibraryNavigationEvent()
}