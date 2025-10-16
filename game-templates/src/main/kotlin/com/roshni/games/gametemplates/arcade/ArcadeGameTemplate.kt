package com.roshni.games.gametemplates.arcade

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.roshni.games.gametemplates.base.BaseGameTemplate
import com.roshni.games.gametemplates.base.GameUiState
import com.roshni.games.gametemplates.base.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Template for arcade games (Endless Runners, Classic Arcade, Bullet Hell, etc.)
 */
abstract class ArcadeGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Arcade-specific state
    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance.asStateFlow()

    private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
    val obstacles: StateFlow<List<Obstacle>> = _obstacles.asStateFlow()

    private val _collectibles = MutableStateFlow<List<Collectible>>(emptyList())
    val collectibles: StateFlow<List<Collectible>> = _collectibles.asStateFlow()

    private val _gameSpeed = MutableStateFlow(1.0f)
    val gameSpeed: StateFlow<Float> = _gameSpeed.asStateFlow()

    // Arcade configuration
    protected val arcadeConfig = ArcadeConfig()

    override fun registerGameSystems() {
        // Arcade games often use physics for movement
        if (arcadeConfig.enablePhysics) {
            gameEngine.registerSystem(physicsSystem)
        }
    }

    override fun initializeGameState() {
        // Initialize arcade-specific state
        _distance.value = 0f
        _obstacles.value = emptyList()
        _collectibles.value = emptyList()
        _gameSpeed.value = arcadeConfig.initialSpeed
    }

    override fun loadGameAssets() {
        // Load common arcade game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "arcade/collect_sound.mp3",
                    "arcade/obstacle_hit.mp3",
                    "arcade/speed_boost.mp3",
                    "arcade/background_music.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load arcade assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup arcade-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onArcadeTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onArcadeDrag(event.x, event.y)
                    }
                    else -> {}
                }
            }

            inputSystem.gestureEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.GestureEvent.Fling -> {
                        onArcadeFling(event.velocityX, event.velocityY)
                    }
                    is com.roshni.games.gameengine.systems.GestureEvent.DoubleTap -> {
                        onArcadeDoubleTap(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup arcade-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.6f) // Higher volume for arcade games
            audioSystem.setSfxVolume(0.8f)
            audioSystem.playMusic("background_music", fadeIn = true)
        }
    }

    override fun initializeGame() {
        // Initialize arcade-specific game state
        startScrolling()
        spawnInitialObstacles()
        spawnInitialCollectibles()
        startArcadeGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update arcade-specific game logic
        updateDistance(deltaTime)
        updateObstacles(deltaTime)
        updateCollectibles(deltaTime)
        updateGameSpeed(deltaTime)
        updateArcadeGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update arcade game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                position = com.roshni.games.gameengine.state.Position(_distance.value, 0f),
                customData = state.customData + mapOf(
                    "distance" to _distance.value.toString(),
                    "obstacles" to _obstacles.value.size.toString(),
                    "collectibles" to _collectibles.value.size.toString(),
                    "gameSpeed" to _gameSpeed.value.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Arcade-specific score handling
        if (newScore > 0) {
            playSoundEffect("collect_sound")

            // Increase game speed based on score
            if (newScore % arcadeConfig.speedIncreaseInterval == 0L) {
                increaseGameSpeed()
            }
        }
    }

    override fun onGameOver() {
        // Arcade-specific game over handling
        playSoundEffect("obstacle_hit")
        stopScrolling()
        clearObstacles()
        clearCollectibles()
    }

    /**
     * Update distance traveled
     */
    protected fun updateDistance(deltaTime: Float) {
        _distance.value += _gameSpeed.value * arcadeConfig.scrollSpeed * deltaTime
    }

    /**
     * Increase game speed
     */
    protected fun increaseGameSpeed() {
        _gameSpeed.value += arcadeConfig.speedIncrement
        playSoundEffect("speed_boost")
    }

    /**
     * Add obstacle
     */
    protected fun addObstacle(obstacle: Obstacle) {
        _obstacles.value = _obstacles.value + obstacle
    }

    /**
     * Remove obstacle
     */
    protected fun removeObstacle(obstacle: Obstacle) {
        _obstacles.value = _obstacles.value - obstacle
    }

    /**
     * Clear all obstacles
     */
    protected fun clearObstacles() {
        _obstacles.value = emptyList()
    }

    /**
     * Add collectible
     */
    protected fun addCollectible(collectible: Collectible) {
        _collectibles.value = _collectibles.value + collectible
    }

    /**
     * Remove collectible
     */
    protected fun removeCollectible(collectible: Collectible) {
        _collectibles.value = _collectibles.value - collectible
    }

    /**
     * Clear all collectibles
     */
    protected fun clearCollectibles() {
        _collectibles.value = emptyList()
    }

    /**
     * Abstract methods for arcade-specific implementation
     */
    protected abstract fun onArcadeTouch(x: Float, y: Float)
    protected abstract fun onArcadeDrag(x: Float, y: Float)
    protected abstract fun onArcadeFling(velocityX: Float, velocityY: Float)
    protected abstract fun onArcadeDoubleTap(x: Float, y: Float)
    protected abstract fun updateObstacles(deltaTime: Float)
    protected abstract fun updateCollectibles(deltaTime: Float)
    protected abstract fun updateGameSpeed(deltaTime: Float)
    protected abstract fun updateArcadeGame(deltaTime: Float)
    protected abstract fun startScrolling()
    protected abstract fun stopScrolling()
    protected abstract fun spawnInitialObstacles()
    protected abstract fun spawnInitialCollectibles()
    protected abstract fun startArcadeGame()

    /**
     * Arcade configuration
     */
    protected data class ArcadeConfig(
        val enablePhysics: Boolean = true,
        val initialSpeed: Float = 1.0f,
        val maxSpeed: Float = 5.0f,
        val speedIncrement: Float = 0.1f,
        val speedIncreaseInterval: Long = 1000L, // Points interval for speed increase
        val scrollSpeed: Float = 100f, // Base scrolling speed
        val obstacleSpawnRate: Float = 0.3f, // Probability per second
        val collectibleSpawnRate: Float = 0.5f // Probability per second
    )

    /**
     * Obstacle data class
     */
    data class Obstacle(
        val id: String,
        val type: ObstacleType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val damage: Float = 1f,
        val points: Int = 0
    )

    /**
     * Obstacle types
     */
    enum class ObstacleType {
        STATIC, MOVING, BREAKABLE, SPIKES, PIT
    }

    /**
     * Collectible data class
     */
    data class Collectible(
        val id: String,
        val type: CollectibleType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val points: Int = 100,
        val effect: CollectibleEffect? = null
    )

    /**
     * Collectible types
     */
    enum class CollectibleType {
        COIN, GEM, POWER_UP, BOOST, MULTIPLIER
    }

    /**
     * Collectible effects
     */
    data class CollectibleEffect(
        val type: EffectType,
        val value: Float,
        val duration: Float? = null // null for instant effects
    )

    /**
     * Effect types
     */
    enum class EffectType {
        SPEED_BOOST, INVINCIBILITY, MAGNETISM, MULTIPLIER
    }

    /**
     * Arcade game view model
     */
    abstract class ArcadeGameViewModel : GameViewModel() {

        private val _distance = MutableStateFlow(0f)
        val distance: StateFlow<Float> = _distance.asStateFlow()

        private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
        val obstacles: StateFlow<List<Obstacle>> = _obstacles.asStateFlow()

        private val _collectibles = MutableStateFlow<List<Collectible>>(emptyList())
        val collectibles: StateFlow<List<Collectible>> = _collectibles.asStateFlow()

        private val _gameSpeed = MutableStateFlow(1.0f)
        val gameSpeed: StateFlow<Float> = _gameSpeed.asStateFlow()

        fun updateDistance(deltaDistance: Float) {
            _distance.value += deltaDistance
        }

        fun increaseGameSpeed() {
            _gameSpeed.value = (_gameSpeed.value + 0.1f).coerceAtMost(5.0f)
        }

        fun addObstacle(obstacle: Obstacle) {
            _obstacles.value = _obstacles.value + obstacle
        }

        fun removeObstacle(obstacle: Obstacle) {
            _obstacles.value = _obstacles.value - obstacle
        }

        fun addCollectible(collectible: Collectible) {
            _collectibles.value = _collectibles.value + collectible
        }

        fun removeCollectible(collectible: Collectible) {
            _collectibles.value = _collectibles.value - collectible
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as ArcadeGameViewModel
        val currentDistance by gameViewModel.distance.collectAsState()
        val currentObstacles by gameViewModel.obstacles.collectAsState()
        val currentCollectibles by gameViewModel.collectibles.collectAsState()
        val currentSpeed by gameViewModel.gameSpeed.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            // Main arcade game content
            ArcadeGameContent(
                distance = currentDistance,
                obstacles = currentObstacles,
                collectibles = currentCollectibles,
                gameSpeed = currentSpeed
            )

            // Game UI overlay
            ArcadeGameOverlay(
                score = _score.collectAsState().value,
                distance = currentDistance,
                gameSpeed = currentSpeed,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    @Composable
    protected abstract fun ArcadeGameContent(
        distance: Float,
        obstacles: List<Obstacle>,
        collectibles: List<Collectible>,
        gameSpeed: Float
    )

    @Composable
    protected abstract fun ArcadeGameOverlay(
        score: Long,
        distance: Float,
        gameSpeed: Float,
        modifier: Modifier
    )
}