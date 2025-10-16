package com.roshni.games.feature.settings.di

import com.roshni.games.core.utils.AndroidUtils
import com.roshni.games.feature.settings.data.datasource.SettingsDataSource
import com.roshni.games.feature.settings.data.datasource.SettingsDataSourceImpl
import com.roshni.games.feature.settings.data.repository.SettingsRepository
import com.roshni.games.feature.settings.data.repository.SettingsRepositoryImpl
import com.roshni.games.feature.settings.domain.repository.SettingsDomainRepository
import com.roshni.games.feature.settings.domain.repository.SettingsDomainRepositoryImpl
import com.roshni.games.feature.settings.domain.usecase.GetSettingsStateUseCase
import com.roshni.games.feature.settings.domain.usecase.UpdateThemeSettingsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SettingsModule {

    @Provides
    @ViewModelScoped
    fun provideSettingsDataSource(
        androidUtils: AndroidUtils
    ): SettingsDataSource {
        return SettingsDataSourceImpl(androidUtils)
    }

    @Provides
    @ViewModelScoped
    fun provideSettingsRepository(
        settingsDataSource: SettingsDataSource
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideSettingsDomainRepository(
        settingsRepository: SettingsRepository
    ): SettingsDomainRepository {
        return SettingsDomainRepositoryImpl(settingsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetSettingsStateUseCase(
        settingsDomainRepository: SettingsDomainRepository
    ): GetSettingsStateUseCase {
        return GetSettingsStateUseCase(settingsDomainRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateThemeSettingsUseCase(
        settingsDomainRepository: SettingsDomainRepository
    ): UpdateThemeSettingsUseCase {
        return UpdateThemeSettingsUseCase(settingsDomainRepository)
    }
}