package com.roshni.games.offline.di

import android.content.Context
import com.roshni.games.offline.ai.AISystem
import com.roshni.games.offline.multiplayer.LocalMultiplayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {

    @Provides
    @Singleton
    fun provideAISystem(): AISystem {
        return AISystem()
    }

    @Provides
    @Singleton
    fun provideLocalMultiplayerManager(
        @ApplicationContext context: Context
    ): LocalMultiplayerManager {
        return LocalMultiplayerManager(context)
    }
}