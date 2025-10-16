package com.roshni.games.gamecatalog.di

import com.roshni.games.gamecatalog.data.repository.GameCatalogRepositoryImpl
import com.roshni.games.gamecatalog.domain.repository.GameCatalogRepository
import com.roshni.games.gamecatalog.domain.usecase.GetGameCatalogUseCase
import com.roshni.games.gamecatalog.domain.usecase.SearchGamesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameCatalogModule {

    @Provides
    @Singleton
    fun provideGameCatalogRepository(): GameCatalogRepository {
        return GameCatalogRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetGameCatalogUseCase(repository: GameCatalogRepository): GetGameCatalogUseCase {
        return GetGameCatalogUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchGamesUseCase(repository: GameCatalogRepository): SearchGamesUseCase {
        return SearchGamesUseCase(repository)
    }
}