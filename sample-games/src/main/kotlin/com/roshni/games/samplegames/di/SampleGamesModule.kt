package com.roshni.games.samplegames.di

import android.content.Context
import android.view.View
import com.roshni.games.samplegames.casual.Match3Game
import com.roshni.games.samplegames.puzzle.SudokuGame
import com.roshni.games.samplegames.strategy.TowerDefenseGame
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SampleGamesModule {

    @Provides
    @Singleton
    fun provideSudokuGame(
        @ApplicationContext context: Context
    ): SudokuGame {
        return SudokuGame(context, View(context))
    }

    @Provides
    @Singleton
    fun provideMatch3Game(
        @ApplicationContext context: Context
    ): Match3Game {
        return Match3Game(context, View(context))
    }

    @Provides
    @Singleton
    fun provideTowerDefenseGame(
        @ApplicationContext context: Context
    ): TowerDefenseGame {
        return TowerDefenseGame(context, View(context))
    }
}