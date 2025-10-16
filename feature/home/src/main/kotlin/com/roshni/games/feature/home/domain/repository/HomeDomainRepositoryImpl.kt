package com.roshni.games.feature.home.domain.repository

import com.roshni.games.core.database.model.GameDifficulty
import com.roshni.games.core.utils.Result
import com.roshni.games.feature.home.data.model.AchievementHighlight
import com.roshni.games.feature.home.data.model.GameCategory
import com.roshni.games.feature.home.data.model.GameRecommendation
import com.roshni.games.feature.home.data.model.HomeScreenData
import com.roshni.games.feature.home.data.model.RecentlyPlayedGame
import com.roshni.games.feature.home.data.repository.HomeRepository
import com.roshni.games.feature.home.domain.model.AchievementRarity
import com.roshni.games.feature.home.domain.model.AchievementSummary
import com.roshni.games.feature.home.domain.model.CategorySummary
import com.roshni.games.feature.home.domain.model.GameSummary
import com.roshni.games.feature.home.domain.model.HomeScreenState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HomeDomainRepositoryImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : HomeDomainRepository {

    override fun getHomeScreenState(): Flow<HomeScreenState> {
        return homeRepository.getHomeScreenData().map { result ->
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    HomeScreenState(
                        recentlyPlayedGames = data.recentlyPlayedGames.map { it.toGameSummary() },
                        recommendedGames = data.recommendedGames.map { it.toGameRecommendation() },
                        popularCategories = data.popularCategories.map { it.toCategorySummary() },
                        achievementHighlights = data.achievementHighlights.map { it.toAchievementSummary() },
                        userStats = data.userStats.toUserStats(),
                        lastUpdated = data.lastUpdated,
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    HomeScreenState(
                        recentlyPlayedGames = emptyList(),
                        recommendedGames = emptyList(),
                        popularCategories = emptyList(),
                        achievementHighlights = emptyList(),
                        userStats = com.roshni.games.feature.home.data.model.UserStats(
                            gamesPlayed = 0,
                            totalPlayTime = 0,
                            averageScore = 0f,
                            achievementsUnlocked = 0,
                            currentStreak = 0,
                            favoriteCategory = null
                        ).toUserStats(),
                        lastUpdated = kotlinx.datetime.Clock.System.now().toLocalDateTime(
                            kotlinx.datetime.TimeZone.currentSystemDefault()
                        ),
                        isLoading = false,
                        error = result.exception.message
                    )
                }
            }
        }
    }

    override suspend fun refreshHomeData(): Boolean {
        return when (val result = homeRepository.refreshHomeData()) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    private fun RecentlyPlayedGame.toGameSummary(): GameSummary {
        return GameSummary(
            id = game.id,
            name = game.name,
            category = game.category,
            difficulty = game.difficulty.name,
            iconUrl = game.iconUrl,
            thumbnailUrl = game.thumbnailUrl,
            lastPlayed = lastPlayed,
            playTime = playTime,
            score = score,
            level = level
        )
    }

    private fun GameRecommendation.toGameRecommendation(): com.roshni.games.feature.home.domain.model.GameRecommendation {
        return com.roshni.games.feature.home.domain.model.GameRecommendation(
            game = game.toGameSummary(),
            reason = reason.name,
            confidence = confidence,
            personalizedScore = personalizedScore
        )
    }

    private fun GameCategory.toCategorySummary(): CategorySummary {
        return CategorySummary(
            id = id,
            name = name,
            iconUrl = iconUrl,
            gameCount = gameCount,
            isPopular = isPopular
        )
    }

    private fun AchievementHighlight.toAchievementSummary(): AchievementSummary {
        return AchievementSummary(
            id = id,
            title = title,
            description = description,
            iconUrl = iconUrl,
            rarity = when (rarity) {
                com.roshni.games.feature.home.data.model.AchievementRarity.COMMON -> AchievementRarity.COMMON
                com.roshni.games.feature.home.data.model.AchievementRarity.RARE -> AchievementRarity.RARE
                com.roshni.games.feature.home.data.model.AchievementRarity.EPIC -> AchievementRarity.EPIC
                com.roshni.games.feature.home.data.model.AchievementRarity.LEGENDARY -> AchievementRarity.LEGENDARY
            },
            unlockedAt = unlockedAt,
            progress = progress
        )
    }

    private fun com.roshni.games.feature.home.data.model.UserStats.toUserStats(): com.roshni.games.feature.home.domain.model.UserStats {
        return com.roshni.games.feature.home.domain.model.UserStats(
            gamesPlayed = gamesPlayed,
            totalPlayTime = totalPlayTime,
            averageScore = averageScore,
            achievementsUnlocked = achievementsUnlocked,
            currentStreak = currentStreak,
            favoriteCategory = favoriteCategory
        )
    }
}