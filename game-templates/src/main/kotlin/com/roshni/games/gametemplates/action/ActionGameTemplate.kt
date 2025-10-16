package com.roshni.games.gametemplates.action

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
 * Template for action games (Platformers, Shooters, Racing, Sports, etc.)
 */
abstract class ActionGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Action-specific state
    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo.asStateFlow()

    private val _multiplier = MutableStateFlow(1.0f)
    val multiplier: StateFlow<Float> = _multiplier.asStateFlow()

    private val _powerUps = MutableStateFlow<List<PowerUp>>(emptyList())
    val powerUps: StateFlow<List<PowerUp>> = _powerUps.asStateFlow()

    private val _enemies = MutableStateFlow<List<GameObject>>(emptyList())
    val enemies: StateFlow<List<GameObject>> = _enemies.asStateFlow()

    // Action configuration
    protected val actionConfig = ActionConfig()

    override fun registerGameSystems() {
        // Register physics system for action games
        if (gameConfig.enablePhysics) {
            gameEngine.registerSystem(physicsSystem)
        }
    }

    override fun initializeGameState() {
        // Initialize action-specific state
        _combo.value = 0
        _multiplier.value = 1.0f
        _powerUps.value = emptyList()
        _enemies.value = emptyList()
    }

    override fun loadGameAssets() {
        // Load common action game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "action/jump_sound.mp3",
                    "action/shoot_sound.mp3",
                    "action/hit_sound.mp3",
                    "action/powerup_sound.mp3",
                    "action/explosion.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load action assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup action-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onActionTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onActionDrag(event.x, event.y)
                    }
                    else -> {}
                }
            }

            inputSystem.gestureEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.GestureEvent.Fling -> {
                        onActionFling(event.velocityX, event.velocityY)
                    }
                    is com.roshni.games.gameengine.systems.GestureEvent.DoubleTap -> {
                        onActionDoubleTap(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup action-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.5f) // Medium volume for action games
            audioSystem.setSfxVolume(0.9f) // High volume for sound effects
        }
    }

    override fun initializeGame() {
        // Initialize action-specific game state
        spawnEnemies()
        spawnPowerUps()
        startActionGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update action-specific game logic
        updateEnemies(deltaTime)
        updatePowerUps(deltaTime)
        updateCombo(deltaTime)
        updateActionGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update action game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                health = calculateHealth(),
                position = getPlayerPosition(),
                customData = state.customData + mapOf(
                    "combo" to _combo.value.toString(),
                    "multiplier" to _multiplier.value.toString(),
                    "powerUps" to _powerUps.value.size.toString(),
                    "enemies" to _enemies.value.size.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Action-specific score handling with combo multiplier
        val comboBonus = (newScore * _combo.value * _multiplier.value).toLong()
        if (comboBonus > 0) {
            addScore(comboBonus)
        }
    }

    override fun onGameOver() {
        // Action-specific game over handling
        playSoundEffect("game_over_sound")
        clearEnemies()
        clearPowerUps()
    }

    /**
     * Increase combo
     */
    protected fun increaseCombo() {
        _combo.value++
        updateMultiplier()

        if (_combo.value > 1) {
            playSoundEffect("combo_sound")
        }
    }

    /**
     * Reset combo
     */
    protected fun resetCombo() {
        _combo.value = 0
        _multiplier.value = 1.0f
    }

    /**
     * Update combo multiplier based on combo count
     */
    private fun updateMultiplier() {
        _multiplier.value = when {
            _combo.value >= 50 -> 5.0f
            _combo.value >= 25 -> 3.0f
            _combo.value >= 10 -> 2.0f
            _combo.value >= 5 -> 1.5f
            else -> 1.0f
        }
    }

    /**
     * Add power-up
     */
    protected fun addPowerUp(powerUp: PowerUp) {
        _powerUps.value = _powerUps.value + powerUp
        playSoundEffect("powerup_sound")
    }

    /**
     * Remove power-up
     */
    protected fun removePowerUp(powerUp: PowerUp) {
        _powerUps.value = _powerUps.value - powerUp
    }

    /**
     * Add enemy
     */
    protected fun addEnemy(enemy: GameObject) {
        _enemies.value = _enemies.value + enemy
    }

    /**
     * Remove enemy
     */
    protected fun removeEnemy(enemy: GameObject) {
        _enemies.value = _enemies.value - enemy
    }

    /**
     * Clear all enemies
     */
    protected fun clearEnemies() {
        _enemies.value = emptyList()
    }

    /**
     * Clear all power-ups
     */
    protected fun clearPowerUps() {
        _powerUps.value = emptyList()
    }

    /**
     * Abstract methods for action-specific implementation
     */
    protected abstract fun onActionTouch(x: Float, y: Float)
    protected abstract fun onActionDrag(x: Float, y: Float)
    protected abstract fun onActionFling(velocityX: Float, velocityY: Float)
    protected abstract fun onActionDoubleTap(x: Float, y: Float)
    protected abstract fun updateEnemies(deltaTime: Float)
    protected abstract fun updatePowerUps(deltaTime: Float)
    protected abstract fun updateCombo(deltaTime: Float)
    protected abstract fun updateActionGame(deltaTime: Float)
    protected abstract fun spawnEnemies()
    protected abstract fun spawnPowerUps()
    protected abstract fun startActionGame()
    protected abstract fun calculateHealth(): Float
    protected abstract fun getPlayerPosition(): com.roshni.games.gameengine.state.Position

    /**
     * Action configuration
     */
    protected data class ActionConfig(
        val enableComboSystem: Boolean = true,
        val enablePowerUps: Boolean = true,
        val enableEnemies: Boolean = true,
        val maxCombo: Int = 100,
        val comboTimeout: Float = 3.0f, // seconds
        val powerUpSpawnRate: Float = 0.1f, // probability per second
        val enemySpawnRate: Float = 0.5f // probability per second
    )

    /**
     * Power-up data class
     */
    data class PowerUp(
        val id: String,
        val type: PowerUpType,
        val x: Float,
        val y: Float,
        val duration: Float? = null, // null for permanent
        val effect: Map<String, Float> = emptyMap()
    )

    /**
     * Power-up types
     */
    enum class PowerUpType {
        HEALTH_BOOST, SPEED_BOOST, SHIELD, MULTIPLIER, WEAPON_UPGRADE
    }

    /**
     * Game object for enemies and obstacles
     */
    data class GameObject(
        val id: String,
        val type: ObjectType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val velocityX: Float = 0f,
        val velocityY: Float = 0f,
        val health: Float = 100f,
        val damage: Float = 10f,
        val points: Int = 100
    )

    /**
     * Object types
     */
    enum class ObjectType {
        ENEMY, OBSTACLE, COLLECTIBLE, POWER_UP
    }

    /**
     * Action game view model
     */
    abstract class ActionGameViewModel : GameViewModel() {

        private val _combo = MutableStateFlow(0)
        val combo: StateFlow<Int> = _combo.asStateFlow()

        private val _multiplier = MutableStateFlow(1.0f)
        val multiplier: StateFlow<Float> = _multiplier.asStateFlow()

        private val _powerUps = MutableStateFlow<List<PowerUp>>(emptyList())
        val powerUps: StateFlow<List<PowerUp>> = _powerUps.asStateFlow()

        private val _enemies = MutableStateFlow<List<GameObject>>(emptyList())
        val enemies: StateFlow<List<GameObject>> = _enemies.asStateFlow()

        fun increaseCombo() {
            _combo.value++
            updateMultiplier()
        }

        fun resetCombo() {
            _combo.value = 0
            _multiplier.value = 1.0f
        }

        private fun updateMultiplier() {
            _multiplier.value = when {
                _combo.value >= 50 -> 5.0f
                _combo.value >= 25 -> 3.0f
                _combo.value >= 10 -> 2.0f
                _combo.value >= 5 -> 1.5f
                else -> 1.0f
            }
        }

        fun addPowerUp(powerUp: PowerUp) {
            _powerUps.value = _powerUps.value + powerUp
        }

        fun removePowerUp(powerUp: PowerUp) {
            _powerUps.value = _powerUps.value - powerUp
        }

        fun addEnemy(enemy: GameObject) {
            _enemies.value = _enemies.value + enemy
        }

        fun removeEnemy(enemy: GameObject) {
            _enemies.value = _enemies.value - enemy
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as ActionGameViewModel
        val currentCombo by gameViewModel.combo.collectAsState()
        val currentMultiplier by gameViewModel.multiplier.collectAsState()
        val currentPowerUps by gameViewModel.powerUps.collectAsState()
        val currentEnemies by gameViewModel.enemies.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            // Main action game content
            ActionGameContent(
                combo = currentCombo,
                multiplier = currentMultiplier,
                powerUps = currentPowerUps,
                enemies = currentEnemies
            )

            // Game UI overlay
            ActionGameOverlay(
                score = _score.collectAsState().value,
                lives = _lives.collectAsState().value,
                level = _level.collectAsState().value,
                combo = currentCombo,
                multiplier = currentMultiplier,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    @Composable
    protected abstract fun ActionGameContent(
        combo: Int,
        multiplier: Float,
        powerUps: List<PowerUp>,
        enemies: List<GameObject>
    )

    @Composable
    protected abstract fun ActionGameOverlay(
        score: Long,
        lives: Int,
        level: Int,
        combo: Int,
        multiplier: Float,
        modifier: Modifier
    )
}