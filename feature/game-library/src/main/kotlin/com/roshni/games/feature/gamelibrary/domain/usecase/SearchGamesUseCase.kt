package com.roshni.games.feature.gamelibrary.domain.usecase

import com.roshni.games.feature.gamelibrary.domain.model.SearchResults
import com.roshni.games.feature.gamelibrary.domain.repository.GameLibraryDomainRepository
import javax.inject.Inject

class SearchGamesUseCase @Inject constructor(
    private val gameLibraryDomainRepository: GameLibraryDomainRepository
) {
    suspend operator fun invoke(query: String): SearchResults? {
        return gameLibraryDomainRepository.searchGames(query)
    }
}