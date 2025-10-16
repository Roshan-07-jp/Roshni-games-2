package com.roshni.games.gameengine.di

import android.content.Context
import com.roshni.games.gameengine.assets.AssetManager
import com.roshni.games.gameengine.core.GameEngine
import com.roshni.games.gameengine.state.GameStateManager
import com.roshni.games.gameengine.systems.AudioSystem
import com.roshni.games.gameengine.systems.InputSystem
import com.roshni.games.gameengine.systems.PhysicsSystem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameEngineModule {

    @Provides
    @Singleton
    fun provideGameEngine(
        @ApplicationContext context: Context
    ): GameEngine {
        // Note: GameEngine needs a game view to be injected later
        // This is a placeholder for the engine instance
        return GameEngine(context, android.view.View(context))
    }

    @Provides
    @Singleton
    fun provideAssetManager(
        @ApplicationContext context: Context
    ): AssetManager {
        return AssetManager(context, "default")
    }

    @Provides
    @Singleton
    fun provideGameStateManager(
        @ApplicationContext context: Context
    ): GameStateManager {
        return GameStateManager(context, "default")
    }

    @Provides
    @Singleton
    fun provideAudioSystem(
        @ApplicationContext context: Context
    ): AudioSystem {
        return AudioSystem(context)
    }

    @Provides
    @Singleton
    fun providePhysicsSystem(): PhysicsSystem {
        return PhysicsSystem()
    }

    @Provides
    @Singleton
    fun provideInputSystem(
        @ApplicationContext context: Context
    ): InputSystem {
        // Note: InputSystem needs a game view to be injected later
        return InputSystem(context, android.view.View(context))
    }
}