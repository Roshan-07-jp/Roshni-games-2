package com.roshni.games.feature.home.domain.usecase

import com.roshni.games.feature.home.domain.model.HomeScreenState
import com.roshni.games.feature.home.domain.repository.HomeDomainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val homeDomainRepository: HomeDomainRepository
) {
    operator fun invoke(): Flow<HomeScreenState> {
        return homeDomainRepository.getHomeScreenState()
    }
}