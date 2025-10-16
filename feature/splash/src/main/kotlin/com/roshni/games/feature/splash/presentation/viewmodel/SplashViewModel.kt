package com.roshni.games.feature.splash.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.core.utils.Result
import com.roshni.games.feature.splash.domain.model.InitializationResult
import com.roshni.games.feature.splash.domain.model.InitializationStep
import com.roshni.games.feature.splash.domain.model.SplashProgress
import com.roshni.games.feature.splash.domain.repository.SplashDomainRepository
import com.roshni.games.feature.splash.domain.usecase.GetAppInitializationStateUseCase
import com.roshni.games.feature.splash.domain.usecase.PerformInitializationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val splashDomainRepository: SplashDomainRepository,
    private val getAppInitializationStateUseCase: GetAppInitializationStateUseCase,
    private val performInitializationUseCase: PerformInitializationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SplashNavigationEvent>()
    val navigationEvent: SharedFlow<SplashNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        startInitialization()
    }

    private fun startInitialization() {
        viewModelScope.launch {
            try {
                // Step 1: Check for updates first
                val updateResult = splashDomainRepository.checkForUpdates()
                when (updateResult) {
                    is InitializationResult.UpdateRequired -> {
                        _uiState.value = _uiState.value.copy(
                            showUpdateDialog = true,
                            updateInfo = updateResult.updateInfo
                        )
                        return@launch
                    }
                    is InitializationResult.Error -> {
                        // Continue with initialization even if update check fails
                    }
                    InitializationResult.Success -> {
                        // No updates, continue with initialization
                    }
                }

                // Step 2: Perform app initialization with progress updates
                performInitializationWithProgress()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Initialization failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun performInitializationWithProgress() {
        val progressSteps = listOf(
            SplashProgress(InitializationStep.CHECKING_VERSION, 0.1f, "Checking version..."),
            SplashProgress(InitializationStep.LOADING_USER_DATA, 0.3f, "Loading user data..."),
            SplashProgress(InitializationStep.INITIALIZING_DATABASE, 0.5f, "Initializing database..."),
            SplashProgress(InitializationStep.LOADING_GAME_DATA, 0.7f, "Loading game data..."),
            SplashProgress(InitializationStep.CHECKING_PERMISSIONS, 0.9f, "Checking permissions..."),
            SplashProgress(InitializationStep.COMPLETE, 1.0f, "Complete!")
        )

        for (step in progressSteps) {
            _uiState.value = _uiState.value.copy(
                currentStep = step,
                progress = step.progress,
                isLoading = step.step != InitializationStep.COMPLETE
            )
            delay(800) // Simulate work time
        }

        // Step 3: Perform actual initialization
        val result = performInitializationUseCase()
        when (result) {
            InitializationResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isComplete = true
                )

                // Navigate to appropriate screen
                delay(500) // Brief pause to show completion
                navigateToNextScreen()
            }
            is InitializationResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
            is InitializationResult.UpdateRequired -> {
                _uiState.value = _uiState.value.copy(
                    showUpdateDialog = true,
                    updateInfo = result.updateInfo
                )
            }
        }
    }

    private suspend fun navigateToNextScreen() {
        // Check if it's first launch to determine navigation
        getAppInitializationStateUseCase().collect { state ->
            if (state.isFirstLaunch) {
                _navigationEvent.emit(SplashNavigationEvent.NavigateToOnboarding)
            } else {
                _navigationEvent.emit(SplashNavigationEvent.NavigateToHome)
            }
        }
    }

    fun onUpdateDialogDismissed() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
        // Continue with initialization after dismissing update dialog
        viewModelScope.launch {
            performInitializationWithProgress()
        }
    }

    fun onUpdateConfirmed() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
        uiState.value.updateInfo?.updateUrl?.let { updateUrl ->
            viewModelScope.launch {
                _navigationEvent.emit(SplashNavigationEvent.NavigateToUpdate(updateUrl))
            }
        }
    }

    fun retryInitialization() {
        _uiState.value = _uiState.value.copy(error = null)
        startInitialization()
    }
}