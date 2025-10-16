package com.roshni.games.feature.splash.data.datasource

import com.roshni.games.core.utils.AndroidUtils
import com.roshni.games.feature.splash.data.model.AppInitializationData
import com.roshni.games.feature.splash.data.model.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

interface SplashDataSource {
    fun getAppInitializationData(): Flow<AppInitializationData>
    suspend fun checkForUpdates(): VersionInfo
    suspend fun initializeApp(): Boolean
    suspend fun preloadEssentialData(): Boolean
}

class SplashDataSourceImpl @Inject constructor(
    private val androidUtils: AndroidUtils
) : SplashDataSource {

    override fun getAppInitializationData(): Flow<AppInitializationData> = flow {
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentVersion = androidUtils.getAppVersionName()
        val lastVersion = androidUtils.getLastAppVersion()
        val isFirstLaunch = lastVersion == null

        val initializationData = AppInitializationData(
            isFirstLaunch = isFirstLaunch,
            lastVersion = lastVersion,
            currentVersion = currentVersion,
            requiresUpdate = false, // Will be determined by checkForUpdates
            initializationTime = currentTime
        )

        emit(initializationData)
    }

    override suspend fun checkForUpdates(): VersionInfo {
        // Simulate network call delay
        delay(1500)

        val currentVersionCode = androidUtils.getAppVersionCode()
        val currentVersionName = androidUtils.getAppVersionName()

        // Simulate version check logic
        // In real implementation, this would check against a remote server
        val latestVersionCode = currentVersionCode + 1 // Simulate update available
        val isUpdateAvailable = latestVersionCode > currentVersionCode

        return VersionInfo(
            versionCode = latestVersionCode,
            versionName = "1.1.0", // Simulate newer version
            isUpdateAvailable = isUpdateAvailable,
            isForceUpdate = false,
            updateUrl = if (isUpdateAvailable) "https://play.google.com/store/apps/details?id=com.roshni.games" else null,
            changelog = if (isUpdateAvailable) {
                listOf(
                    "• Performance improvements",
                    "• Bug fixes",
                    "• New game modes",
                    "• Enhanced UI/UX"
                )
            } else emptyList()
        )
    }

    override suspend fun initializeApp(): Boolean {
        return try {
            // Simulate app initialization steps
            delay(800)
            // Initialize core components, check permissions, etc.
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun preloadEssentialData(): Boolean {
        return try {
            // Simulate data preloading
            delay(1200)
            // Preload essential game data, user preferences, etc.
            true
        } catch (e: Exception) {
            false
        }
    }
}