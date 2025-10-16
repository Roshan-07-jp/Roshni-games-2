package com.roshni.games.progression.di

import com.roshni.games.progression.achievement.AchievementManager
import com.roshni.games.progression.challenges.ChallengeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProgressionModule {

    @Provides
    @Singleton
    fun provideAchievementManager(): AchievementManager {
        return AchievementManager()
    }

    @Provides
    @Singleton
    fun provideChallengeManager(): ChallengeManager {
        return ChallengeManager()
    }
}