package com.roshni.games.feature.profile.domain.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.profile.data.model.Achievement
import com.roshni.games.feature.profile.data.model.AchievementCategory
import com.roshni.games.feature.profile.data.model.AchievementRarity
import com.roshni.games.feature.profile.data.model.ActivityType
import com.roshni.games.feature.profile.data.model.GameStatistics
import com.roshni.games.feature.profile.data.model.ProfileActivity
import com.roshni.games.feature.profile.data.model.ProfileCustomization
import com.roshni.games.feature.profile.data.model.ProfileVisibility
import com.roshni.games.feature.profile.data.model.UserPreferences
import com.roshni.games.feature.profile.data.model.UserProfile
import com.roshni.games.feature.profile.data.repository.ProfileRepository
import com.roshni.games.feature.profile.domain.model.Achievement as DomainAchievement
import com.roshni.games.feature.profile.domain.model.AchievementCategory as DomainAchievementCategory
import com.roshni.games.feature.profile.domain.model.AchievementRarity as DomainAchievementRarity
import com.roshni.games.feature.profile.domain.model.ActivityType as DomainActivityType
import com.roshni.games.feature.profile.domain.model.GameStatistics as DomainGameStatistics
import com.roshni.games.feature.profile.domain.model.ProfileActivity as DomainProfileActivity
import com.roshni.games.feature.profile.domain.model.ProfileCustomization as DomainProfileCustomization
import com.roshni.games.feature.profile.domain.model.ProfileState
import com.roshni.games.feature.profile.domain.model.ProfileVisibility as DomainProfileVisibility
import com.roshni.games.feature.profile.domain.model.UserPreferences as DomainUserPreferences
import com.roshni.games.feature.profile.domain.model.UserProfile as DomainUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileDomainRepositoryImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : ProfileDomainRepository {

    override fun getProfileState(): Flow<ProfileState> {
        val profileFlow = profileRepository.getUserProfile()
        val statsFlow = profileRepository.getGameStatistics()
        val achievementsFlow = profileRepository.getAchievements()
        val activityFlow = profileRepository.getRecentActivity()
        val customizationFlow = profileRepository.getProfileCustomization()

        return combine(
            profileFlow,
            statsFlow,
            achievementsFlow,
            activityFlow,
            customizationFlow
        ) { profileResult, statsResult, achievementsResult, activityResult, customizationResult ->
            ProfileState(
                userProfile = when (profileResult) {
                    is Result.Success -> profileResult.data.toDomainUserProfile()
                    is Result.Error -> null
                },
                gameStatistics = when (statsResult) {
                    is Result.Success -> statsResult.data.toDomainGameStatistics()
                    is Result.Error -> DomainGameStatistics(
                        totalGamesPlayed = 0,
                        totalPlayTime = 0,
                        averageScore = 0f,
                        highestScore = 0,
                        gamesCompleted = 0,
                        currentStreak = 0,
                        longestStreak = 0,
                        favoriteCategory = null,
                        categoryStats = emptyMap(),
                        difficultyStats = emptyMap(),
                        monthlyStats = emptyList(),
                        lastUpdated = kotlinx.datetime.Clock.System.now().toLocalDateTime(
                            kotlinx.datetime.TimeZone.currentSystemDefault()
                        )
                    )
                },
                achievements = when (achievementsResult) {
                    is Result.Success -> achievementsResult.data.map { it.toDomainAchievement() }
                    is Result.Error -> emptyList()
                },
                recentActivity = when (activityResult) {
                    is Result.Success -> activityResult.data.map { it.toDomainProfileActivity() }
                    is Result.Error -> emptyList()
                },
                customization = when (customizationResult) {
                    is Result.Success -> customizationResult.data.toDomainProfileCustomization()
                    is Result.Error -> DomainProfileCustomization(
                        avatarOptions = emptyList(),
                        selectedAvatarId = "default",
                        backgroundColor = "#FFFFFF",
                        accentColor = "#6200EE",
                        showStats = true,
                        showAchievements = true,
                        showActivity = true,
                        profileVisibility = DomainProfileVisibility.PUBLIC
                    )
                },
                isLoading = false,
                error = null
            )
        }
    }

    override suspend fun updateUserProfile(profile: DomainUserProfile): Boolean {
        return when (val result = profileRepository.updateUserProfile(profile.toDataUserProfile())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateUserPreferences(preferences: DomainUserPreferences): Boolean {
        return when (val result = profileRepository.updateUserPreferences(preferences.toDataUserPreferences())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateProfileCustomization(customization: DomainProfileCustomization): Boolean {
        return when (val result = profileRepository.updateProfileCustomization(customization.toDataProfileCustomization())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Boolean {
        return when (val result = profileRepository.unlockAchievement(achievementId)) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun refreshProfileData(): Boolean {
        return when (val result = profileRepository.refreshProfileData()) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    // Conversion functions
    private fun UserProfile.toDomainUserProfile(): DomainUserProfile {
        return DomainUserProfile(
            id = id,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            joinDate = joinDate,
            lastActive = lastActive,
            isOnline = isOnline,
            level = level,
            experience = experience,
            experienceToNextLevel = experienceToNextLevel,
            preferences = preferences.toDomainUserPreferences()
        )
    }

    private fun UserPreferences.toDomainUserPreferences(): DomainUserPreferences {
        return DomainUserPreferences(
            favoriteCategories = favoriteCategories,
            preferredDifficulty = preferredDifficulty,
            notificationsEnabled = notificationsEnabled,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            themeMode = themeMode,
            language = language
        )
    }

    private fun Achievement.toDomainAchievement(): DomainAchievement {
        return DomainAchievement(
            id = id,
            title = title,
            description = description,
            iconUrl = iconUrl,
            category = when (category) {
                AchievementCategory.GAMEPLAY -> DomainAchievementCategory.GAMEPLAY
                AchievementCategory.SOCIAL -> DomainAchievementCategory.SOCIAL
                AchievementCategory.PROGRESSION -> DomainAchievementCategory.PROGRESSION
                AchievementCategory.SPECIAL -> DomainAchievementCategory.SPECIAL
                AchievementCategory.HIDDEN -> DomainAchievementCategory.HIDDEN
            },
            rarity = when (rarity) {
                AchievementRarity.COMMON -> DomainAchievementRarity.COMMON
                AchievementRarity.RARE -> DomainAchievementRarity.RARE
                AchievementRarity.EPIC -> DomainAchievementRarity.EPIC
                AchievementRarity.LEGENDARY -> DomainAchievementRarity.LEGENDARY
            },
            points = points,
            unlockedAt = unlockedAt,
            progress = progress,
            maxProgress = maxProgress,
            isSecret = isSecret,
            requirements = requirements.map { req ->
                com.roshni.games.feature.profile.domain.model.AchievementRequirement(
                    type = when (req.type) {
                        com.roshni.games.feature.profile.data.model.RequirementType.GAMES_PLAYED -> com.roshni.games.feature.profile.domain.model.RequirementType.GAMES_PLAYED
                        com.roshni.games.feature.profile.data.model.RequirementType.SCORE_ACHIEVED -> com.roshni.games.feature.profile.domain.model.RequirementType.SCORE_ACHIEVED
                        com.roshni.games.feature.profile.data.model.RequirementType.TIME_PLAYED -> com.roshni.games.feature.profile.domain.model.RequirementType.TIME_PLAYED
                        com.roshni.games.feature.profile.data.model.RequirementType.STREAK_DAYS -> com.roshni.games.feature.profile.domain.model.RequirementType.STREAK_DAYS
                        com.roshni.games.feature.profile.data.model.RequirementType.GAMES_COMPLETED -> com.roshni.games.feature.profile.domain.model.RequirementType.GAMES_COMPLETED
                    },
                    target = req.target,
                    currentValue = req.currentValue,
                    requiredValue = req.requiredValue,
                    description = req.description
                )
            }
        )
    }

    private fun GameStatistics.toDomainGameStatistics(): DomainGameStatistics {
        return DomainGameStatistics(
            totalGamesPlayed = totalGamesPlayed,
            totalPlayTime = totalPlayTime,
            averageScore = averageScore,
            highestScore = highestScore,
            gamesCompleted = gamesCompleted,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            favoriteCategory = favoriteCategory,
            categoryStats = categoryStats.mapValues { (_, stats) ->
                com.roshni.games.feature.profile.domain.model.CategoryStats(
                    category = stats.category,
                    gamesPlayed = stats.gamesPlayed,
                    bestScore = stats.bestScore,
                    totalPlayTime = stats.totalPlayTime,
                    averageScore = stats.averageScore
                )
            },
            difficultyStats = difficultyStats.mapValues { (_, stats) ->
                com.roshni.games.feature.profile.domain.model.DifficultyStats(
                    difficulty = stats.difficulty,
                    gamesPlayed = stats.gamesPlayed,
                    winRate = stats.winRate,
                    averageScore = stats.averageScore
                )
            },
            monthlyStats = monthlyStats.map { stats ->
                com.roshni.games.feature.profile.domain.model.MonthlyStats(
                    month = stats.month,
                    gamesPlayed = stats.gamesPlayed,
                    totalScore = stats.totalScore,
                    playTime = stats.playTime,
                    achievementsUnlocked = stats.achievementsUnlocked
                )
            },
            lastUpdated = lastUpdated
        )
    }

    private fun ProfileActivity.toDomainProfileActivity(): DomainProfileActivity {
        return DomainProfileActivity(
            id = id,
            type = when (type) {
                ActivityType.GAME_COMPLETED -> DomainActivityType.GAME_COMPLETED
                ActivityType.ACHIEVEMENT_UNLOCKED -> DomainActivityType.ACHIEVEMENT_UNLOCKED
                ActivityType.HIGH_SCORE -> DomainActivityType.HIGH_SCORE
                ActivityType.LEVEL_UP -> DomainActivityType.LEVEL_UP
                ActivityType.STREAK_MILESTONE -> DomainActivityType.STREAK_MILESTONE
            },
            title = title,
            description = description,
            timestamp = timestamp,
            gameId = gameId,
            score = score,
            achievementId = achievementId
        )
    }

    private fun ProfileCustomization.toDomainProfileCustomization(): DomainProfileCustomization {
        return DomainProfileCustomization(
            avatarOptions = avatarOptions.map { option ->
                com.roshni.games.feature.profile.domain.model.AvatarOption(
                    id = option.id,
                    name = option.name,
                    iconUrl = option.iconUrl,
                    color = option.color,
                    isPremium = option.isPremium,
                    isUnlocked = option.isUnlocked
                )
            },
            selectedAvatarId = selectedAvatarId,
            backgroundColor = backgroundColor,
            accentColor = accentColor,
            showStats = showStats,
            showAchievements = showAchievements,
            showActivity = showActivity,
            profileVisibility = when (profileVisibility) {
                ProfileVisibility.PUBLIC -> DomainProfileVisibility.PUBLIC
                ProfileVisibility.FRIENDS_ONLY -> DomainProfileVisibility.FRIENDS_ONLY
                ProfileVisibility.PRIVATE -> DomainProfileVisibility.PRIVATE
            }
        )
    }

    // Reverse conversions (Domain to Data)
    private fun DomainUserProfile.toDataUserProfile(): UserProfile {
        return UserProfile(
            id = id,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            joinDate = joinDate,
            lastActive = lastActive,
            isOnline = isOnline,
            level = level,
            experience = experience,
            experienceToNextLevel = experienceToNextLevel,
            preferences = preferences.toDataUserPreferences()
        )
    }

    private fun DomainUserPreferences.toDataUserPreferences(): UserPreferences {
        return UserPreferences(
            favoriteCategories = favoriteCategories,
            preferredDifficulty = preferredDifficulty,
            notificationsEnabled = notificationsEnabled,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            themeMode = themeMode,
            language = language
        )
    }

    private fun DomainProfileCustomization.toDataProfileCustomization(): ProfileCustomization {
        return ProfileCustomization(
            avatarOptions = avatarOptions.map { option ->
                com.roshni.games.feature.profile.data.model.AvatarOption(
                    id = option.id,
                    name = option.name,
                    iconUrl = option.iconUrl,
                    color = option.color,
                    isPremium = option.isPremium,
                    isUnlocked = option.isUnlocked
                )
            },
            selectedAvatarId = selectedAvatarId,
            backgroundColor = backgroundColor,
            accentColor = accentColor,
            showStats = showStats,
            showAchievements = showAchievements,
            showActivity = showActivity,
            profileVisibility = when (profileVisibility) {
                DomainProfileVisibility.PUBLIC -> ProfileVisibility.PUBLIC
                DomainProfileVisibility.FRIENDS_ONLY -> ProfileVisibility.FRIENDS_ONLY
                DomainProfileVisibility.PRIVATE -> ProfileVisibility.PRIVATE
            }
        )
    }
}