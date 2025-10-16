package com.roshni.games.service.backgroundsync.di

import com.roshni.games.service.backgroundsync.data.datasource.LocalSyncDataSource
import com.roshni.games.service.backgroundsync.data.datasource.SyncDataSource
import com.roshni.games.service.backgroundsync.data.repository.SyncRepository
import com.roshni.games.service.backgroundsync.data.repository.SyncRepositoryImpl
import com.roshni.games.service.backgroundsync.BackgroundSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackgroundSyncModule {

    @Provides
    @Singleton
    fun provideSyncDataSource(): SyncDataSource {
        return LocalSyncDataSource()
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        dataSource: SyncDataSource
    ): SyncRepository {
        return SyncRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideBackgroundSyncService(
        repository: SyncRepository
    ): BackgroundSyncService {
        return BackgroundSyncService(repository)
    }
}