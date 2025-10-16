package com.roshni.games.service.gameloader.domain.repository

import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.model.GameModuleLoadState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for game module operations
 */
interface GameModuleDomainRepository {

    /**
     * Get all available game modules
     */
    fun getAvailableModules(): Flow<List<GameModuleDomain>>

    /**
     * Get a specific game module by ID
     */
    fun getModule(moduleId: String): Flow<GameModuleDomain?>

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
    fun getModulesByCategory(category: String): Flow<List<GameModuleDomain>>

    /**
     * Get modules by difficulty
     */
    fun getModulesByDifficulty(difficulty: com.roshni.games.service.gameloader.domain.model.GameDifficulty): Flow<List<GameModuleDomain>>

    /**
     * Search modules by query
     */
    fun searchModules(query: String): Flow<List<GameModuleDomain>>

    /**
     * Get loaded modules
     */
    fun getLoadedModules(): Flow<List<GameModuleDomain>>
}