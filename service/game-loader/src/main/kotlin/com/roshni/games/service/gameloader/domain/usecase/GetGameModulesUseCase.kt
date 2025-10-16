package com.roshni.games.service.gameloader.domain.usecase

import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use case for getting all available game modules
 */
class GetGameModulesUseCase(
    private val repository: GameModuleDomainRepository
) {

    operator fun invoke(): Flow<List<GameModuleDomain>> {
        Timber.d("UseCase: Getting all game modules")
        return repository.getAvailableModules()
    }
}