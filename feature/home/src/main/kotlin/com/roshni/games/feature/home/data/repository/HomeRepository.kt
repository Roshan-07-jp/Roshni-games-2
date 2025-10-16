package com.roshni.games.feature.home.data.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.home.data.datasource.HomeDataSource
import com.roshni.games.feature.home.data.model.HomeScreenData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface HomeRepository {
    fun getHomeScreenData(): Flow<Result<HomeScreenData>>
    suspend fun refreshHomeData(): Result<Boolean>
}

class HomeRepositoryImpl @Inject constructor(
    private val homeDataSource: HomeDataSource
) : HomeRepository {

    override fun getHomeScreenData(): Flow<Result<HomeScreenData>> {
        return homeDataSource.getHomeScreenData()
            .map { data -> Result.Success(data) }
            .catch { error -> Result.Error(error) }
    }

    override suspend fun refreshHomeData(): Result<Boolean> {
        return try {
            val success = homeDataSource.refreshHomeData()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}