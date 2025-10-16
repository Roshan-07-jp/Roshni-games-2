package com.roshni.games.feature.search.di

import com.roshni.games.feature.search.domain.GlobalSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SearchModule {

    @Provides
    @ViewModelScoped
    fun provideGlobalSearchService(): GlobalSearchService {
        return GlobalSearchService()
    }
}