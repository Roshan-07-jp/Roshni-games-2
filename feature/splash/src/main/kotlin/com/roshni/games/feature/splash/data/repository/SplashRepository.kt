package com.roshni.games.feature.splash.data.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.splash.data.datasource.SplashDataSource
import com.roshni.games.feature.splash.data.model.AppInitializationData
import com.roshni.games.feature.splash.data.model.VersionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SplashRepository {
    fun getAppInitializationData(): Flow<Result<AppInitializationData>>
    suspend fun checkForUpdates(): Result<VersionInfo>
    suspend fun initializeApp(): Result<Boolean>
    suspend fun preloadEssentialData(): Result<Boolean>
}

class SplashRepositoryImpl @Inject constructor(
    private val splashDataSource: SplashDataSource
) : SplashRepository {

    override fun getAppInitializationData(): Flow<Result<AppInitializationData>> {
        return splashDataSource.getAppInitializationData()
            .map { data -> Result.Success(data) }
            .catch { error -> Result.Error(error) }
    }

    override suspend fun checkForUpdates(): Result<VersionInfo> {
        return try {
            val versionInfo = splashDataSource.checkForUpdates()
            Result.Success(versionInfo)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun initializeApp(): Result<Boolean> {
        return try {
            val success = splashDataSource.initializeApp()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun preloadEssentialData(): Result<Boolean> {
        return try {
            val success = splashDataSource.preloadEssentialData()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}