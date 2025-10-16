package com.roshni.games.feature.gameplayer.domain

import com.roshni.games.feature.gameplayer.data.model.AchievementProgress
import com.roshni.games.feature.gameplayer.data.model.CrashRecoveryData
import com.roshni.games.feature.gameplayer.data.model.GameDifficulty
import com.roshni.games.feature.gameplayer.data.model.GamePerformanceMetrics
import com.roshni.games.feature.gameplayer.data.model.GamePlayerPreferences
import com.roshni.games.feature.gameplayer.data.model.GameSaveState
import com.roshni.games.feature.gameplayer.data.model.GameSession
import com.roshni.games.feature.gameplayer.data.model.GameSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

/**
 * Manages game sessions, performance monitoring, and crash recovery
 */
class GameSessionManager {

    private val _currentSession = MutableStateFlow<GameSession?>(null)
    private val _performanceMetrics = MutableStateFlow<GamePerformanceMetrics?>(null)
    private val _achievementProgress = MutableStateFlow<List<AchievementProgress>>(emptyList())
    private val _crashRecoveryData = MutableStateFlow<CrashRecoveryData?>(null)
    private val _playerPreferences = MutableStateFlow(
        GamePlayerPreferences(
            autoSaveEnabled = true,
            performanceMonitoringEnabled = true,
            crashReportingEnabled = true,
            achievementNotificationsEnabled = true
        )
    )

    // Public flows
    val currentSession: StateFlow<GameSession?> = _currentSession.asStateFlow()
    val performanceMetrics: StateFlow<GamePerformanceMetrics?> = _performanceMetrics.asStateFlow()
    val achievementProgress: StateFlow<List<AchievementProgress>> = _achievementProgress.asStateFlow()
    val crashRecoveryData: StateFlow<CrashRecoveryData?> = _crashRecoveryData.asStateFlow()
    val playerPreferences: StateFlow<GamePlayerPreferences> = _playerPreferences.asStateFlow()

    private var sessionStartTime: Long = 0
    private var lastSaveTime: Long = 0
    private val frameTimes = mutableListOf<Long>()
    private val memoryReadings = mutableListOf<Long>()

    /**
     * Start a new game session
     */
    suspend fun startGameSession(
        gameId: String,
        playerId: String,
        difficulty: GameDifficulty = GameDifficulty.NORMAL,
        isMultiplayer: Boolean = false,
        multiplayerPlayers: List<String> = emptyList()
    ): Result<String> {
        return try {
            // Check if there's already an active session
            if (_currentSession.value != null) {
                return Result.failure(IllegalStateException("A game session is already active"))
            }

            // Check for crash recovery data
            val recoveryData = _crashRecoveryData.value
            if (recoveryData != null && recoveryData.canRecover) {
                Timber.d("Found crash recovery data for game: $gameId")
                // Could offer recovery here
            }

            val session = GameSession(
                id = UUID.randomUUID().toString(),
                gameId = gameId,
                playerId = playerId,
                startTime = System.currentTimeMillis(),
                status = GameSessionStatus.STARTING,
                isMultiplayer = isMultiplayer,
                multiplayerPlayers = multiplayerPlayers
            )

            _currentSession.value = session
            sessionStartTime = System.currentTimeMillis()
            lastSaveTime = sessionStartTime

            // Initialize performance monitoring
            if (_playerPreferences.value.performanceMonitoringEnabled) {
                startPerformanceMonitoring()
            }

            Timber.d("Started game session: ${session.id} for game: $gameId")
            Result.success(session.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to start game session")
            Result.failure(e)
        }
    }

    /**
     * Update game session status
     */
    fun updateSessionStatus(status: GameSessionStatus): Result<Unit> {
        return try {
            val current = _currentSession.value ?: return Result.failure(IllegalStateException("No active session"))

            val updatedSession = current.copy(
                status = status,
                endTime = if (status == GameSessionStatus.COMPLETED || status == GameSessionStatus.FAILED) {
                    System.currentTimeMillis()
                } else null,
                duration = if (current.endTime != null) {
                    current.endTime!! - current.startTime
                } else {
                    System.currentTimeMillis() - current.startTime
                }
            )

            _currentSession.value = updatedSession

            // End performance monitoring if session is ending
            if (status == GameSessionStatus.COMPLETED || status == GameSessionStatus.FAILED || status == GameSessionStatus.CRASHED) {
                endPerformanceMonitoring()
            }

            Timber.d("Updated session status: ${current.id} -> $status")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update session status")
            Result.failure(e)
        }
    }

    /**
     * Update game score and level
     */
    fun updateGameProgress(score: Long, level: Int): Result<Unit> {
        return try {
            val current = _currentSession.value ?: return Result.failure(IllegalStateException("No active session"))

            val updatedSession = current.copy(
                score = score,
                level = level
            )

            _currentSession.value = updatedSession

            // Check for achievements
            checkAchievements(score, level)

            // Auto-save if enabled
            if (_playerPreferences.value.autoSaveEnabled) {
                autoSave()
            }

            Timber.d("Updated game progress: score=$score, level=$level")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update game progress")
            Result.failure(e)
        }
    }

    /**
     * Save game state
     */
    suspend fun saveGameState(gameData: Map<String, Any>): Result<Unit> {
        return try {
            val current = _currentSession.value ?: return Result.failure(IllegalStateException("No active session"))

            val saveState = GameSaveState(
                sessionId = current.id,
                gameId = current.gameId,
                playerId = current.playerId,
                timestamp = System.currentTimeMillis(),
                gameData = gameData,
                progress = calculateProgress(),
                level = current.level,
                score = current.score
            )

            // In real implementation, this would save to database
            val currentSession = _currentSession.value
            if (currentSession != null) {
                val updatedSession = currentSession.copy(saveData = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    kotlinx.serialization.json.Json.encodeToJsonElement(saveState)
                ))
                _currentSession.value = updatedSession
            }

            lastSaveTime = System.currentTimeMillis()
            Timber.d("Saved game state for session: ${current.id}")

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save game state")
            Result.failure(e)
        }
    }

