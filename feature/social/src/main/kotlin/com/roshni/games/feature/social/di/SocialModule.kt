package com.roshni.games.feature.social.di

import com.roshni.games.feature.social.domain.SocialService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SocialModule {

    @Provides
    @ViewModelScoped
    fun provideSocialService(): SocialService {
        return SocialService()
    }
}