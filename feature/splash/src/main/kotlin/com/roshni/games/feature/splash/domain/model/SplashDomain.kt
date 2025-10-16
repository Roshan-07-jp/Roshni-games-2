package com.roshni.games.feature.splash.domain.model

import kotlinx.datetime.LocalDateTime

data class AppInitializationState(
    val isFirstLaunch: Boolean,
    val currentVersion: String,
    val requiresUpdate: Boolean,
    val updateInfo: UpdateInfo?,
    val isInitialized: Boolean,
    val initializationTime: LocalDateTime
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val isUpdateAvailable: Boolean,
    val isForceUpdate: Boolean,
    val updateUrl: String?,
    val changelog: List<String>
)

sealed class InitializationResult {
    object Success : InitializationResult()
    data class Error(val message: String) : InitializationResult()
    data class UpdateRequired(val updateInfo: UpdateInfo) : InitializationResult()
}

data class SplashProgress(
    val step: InitializationStep,
    val progress: Float,
    val message: String
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