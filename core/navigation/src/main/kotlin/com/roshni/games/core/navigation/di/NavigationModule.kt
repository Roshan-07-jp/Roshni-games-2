package com.roshni.games.core.navigation.di

import com.roshni.games.core.navigation.controller.NavigationFlowController
import com.roshni.games.core.navigation.controller.NavigationFlowControllerImpl
import com.roshni.games.core.navigation.integration.NavigationIntegration
import com.roshni.games.core.navigation.integration.NavigationIntegrationFactory
import com.roshni.games.core.navigation.model.NavigationEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideNavigationEventBus(): NavigationEventBus {
        return NavigationEventBus()
    }

    @Provides
    @Singleton
    fun provideNavigationFlowController(
        @NavigationDispatcher dispatcher: CoroutineDispatcher,
        eventBus: NavigationEventBus
    ): NavigationFlowController {
        return NavigationFlowControllerImpl(dispatcher, eventBus)
    }

    @Provides
    @Singleton
    fun provideNavigationIntegration(
        navigationFlowController: NavigationFlowController,
        @NavigationDispatcher dispatcher: CoroutineDispatcher
    ): NavigationIntegration {
        return NavigationIntegration(navigationFlowController, dispatcher)
    }

    @Provides
    @ActivityScoped
    @NavigationDispatcher
    fun provideNavigationDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Module
@InstallIn(ActivityComponent::class)
object NavigationActivityModule {

    @Provides
    @ActivityScoped
    fun provideNavigationIntegrationFactory(
        navigationFlowController: NavigationFlowController
    ): NavigationIntegrationFactory {
        return NavigationIntegrationFactory
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NavigationDispatcher