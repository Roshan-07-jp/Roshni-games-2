package com.roshni.games.multiplayer.di

import com.roshni.games.multiplayer.leaderboard.LeaderboardService
import com.roshni.games.multiplayer.matchmaking.MatchmakingService
import com.roshni.games.multiplayer.network.MultiplayerClient
import com.roshni.games.multiplayer.sync.GameSynchronizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.URI
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MultiplayerModule {

    @Provides
    @Singleton
    fun provideMultiplayerClient(): MultiplayerClient {
        // In real implementation, this would use actual server URI and credentials
        val serverUri = URI("ws://localhost:8080/multiplayer")
        val playerId = "player_${System.currentTimeMillis()}"
        return MultiplayerClient(serverUri, playerId)
    }

    @Provides
    @Singleton
    fun provideMatchmakingService(): MatchmakingService {
        return MatchmakingService()
    }

    @Provides
    @Singleton
    fun provideLeaderboardService(): LeaderboardService {
        return LeaderboardService()
    }

    @Provides
    @Singleton
    fun provideGameSynchronizer(): GameSynchronizer {
        // In real implementation, this would use actual session and player IDs
        return GameSynchronizer("default_session", "default_player")
    }
}