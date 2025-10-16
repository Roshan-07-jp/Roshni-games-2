package com.roshni.games.service.gameloader.domain.usecase

import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.model.GameModuleLoadState
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Use case for loading a game module
 */
class LoadGameModuleUseCase(
    private val repository: GameModuleDomainRepository
) {

    suspend operator fun invoke(moduleId: String): Flow<GameModuleLoadState> = flow {
        Timber.d("UseCase: Loading module $moduleId")

        try {
            // Emit loading state
            emit(GameModuleLoadState.Loading)

            // Load the module
            val result = repository.loadModule(moduleId)

            when (result) {
                is GameModuleLoadState.Success -> {
                    Timber.d("UseCase: Successfully loaded module ${result.module.name}")
                    emit(result)
                }
                is GameModuleLoadState.Error -> {
                    Timber.e(result.throwable, "UseCase: Failed to load module $moduleId")
                    emit(result)
                }
                else -> {
                    // This shouldn't happen, but handle it just in case
                    emit(result)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "UseCase: Exception while loading module $moduleId")
            emit(GameModuleLoadState.Error(e))
        }
    }
}