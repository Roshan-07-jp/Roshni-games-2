package com.roshni.games.feature.splash.di

import com.roshni.games.core.utils.AndroidUtils
import com.roshni.games.feature.splash.data.datasource.SplashDataSource
import com.roshni.games.feature.splash.data.datasource.SplashDataSourceImpl
import com.roshni.games.feature.splash.data.repository.SplashRepository
import com.roshni.games.feature.splash.data.repository.SplashRepositoryImpl
import com.roshni.games.feature.splash.domain.repository.SplashDomainRepository
import com.roshni.games.feature.splash.domain.repository.SplashDomainRepositoryImpl
import com.roshni.games.feature.splash.domain.usecase.GetAppInitializationStateUseCase
import com.roshni.games.feature.splash.domain.usecase.PerformInitializationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SplashModule {

    @Provides
    @ViewModelScoped
    fun provideSplashDataSource(
        androidUtils: AndroidUtils
    ): SplashDataSource {
        return SplashDataSourceImpl(androidUtils)
    }

    @Provides
    @ViewModelScoped
    fun provideSplashRepository(
        splashDataSource: SplashDataSource
    ): SplashRepository {
        return SplashRepositoryImpl(splashDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideSplashDomainRepository(
        splashRepository: SplashRepository
    ): SplashDomainRepository {
        return SplashDomainRepositoryImpl(splashRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetAppInitializationStateUseCase(
        splashDomainRepository: SplashDomainRepository
    ): GetAppInitializationStateUseCase {
        return GetAppInitializationStateUseCase(splashDomainRepository)
    }

    @Provides
    @ViewModelScoped
    fun providePerformInitializationUseCase(
        splashDomainRepository: SplashDomainRepository
    ): PerformInitializationUseCase {
        return PerformInitializationUseCase(splashDomainRepository)
    }
}