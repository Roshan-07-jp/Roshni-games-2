package com.roshni.games.gameengine.core

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Core game engine that manages the game loop, rendering, and state
 */
class GameEngine(
    private val context: Context,
    private val gameView: View,
    private val targetFps: Int = 60
) : LifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Game + SupervisorJob())
    private var gameLoopJob: Job? = null
    private var renderJob: Job? = null

    // Engine state
    private val _engineState = MutableStateFlow(EngineState.STOPPED)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _frameTime = MutableStateFlow(0L)
    val frameTime: StateFlow<Long> = _frameTime.asStateFlow()

    // Game loop timing
    private val frameInterval = 1_000_000_000L / targetFps // nanoseconds
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fpsTimer = 0L

    // Registered systems
    private val gameSystems = mutableListOf<GameSystem>()
    private val renderSystems = mutableListOf<RenderSystem>()

    // Input system
    private var inputSystem: InputSystem? = null

    // Audio system
    private var audioSystem: AudioSystem? = null

    // Physics system
    private var physicsSystem: PhysicsSystem? = null

    /**
     * Initialize the game engine
     */
    fun initialize() {
        Timber.d("Initializing game engine with target FPS: $targetFps")

        // Create core systems
        inputSystem = InputSystem(context, gameView)
        audioSystem = AudioSystem(context)
        physicsSystem = PhysicsSystem()

        // Register core systems
        registerSystem(inputSystem!!)
        registerSystem(audioSystem!!)
        registerSystem(physicsSystem!!)

        _engineState.value = EngineState.INITIALIZED
        Timber.d("Game engine initialized successfully")
    }

    /**
     * Start the game engine
     */
    fun start() {
        if (_engineState.value != EngineState.INITIALIZED && _engineState.value != EngineState.PAUSED) {
            Timber.w("Cannot start engine from state: ${_engineState.value}")
            return
        }

        Timber.d("Starting game engine")
        _engineState.value = EngineState.STARTING

        // Initialize all systems
        gameSystems.forEach { it.initialize() }

        // Start game loop
        startGameLoop()

        // Start rendering
        startRendering()

        _engineState.value = EngineState.RUNNING
        Timber.d("Game engine started successfully")
    }

    /**
     * Pause the game engine
     */
    fun pause() {
        if (_engineState.value != EngineState.RUNNING) {
            return
        }

        Timber.d("Pausing game engine")
        _engineState.value = EngineState.PAUSING

        // Pause all systems
        gameSystems.forEach { it.pause() }

        _engineState.value = EngineState.PAUSED
        Timber.d("Game engine paused")
    }

    /**
     * Resume the game engine
     */
    fun resume() {
        if (_engineState.value != EngineState.PAUSED) {
            return
        }

        Timber.d("Resuming game engine")
        _engineState.value = EngineState.RESUMING

        // Resume all systems
        gameSystems.forEach { it.resume() }

        _engineState.value = EngineState.RUNNING
        Timber.d("Game engine resumed")
    }

    /**
     * Stop the game engine
     */
    fun stop() {
        if (_engineState.value == EngineState.STOPPED) {
            return
        }

        Timber.d("Stopping game engine")
        _engineState.value = EngineState.STOPPING

        // Stop game loop
        stopGameLoop()

        // Stop rendering
        stopRendering()

        // Cleanup all systems
        gameSystems.forEach { it.cleanup() }

        _engineState.value = EngineState.STOPPED
        Timber.d("Game engine stopped")
    }

    /**
     * Register a game system
     */
    fun registerSystem(system: GameSystem) {
        if (!gameSystems.contains(system)) {
            gameSystems.add(system)
            Timber.d("Registered game system: ${system::class.simpleName}")
        }
    }

    /**
     * Unregister a game system
     */
    fun unregisterSystem(system: GameSystem) {
        gameSystems.remove(system)
        Timber.d("Unregistered game system: ${system::class.simpleName}")
    }

    /**
     * Register a render system
     */
    fun registerRenderSystem(system: RenderSystem) {
        if (!renderSystems.contains(system)) {
            renderSystems.add(system)
            Timber.d("Registered render system: ${system::class.simpleName}")
        }
    }

    /**
     * Get input system
     */
    fun getInputSystem(): InputSystem? = inputSystem

    /**
     * Get audio system
     */
    fun getAudioSystem(): AudioSystem? = audioSystem

    /**
     * Get physics system
     */
    fun getPhysicsSystem(): PhysicsSystem? = physicsSystem

    /**
     * Start the main game loop
     */
    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            lastFrameTime = System.nanoTime()

            while (isActive && _engineState.value == EngineState.RUNNING) {
                val frameStartTime = System.nanoTime()

                try {
                    // Fixed timestep updates
                    val deltaTime = (frameStartTime - lastFrameTime) / 1_000_000f // Convert to milliseconds
                    update(deltaTime)

                    // Update frame timing
                    val frameDuration = System.nanoTime() - frameStartTime
                    _frameTime.value = frameDuration / 1_000_000L // Convert to milliseconds

                    // Calculate FPS
                    frameCount++
                    if (System.currentTimeMillis() - fpsTimer >= 1000) {
                        _fps.value = frameCount.toFloat()
                        frameCount = 0
                        fpsTimer = System.currentTimeMillis()
                    }

                    // Sleep to maintain target FPS
                    val sleepTime = frameInterval - frameDuration
                    if (sleepTime > 0) {
                        delay(sleepTime / 1_000_000L) // Convert to milliseconds
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Error in game loop")
                }

                lastFrameTime = frameStartTime
            }
        }
    }

    /**
     * Stop the game loop
     */
    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    /**
     * Start rendering
     */
    private fun startRendering() {
        renderJob = scope.launch {
            while (isActive && _engineState.value == EngineState.RUNNING) {
                try {
                    render()
                    // Small delay to prevent excessive rendering
                    delay(1)
                } catch (e: Exception) {
                    Timber.e(e, "Error in rendering")
                }
            }
        }
    }

    /**
     * Stop rendering
     */
    private fun stopRendering() {
        renderJob?.cancel()
        renderJob = null
    }

    /**
     * Update all game systems
     */
    private fun update(deltaTime: Float) {
        gameSystems.forEach { system ->
            try {
                system.update(deltaTime)
            } catch (e: Exception) {
                Timber.e(e, "Error updating system: ${system::class.simpleName}")
            }
        }
    }

    /**
     * Render all render systems
     */
    private fun render() {
        renderSystems.forEach { system ->
            try {
                system.render()
            } catch (e: Exception) {
                Timber.e(e, "Error rendering system: ${system::class.simpleName}")
            }
        }
    }

    /**
     * Lifecycle observer methods
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        if (_engineState.value == EngineState.RUNNING) {
            pause()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (_engineState.value == EngineState.PAUSED) {
            resume()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        stop()
        scope.cancel()
    }
}

/**
 * Engine states
 */
enum class EngineState {
    STOPPED,
    INITIALIZED,
    STARTING,
    RUNNING,
    PAUSING,
    PAUSED,
    RESUMING,
    STOPPING
}

/**
 * Base interface for all game systems
 */
abstract class GameSystem {
    open fun initialize() {}
    open fun update(deltaTime: Float) {}
    open fun pause() {}
    open fun resume() {}
    open fun cleanup() {}
}

/**
 * Base interface for render systems
 */
abstract class RenderSystem : GameSystem() {
    abstract fun render()
}