package com.roshni.games.feature.accessibility.di

import android.content.Context
import com.roshni.games.feature.accessibility.AccessibilityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AccessibilityModule {

    @Provides
    @ViewModelScoped
    fun provideAccessibilityService(
        @ApplicationContext context: Context
    ): AccessibilityService {
        return AccessibilityService(context)
    }
}