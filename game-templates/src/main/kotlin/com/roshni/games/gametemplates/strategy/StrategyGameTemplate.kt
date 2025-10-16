package com.roshni.games.gametemplates.strategy

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
 * Template for strategy games (Tower Defense, RTS, Turn-based, Board Games, etc.)
 */
abstract class StrategyGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Strategy-specific state
    private val _resources = MutableStateFlow<Map<ResourceType, Int>>(emptyMap())
    val resources: StateFlow<Map<ResourceType, Int>> = _resources.asStateFlow()

    private val _units = MutableStateFlow<List<GameUnit>>(emptyList())
    val units: StateFlow<List<GameUnit>> = _units.asStateFlow()

    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _turnCount = MutableStateFlow(1)
    val turnCount: StateFlow<Int> = _turnCount.asStateFlow()

    // Strategy configuration
    protected val strategyConfig = StrategyConfig()

    override fun registerGameSystems() {
        // Strategy games typically don't need physics unless specified
        if (strategyConfig.enableRealTimePhysics) {
            gameEngine.registerSystem(physicsSystem)
        }
    }

    override fun initializeGameState() {
        // Initialize strategy-specific state
        _resources.value = initializeResources()
        _units.value = emptyList()
        _buildings.value = emptyList()
        _isPlayerTurn.value = true
        _turnCount.value = 1
    }

    override fun loadGameAssets() {
        // Load common strategy game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "strategy/build_sound.mp3",
                    "strategy/unit_sound.mp3",
                    "strategy/attack_sound.mp3",
                    "strategy/victory_sound.mp3",
                    "strategy/defeat_sound.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load strategy assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup strategy-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onStrategyTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onStrategyDrag(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup strategy-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.4f) // Lower volume for strategy games
            audioSystem.setSfxVolume(0.6f)
        }
    }

    override fun initializeGame() {
        // Initialize strategy-specific game state
        setupGameBoard()
        initializePlayerUnits()
        startStrategyGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update strategy-specific game logic
        if (strategyConfig.isRealTime) {
            updateRealTime(deltaTime)
        } else {
            updateTurnBased(deltaTime)
        }

        updateStrategyGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update strategy game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "resources" to _resources.value.toString(),
                    "units" to _units.value.size.toString(),
                    "buildings" to _buildings.value.size.toString(),
                    "turnCount" to _turnCount.value.toString(),
                    "isPlayerTurn" to _isPlayerTurn.value.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Strategy-specific score handling
        if (newScore > 0) {
            playSoundEffect("score_sound")
        }
    }

    override fun onGameOver() {
        // Strategy-specific game over handling
        if (_score.value > 0) {
            playSoundEffect("victory_sound")
        } else {
            playSoundEffect("defeat_sound")
        }
    }

    /**
     * Add resource
     */
    protected fun addResource(type: ResourceType, amount: Int) {
        val current = _resources.value[type] ?: 0
        _resources.value = _resources.value + (type to current + amount)
    }

    /**
     * Remove resource
     */
    protected fun removeResource(type: ResourceType, amount: Int): Boolean {
        val current = _resources.value[type] ?: 0
        return if (current >= amount) {
            _resources.value = _resources.value + (type to current - amount)
            true
        } else {
            false
        }
    }

    /**
     * Check if player can afford cost
     */
    protected fun canAfford(cost: Map<ResourceType, Int>): Boolean {
        return cost.all { (type, amount) ->
            (_resources.value[type] ?: 0) >= amount
        }
    }

    /**
     * Spend resources
     */
    protected fun spendResources(cost: Map<ResourceType, Int>): Boolean {
        return if (canAfford(cost)) {
            cost.forEach { (type, amount) ->
                removeResource(type, amount)
            }
            true
        } else {
            false
        }
    }

    /**
     * Add unit
     */
    protected fun addUnit(unit: GameUnit) {
        _units.value = _units.value + unit
        playSoundEffect("unit_sound")
    }

    /**
     * Remove unit
     */
    protected fun removeUnit(unit: GameUnit) {
        _units.value = _units.value - unit
    }

    /**
     * Add building
     */
    protected fun addBuilding(building: Building) {
        _buildings.value = _buildings.value + building
        playSoundEffect("build_sound")
    }

    /**
     * Remove building
     */
    protected fun removeBuilding(building: Building) {
        _buildings.value = _buildings.value - building
    }

    /**
     * End current turn
     */
    protected fun endTurn() {
        if (strategyConfig.isTurnBased) {
            _isPlayerTurn.value = false
            _turnCount.value++

            // Process AI turn or next player
            processNextTurn()

            // Start next turn after delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(strategyConfig.turnDelay)
                _isPlayerTurn.value = true
            }
        }
    }

    /**
     * Abstract methods for strategy-specific implementation
     */
    protected abstract fun initializeResources(): Map<ResourceType, Int>
    protected abstract fun setupGameBoard()
    protected abstract fun initializePlayerUnits()
    protected abstract fun startStrategyGame()
    protected abstract fun updateRealTime(deltaTime: Float)
    protected abstract fun updateTurnBased(deltaTime: Float)
    protected abstract fun updateStrategyGame(deltaTime: Float)
    protected abstract fun onStrategyTouch(x: Float, y: Float)
    protected abstract fun onStrategyDrag(x: Float, y: Float)
    protected abstract fun processNextTurn()

    /**
     * Strategy configuration
     */
    protected data class StrategyConfig(
        val isTurnBased: Boolean = true,
        val isRealTime: Boolean = false,
        val enableRealTimePhysics: Boolean = false,
        val maxUnits: Int = 50,
        val maxBuildings: Int = 20,
        val turnDelay: Long = 2000L, // 2 seconds between turns
        val resourceTypes: List<ResourceType> = listOf(ResourceType.GOLD, ResourceType.WOOD, ResourceType.FOOD)
    )

    /**
     * Resource types
     */
    enum class ResourceType(val displayName: String, val icon: String) {
        GOLD("Gold", "🪙"),
        WOOD("Wood", "🪵"),
        FOOD("Food", "🌾"),
        STONE("Stone", "🪨"),
        IRON("Iron", "⚔️"),
        MANA("Mana", "🔮")
    }

    /**
     * Game unit
     */
    data class GameUnit(
        val id: String,
        val type: UnitType,
        val playerId: String,
        val x: Float,
        val y: Float,
        val health: Float = 100f,
        val maxHealth: Float = 100f,
        val attack: Float = 10f,
        val defense: Float = 5f,
        val speed: Float = 1.0f,
        val range: Float = 1.0f,
        val cost: Map<ResourceType, Int> = emptyMap()
    )

    /**
     * Unit types
     */
    enum class UnitType {
        WARRIOR, ARCHER, MAGE, WORKER, CAVALRY, FLYING
    }

    /**
     * Building
     */
    data class Building(
        val id: String,
        val type: BuildingType,
        val playerId: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val health: Float = 100f,
        val maxHealth: Float = 100f,
        val generatesResources: Map<ResourceType, Int> = emptyMap(),
        val cost: Map<ResourceType, Int> = emptyMap()
    )

    /**
     * Building types
     */
    enum class BuildingType {
        BASE, BARRACKS, ARCHERY_RANGE, MAGE_TOWER, FARM, MINE, TOWN_HALL
    }

    /**
     * Strategy game view model
     */
    abstract class StrategyGameViewModel : GameViewModel() {

        private val _resources = MutableStateFlow<Map<ResourceType, Int>>(emptyMap())
        val resources: StateFlow<Map<ResourceType, Int>> = _resources.asStateFlow()

        private val _units = MutableStateFlow<List<GameUnit>>(emptyList())
        val units: StateFlow<List<GameUnit>> = _units.asStateFlow()

        private val _buildings = MutableStateFlow<List<Building>>(emptyList())
        val buildings: StateFlow<List<Building>> = _buildings.asStateFlow()

        private val _isPlayerTurn = MutableStateFlow(true)
        val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

        private val _turnCount = MutableStateFlow(1)
        val turnCount: StateFlow<Int> = _turnCount.asStateFlow()

        fun addResource(type: ResourceType, amount: Int) {
            val current = _resources.value[type] ?: 0
            _resources.value = _resources.value + (type to current + amount)
        }

        fun removeResource(type: ResourceType, amount: Int): Boolean {
            val current = _resources.value[type] ?: 0
            return if (current >= amount) {
                _resources.value = _resources.value + (type to current - amount)
                true
            } else {
                false
            }
        }

        fun addUnit(unit: GameUnit) {
            _units.value = _units.value + unit
        }

        fun removeUnit(unit: GameUnit) {
            _units.value = _units.value - unit
        }

        fun addBuilding(building: Building) {
            _buildings.value = _buildings.value + building
        }

        fun removeBuilding(building: Building) {
            _buildings.value = _buildings.value - building
        }

        fun endTurn() {
            _isPlayerTurn.value = false
            _turnCount.value++
        }

        fun startNextTurn() {
            _isPlayerTurn.value = true
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as StrategyGameViewModel
        val currentResources by gameViewModel.resources.collectAsState()
        val currentUnits by gameViewModel.units.collectAsState()
        val currentBuildings by gameViewModel.buildings.collectAsState()
        val playerTurn by gameViewModel.isPlayerTurn.collectAsState()
        val currentTurn by gameViewModel.turnCount.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Strategy game header
            StrategyGameHeader(
                resources = currentResources,
                turnCount = currentTurn,
                isPlayerTurn = playerTurn,
                onEndTurn = { endTurn() }
            )

            // Main strategy game content
            StrategyGameContent(
                units = currentUnits,
                buildings = currentBuildings,
                isPlayerTurn = playerTurn
            )

            // Turn indicator
            if (strategyConfig.isTurnBased) {
                TurnIndicator(
                    isPlayerTurn = playerTurn,
                    turnCount = currentTurn,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    @Composable
    protected abstract fun StrategyGameHeader(
        resources: Map<ResourceType, Int>,
        turnCount: Int,
        isPlayerTurn: Boolean,
        onEndTurn: () -> Unit
    )

    @Composable
    protected abstract fun StrategyGameContent(
        units: List<GameUnit>,
        buildings: List<Building>,
        isPlayerTurn: Boolean
    )

    @Composable
    protected abstract fun TurnIndicator(
        isPlayerTurn: Boolean,
        turnCount: Int,
        modifier: Modifier
    )
}