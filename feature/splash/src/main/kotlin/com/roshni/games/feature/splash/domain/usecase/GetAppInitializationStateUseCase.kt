package com.roshni.games.feature.splash.domain.usecase

import com.roshni.games.feature.splash.domain.model.AppInitializationState
import com.roshni.games.feature.splash.domain.repository.SplashDomainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppInitializationStateUseCase @Inject constructor(
    private val splashDomainRepository: SplashDomainRepository
) {
    operator fun invoke(): Flow<AppInitializationState> {
        return splashDomainRepository.getAppInitializationState()
    }
}