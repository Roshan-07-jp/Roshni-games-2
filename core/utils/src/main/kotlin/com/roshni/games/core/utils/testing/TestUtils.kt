package com.roshni.games.core.utils.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Testing utilities for Roshni Games
 */

/**
 * Test coroutine dispatcher rule for JUnit 4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Test coroutine dispatcher for JUnit 5
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestDispatcherProvider {
    val testDispatcher = StandardTestDispatcher()

    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    fun teardown() {
        Dispatchers.resetMain()
    }
}

/**
 * Mock data providers for testing
 */
object MockDataProvider {

    fun createMockGameModule() = com.roshni.games.service.gameloader.data.model.GameModule(
        id = "test-game-1",
        name = "Test Puzzle Game",
        version = "1.0.0",
        description = "A test puzzle game for unit testing",
        author = "Test Author",
        category = "Puzzle",
        difficulty = com.roshni.games.service.gameloader.data.model.GameDifficulty.MEDIUM,
        minPlayers = 1,
        maxPlayers = 4,
        estimatedDuration = 15,
        entryPoint = "com.test.TestGameActivity",
        tags = listOf("test", "puzzle")
    )

    fun createMockSyncOperation() = com.roshni.games.service.backgroundsync.data.model.SyncOperation(
        id = "test-sync-1",
        type = com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_SCORE,
        data = mapOf("score" to 1000, "gameId" to "test-game"),
        timestamp = System.currentTimeMillis(),
        priority = com.roshni.games.service.backgroundsync.data.model.SyncPriority.NORMAL
    )

    fun createMockAchievement() = com.roshni.games.feature.achievements.data.model.Achievement(
        id = "test-achievement-1",
        name = "Test Achievement",
        description = "A test achievement for unit testing",
        category = com.roshni.games.feature.achievements.data.model.AchievementCategory.GAMEPLAY,
        type = com.roshni.games.feature.achievements.data.model.AchievementType.SCORE_BASED,
        targetValue = 1000f,
        points = 50,
        rarity = com.roshni.games.feature.achievements.data.model.AchievementRarity.COMMON
    )

    fun createMockLeaderboardEntry() = com.roshni.games.feature.leaderboard.data.model.LeaderboardEntry(
        id = "test-entry-1",
        playerId = "test-player-1",
        playerName = "Test Player",
        score = 10000,
        rank = 1,
        achievedAt = System.currentTimeMillis()
    )

    fun createMockSocialProfile() = com.roshni.games.feature.social.data.model.SocialProfile(
        id = "test-player-1",
        name = "TestPlayer",
        displayName = "Test Player",
        level = 25,
        experience = 50000,
        totalScore = 250000,
        gamesPlayed = 75,
        achievementsUnlocked = 45,
        friendsCount = 12,
        isOnline = true
    )
}

/**
 * Test assertion utilities
 */
object TestAssertions {

    fun assertGameModuleValid(module: com.roshni.games.service.gameloader.data.model.GameModule) {
        assert(module.id.isNotBlank()) { "Game module ID should not be blank" }
        assert(module.name.isNotBlank()) { "Game module name should not be blank" }
        assert(module.version.isNotBlank()) { "Game module version should not be blank" }
        assert(module.entryPoint.isNotBlank()) { "Game module entry point should not be blank" }
        assert(module.minPlayers > 0) { "Minimum players should be greater than 0" }
        assert(module.maxPlayers >= module.minPlayers) { "Maximum players should be >= minimum players" }
    }

    fun assertSyncOperationValid(operation: com.roshni.games.service.backgroundsync.data.model.SyncOperation) {
        assert(operation.id.isNotBlank()) { "Sync operation ID should not be blank" }
        assert(operation.timestamp > 0) { "Sync operation timestamp should be valid" }
        assert(operation.maxRetries >= 0) { "Max retries should be non-negative" }
        assert(operation.retryCount <= operation.maxRetries) { "Current retry count should not exceed max retries" }
    }

    fun assertAchievementValid(achievement: com.roshni.games.feature.achievements.data.model.Achievement) {
        assert(achievement.id.isNotBlank()) { "Achievement ID should not be blank" }
        assert(achievement.name.isNotBlank()) { "Achievement name should not be blank" }
        assert(achievement.targetValue > 0) { "Achievement target value should be positive" }
        assert(achievement.points >= 0) { "Achievement points should be non-negative" }
    }

    fun assertLeaderboardEntryValid(entry: com.roshni.games.feature.leaderboard.data.model.LeaderboardEntry) {
        assert(entry.id.isNotBlank()) { "Leaderboard entry ID should not be blank" }
        assert(entry.playerId.isNotBlank()) { "Player ID should not be blank" }
        assert(entry.playerName.isNotBlank()) { "Player name should not be blank" }
        assert(entry.score >= 0) { "Score should be non-negative" }
        assert(entry.rank > 0) { "Rank should be positive" }
        assert(entry.achievedAt > 0) { "Achieved timestamp should be valid" }
    }

    fun assertSocialProfileValid(profile: com.roshni.games.feature.social.data.model.SocialProfile) {
        assert(profile.id.isNotBlank()) { "Social profile ID should not be blank" }
        assert(profile.name.isNotBlank()) { "Social profile name should not be blank" }
        assert(profile.level >= 1) { "Level should be at least 1" }
        assert(profile.experience >= 0) { "Experience should be non-negative" }
        assert(profile.totalScore >= 0) { "Total score should be non-negative" }
        assert(profile.gamesPlayed >= 0) { "Games played should be non-negative" }
        assert(profile.friendsCount >= 0) { "Friends count should be non-negative" }
    }
}

/**
 * Test constants
 */
object TestConstants {
    const val TEST_PLAYER_ID = "test-player-123"
    const val TEST_GAME_ID = "test-game-456"
    const val TEST_SESSION_ID = "test-session-789"
    const val TEST_TIMEOUT = 10_000L // 10 seconds
}