package com.roshni.games.service.gameloader.domain.usecase

import com.roshni.games.service.gameloader.domain.model.GameModuleDomain
import com.roshni.games.service.gameloader.domain.repository.GameModuleDomainRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use case for searching game modules
 */
class SearchGameModulesUseCase(
    private val repository: GameModuleDomainRepository
) {

    operator fun invoke(query: String): Flow<List<GameModuleDomain>> {
        Timber.d("UseCase: Searching modules with query: $query")
        return repository.searchModules(query)
    }
}