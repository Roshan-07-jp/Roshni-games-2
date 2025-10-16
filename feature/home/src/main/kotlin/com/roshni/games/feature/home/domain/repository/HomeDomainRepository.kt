package com.roshni.games.feature.home.domain.repository

import com.roshni.games.feature.home.domain.model.HomeScreenState
import kotlinx.coroutines.flow.Flow

interface HomeDomainRepository {
    fun getHomeScreenState(): Flow<HomeScreenState>
    suspend fun refreshHomeData(): Boolean
}