package com.roshni.games.feature.splash.presentation.viewmodel

import com.roshni.games.feature.splash.domain.model.InitializationResult
import com.roshni.games.feature.splash.domain.model.SplashProgress

data class SplashUiState(
    val isLoading: Boolean = true,
    val progress: Float = 0f,
    val currentStep: SplashProgress = SplashProgress(
        step = com.roshni.games.feature.splash.domain.model.InitializationStep.IDLE,
        progress = 0f,
        message = "Initializing..."
    ),
    val error: String? = null,
    val isComplete: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val updateInfo: com.roshni.games.feature.splash.domain.model.UpdateInfo? = null,
    val isFirstLaunch: Boolean = false
)

sealed class SplashNavigationEvent {
    object NavigateToHome : SplashNavigationEvent()
    object NavigateToOnboarding : SplashNavigationEvent()
    data class NavigateToUpdate(val updateUrl: String) : SplashNavigationEvent()
    data class ShowError(val message: String) : SplashNavigationEvent()
}