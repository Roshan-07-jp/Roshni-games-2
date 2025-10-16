package com.roshni.games.feature.profile.data.datasource

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.feature.profile.data.model.Achievement
import com.roshni.games.feature.profile.data.model.AchievementCategory
import com.roshni.games.feature.profile.data.model.AchievementRarity
import com.roshni.games.feature.profile.data.model.ActivityType
import com.roshni.games.feature.profile.data.model.CategoryStats
import com.roshni.games.feature.profile.data.model.DifficultyStats
import com.roshni.games.feature.profile.data.model.GameStatistics
import com.roshni.games.feature.profile.data.model.ProfileActivity
import com.roshni.games.feature.profile.data.model.ProfileCustomization
import com.roshni.games.feature.profile.data.model.UserPreferences
import com.roshni.games.feature.profile.data.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.random.Random

interface ProfileDataSource {
    fun getUserProfile(): Flow<UserProfile>
    fun getGameStatistics(): Flow<GameStatistics>
    fun getAchievements(): Flow<List<Achievement>>
    fun getRecentActivity(limit: Int = 10): Flow<List<ProfileActivity>>
    fun getProfileCustomization(): Flow<ProfileCustomization>
    suspend fun updateUserProfile(profile: UserProfile): Boolean
    suspend fun updateUserPreferences(preferences: UserPreferences): Boolean
    suspend fun updateProfileCustomization(customization: ProfileCustomization): Boolean
    suspend fun unlockAchievement(achievementId: String): Boolean
    suspend fun refreshProfileData(): Boolean
}

