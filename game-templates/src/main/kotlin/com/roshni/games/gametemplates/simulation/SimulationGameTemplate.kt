package com.roshni.games.gametemplates.simulation

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
 * Template for simulation games (Life Sim, Business, Tycoon, Farming, City Builder, etc.)
 */
abstract class SimulationGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Simulation-specific state
    private val _simulationSpeed = MutableStateFlow(1.0f)
    val simulationSpeed: StateFlow<Float> = _simulationSpeed.asStateFlow()

    private val _simulationObjects = MutableStateFlow<List<SimulationObject>>(emptyList())
    val simulationObjects: StateFlow<List<SimulationObject>> = _simulationObjects.asStateFlow()

    private val _environment = MutableStateFlow<Environment>(Environment())
    val environment: StateFlow<Environment> = _environment.asStateFlow()

    private val _statistics = MutableStateFlow<Map<String, Double>>(emptyMap())
    val statistics: StateFlow<Map<String, Double>> = _statistics.asStateFlow()

    // Simulation configuration
    protected val simulationConfig = SimulationConfig()

    override fun registerGameSystems() {
        // Simulation games may use physics for realistic interactions
        if (simulationConfig.enablePhysics) {
            gameEngine.registerSystem(physicsSystem)
        }
    }

    override fun initializeGameState() {
        // Initialize simulation-specific state
        _simulationSpeed.value = 1.0f
        _simulationObjects.value = emptyList()
        _environment.value = Environment()
        _statistics.value = emptyMap()
    }

    override fun loadGameAssets() {
        // Load common simulation game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "simulation/build_sound.mp3",
                    "simulation/ambient_sound.mp3",
                    "simulation/notification.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load simulation assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup simulation-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onSimulationTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onSimulationDrag(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup simulation-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.4f) // Medium volume for simulation games
            audioSystem.setSfxVolume(0.5f)
            audioSystem.playMusic("ambient_sound", fadeIn = true)
        }
    }

    override fun initializeGame() {
        // Initialize simulation-specific game state
        initializeEnvironment()
        createInitialObjects()
        startSimulation()
    }

    override fun updateGame(deltaTime: Float) {
        // Update simulation-specific game logic
        val adjustedDeltaTime = deltaTime * _simulationSpeed.value

        updateEnvironment(adjustedDeltaTime)
        updateSimulationObjects(adjustedDeltaTime)
        updateStatistics(adjustedDeltaTime)
        updateSimulationGame(adjustedDeltaTime)
    }

    override fun updateGameProgress() {
        // Update simulation game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "simulationSpeed" to _simulationSpeed.value.toString(),
                    "objectCount" to _simulationObjects.value.size.toString(),
                    "statistics" to _statistics.value.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Simulation-specific score handling
        if (newScore > 0) {
            playSoundEffect("notification")
        }
    }

    override fun onGameOver() {
        // Simulation-specific game over handling
        playSoundEffect("notification")
    }

    /**
     * Set simulation speed
     */
    protected fun setSimulationSpeed(speed: Float) {
        _simulationSpeed.value = speed.coerceIn(simulationConfig.minSpeed, simulationConfig.maxSpeed)
    }

    /**
     * Add simulation object
     */
    protected fun addSimulationObject(obj: SimulationObject) {
        _simulationObjects.value = _simulationObjects.value + obj
        playSoundEffect("build_sound")
    }

    /**
     * Remove simulation object
     */
    protected fun removeSimulationObject(obj: SimulationObject) {
        _simulationObjects.value = _simulationObjects.value - obj
    }

    /**
     * Update environment
     */
    protected fun updateEnvironment(deltaTime: Float) {
        _environment.value = _environment.value.copy(
            timeOfDay = (_environment.value.timeOfDay + deltaTime * _simulationSpeed.value) % 24f,
            seasonProgress = (_environment.value.seasonProgress + deltaTime * _simulationSpeed.value / 8640f) % 1f // 90 days per season
        )
    }

    /**
     * Update statistics
     */
    protected fun updateStatistics(deltaTime: Float) {
        val currentStats = _statistics.value.toMutableMap()

        // Update basic statistics
        currentStats["totalObjects"] = _simulationObjects.value.size.toDouble()
        currentStats["simulationTime"] = _environment.value.timeOfDay.toDouble()
        currentStats["population"] = _simulationObjects.value.filter { it.type == ObjectType.PEOPLE }.size.toDouble()

        _statistics.value = currentStats
    }

    /**
     * Abstract methods for simulation-specific implementation
     */
    protected abstract fun initializeEnvironment()
    protected abstract fun createInitialObjects()
    protected abstract fun startSimulation()
    protected abstract fun updateSimulationObjects(deltaTime: Float)
    protected abstract fun updateSimulationGame(deltaTime: Float)
    protected abstract fun onSimulationTouch(x: Float, y: Float)
    protected abstract fun onSimulationDrag(x: Float, y: Float)

    /**
     * Simulation configuration
     */
    protected data class SimulationConfig(
        val enablePhysics: Boolean = false,
        val minSpeed: Float = 0.1f,
        val maxSpeed: Float = 10.0f,
        val defaultSpeed: Float = 1.0f,
        val enableAging: Boolean = true,
        val enableSeasons: Boolean = true,
        val enableDayNight: Boolean = true,
        val resourceDecay: Float = 0.1f, // Resources decay over time
        val populationGrowth: Float = 0.01f // Population growth rate
    )

    /**
     * Environment data class
     */
    data class Environment(
        val timeOfDay: Float = 12f, // 0-24 hours
        val seasonProgress: Float = 0f, // 0-1 (0 = spring, 0.25 = summer, etc.)
        val temperature: Float = 20f, // Celsius
        val humidity: Float = 0.5f, // 0-1
        val globalEvents: List<String> = emptyList()
    )

    /**
     * Simulation object
     */
    data class SimulationObject(
        val id: String,
        val type: ObjectType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val age: Float = 0f,
        val health: Float = 100f,
        val happiness: Float = 50f,
        val properties: Map<String, Any> = emptyMap()
    )

    /**
     * Object types
     */
    enum class ObjectType {
        PEOPLE, BUILDING, VEHICLE, ANIMAL, PLANT, RESOURCE
    }

    /**
     * Simulation game view model
     */
    abstract class SimulationGameViewModel : GameViewModel() {

        private val _simulationSpeed = MutableStateFlow(1.0f)
        val simulationSpeed: StateFlow<Float> = _simulationSpeed.asStateFlow()

        private val _simulationObjects = MutableStateFlow<List<SimulationObject>>(emptyList())
        val simulationObjects: StateFlow<List<SimulationObject>> = _simulationObjects.asStateFlow()

        private val _environment = MutableStateFlow(Environment())
        val environment: StateFlow<Environment> = _environment.asStateFlow()

        private val _statistics = MutableStateFlow<Map<String, Double>>(emptyMap())
        val statistics: StateFlow<Map<String, Double>> = _statistics.asStateFlow()

        fun setSimulationSpeed(speed: Float) {
            _simulationSpeed.value = speed
        }

        fun addSimulationObject(obj: SimulationObject) {
            _simulationObjects.value = _simulationObjects.value + obj
        }

        fun removeSimulationObject(obj: SimulationObject) {
            _simulationObjects.value = _simulationObjects.value - obj
        }

        fun updateEnvironment(environment: Environment) {
            _environment.value = environment
        }

        fun updateStatistics(stats: Map<String, Double>) {
            _statistics.value = stats
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as SimulationGameViewModel
        val currentSpeed by gameViewModel.simulationSpeed.collectAsState()
        val currentObjects by gameViewModel.simulationObjects.collectAsState()
        val currentEnvironment by gameViewModel.environment.collectAsState()
        val currentStats by gameViewModel.statistics.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Simulation game header
            SimulationGameHeader(
                simulationSpeed = currentSpeed,
                environment = currentEnvironment,
                statistics = currentStats,
                onSpeedChange = { setSimulationSpeed(it) }
            )

            // Main simulation content
            SimulationGameContent(
                objects = currentObjects,
                environment = currentEnvironment
            )
        }
    }

    @Composable
    protected abstract fun SimulationGameHeader(
        simulationSpeed: Float,
        environment: Environment,
        statistics: Map<String, Double>,
        onSpeedChange: (Float) -> Unit
    )

    @Composable
    protected abstract fun SimulationGameContent(
        objects: List<SimulationObject>,
        environment: Environment
    )
}