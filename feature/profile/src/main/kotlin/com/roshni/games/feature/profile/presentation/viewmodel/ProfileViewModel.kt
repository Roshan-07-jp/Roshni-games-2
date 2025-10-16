package com.roshni.games.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.feature.profile.domain.model.ProfileNavigationEvent
import com.roshni.games.feature.profile.domain.model.ProfileState
import com.roshni.games.feature.profile.domain.repository.ProfileDomainRepository
import com.roshni.games.feature.profile.domain.usecase.GetProfileStateUseCase
import com.roshni.games.feature.profile.domain.usecase.UpdateUserProfileUseCase
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
class ProfileViewModel @Inject constructor(
    private val profileDomainRepository: ProfileDomainRepository,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileNavigationEvent>()
    val navigationEvent: SharedFlow<ProfileNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        getProfileStateUseCase().onEach { profileState ->
            _uiState.update { currentState ->
                currentState.copy(profileState = profileState)
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SelectTab -> selectTab(action.tab)
            ProfileAction.EditProfile -> showEditProfile()
            ProfileAction.CustomizeProfile -> showCustomization()
            is ProfileAction.ViewAchievement -> viewAchievement(action.achievement)
            is ProfileAction.NavigateToGame -> navigateToGame(action.gameId)
            ProfileAction.RefreshProfile -> refreshProfile()
            ProfileAction.DismissDialogs -> dismissDialogs()
        }
    }

    private fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun showEditProfile() {
        _uiState.update { it.copy(showEditProfileDialog = true) }
    }

    private fun showCustomization() {
        _uiState.update { it.copy(showCustomizationDialog = true) }
    }

    private fun viewAchievement(achievement: com.roshni.games.feature.profile.domain.model.Achievement) {
        _uiState.update {
            it.copy(
                showAchievementDetailsDialog = true,
                selectedAchievement = achievement
            )
        }
    }

    private fun navigateToGame(gameId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(ProfileNavigationEvent.NavigateToGame(gameId))
        }
    }

    private fun refreshProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(
                        profileState = currentState.profileState.copy(isLoading = true)
                    )
                }

                val success = profileDomainRepository.refreshProfileData()
                if (!success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            profileState = currentState.profileState.copy(
                                isLoading = false,
                                error = "Failed to refresh profile"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        profileState = currentState.profileState.copy(
                            isLoading = false,
                            error = "Failed to refresh profile: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun dismissDialogs() {
        _uiState.update {
            it.copy(
                showEditProfileDialog = false,
                showCustomizationDialog = false,
                showAchievementDetailsDialog = false,
                selectedAchievement = null
            )
        }
    }
}