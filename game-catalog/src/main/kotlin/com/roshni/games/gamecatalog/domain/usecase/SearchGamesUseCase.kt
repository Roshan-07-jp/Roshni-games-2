package com.roshni.games.gamecatalog.domain.usecase

import com.roshni.games.gamecatalog.data.model.GameDefinition
import com.roshni.games.gamecatalog.domain.repository.GameCatalogRepository

/**
 * Use case for searching games
 */
class SearchGamesUseCase(
    private val repository: GameCatalogRepository
) {
    suspend operator fun invoke(query: String): Result<List<GameDefinition>> {
        return repository.searchGames(query)
    }
}