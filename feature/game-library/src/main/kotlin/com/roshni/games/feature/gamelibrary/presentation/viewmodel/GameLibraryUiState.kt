package com.roshni.games.feature.gamelibrary.presentation.viewmodel

import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryState

data class GameLibraryUiState(
    val gameLibraryState: GameLibraryState = GameLibraryState(
        games = emptyList(),
        categories = emptyList(),
        filter = com.roshni.games.feature.gamelibrary.domain.model.GameFilter(),
        sortOptions = emptyList(),
        selectedSortOption = com.roshni.games.feature.gamelibrary.domain.model.SortOption("name", "Name"),
        viewMode = com.roshni.games.feature.gamelibrary.domain.model.ViewMode.GRID,
        searchQuery = "",
        isLoading = true,
        error = null,
        hasMorePages = false,
        currentPage = 0,
        searchResults = null
    ),
    val showSearchBar: Boolean = false,
    val showFilters: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val selectedCategories: List<String> = emptyList(),
    val selectedDifficulties: List<String> = emptyList()
)

sealed class GameLibraryAction {
    object ToggleViewMode : GameLibraryAction()
    object ToggleSearch : GameLibraryAction()
    object ToggleFilters : GameLibraryAction()
    data class SearchQueryChanged(val query: String) : GameLibraryAction()
    data class CategoryFilterChanged(val categories: List<String>) : GameLibraryAction()
    data class DifficultyFilterChanged(val difficulties: List<String>) : GameLibraryAction()
    data class SortOptionSelected(val sortOption: com.roshni.games.feature.gamelibrary.domain.model.SortOption) : GameLibraryAction()
    data class GameClicked(val gameId: String) : GameLibraryAction()
    data class CategoryClicked(val categoryId: String) : GameLibraryAction()
    data class ToggleFavorite(val gameId: String) : GameLibraryAction()
    object RefreshLibrary : GameLibraryAction()
    object LoadMoreGames : GameLibraryAction()
    object ClearFilters : GameLibraryAction()
    object DismissError : GameLibraryAction()
}