package com.roshni.games.feature.parentalcontrols.di

import com.roshni.games.feature.parentalcontrols.domain.SecurityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ParentalControlsModule {

    @Provides
    @ViewModelScoped
    fun provideSecurityService(): SecurityService {
        return SecurityService()
    }
}