    /**
     * Load game state
     */
    fun loadGameState(sessionId: String): Result<GameSaveState?> {
        return try {
            val current = _currentSession.value
            if (current == null || current.id != sessionId) {
                return Result.failure(IllegalStateException("Session not found or not active"))
            }

            // In real implementation, this would load from database
            val saveData = current.saveData
            if (saveData == null) {
                return Result.success(null)
            }

            // Parse save data (simplified for this example)
            val saveState = GameSaveState(
                sessionId = sessionId,
                gameId = current.gameId,
                playerId = current.playerId,
                timestamp = System.currentTimeMillis(),
                gameData = emptyMap(),
                progress = 0.5f,
                level = current.level,
                score = current.score
            )

            Timber.d("Loaded game state for session: $sessionId")
            Result.success(saveState)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load game state")
            Result.failure(e)
        }
    }

    /**
     * End current game session
     */
    suspend fun endGameSession(): Result<Unit> {
        return try {
            val current = _currentSession.value ?: return Result.failure(IllegalStateException("No active session"))

            // Final save
            if (_playerPreferences.value.autoSaveEnabled) {
                saveGameState(emptyMap())
            }

            // End performance monitoring
            endPerformanceMonitoring()

            // Update session status
            updateSessionStatus(GameSessionStatus.COMPLETED)

            // Clear current session after a delay to allow for final processing
            kotlinx.coroutines.delay(1000)
            _currentSession.value = null

            Timber.d("Ended game session: ${current.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to end game session")
            Result.failure(e)
        }
    }

    /**
     * Handle game crash
     */
    fun handleGameCrash(crashReason: String, gameStateBeforeCrash: String? = null): Result<Unit> {
        return try {
            val current = _currentSession.value
            if (current == null) {
                Timber.w("No active session to handle crash for")
                return Result.success(Unit)
            }

            // Create crash recovery data
            val recoveryData = CrashRecoveryData(
                sessionId = current.id,
                gameId = current.gameId,
                crashTime = System.currentTimeMillis(),
                crashReason = crashReason,
                gameStateBeforeCrash = gameStateBeforeCrash,
                canRecover = gameStateBeforeCrash != null,
                recoveryActions = listOf("Restore from last save", "Start new game")
            )

            _crashRecoveryData.value = recoveryData
            _currentSession.value = current.copy(status = GameSessionStatus.CRASHED)

            // End performance monitoring
            endPerformanceMonitoring()

            Timber.d("Handled game crash for session: ${current.id}, reason: $crashReason")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to handle game crash")
            Result.failure(e)
        }
    }

    /**
     * Attempt crash recovery
     */
    suspend fun attemptCrashRecovery(): Result<Unit> {
        return try {
            val recoveryData = _crashRecoveryData.value
            if (recoveryData == null || !recoveryData.canRecover) {
                return Result.failure(IllegalStateException("No recoverable crash data available"))
            }

            // In real implementation, this would restore the game state
            Timber.d("Attempting crash recovery for session: ${recoveryData.sessionId}")

            // Clear crash recovery data
            _crashRecoveryData.value = null

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to attempt crash recovery")
            Result.failure(e)
        }
    }

