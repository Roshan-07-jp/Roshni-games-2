package com.roshni.games.feature.home.data.datasource

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.core.database.model.GameEntity
import com.roshni.games.feature.home.data.model.AchievementHighlight
import com.roshni.games.feature.home.data.model.GameCategory
import com.roshni.games.feature.home.data.model.GameRecommendation
import com.roshni.games.feature.home.data.model.HomeScreenData
import com.roshni.games.feature.home.data.model.RecentlyPlayedGame
import com.roshni.games.feature.home.data.model.RecommendationReason
import com.roshni.games.feature.home.data.model.UserStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.random.Random

interface HomeDataSource {
    fun getHomeScreenData(): Flow<HomeScreenData>
    suspend fun refreshHomeData(): Boolean
    suspend fun getRecentlyPlayedGames(limit: Int = 5): List<RecentlyPlayedGame>
    suspend fun getGameRecommendations(limit: Int = 10): List<GameRecommendation>
    suspend fun getPopularCategories(): List<GameCategory>
    suspend fun getAchievementHighlights(limit: Int = 3): List<AchievementHighlight>
    suspend fun getUserStats(): UserStats
}

class HomeDataSourceImpl @Inject constructor(
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val scoreDao: ScoreDao
) : HomeDataSource {

    override fun getHomeScreenData(): Flow<HomeScreenData> = flow {
        // Simulate loading delay
        delay(800)

        val recentlyPlayedGames = getRecentlyPlayedGames()
        val recommendedGames = getGameRecommendations()
        val popularCategories = getPopularCategories()
        val achievementHighlights = getAchievementHighlights()
        val userStats = getUserStats()

        val homeData = HomeScreenData(
            recentlyPlayedGames = recentlyPlayedGames,
            recommendedGames = recommendedGames,
            popularCategories = popularCategories,
            achievementHighlights = achievementHighlights,
            userStats = userStats,
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )

        emit(homeData)
    }

    override suspend fun refreshHomeData(): Boolean {
        return try {
            delay(500)
            // In real implementation, this would refresh data from network
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRecentlyPlayedGames(limit: Int): List<RecentlyPlayedGame> {
        return try {
            // Get games from database and simulate recent play data
            val games = gameDao.getActiveGames().take(limit)

            games.map { game ->
                RecentlyPlayedGame(
                    game = game,
                    lastPlayed = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).minus(
                        Random.nextLong(1, 24 * 7) // Random time within last week
                    ),
                    playTime = Random.nextLong(15, 180), // 15 minutes to 3 hours
                    score = Random.nextInt(100, 10000),
                    level = Random.nextInt(1, 50)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGameRecommendations(limit: Int): List<GameRecommendation> {
        return try {
            val games = gameDao.getActiveGames().take(limit)

            games.map { game ->
                val reasons = RecommendationReason.values()
                GameRecommendation(
                    game = game,
                    reason = reasons[Random.nextInt(reasons.size)],
                    confidence = Random.nextFloat(),
                    personalizedScore = Random.nextFloat()
                )
            }.sortedByDescending { it.personalizedScore }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularCategories(): List<GameCategory> {
        return listOf(
            GameCategory(
                id = "puzzle",
                name = "Puzzle",
                iconUrl = null,
                gameCount = 25,
                isPopular = true
            ),
            GameCategory(
                id = "action",
                name = "Action",
                iconUrl = null,
                gameCount = 18,
                isPopular = true
            ),
            GameCategory(
                id = "strategy",
                name = "Strategy",
                iconUrl = null,
                gameCount = 12,
                isPopular = false
            ),
            GameCategory(
                id = "casual",
                name = "Casual",
                iconUrl = null,
                gameCount = 30,
                isPopular = true
            ),
            GameCategory(
                id = "adventure",
                name = "Adventure",
                iconUrl = null,
                gameCount = 15,
                isPopular = false
            )
        )
    }

    override suspend fun getAchievementHighlights(limit: Int): List<AchievementHighlight> {
        return listOf(
            AchievementHighlight(
                id = "first_win",
                title = "First Victory",
                description = "Win your first game",
                iconUrl = null,
                rarity = com.roshni.games.feature.home.data.model.AchievementRarity.COMMON,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                progress = 1.0f
            ),
            AchievementHighlight(
                id = "speed_demon",
                title = "Speed Demon",
                description = "Complete a level in under 30 seconds",
                iconUrl = null,
                rarity = com.roshni.games.feature.home.data.model.AchievementRarity.RARE,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                progress = 1.0f
            ),
            AchievementHighlight(
                id = "perfectionist",
                title = "Perfectionist",
                description = "Achieve a perfect score",
                iconUrl = null,
                rarity = com.roshni.games.feature.home.data.model.AchievementRarity.EPIC,
                unlockedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                progress = 1.0f
            )
        ).take(limit)
    }

    override suspend fun getUserStats(): UserStats {
        return UserStats(
            gamesPlayed = 42,
            totalPlayTime = 1800, // 30 hours
            averageScore = 7500f,
            achievementsUnlocked = 15,
            currentStreak = 7,
            favoriteCategory = "Puzzle"
        )
    }
}