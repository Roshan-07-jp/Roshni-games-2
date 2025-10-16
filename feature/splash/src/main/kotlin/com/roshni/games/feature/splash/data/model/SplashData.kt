package com.roshni.games.feature.splash.data.model

import kotlinx.datetime.LocalDateTime

data class AppInitializationData(
    val isFirstLaunch: Boolean = false,
    val lastVersion: String? = null,
    val currentVersion: String,
    val requiresUpdate: Boolean = false,
    val updateUrl: String? = null,
    val initializationTime: LocalDateTime
)

data class SplashScreenState(
    val isLoading: Boolean = true,
    val progress: Float = 0f,
    val currentStep: InitializationStep = InitializationStep.IDLE,
    val error: String? = null,
    val isComplete: Boolean = false
)

enum class InitializationStep {
    IDLE,
    CHECKING_VERSION,
    LOADING_USER_DATA,
    INITIALIZING_DATABASE,
    LOADING_GAME_DATA,
    CHECKING_PERMISSIONS,
    COMPLETE,
    ERROR
}

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val isUpdateAvailable: Boolean,
    val isForceUpdate: Boolean = false,
    val updateUrl: String? = null,
    val changelog: List<String> = emptyList()
)