package com.roshni.games.feature.gameplayer.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for game player functionality
 */

/**
 * Game session state
 */
@Serializable
data class GameSession(
    val id: String,
    val gameId: String,
    val playerId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: GameSessionStatus,
    val score: Long = 0,
    val level: Int = 1,
    val achievements: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val saveData: String? = null, // JSON string of game save state
    val duration: Long? = null, // in milliseconds
    val isMultiplayer: Boolean = false,
    val multiplayerPlayers: List<String> = emptyList()
)

/**
 * Game session status
 */
@Serializable
enum class GameSessionStatus {
    STARTING,
    PLAYING,
    PAUSED,
    COMPLETED,
    FAILED,
    CRASHED
}

/**
 * Game save state
 */
@Serializable
data class GameSaveState(
    val sessionId: String,
    val gameId: String,
    val playerId: String,
    val timestamp: Long,
    val gameData: Map<String, Any>, // Game-specific save data
    val progress: Float, // 0.0 to 1.0
    val level: Int,
    val score: Long,
    val inventory: Map<String, Int> = emptyMap(),
    val unlockedContent: List<String> = emptyList()
)

/**
 * Performance metrics for game session
 */
@Serializable
data class GamePerformanceMetrics(
    val sessionId: String,
    val gameId: String,
    val averageFps: Float,
    val minFps: Float,
    val maxFps: Float,
    val memoryUsagePeak: Long, // in bytes
    val memoryUsageAverage: Long,
    val batteryDrain: Float, // percentage
    val networkLatency: Long? = null, // in milliseconds
    val frameDrops: Int,
    val loadTime: Long, // in milliseconds
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Achievement progress during gameplay
 */
@Serializable
data class AchievementProgress(
    val achievementId: String,
    val currentProgress: Float, // 0.0 to 1.0
    val targetValue: Float,
    val currentValue: Float,
    val isCompleted: Boolean = false,
    val unlockedAt: Long? = null
)

/**
 * Crash recovery data
 */
@Serializable
data class CrashRecoveryData(
    val sessionId: String,
    val gameId: String,
    val crashTime: Long,
    val crashReason: String,
    val gameStateBeforeCrash: String? = null, // Last known good state
    val canRecover: Boolean,
    val recoveryActions: List<String> = emptyList()
)

/**
 * Game player preferences
 */
@Serializable
data class GamePlayerPreferences(
    val autoSaveEnabled: Boolean = true,
    val autoSaveIntervalSeconds: Long = 30,
    val performanceMonitoringEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val achievementNotificationsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val difficulty: GameDifficulty = GameDifficulty.NORMAL
)

/**
 * Game difficulty levels
 */
@Serializable
enum class GameDifficulty {
    EASY, NORMAL, HARD, EXPERT
}