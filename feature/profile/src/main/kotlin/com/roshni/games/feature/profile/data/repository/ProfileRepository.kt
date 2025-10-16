package com.roshni.games.feature.profile.data.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.profile.data.datasource.ProfileDataSource
import com.roshni.games.feature.profile.data.model.Achievement
import com.roshni.games.feature.profile.data.model.GameStatistics
import com.roshni.games.feature.profile.data.model.ProfileActivity
import com.roshni.games.feature.profile.data.model.ProfileCustomization
import com.roshni.games.feature.profile.data.model.UserPreferences
import com.roshni.games.feature.profile.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ProfileRepository {
    fun getUserProfile(): Flow<Result<UserProfile>>
    fun getGameStatistics(): Flow<Result<GameStatistics>>
    fun getAchievements(): Flow<Result<List<Achievement>>>
    fun getRecentActivity(limit: Int = 10): Flow<Result<List<ProfileActivity>>>
    fun getProfileCustomization(): Flow<Result<ProfileCustomization>>
    suspend fun updateUserProfile(profile: UserProfile): Result<Boolean>
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Boolean>
    suspend fun updateProfileCustomization(customization: ProfileCustomization): Result<Boolean>
    suspend fun unlockAchievement(achievementId: String): Result<Boolean>
    suspend fun refreshProfileData(): Result<Boolean>
}

class ProfileRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : ProfileRepository {

    override fun getUserProfile(): Flow<Result<UserProfile>> {
        return profileDataSource.getUserProfile()
            .map { profile -> Result.Success(profile) }
            .catch { error -> Result.Error(error) }
    }

    override fun getGameStatistics(): Flow<Result<GameStatistics>> {
        return profileDataSource.getGameStatistics()
            .map { stats -> Result.Success(stats) }
            .catch { error -> Result.Error(error) }
    }

    override fun getAchievements(): Flow<Result<List<Achievement>>> {
        return profileDataSource.getAchievements()
            .map { achievements -> Result.Success(achievements) }
            .catch { error -> Result.Error(error) }
    }

    override fun getRecentActivity(limit: Int): Flow<Result<List<ProfileActivity>>> {
        return profileDataSource.getRecentActivity(limit)
            .map { activities -> Result.Success(activities) }
            .catch { error -> Result.Error(error) }
    }

    override fun getProfileCustomization(): Flow<Result<ProfileCustomization>> {
        return profileDataSource.getProfileCustomization()
            .map { customization -> Result.Success(customization) }
            .catch { error -> Result.Error(error) }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Boolean> {
        return try {
            val success = profileDataSource.updateUserProfile(profile)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Boolean> {
        return try {
            val success = profileDataSource.updateUserPreferences(preferences)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateProfileCustomization(customization: ProfileCustomization): Result<Boolean> {
        return try {
            val success = profileDataSource.updateProfileCustomization(customization)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Result<Boolean> {
        return try {
            val success = profileDataSource.unlockAchievement(achievementId)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshProfileData(): Result<Boolean> {
        return try {
            val success = profileDataSource.refreshProfileData()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}