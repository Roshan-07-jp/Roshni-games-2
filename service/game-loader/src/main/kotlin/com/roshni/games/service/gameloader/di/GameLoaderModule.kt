package com.roshni.games.service.gameloader.di

import com.roshni.games.service.gameloader.data.datasource.GameModuleDataSource
import com.roshni.games.service.gameloader.data.datasource.LocalGameModuleDataSource
import com.roshni.games.service.gameloader.data.repository.GameModuleRepository
import com.roshni.games.service.gameloader.data.repository.GameModuleRepositoryImpl
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepository
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepositoryImpl
import com.roshni.games.service.gameloader.domain.usecase.GetGameModulesUseCase
import com.roshni.games.service.gameloader.domain.usecase.LoadGameModuleUseCase
import com.roshni.games.service.gameloader.domain.usecase.SearchGameModulesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameLoaderModule {

    @Provides
    @Singleton
    fun provideGameModuleDataSource(): GameModuleDataSource {
        return LocalGameModuleDataSource()
    }

    @Provides
    @Singleton
    fun provideGameModuleRepository(
        dataSource: GameModuleDataSource
    ): GameModuleRepository {
        return GameModuleRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideGameModuleDomainRepository(
        repository: GameModuleRepository
    ): GameModuleDomainRepository {
        return GameModuleDomainRepositoryImpl(repository)
    }

    @Provides
    @Singleton
    fun provideGetGameModulesUseCase(
        repository: GameModuleDomainRepository
    ): GetGameModulesUseCase {
        return GetGameModulesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideLoadGameModuleUseCase(
        repository: GameModuleDomainRepository
    ): LoadGameModuleUseCase {
        return LoadGameModuleUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchGameModulesUseCase(
        repository: GameModuleDomainRepository
    ): SearchGameModulesUseCase {
        return SearchGameModulesUseCase(repository)
    }
}