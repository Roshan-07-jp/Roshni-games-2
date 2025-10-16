package com.roshni.games.service.gameloader.domain.repository

import com.roshni.games.service.gameloader.data.repository.GameModuleRepository
import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.model.GameModuleLoadState
import com.roshni.games.service.gameloader.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of domain repository that delegates to data repository
 */
class GameModuleDomainRepositoryImpl(
    private val dataRepository: GameModuleRepository
) : GameModuleDomainRepository {

    override fun getAvailableModules(): Flow<List<GameModuleDomain>> {
        return dataRepository.getAvailableModules().map { modules ->
            modules.map { it.toDomain() }
        }
    }

    override fun getModule(moduleId: String): Flow<GameModuleDomain?> {
        return dataRepository.getModule(moduleId).map { module ->
            module?.toDomain()
        }
    }

    override suspend fun loadModule(moduleId: String): GameModuleLoadState {
        return dataRepository.loadModule(moduleId).toDomain()
    }

    override suspend fun unloadModule(moduleId: String): Boolean {
        return dataRepository.unloadModule(moduleId)
    }

    override fun isModuleLoaded(moduleId: String): Flow<Boolean> {
        return dataRepository.isModuleLoaded(moduleId)
    }

    override fun getModuleLoadState(moduleId: String): Flow<GameModuleLoadState> {
        return dataRepository.getModuleLoadState(moduleId).map { state ->
            state.toDomain()
        }
    }

    override suspend fun refreshModules(): Result<Unit> {
        return dataRepository.refreshModules()
    }

    override fun getModulesByCategory(category: String): Flow<List<GameModuleDomain>> {
        return dataRepository.getModulesByCategory(category).map { modules ->
            modules.map { it.toDomain() }
        }
    }

    override fun getModulesByDifficulty(difficulty: com.roshni.games.service.gameloader.domain.model.GameDifficulty): Flow<List<GameModuleDomain>> {
        return dataRepository.getModulesByDifficulty(difficulty.toData()).map { modules ->
            modules.map { it.toDomain() }
        }
    }

    override fun searchModules(query: String): Flow<List<GameModuleDomain>> {
        return dataRepository.searchModules(query).map { modules ->
            modules.map { it.toDomain() }
        }
    }

    override fun getLoadedModules(): Flow<List<GameModuleDomain>> {
        return dataRepository.getLoadedModules().map { modules ->
            modules.map { it.toDomain() }
        }
    }
}