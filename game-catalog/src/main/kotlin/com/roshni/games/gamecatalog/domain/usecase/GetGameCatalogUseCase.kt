package com.roshni.games.gamecatalog.domain.usecase

import com.roshni.games.gamecatalog.data.model.GameCatalog
import com.roshni.games.gamecatalog.domain.repository.GameCatalogRepository

/**
 * Use case for getting the complete game catalog
 */
class GetGameCatalogUseCase(
    private val repository: GameCatalogRepository
) {
    suspend operator fun invoke(): Result<GameCatalog> {
        return repository.getGameCatalog()
    }
}