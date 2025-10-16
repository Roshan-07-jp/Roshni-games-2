package com.roshni.games.opensource.adapter

import android.content.Context
import android.view.View
import com.roshni.games.gameengine.core.GameEngine
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified interface for all game types in the open source framework
 * Provides consistent API regardless of game implementation
 */
abstract class GameAdapter {

    /**
     * Unique identifier for this game
     */
    abstract val gameId: String

    /**
     * Human-readable game name
     */
    abstract val gameName: String

    /**
     * Game description
     */
    abstract val description: String

    /**
     * Game category
     */
    abstract val category: GameCategory

    /**
     * Initialize the game adapter
     */
    abstract suspend fun initialize(context: Context): Result<Unit>

    /**
     * Create game view for display
     */
    abstract fun createGameView(context: Context): View

    /**
     * Get game engine instance for this game
     */
    abstract fun getGameEngine(gameView: View): GameEngine

    /**
     * Handle game pause
     */
    abstract fun onPause()

    /**
     * Handle game resume
     */
    abstract fun onResume()

    /**
     * Handle game destroy
     */
    abstract fun onDestroy()

    /**
     * Get current game state
     */
    abstract fun getGameState(): StateFlow<GameState>

    /**
     * Save game state
     */
    abstract suspend fun saveGameState(): Result<Unit>

    /**
     * Load game state
     */
    abstract suspend fun loadGameState(): Result<Unit>

    /**
     * Get game controls configuration
     */
    abstract fun getControlsConfig(): ControlsConfig

    /**
     * Handle touch input
     */
    abstract fun handleTouchInput(x: Float, y: Float, action: TouchAction)

    /**
     * Get game assets requirements
     */
    abstract fun getAssetRequirements(): AssetRequirements

    /**
     * Check if game supports feature
     */
    abstract fun supportsFeature(feature: GameFeature): Boolean

    /**
     * Get game performance metrics
     */
    abstract fun getPerformanceMetrics(): PerformanceMetrics
}

/**
 * Game state representation
 */
data class GameState(
    val isRunning: Boolean,
    val isPaused: Boolean,
    val score: Int = 0,
    val level: Int = 1,
    val lives: Int = 0,
    val gameData: Map<String, Any> = emptyMap()
)

/**
 * Touch input actions
 */
enum class TouchAction {
    DOWN, UP, MOVE, CANCEL
}

/**
 * Game controls configuration
 */
data class ControlsConfig(
    val touchEnabled: Boolean = true,
    val swipeEnabled: Boolean = false,
    val multiTouchEnabled: Boolean = false,
    val gestureEnabled: Boolean = false,
    val virtualButtons: List<VirtualButton> = emptyList()
)

/**
 * Virtual button configuration
 */
data class VirtualButton(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String,
    val action: String
)

/**
 * Asset requirements for the game
 */
data class AssetRequirements(
    val images: List<String> = emptyList(),
    val audio: List<String> = emptyList(),
    val fonts: List<String> = emptyList(),
    val dataFiles: List<String> = emptyList(),
    val totalSizeBytes: Long = 0L
)

/**
 * Game features enumeration
 */
enum class GameFeature {
    SAVE_LOAD, HIGH_SCORE, ACHIEVEMENTS, MULTIPLAYER,
    LEADERBOARD, SETTINGS, TUTORIAL, SOUND, MUSIC
}

/**
 * Performance metrics for the game
 */
data class PerformanceMetrics(
    val averageFps: Float = 0f,
    val memoryUsage: Long = 0L,
    val loadTime: Long = 0L,
    val frameTime: Float = 0f
)

/**
 * Game category enumeration
 */
enum class GameCategory {
    PUZZLE, CARD, ARCADE, STRATEGY, TRIVIA,
    ACTION, BOARD, CASUAL, WORD, MATH,
    MEMORY, LOGIC, ADVENTURE, SIMULATION
}