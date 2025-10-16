package com.roshni.games.feature.splash.domain.usecase

import com.roshni.games.feature.splash.domain.model.InitializationResult
import com.roshni.games.feature.splash.domain.repository.SplashDomainRepository
import javax.inject.Inject

class PerformInitializationUseCase @Inject constructor(
    private val splashDomainRepository: SplashDomainRepository
) {
    suspend operator fun invoke(): InitializationResult {
        return splashDomainRepository.performInitialization()
    }
}