    /**
     * Update player preferences
     */
    fun updatePlayerPreferences(preferences: GamePlayerPreferences): Result<Unit> {
        return try {
            _playerPreferences.value = preferences

            // Restart performance monitoring if preferences changed
            if (preferences.performanceMonitoringEnabled && _currentSession.value != null) {
                startPerformanceMonitoring()
            } else if (!preferences.performanceMonitoringEnabled) {
                endPerformanceMonitoring()
            }

            Timber.d("Updated player preferences: $preferences")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update player preferences")
            Result.failure(e)
        }
    }

    /**
     * Record frame time for performance monitoring
     */
    fun recordFrameTime(frameTimeNanos: Long) {
        if (!_playerPreferences.value.performanceMonitoringEnabled) return

        frameTimes.add(frameTimeNanos)

        // Keep only last 60 frames for calculation
        if (frameTimes.size > 60) {
            frameTimes.removeAt(0)
        }

        // Update FPS every 10 frames
        if (frameTimes.size % 10 == 0) {
            updatePerformanceMetrics()
        }
    }

    /**
     * Record memory usage for performance monitoring
     */
    fun recordMemoryUsage(memoryBytes: Long) {
        if (!_playerPreferences.value.performanceMonitoringEnabled) return

        memoryReadings.add(memoryBytes)

        // Keep only last 10 readings
        if (memoryReadings.size > 10) {
            memoryReadings.removeAt(0)
        }
    }

    /**
     * Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        frameTimes.clear()
        memoryReadings.clear()
        Timber.d("Started performance monitoring")
    }

    /**
     * End performance monitoring and save metrics
     */
    private fun endPerformanceMonitoring() {
        updatePerformanceMetrics()

        val current = _currentSession.value
        if (current != null && _performanceMetrics.value != null) {
            // In real implementation, this would save to database
            Timber.d("Saved performance metrics for session: ${current.id}")
        }

        frameTimes.clear()
        memoryReadings.clear()
        Timber.d("Ended performance monitoring")
    }

    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        if (frameTimes.isEmpty()) return

        val current = _currentSession.value ?: return

        val frameTimeAvgNanos = frameTimes.average()
        val fps = 1_000_000_000.0 / frameTimeAvgNanos // Convert nanoseconds to FPS

        val memoryAvg = if (memoryReadings.isNotEmpty()) {
            memoryReadings.average().toLong()
        } else 0

        val memoryPeak = if (memoryReadings.isNotEmpty()) {
            memoryReadings.maxOrNull() ?: 0
        } else 0

        val metrics = GamePerformanceMetrics(
            sessionId = current.id,
            gameId = current.gameId,
            averageFps = fps.toFloat(),
            minFps = frameTimes.minOf { 1_000_000_000.0 / it }.toFloat(),
            maxFps = frameTimes.maxOf { 1_000_000_000.0 / it }.toFloat(),
            memoryUsagePeak = memoryPeak,
            memoryUsageAverage = memoryAvg,
            batteryDrain = 0f, // Would need battery monitoring
            frameDrops = frameTimes.count { it > 33_333_333 }, // Frames > 33ms (30 FPS)
            loadTime = System.currentTimeMillis() - sessionStartTime
        )

        _performanceMetrics.value = metrics
    }

    /**
     * Check for achievements based on score and level
     */
    private fun checkAchievements(score: Long, level: Int) {
        // In real implementation, this would check against achievement definitions
        val progress = listOf(
            AchievementProgress(
                achievementId = "first_level",
                currentProgress = if (level >= 1) 1f else 0f,
                targetValue = 1f,
                currentValue = level.toFloat(),
                isCompleted = level >= 1
            ),
            AchievementProgress(
                achievementId = "score_master",
                currentProgress = score.toFloat() / 10000f,
                targetValue = 10000f,
                currentValue = score.toFloat(),
                isCompleted = score >= 10000
            )
        )

        _achievementProgress.value = progress.filter { it.currentProgress > 0 }
    }

    /**
     * Calculate game progress (0.0 to 1.0)
     */
    private fun calculateProgress(): Float {
        val current = _currentSession.value ?: return 0f
        // Simple progress calculation based on level
        return minOf(current.level.toFloat() / 10f, 1f)
    }

    /**
     * Auto-save if interval has passed
     */
    private fun autoSave() {
        val currentTime = System.currentTimeMillis()
        val interval = _playerPreferences.value.autoSaveIntervalSeconds * 1000

        if (currentTime - lastSaveTime > interval) {
            // Trigger save in coroutine scope
            kotlinx.coroutines.GlobalScope.launch {
                saveGameState(emptyMap())
            }
        }
    }
}