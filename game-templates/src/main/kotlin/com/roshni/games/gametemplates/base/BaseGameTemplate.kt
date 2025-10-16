package com.roshni.games.gametemplates.base

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.gameengine.assets.AssetManager
import com.roshni.games.gameengine.core.GameEngine
import com.roshni.games.gameengine.core.GameSystem
import com.roshni.games.gameengine.state.GameState
import com.roshni.games.gameengine.state.GameStateManager
import com.roshni.games.gameengine.systems.AudioSystem
import com.roshni.games.gameengine.systems.InputSystem
import com.roshni.games.gameengine.systems.PhysicsSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base game template that provides common functionality for all game types
 */
abstract class BaseGameTemplate(
    protected val context: Context,
    protected val gameId: String,
    protected val gameView: View
) : GameSystem() {

    // Game engine and systems
    protected lateinit var gameEngine: GameEngine
    protected lateinit var assetManager: AssetManager
    protected lateinit var gameStateManager: GameStateManager
    protected lateinit var audioSystem: AudioSystem
    protected lateinit var inputSystem: InputSystem
    protected lateinit var physicsSystem: PhysicsSystem

    // Game state
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    private val _score = MutableStateFlow(0L)
    val score: StateFlow<Long> = _score.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    // Game configuration
    protected val gameConfig = GameConfig()

    override fun initialize() {
        Timber.d("Initializing base game template for: $gameId")

        // Initialize game engine
        gameEngine = GameEngine(context, gameView, gameConfig.targetFps)
        gameEngine.initialize()

        // Get system references
        assetManager = gameEngine.getAssetManager() ?: AssetManager(context, gameId)
        gameStateManager = GameStateManager(context, gameId)
        audioSystem = gameEngine.getAudioSystem()!!
        inputSystem = gameEngine.getInputSystem()!!
        physicsSystem = gameEngine.getPhysicsSystem()!!

        // Register game-specific systems
        registerGameSystems()

        // Initialize game state
        initializeGameState()

        // Load game assets
        loadGameAssets()

        // Setup input handling
        setupInputHandling()

        // Setup audio
        setupAudio()

        Timber.d("Base game template initialized for: $gameId")
    }

    override fun update(deltaTime: Float) {
        // Update game-specific logic
        updateGame(deltaTime)

        // Update score and level if needed
        updateGameProgress()
    }

    override fun pause() {
        Timber.d("Pausing game: $gameId")
        _isGameActive.value = false
        gameEngine.pause()
    }

    override fun resume() {
        Timber.d("Resuming game: $gameId")
        _isGameActive.value = true
        gameEngine.resume()
    }

    override fun cleanup() {
        Timber.d("Cleaning up game: $gameId")

        // Cleanup game engine
        gameEngine.stop()

        // Cleanup asset manager
        assetManager.cleanup()

        // Save final state
        _gameState.value?.let { state ->
            viewModelScope.launch {
                gameStateManager.saveGameState(state)
            }
        }
    }

    /**
     * Start new game
     */
    fun startNewGame() {
        Timber.d("Starting new game: $gameId")

        // Create initial game state
        val initialState = createInitialGameState()
        _gameState.value = initialState

        // Reset game variables
        _score.value = 0
        _level.value = 1
        _lives.value = gameConfig.maxLives

        // Initialize game-specific state
        initializeGame()

        // Start game engine
        gameEngine.start()
        _isGameActive.value = true

        Timber.d("New game started: $gameId")
    }

    /**
     * Pause/Resume game
     */
    fun togglePause() {
        if (_isGameActive.value) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * End current game
     */
    fun endGame() {
        Timber.d("Ending game: $gameId")

        _isGameActive.value = false
        gameEngine.stop()

        // Save final score
        _gameState.value?.let { state ->
            val finalState = state.copy(
                score = _score.value,
                level = _level.value,
                timestamp = System.currentTimeMillis()
            )

            viewModelScope.launch {
                gameStateManager.saveGameState(finalState)
            }
        }
    }

    /**
     * Add score
     */
    fun addScore(points: Long) {
        _score.value += points
        onScoreChanged(_score.value)
    }

    /**
     * Set score
     */
    fun setScore(points: Long) {
        _score.value = points
        onScoreChanged(_score.value)
    }

    /**
     * Add lives
     */
    fun addLives(lifeCount: Int) {
        _lives.value = (_lives.value + lifeCount).coerceAtMost(gameConfig.maxLives)
        onLivesChanged(_lives.value)
    }

    /**
     * Remove lives
     */
    fun removeLives(lifeCount: Int) {
        _lives.value = (_lives.value - lifeCount).coerceAtLeast(0)
        onLivesChanged(_lives.value)

        if (_lives.value <= 0) {
            onGameOver()
        }
    }

    /**
     * Advance to next level
     */
    fun nextLevel() {
        _level.value++
        onLevelChanged(_level.value)
    }

    /**
     * Abstract methods that must be implemented by specific game types
     */
    protected abstract fun registerGameSystems()
    protected abstract fun initializeGameState()
    protected abstract fun loadGameAssets()
    protected abstract fun setupInputHandling()
    protected abstract fun setupAudio()
    protected abstract fun initializeGame()
    protected abstract fun updateGame(deltaTime: Float)
    protected abstract fun updateGameProgress()

    /**
     * Game configuration class
     */
    protected data class GameConfig(
        val targetFps: Int = 60,
        val maxLives: Int = 3,
        val enablePhysics: Boolean = false,
        val enableAudio: Boolean = true,
        val enableAutoSave: Boolean = true,
        val autoSaveInterval: Long = 30000L // 30 seconds
    )

    /**
     * Event callbacks
     */
    protected open fun onScoreChanged(newScore: Long) {}
    protected open fun onLivesChanged(newLives: Int) {}
    protected open fun onLevelChanged(newLevel: Int) {}
    protected open fun onGameOver() {}

    /**
     * Create initial game state - override in subclasses for game-specific state
     */
    protected open fun createInitialGameState(): GameState {
        return GameState(
            gameId = gameId,
            playerId = "player_${System.currentTimeMillis()}",
            level = 1,
            score = 0,
            lives = gameConfig.maxLives,
            health = 100f,
            position = com.roshni.games.gameengine.state.Position(0f, 0f),
            inventory = emptyMap(),
            achievements = emptySet(),
            statistics = emptyMap(),
            customData = emptyMap()
        )
    }

    /**
     * Get game view for Compose integration
     */
    @Composable
    abstract fun GameContent()

    /**
     * Get game view model for Compose integration
     */
    abstract fun getGameViewModel(): GameViewModel
}

/**
 * Base game view model for Compose integration
 */
abstract class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected fun updateUiState(newState: GameUiState) {
        _uiState.value = newState
    }

    protected fun setError(errorMessage: String?) {
        _error.value = errorMessage
    }

    fun clearError() {
        _error.value = null
    }
}

/**
 * Game UI states
 */
sealed class GameUiState {
    data object Loading : GameUiState()
    data object Playing : GameUiState()
    data object Paused : GameUiState()
    data object GameOver : GameUiState()
    data class Error(val message: String) : GameUiState()
}