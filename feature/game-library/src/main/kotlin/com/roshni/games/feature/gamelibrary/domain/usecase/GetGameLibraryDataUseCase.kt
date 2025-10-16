package com.roshni.games.feature.gamelibrary.domain.usecase

import com.roshni.games.feature.gamelibrary.domain.model.GameFilter
import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryState
import com.roshni.games.feature.gamelibrary.domain.model.SortOption
import com.roshni.games.feature.gamelibrary.domain.model.ViewMode
import com.roshni.games.feature.gamelibrary.domain.repository.GameLibraryDomainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGameLibraryDataUseCase @Inject constructor(
    private val gameLibraryDomainRepository: GameLibraryDomainRepository
) {
    operator fun invoke(
        filter: GameFilter = GameFilter(),
        sortOption: SortOption = SortOption("name", "Name"),
        searchQuery: String = "",
        viewMode: ViewMode = ViewMode.GRID
    ): Flow<GameLibraryState> {
        return gameLibraryDomainRepository.getGameLibraryState(filter, sortOption, searchQuery, viewMode)
    }
}