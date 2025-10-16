package com.roshni.games.feature.splash.domain.repository

import com.roshni.games.feature.splash.domain.model.AppInitializationState
import com.roshni.games.feature.splash.domain.model.InitializationResult
import kotlinx.coroutines.flow.Flow

interface SplashDomainRepository {
    fun getAppInitializationState(): Flow<AppInitializationState>
    suspend fun performInitialization(): InitializationResult
    suspend fun checkForUpdates(): InitializationResult
}