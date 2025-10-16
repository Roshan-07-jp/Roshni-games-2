package com.roshni.games.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // This module serves as the main entry point for all feature modules
    // Individual feature modules are included via their own @Module annotations
    // in their respective DI modules

    // Core modules are included in their respective packages:
    // - DatabaseModule (core/database)
    // - NetworkModule (core/network)
    // - UiModule (core/ui)
    // - UtilsModule (core/utils)
    // - DesignSystemModule (core/design-system)
    // - NavigationModule (core/navigation)

    // Feature modules are included in their respective packages:
    // - SplashModule (feature/splash)
    // - HomeModule (feature/home)
    // - GameLibraryModule (feature/game-library)
    // - SettingsModule (feature/settings)
    // - ProfileModule (feature/profile)

    // Service modules are included in their respective packages:
    // - GameLoaderModule (service/game-loader)
    // - BackgroundSyncModule (service/background-sync)
    // - AnalyticsModule (service/analytics)
}