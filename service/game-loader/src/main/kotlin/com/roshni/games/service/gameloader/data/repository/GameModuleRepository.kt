package com.roshni.games.service.gameloader.data.repository

import com.roshni.games.service.gameloader.data.datasource.GameModuleDataSource
import com.roshni.games.service.gameloader.data.model.GameModule
import com.roshni.games.service.gameloader.data.model.GameModuleLoadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Repository interface for game module operations
 */
interface GameModuleRepository {

    /**
     * Get all available game modules
     */
    fun getAvailableModules(): Flow<List<GameModule>>

    /**
     * Get a specific game module by ID
     */
    fun getModule(moduleId: String): Flow<GameModule?>

    /**
     * Load a game module
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
    fun getModuleLoadState(moduleId: String): Flow<GameModuleLoadState>

    /**
     * Refresh modules from remote source
     */
    suspend fun refreshModules(): Result<Unit>

    /**
     * Get modules by category
     */
    fun getModulesByCategory(category: String): Flow<List<GameModule>>

    /**
     * Get modules by difficulty
     */
    fun getModulesByDifficulty(difficulty: com.roshni.games.service.gameloader.data.model.GameDifficulty): Flow<List<GameModule>>

    /**
     * Search modules by query
     */
    fun searchModules(query: String): Flow<List<GameModule>>

    /**
     * Get loaded modules
     */
    fun getLoadedModules(): Flow<List<GameModule>>
}

/**
 * Implementation of game module repository
 */
class GameModuleRepositoryImpl(
    private val dataSource: GameModuleDataSource
) : GameModuleRepository {

    override fun getAvailableModules(): Flow<List<GameModule>> {
        return dataSource.getAvailableModules()
    }

    override fun getModule(moduleId: String): Flow<GameModule?> {
        return dataSource.getModule(moduleId)
    }

    override suspend fun loadModule(moduleId: String): GameModuleLoadState {
        Timber.d("Repository: Loading module $moduleId")
        return dataSource.loadModule(moduleId)
    }

    override suspend fun unloadModule(moduleId: String): Boolean {
        Timber.d("Repository: Unloading module $moduleId")
        return dataSource.unloadModule(moduleId)
    }

    override fun isModuleLoaded(moduleId: String): Flow<Boolean> {
        return dataSource.isModuleLoaded(moduleId)
    }

    override fun getModuleLoadState(moduleId: String): Flow<GameModuleLoadState> {
        return dataSource.getModuleLoadState(moduleId).let { stateFlow ->
            kotlinx.coroutines.flow.map(stateFlow) { it }
        }
    }

    override suspend fun refreshModules(): Result<Unit> {
        Timber.d("Repository: Refreshing modules")
        return dataSource.refreshModules()
    }

    override fun getModulesByCategory(category: String): Flow<List<GameModule>> {
        return dataSource.getAvailableModules().map { modules ->
            modules.filter { it.category.equals(category, ignoreCase = true) }
        }
    }

    override fun getModulesByDifficulty(difficulty: com.roshni.games.service.gameloader.data.model.GameDifficulty): Flow<List<GameModule>> {
        return dataSource.getAvailableModules().map { modules ->
            modules.filter { it.difficulty == difficulty }
        }
    }

    override fun searchModules(query: String): Flow<List<GameModule>> {
        return dataSource.getAvailableModules().map { modules ->
            modules.filter { module ->
                module.name.contains(query, ignoreCase = true) ||
                module.description.contains(query, ignoreCase = true) ||
                module.tags.any { it.contains(query, ignoreCase = true) } ||
                module.category.contains(query, ignoreCase = true)
            }
        }
    }

    override fun getLoadedModules(): Flow<List<GameModule>> {
        return combine(
            dataSource.getAvailableModules(),
            dataSource.isModuleLoaded("").let { loadedIdsFlow ->
                kotlinx.coroutines.flow.map(loadedIdsFlow) { loadedIds ->
                    // This is a simplified implementation
                    // In a real scenario, you'd track loaded module IDs separately
                    emptyList<String>()
                }
            }
        ) { modules, loadedIds ->
            // For now, return empty list as we don't have a way to track loaded modules
            // In a real implementation, you'd maintain a separate state for loaded modules
            emptyList()
        }
    }
}