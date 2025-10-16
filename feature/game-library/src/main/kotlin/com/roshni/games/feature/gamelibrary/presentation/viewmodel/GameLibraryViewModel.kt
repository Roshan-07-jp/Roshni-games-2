package com.roshni.games.feature.gamelibrary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.feature.gamelibrary.domain.model.GameFilter
import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryNavigationEvent
import com.roshni.games.feature.gamelibrary.domain.model.SearchResults
import com.roshni.games.feature.gamelibrary.domain.model.SortOption
import com.roshni.games.feature.gamelibrary.domain.model.ViewMode
import com.roshni.games.feature.gamelibrary.domain.repository.GameLibraryDomainRepository
import com.roshni.games.feature.gamelibrary.domain.usecase.GetGameLibraryDataUseCase
import com.roshni.games.feature.gamelibrary.domain.usecase.SearchGamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    private val gameLibraryDomainRepository: GameLibraryDomainRepository,
    private val getGameLibraryDataUseCase: GetGameLibraryDataUseCase,
    private val searchGamesUseCase: SearchGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameLibraryUiState())
    val uiState: StateFlow<GameLibraryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<GameLibraryNavigationEvent>()
    val navigationEvent: SharedFlow<GameLibraryNavigationEvent> = _navigationEvent.asSharedFlow()

    private var searchResults: SearchResults? = null

    init {
        loadGameLibrary()
        loadCategories()
    }

    private fun loadGameLibrary() {
        val currentState = _uiState.value

        getGameLibraryDataUseCase(
            filter = currentState.gameLibraryState.filter,
            sortOption = currentState.gameLibraryState.selectedSortOption,
            searchQuery = currentState.searchQuery,
            viewMode = currentState.gameLibraryState.viewMode
        ).onEach { gameLibraryState ->
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    gameLibraryState = gameLibraryState.copy(
                        searchResults = searchResults
                    )
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = gameLibraryDomainRepository.getCategories()
                _uiState.update { currentState ->
                    currentState.copy(
                        gameLibraryState = currentState.gameLibraryState.copy(
                            categories = categories
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun onAction(action: GameLibraryAction) {
        when (action) {
            GameLibraryAction.ToggleViewMode -> toggleViewMode()
            GameLibraryAction.ToggleSearch -> toggleSearch()
            GameLibraryAction.ToggleFilters -> toggleFilters()
            is GameLibraryAction.SearchQueryChanged -> onSearchQueryChanged(action.query)
            is GameLibraryAction.CategoryFilterChanged -> onCategoryFilterChanged(action.categories)
            is GameLibraryAction.DifficultyFilterChanged -> onDifficultyFilterChanged(action.difficulties)
            is GameLibraryAction.SortOptionSelected -> onSortOptionSelected(action.sortOption)
            is GameLibraryAction.GameClicked -> onGameClicked(action.gameId)
            is GameLibraryAction.CategoryClicked -> onCategoryClicked(action.categoryId)
            is GameLibraryAction.ToggleFavorite -> onToggleFavorite(action.gameId)
            GameLibraryAction.RefreshLibrary -> refreshLibrary()
            GameLibraryAction.LoadMoreGames -> loadMoreGames()
            GameLibraryAction.ClearFilters -> clearFilters()
            GameLibraryAction.DismissError -> dismissError()
        }
    }

    private fun toggleViewMode() {
        _uiState.update { currentState ->
            val newViewMode = when (currentState.gameLibraryState.viewMode) {
                ViewMode.GRID -> ViewMode.LIST
                ViewMode.LIST -> ViewMode.GRID
            }
            currentState.copy(
                gameLibraryState = currentState.gameLibraryState.copy(
                    viewMode = newViewMode
                )
            )
        }
    }

    private fun toggleSearch() {
        _uiState.update { currentState ->
            currentState.copy(
                showSearchBar = !currentState.showSearchBar,
                isSearchActive = !currentState.isSearchActive
            )
        }
    }

    private fun toggleFilters() {
        _uiState.update { currentState ->
            currentState.copy(showFilters = !currentState.showFilters)
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { currentState ->
            currentState.copy(searchQuery = query)
        }

        if (query.length >= 2) {
            performSearch(query)
        } else {
            searchResults = null
            loadGameLibrary()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                val results = searchGamesUseCase(query)
                searchResults = results

                _uiState.update { currentState ->
                    currentState.copy(
                        gameLibraryState = currentState.gameLibraryState.copy(
                            searchResults = results
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle search error
            }
        }
    }

    private fun onCategoryFilterChanged(categories: List<String>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategories = categories,
                gameLibraryState = currentState.gameLibraryState.copy(
                    filter = currentState.gameLibraryState.filter.copy(
                        categories = categories
                    )
                )
            )
        }
        loadGameLibrary()
    }

    private fun onDifficultyFilterChanged(difficulties: List<String>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDifficulties = difficulties,
                gameLibraryState = currentState.gameLibraryState.copy(
                    filter = currentState.gameLibraryState.filter.copy(
                        difficulties = difficulties
                    )
                )
            )
        }
        loadGameLibrary()
    }

    private fun onSortOptionSelected(sortOption: SortOption) {
        _uiState.update { currentState ->
            currentState.copy(
                gameLibraryState = currentState.gameLibraryState.copy(
                    selectedSortOption = sortOption,
                    sortOptions = currentState.gameLibraryState.sortOptions.map {
                        it.copy(isSelected = it.id == sortOption.id)
                    }
                )
            )
        }
        loadGameLibrary()
    }

    private fun onGameClicked(gameId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(GameLibraryNavigationEvent.NavigateToGame(gameId))
        }
    }

    private fun onCategoryClicked(categoryId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(GameLibraryNavigationEvent.NavigateToCategory(categoryId))
        }
    }

    private fun onToggleFavorite(gameId: String) {
        viewModelScope.launch {
            try {
                gameLibraryDomainRepository.toggleFavorite(gameId)
                // Refresh data to show updated favorite status
                loadGameLibrary()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun refreshLibrary() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(gameLibraryState = it.gameLibraryState.copy(isLoading = true)) }
                val success = gameLibraryDomainRepository.refreshLibrary()
                if (success) {
                    loadGameLibrary()
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            gameLibraryState = currentState.gameLibraryState.copy(
                                isLoading = false,
                                error = "Failed to refresh library"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        gameLibraryState = currentState.gameLibraryState.copy(
                            isLoading = false,
                            error = "Failed to refresh library: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun loadMoreGames() {
        // Implementation for pagination would go here
    }

    private fun clearFilters() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategories = emptyList(),
                selectedDifficulties = emptyList(),
                gameLibraryState = currentState.gameLibraryState.copy(
                    filter = GameFilter()
                )
            )
        }
        loadGameLibrary()
    }

    private fun dismissError() {
        _uiState.update { currentState ->
            currentState.copy(
                gameLibraryState = currentState.gameLibraryState.copy(error = null)
            )
        }
    }
}