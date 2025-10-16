package com.roshni.games.feature.gamelibrary.di

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.feature.gamelibrary.data.datasource.GameLibraryDataSource
import com.roshni.games.feature.gamelibrary.data.datasource.GameLibraryDataSourceImpl
import com.roshni.games.feature.gamelibrary.data.repository.GameLibraryRepository
import com.roshni.games.feature.gamelibrary.data.repository.GameLibraryRepositoryImpl
import com.roshni.games.feature.gamelibrary.domain.repository.GameLibraryDomainRepository
import com.roshni.games.feature.gamelibrary.domain.repository.GameLibraryDomainRepositoryImpl
import com.roshni.games.feature.gamelibrary.domain.usecase.GetGameLibraryDataUseCase
import com.roshni.games.feature.gamelibrary.domain.usecase.SearchGamesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object GameLibraryModule {

    @Provides
    @ViewModelScoped
    fun provideGameLibraryDataSource(
        gameDao: GameDao
    ): GameLibraryDataSource {
        return GameLibraryDataSourceImpl(gameDao)
    }

    @Provides
    @ViewModelScoped
    fun provideGameLibraryRepository(
        gameLibraryDataSource: GameLibraryDataSource
    ): GameLibraryRepository {
        return GameLibraryRepositoryImpl(gameLibraryDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideGameLibraryDomainRepository(
        gameLibraryRepository: GameLibraryRepository
    ): GameLibraryDomainRepository {
        return GameLibraryDomainRepositoryImpl(gameLibraryRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetGameLibraryDataUseCase(
        gameLibraryDomainRepository: GameLibraryDomainRepository
    ): GetGameLibraryDataUseCase {
        return GetGameLibraryDataUseCase(gameLibraryDomainRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSearchGamesUseCase(
        gameLibraryDomainRepository: GameLibraryDomainRepository
    ): SearchGamesUseCase {
        return SearchGamesUseCase(gameLibraryDomainRepository)
    }
}