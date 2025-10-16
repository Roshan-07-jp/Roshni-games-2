package com.roshni.games.metadata.di

import android.content.Context
import com.roshni.games.metadata.config.GameConfigurationManager
import com.roshni.games.metadata.localization.LocalizationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetadataModule {

    @Provides
    @Singleton
    fun provideGameConfigurationManager(
        @ApplicationContext context: Context
    ): GameConfigurationManager {
        return GameConfigurationManager(context)
    }

    @Provides
    @Singleton
    fun provideLocalizationManager(): LocalizationManager {
        return LocalizationManager()
    }
}