package com.roshni.games.service.gameloader

import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.model.GameModuleLoadState
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepository
import com.roshni.games.service.gameloader.domain.usecase.GetGameModulesUseCase
import com.roshni.games.service.gameloader.domain.usecase.LoadGameModuleUseCase
import com.roshni.games.service.gameloader.domain.usecase.SearchGameModulesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Main service class for game module loading and management
 */
class GameLoaderService(
    private val repository: GameModuleDomainRepository,
    private val getGameModulesUseCase: GetGameModulesUseCase,
    private val loadGameModuleUseCase: LoadGameModuleUseCase,
    private val searchGameModulesUseCase: SearchGameModulesUseCase
) {

    /**
     * Initialize the game loader service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing GameLoaderService")
            repository.refreshModules()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize GameLoaderService")
            Result.failure(e)
        }
    }

    /**
     * Get all available game modules
     */
    fun getAvailableModules(): Flow<List<GameModuleDomain>> {
        return getGameModulesUseCase()
    }

    /**
     * Get a specific game module by ID
     */
    fun getModule(moduleId: String): Flow<GameModuleDomain?> {
        return repository.getModule(moduleId)
    }

    /**
     * Load a game module
     */
    suspend fun loadModule(moduleId: String): Flow<GameModuleLoadState> {
        return loadGameModuleUseCase(moduleId)
    }

    /**
     * Unload a game module
     */
    suspend fun unloadModule(moduleId: String): Boolean {
        return repository.unloadModule(moduleId)
    }

    /**
     * Check if a module is loaded
     */
    fun isModuleLoaded(moduleId: String): Flow<Boolean> {
        return repository.isModuleLoaded(moduleId)
    }

    /**
     * Get loading state for a module
     */
    fun getModuleLoadState(moduleId: String): Flow<GameModuleLoadState> {
        return repository.getModuleLoadState(moduleId)
    }

    /**
     * Search game modules
     */
    fun searchModules(query: String): Flow<List<GameModuleDomain>> {
        return searchGameModulesUseCase(query)
    }

    /**
     * Get modules by category
     */
    fun getModulesByCategory(category: String): Flow<List<GameModuleDomain>> {
        return repository.getModulesByCategory(category)
    }

    /**
     * Get modules by difficulty
     */
    fun getModulesByDifficulty(difficulty: com.roshni.games.service.gameloader.domain.model.GameDifficulty): Flow<List<GameModuleDomain>> {
        return repository.getModulesByDifficulty(difficulty)
    }

    /**
     * Get loaded modules
     */
    fun getLoadedModules(): Flow<List<GameModuleDomain>> {
        return repository.getLoadedModules()
    }

    /**
     * Refresh modules from remote source
     */
    suspend fun refreshModules(): Result<Unit> {
        return repository.refreshModules()
    }

    /**
     * Get service statistics
     */
    fun getServiceStats(): Flow<ServiceStats> = flow {
        repository.getAvailableModules().collect { modules ->
            val loadedModules = repository.getLoadedModules().collect { loaded ->
                ServiceStats(
                    totalModules = modules.size,
                    loadedModules = loaded.size,
                    availableCategories = modules.map { it.category }.distinct().size,
                    lastRefreshTime = kotlinx.datetime.Clock.System.now()
                )
            }
        }
    }
}

/**
 * Service statistics data class
 */
data class ServiceStats(
    val totalModules: Int,
    val loadedModules: Int,
    val availableCategories: Int,
    val lastRefreshTime: kotlinx.datetime.Instant
)