package com.roshni.games.feature.splash.domain.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.splash.data.model.AppInitializationData
import com.roshni.games.feature.splash.data.repository.SplashRepository
import com.roshni.games.feature.splash.domain.model.AppInitializationState
import com.roshni.games.feature.splash.domain.model.InitializationResult
import com.roshni.games.feature.splash.domain.model.UpdateInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SplashDomainRepositoryImpl @Inject constructor(
    private val splashRepository: SplashRepository
) : SplashDomainRepository {

    override fun getAppInitializationState(): Flow<AppInitializationState> {
        return splashRepository.getAppInitializationData().map { result ->
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    AppInitializationState(
                        isFirstLaunch = data.isFirstLaunch,
                        currentVersion = data.currentVersion,
                        requiresUpdate = data.requiresUpdate,
                        updateInfo = null, // Will be populated by checkForUpdates
                        isInitialized = false,
                        initializationTime = data.initializationTime
                    )
                }
                is Result.Error -> {
                    // Handle error case - return default state
                    AppInitializationState(
                        isFirstLaunch = false,
                        currentVersion = "1.0.0",
                        requiresUpdate = false,
                        updateInfo = null,
                        isInitialized = false,
                        initializationTime = kotlinx.datetime.Clock.System.now().toLocalDateTime(
                            kotlinx.datetime.TimeZone.currentSystemDefault()
                        )
                    )
                }
            }
        }
    }

    override suspend fun performInitialization(): InitializationResult {
        return try {
            // Step 1: Initialize app core components
            val initResult = splashRepository.initializeApp()
            if (initResult is Result.Error) {
                return InitializationResult.Error("Failed to initialize app: ${initResult.exception.message}")
            }

            // Step 2: Preload essential data
            val preloadResult = splashRepository.preloadEssentialData()
            if (preloadResult is Result.Error) {
                return InitializationResult.Error("Failed to preload data: ${preloadResult.exception.message}")
            }

            InitializationResult.Success
        } catch (e: Exception) {
            InitializationResult.Error("Initialization failed: ${e.message}")
        }
    }

    override suspend fun checkForUpdates(): InitializationResult {
        return try {
            val result = splashRepository.checkForUpdates()
            when (result) {
                is Result.Success -> {
                    val versionInfo = result.data
                    if (versionInfo.isUpdateAvailable) {
                        val updateInfo = UpdateInfo(
                            versionCode = versionInfo.versionCode,
                            versionName = versionInfo.versionName,
                            isUpdateAvailable = versionInfo.isUpdateAvailable,
                            isForceUpdate = versionInfo.isForceUpdate,
                            updateUrl = versionInfo.updateUrl,
                            changelog = versionInfo.changelog
                        )
                        InitializationResult.UpdateRequired(updateInfo)
                    } else {
                        InitializationResult.Success
                    }
                }
                is Result.Error -> {
                    InitializationResult.Error("Failed to check for updates: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            InitializationResult.Error("Update check failed: ${e.message}")
        }
    }
}