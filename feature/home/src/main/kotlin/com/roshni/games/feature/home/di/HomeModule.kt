package com.roshni.games.feature.home.di

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.feature.home.data.datasource.HomeDataSource
import com.roshni.games.feature.home.data.datasource.HomeDataSourceImpl
import com.roshni.games.feature.home.data.repository.HomeRepository
import com.roshni.games.feature.home.data.repository.HomeRepositoryImpl
import com.roshni.games.feature.home.domain.repository.HomeDomainRepository
import com.roshni.games.feature.home.domain.repository.HomeDomainRepositoryImpl
import com.roshni.games.feature.home.domain.usecase.GetHomeScreenDataUseCase
import com.roshni.games.feature.home.domain.usecase.RefreshHomeDataUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object HomeModule {

    @Provides
    @ViewModelScoped
    fun provideHomeDataSource(
        gameDao: GameDao,
        playerDao: PlayerDao,
        scoreDao: ScoreDao
    ): HomeDataSource {
        return HomeDataSourceImpl(gameDao, playerDao, scoreDao)
    }

    @Provides
    @ViewModelScoped
    fun provideHomeRepository(
        homeDataSource: HomeDataSource
    ): HomeRepository {
        return HomeRepositoryImpl(homeDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideHomeDomainRepository(
        homeRepository: HomeRepository
    ): HomeDomainRepository {
        return HomeDomainRepositoryImpl(homeRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetHomeScreenDataUseCase(
        homeDomainRepository: HomeDomainRepository
    ): GetHomeScreenDataUseCase {
        return GetHomeScreenDataUseCase(homeDomainRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideRefreshHomeDataUseCase(
        homeDomainRepository: HomeDomainRepository
    ): RefreshHomeDataUseCase {
        return RefreshHomeDataUseCase(homeDomainRepository)
    }
}