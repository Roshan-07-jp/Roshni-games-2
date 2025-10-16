package com.roshni.games.service.analytics.di

import com.roshni.games.service.analytics.AnalyticsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsService(): AnalyticsService {
        return AnalyticsService()
    }
}