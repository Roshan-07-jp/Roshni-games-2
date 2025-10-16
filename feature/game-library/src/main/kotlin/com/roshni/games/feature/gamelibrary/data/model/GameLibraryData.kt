package com.roshni.games.feature.gamelibrary.data.model

import com.roshni.games.core.database.model.GameEntity
import kotlinx.datetime.LocalDateTime

data class GameLibraryItem(
    val game: GameEntity,
    val isInstalled: Boolean = true,
    val isFavorite: Boolean = false,
    val userRating: Float? = null,
    val playCount: Int = 0,
    val lastPlayed: LocalDateTime? = null,
    val totalPlayTime: Long = 0, // in minutes
    val highScore: Int? = null,
    val achievementsUnlocked: Int = 0,
    val totalAchievements: Int = 0
)

data class GameCategory(
    val id: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val gameCount: Int,
    val isPopular: Boolean = false,
    val color: String? = null
)

data class GameFilter(
    val categories: List<String> = emptyList(),
    val difficulties: List<String> = emptyList(),
    val minRating: Float? = null,
    val maxRating: Float? = null,
    val installedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val multiplayerOnly: Boolean = false
)

data class GameSortOption(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

data class GameLibraryState(
    val games: List<GameLibraryItem> = emptyList(),
    val categories: List<GameCategory> = emptyList(),
    val filter: GameFilter = GameFilter(),
    val sortOptions: List<GameSortOption> = defaultSortOptions(),
    val selectedSortOption: GameSortOption = defaultSortOptions().first(),
    val viewMode: GameViewMode = GameViewMode.GRID,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = false,
    val currentPage: Int = 0
)

enum class GameViewMode {
    GRID, LIST
}

data class GameLibrarySearchResult(
    val games: List<GameLibraryItem>,
    val totalCount: Int,
    val hasMoreResults: Boolean,
    val searchTime: Long // in milliseconds
)

private fun defaultSortOptions() = listOf(
    GameSortOption("name", "Name"),
    GameSortOption("popularity", "Popularity"),
    GameSortOption("rating", "Rating"),
    GameSortOption("recently_added", "Recently Added"),
    GameSortOption("last_played", "Last Played")
)