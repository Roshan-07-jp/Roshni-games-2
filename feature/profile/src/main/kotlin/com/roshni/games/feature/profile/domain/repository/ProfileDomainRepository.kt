package com.roshni.games.feature.profile.domain.repository

import com.roshni.games.feature.profile.domain.model.Achievement
import com.roshni.games.feature.profile.domain.model.GameStatistics
import com.roshni.games.feature.profile.domain.model.ProfileActivity
import com.roshni.games.feature.profile.domain.model.ProfileCustomization
import com.roshni.games.feature.profile.domain.model.ProfileState
import com.roshni.games.feature.profile.domain.model.UserPreferences
import com.roshni.games.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileDomainRepository {
    fun getProfileState(): Flow<ProfileState>
    suspend fun updateUserProfile(profile: UserProfile): Boolean
    suspend fun updateUserPreferences(preferences: UserPreferences): Boolean
    suspend fun updateProfileCustomization(customization: ProfileCustomization): Boolean
    suspend fun unlockAchievement(achievementId: String): Boolean
    suspend fun refreshProfileData(): Boolean
}