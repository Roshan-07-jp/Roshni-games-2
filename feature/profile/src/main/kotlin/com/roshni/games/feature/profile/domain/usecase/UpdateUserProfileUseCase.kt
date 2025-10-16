package com.roshni.games.feature.profile.domain.usecase

import com.roshni.games.feature.profile.domain.model.UserProfile
import com.roshni.games.feature.profile.domain.repository.ProfileDomainRepository
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val profileDomainRepository: ProfileDomainRepository
) {
    suspend operator fun invoke(profile: UserProfile): Boolean {
        return profileDomainRepository.updateUserProfile(profile)
    }
}