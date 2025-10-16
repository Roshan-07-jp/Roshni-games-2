package com.roshni.games.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.feature.home.domain.model.HomeNavigationEvent
import com.roshni.games.feature.home.domain.repository.HomeDomainRepository
import com.roshni.games.feature.home.domain.usecase.GetHomeScreenDataUseCase
import com.roshni.games.feature.home.domain.usecase.RefreshHomeDataUseCase
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
class HomeViewModel @Inject constructor(
    private val homeDomainRepository: HomeDomainRepository,
    private val getHomeScreenDataUseCase: GetHomeScreenDataUseCase,
    private val refreshHomeDataUseCase: RefreshHomeDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<HomeNavigationEvent>()
    val navigationEvent: SharedFlow<HomeNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        getHomeScreenDataUseCase().onEach { homeState ->
            _uiState.update { currentState ->
                currentState.copy(
                    homeState = homeState,
                    isRefreshing = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RefreshData -> refreshData()
            HomeAction.DismissWelcomeMessage -> dismissWelcomeMessage()
            is HomeAction.NavigateToGame -> navigateToGame(action.gameId)
            is HomeAction.NavigateToCategory -> navigateToCategory(action.categoryId)
            HomeAction.NavigateToGameLibrary -> navigateToGameLibrary()
            HomeAction.NavigateToProfile -> navigateToProfile()
            HomeAction.NavigateToSettings -> navigateToSettings()
        }
    }

    private fun refreshData() {
        _uiState.update { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            try {
                val success = refreshHomeDataUseCase()
                if (!success) {
                    // Handle refresh error if needed
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun dismissWelcomeMessage() {
        _uiState.update { it.copy(showWelcomeMessage = false) }
    }

    private fun navigateToGame(gameId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.NavigateToGame(gameId))
        }
    }

    private fun navigateToCategory(categoryId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.NavigateToCategory(categoryId))
        }
    }

    private fun navigateToGameLibrary() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.NavigateToGameLibrary)
        }
    }

    private fun navigateToProfile() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.NavigateToProfile)
        }
    }

    private fun navigateToSettings() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.NavigateToSettings)
        }
    }
}