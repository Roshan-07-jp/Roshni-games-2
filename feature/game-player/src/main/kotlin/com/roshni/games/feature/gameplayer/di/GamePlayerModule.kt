package com.roshni.games.feature.gameplayer.di

import com.roshni.games.feature.gameplayer.domain.GameSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object GamePlayerModule {

    @Provides
    @ViewModelScoped
    fun provideGameSessionManager(): GameSessionManager {
        return GameSessionManager()
    }
}