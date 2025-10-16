package com.roshni.games.feature.leaderboard.di

import com.roshni.games.feature.leaderboard.domain.LeaderboardService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object LeaderboardModule {

    @Provides
    @ViewModelScoped
    fun provideLeaderboardService(): LeaderboardService {
        return LeaderboardService()
    }
}