package com.roshni.games.service.gameloader.data.datasource

import com.roshni.games.service.gameloader.data.model.GameModule
import com.roshni.games.service.gameloader.data.model.GameModuleLoadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Data source for game module operations
 */
interface GameModuleDataSource {

    /**
     * Get all available game modules
     */
    fun getAvailableModules(): Flow<List<GameModule>>

    /**
     * Get a specific game module by ID
     */
    fun getModule(moduleId: String): Flow<GameModule?>

    /**
     * Load a game module dynamically
     */
    suspend fun loadModule(moduleId: String): GameModuleLoadState

    /**
     * Unload a game module
     */
    suspend fun unloadModule(moduleId: String): Boolean

    /**
     * Check if a module is loaded
     */
    fun isModuleLoaded(moduleId: String): Flow<Boolean>

    /**
     * Get loading state for a module
     */
    fun getModuleLoadState(moduleId: String): StateFlow<GameModuleLoadState>

    /**
     * Refresh module list from remote source
     */
    suspend fun refreshModules(): Result<Unit>

    /**
     * Download and install a new module
     */
    suspend fun downloadModule(module: GameModule): Result<GameModule>
}

/**
 * Local implementation of game module data source
 */
class LocalGameModuleDataSource : GameModuleDataSource {

    private val _availableModules = MutableStateFlow<List<GameModule>>(emptyList())
    private val _loadedModules = MutableStateFlow<Set<String>>(emptySet())
    private val _moduleLoadStates = MutableStateFlow<Map<String, GameModuleLoadState>>(emptyMap())

    override fun getAvailableModules(): Flow<List<GameModule>> = _availableModules.asStateFlow()

    override fun getModule(moduleId: String): Flow<GameModule?> {
        return _availableModules.asStateFlow().let { flow ->
            kotlinx.coroutines.flow.map { modules ->
                modules.find { it.id == moduleId }
            }
        }
    }

    override suspend fun loadModule(moduleId: String): GameModuleLoadState {
        Timber.d("Loading game module: $moduleId")

        // Update loading state
        _moduleLoadStates.value = _moduleLoadStates.value.toMutableMap().apply {
            put(moduleId, GameModuleLoadState.Loading)
        }

        return try {
            // Find the module
            val module = _availableModules.value.find { it.id == moduleId }

            if (module == null) {
                val error = GameModuleLoadState.Error(IllegalArgumentException("Module not found: $moduleId"))
                _moduleLoadStates.value = _moduleLoadStates.value.toMutableMap().apply {
                    put(moduleId, error)
                }
                return error
            }

            // Simulate dynamic loading (in real implementation, this would use ClassLoader or similar)
            // For now, we'll just mark it as loaded
            _loadedModules.value = _loadedModules.value + moduleId

            val success = GameModuleLoadState.Success(module)
            _moduleLoadStates.value = _moduleLoadStates.value.toMutableMap().apply {
                put(moduleId, success)
            }

            Timber.d("Successfully loaded game module: $moduleId")
            success

        } catch (e: Exception) {
            Timber.e(e, "Failed to load game module: $moduleId")
            val error = GameModuleLoadState.Error(e)
            _moduleLoadStates.value = _moduleLoadStates.value.toMutableMap().apply {
                put(moduleId, error)
            }
            error
        }
    }

    override suspend fun unloadModule(moduleId: String): Boolean {
        Timber.d("Unloading game module: $moduleId")

        return try {
            _loadedModules.value = _loadedModules.value - moduleId
            _moduleLoadStates.value = _moduleLoadStates.value.toMutableMap().apply {
                remove(moduleId)
            }
            Timber.d("Successfully unloaded game module: $moduleId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to unload game module: $moduleId")
            false
        }
    }

    override fun isModuleLoaded(moduleId: String): Flow<Boolean> {
        return _loadedModules.asStateFlow().let { flow ->
            kotlinx.coroutines.flow.map { loadedModules ->
                moduleId in loadedModules
            }
        }
    }

    override fun getModuleLoadState(moduleId: String): StateFlow<GameModuleLoadState> {
        return kotlinx.coroutines.flow.combine(
            _moduleLoadStates.asStateFlow(),
            kotlinx.coroutines.flow.flowOf(Unit)
        ) { states, _ ->
            states[moduleId] ?: GameModuleLoadState.Idle
        }.stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = GameModuleLoadState.Idle
        )
    }

    override suspend fun refreshModules(): Result<Unit> {
        return try {
            // In a real implementation, this would fetch from a remote API or file system
            // For now, we'll simulate with some sample data
            val sampleModules = listOf(
                GameModule(
                    id = "puzzle-game-1",
                    name = "Mind Bender",
                    version = "1.0.0",
                    description = "A challenging puzzle game that tests your logic skills",
                    author = "Roshni Games",
                    category = "Puzzle",
                    difficulty = com.roshni.games.service.gameloader.data.model.GameDifficulty.MEDIUM,
                    minPlayers = 1,
                    maxPlayers = 1,
                    estimatedDuration = 15,
                    entryPoint = "com.roshni.games.puzzle.MindBenderActivity",
                    tags = listOf("logic", "brain-training", "offline")
                ),
                GameModule(
                    id = "arcade-game-1",
                    name = "Space Shooter",
                    version = "1.2.0",
                    description = "Fast-paced arcade action in outer space",
                    author = "Roshni Games",
                    category = "Arcade",
                    difficulty = com.roshni.games.service.gameloader.data.model.GameDifficulty.EASY,
                    minPlayers = 1,
                    maxPlayers = 2,
                    estimatedDuration = 10,
                    entryPoint = "com.roshni.games.arcade.SpaceShooterActivity",
                    tags = listOf("action", "multiplayer", "arcade")
                )
            )

            _availableModules.value = sampleModules
            Timber.d("Refreshed game modules: ${sampleModules.size} modules available")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh game modules")
            Result.failure(e)
        }
    }

    override suspend fun downloadModule(module: GameModule): Result<GameModule> {
        return try {
            // In a real implementation, this would download from the provided URL
            // For now, we'll just return the module as-is
            Timber.d("Downloading module: ${module.name}")
            Result.success(module)

        } catch (e: Exception) {
            Timber.e(e, "Failed to download module: ${module.name}")
            Result.failure(e)
        }
    }
}