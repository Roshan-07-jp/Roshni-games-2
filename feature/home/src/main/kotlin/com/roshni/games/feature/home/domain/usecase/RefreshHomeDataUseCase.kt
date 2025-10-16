package com.roshni.games.feature.home.domain.usecase

import com.roshni.games.feature.home.domain.repository.HomeDomainRepository
import javax.inject.Inject

class RefreshHomeDataUseCase @Inject constructor(
    private val homeDomainRepository: HomeDomainRepository
) {
    suspend operator fun invoke(): Boolean {
        return homeDomainRepository.refreshHomeData()
    }
}