class ProfileDataSourceImpl @Inject constructor(
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val scoreDao: ScoreDao
) : ProfileDataSource {

    override fun getUserProfile(): Flow<UserProfile> = flow {
        delay(300)

        val profile = UserProfile(
            id = "user_123",
            displayName = "Alex Gamer",
            email = "alex@example.com",
            avatarUrl = null,
            bio = "Passionate gamer who loves puzzle and strategy games!",
            joinDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(365, kotlinx.datetime.DateTimeUnit.DAY),
            lastActive = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isOnline = true,
            level = 15,
            experience = 8750,
            experienceToNextLevel = 1000,
            preferences = UserPreferences(
                favoriteCategories = listOf("Puzzle", "Strategy"),
                preferredDifficulty = "Medium",
                notificationsEnabled = true,
                soundEnabled = true,
                vibrationEnabled = true
            )
        )

        emit(profile)
    }

    override fun getGameStatistics(): Flow<GameStatistics> = flow {
        delay(400)

        val stats = GameStatistics(
            totalGamesPlayed = 156,
            totalPlayTime = 2340, // 39 hours
            averageScore = 8750f,
            highestScore = 45600,
            gamesCompleted = 89,
            currentStreak = 7,
            longestStreak = 23,
            favoriteCategory = "Puzzle",
            categoryStats = mapOf(
                "Puzzle" to CategoryStats(
                    category = "Puzzle",
                    gamesPlayed = 45,
                    bestScore = 45600,
                    totalPlayTime = 720,
                    averageScore = 9200f
                ),
                "Strategy" to CategoryStats(
                    category = "Strategy",
                    gamesPlayed = 38,
                    bestScore = 32100,
                    totalPlayTime = 680,
                    averageScore = 8500f
                ),
                "Action" to CategoryStats(
                    category = "Action",
                    gamesPlayed = 32,
                    bestScore = 28900,
                    totalPlayTime = 540,
                    averageScore = 7800f
                )
            ),
            difficultyStats = mapOf(
                "Easy" to DifficultyStats(
                    difficulty = "Easy",
                    gamesPlayed = 45,
                    winRate = 0.95f,
                    averageScore = 8500f
                ),
                "Medium" to DifficultyStats(
                    difficulty = "Medium",
                    gamesPlayed = 78,
                    winRate = 0.82f,
                    averageScore = 8900f
                ),
                "Hard" to DifficultyStats(
                    difficulty = "Hard",
                    gamesPlayed = 33,
                    winRate = 0.67f,
                    averageScore = 9200f
                )
            ),
            monthlyStats = listOf(
                com.roshni.games.feature.profile.data.model.MonthlyStats(
                    month = "2024-01",
                    gamesPlayed = 23,
                    totalScore = 189000,
                    playTime = 345,
                    achievementsUnlocked = 3
                ),
                com.roshni.games.feature.profile.data.model.MonthlyStats(
                    month = "2023-12",
                    gamesPlayed = 28,
                    totalScore = 245000,
                    playTime = 420,
                    achievementsUnlocked = 5
                )
            )
        )

        emit(stats)
    }

    override fun getAchievements(): Flow<List<Achievement>> = flow {
        delay(500)

        val achievements = listOf(
            Achievement(
                id = "first_win",
                title = "First Victory",
                description = "Win your first game",
                iconUrl = null,
                category = AchievementCategory.GAMEPLAY,
                rarity = AchievementRarity.COMMON,
                points = 10,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(30, kotlinx.datetime.DateTimeUnit.DAY),
                progress = 1.0f,
                maxProgress = 1.0f
            ),
            Achievement(
                id = "speed_demon",
                title = "Speed Demon",
                description = "Complete a level in under 30 seconds",
                iconUrl = null,
                category = AchievementCategory.GAMEPLAY,
                rarity = AchievementRarity.RARE,
                points = 25,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(15, kotlinx.datetime.DateTimeUnit.DAY),
                progress = 1.0f,
                maxProgress = 1.0f
            ),
            Achievement(
                id = "perfectionist",
                title = "Perfectionist",
                description = "Achieve a perfect score",
                iconUrl = null,
                category = AchievementCategory.GAMEPLAY,
                rarity = AchievementRarity.EPIC,
                points = 50,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(7, kotlinx.datetime.DateTimeUnit.DAY),
                progress = 1.0f,
                maxProgress = 1.0f
            ),
            Achievement(
                id = "streak_master",
                title = "Streak Master",
                description = "Maintain a 7-day playing streak",
                iconUrl = null,
                category = AchievementCategory.PROGRESSION,
                rarity = AchievementRarity.RARE,
                points = 30,
                unlockedAt = null,
                progress = 0.8f,
                maxProgress = 1.0f
            ),
            Achievement(
                id = "social_butterfly",
                title = "Social Butterfly",
                description = "Play with 10 different friends",
                iconUrl = null,
                category = AchievementCategory.SOCIAL,
                rarity = AchievementRarity.EPIC,
                points = 40,
                unlockedAt = null,
                progress = 0.3f,
                maxProgress = 1.0f
            )
        )

        emit(achievements)
    }

    override fun getRecentActivity(limit: Int): Flow<List<ProfileActivity>> = flow {
        delay(200)

        val activities = listOf(
            ProfileActivity(
                id = "activity_1",
                type = ActivityType.ACHIEVEMENT_UNLOCKED,
                title = "Achievement Unlocked!",
                description = "Unlocked 'Perfectionist' achievement",
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(2, kotlinx.datetime.DateTimeUnit.DAY),
                achievementId = "perfectionist"
            ),
            ProfileActivity(
                id = "activity_2",
                type = ActivityType.HIGH_SCORE,
                title = "New High Score!",
                description = "Set a new high score of 45,600 in Puzzle Master",
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(3, kotlinx.datetime.DateTimeUnit.DAY),
                gameId = "puzzle_master",
                score = 45600
            ),
            ProfileActivity(
                id = "activity_3",
                type = ActivityType.GAME_COMPLETED,
                title = "Game Completed",
                description = "Completed all levels in Strategy Quest",
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(5, kotlinx.datetime.DateTimeUnit.DAY),
                gameId = "strategy_quest"
            ),
            ProfileActivity(
                id = "activity_4",
                type = ActivityType.LEVEL_UP,
                title = "Level Up!",
                description = "Reached level 15",
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(7, kotlinx.datetime.DateTimeUnit.DAY)
            )
        ).take(limit)

        emit(activities)
    }

    override fun getProfileCustomization(): Flow<ProfileCustomization> = flow {
        delay(200)

        val customization = ProfileCustomization(
            selectedAvatarId = "gamer",
            backgroundColor = "#F5F5F5",
            accentColor = "#6200EE",
            showStats = true,
            showAchievements = true,
            showActivity = true,
            profileVisibility = com.roshni.games.feature.profile.data.model.ProfileVisibility.PUBLIC
        )

        emit(customization)
    }

    override suspend fun updateUserProfile(profile: UserProfile): Boolean {
        delay(300)
        return try {
            // In real implementation, save to database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Boolean {
        delay(200)
        return try {
            // In real implementation, save to database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateProfileCustomization(customization: ProfileCustomization): Boolean {
        delay(200)
        return try {
            // In real implementation, save to database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Boolean {
        delay(500)
        return try {
            // In real implementation, update achievement status in database
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refreshProfileData(): Boolean {
        delay(800)
        return try {
            // In real implementation, sync with server
            true
        } catch (e: Exception) {
            false
        }
    }
}