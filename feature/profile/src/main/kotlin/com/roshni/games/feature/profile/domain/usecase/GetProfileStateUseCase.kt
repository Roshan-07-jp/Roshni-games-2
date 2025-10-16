package com.roshni.games.feature.profile.domain.usecase

import com.roshni.games.feature.profile.domain.model.ProfileState
import com.roshni.games.feature.profile.domain.repository.ProfileDomainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileStateUseCase @Inject constructor(
    private val profileDomainRepository: ProfileDomainRepository
) {
    operator fun invoke(): Flow<ProfileState> {
        return profileDomainRepository.getProfileState()
    }
}