package com.roshni.games.feature.achievements.di

import com.roshni.games.feature.achievements.domain.AchievementService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AchievementModule {

    @Provides
    @ViewModelScoped
    fun provideAchievementService(): AchievementService {
        return AchievementService()
    }
}