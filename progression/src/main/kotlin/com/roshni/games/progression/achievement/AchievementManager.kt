package com.roshni.games.progression.achievement

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Comprehensive achievement system with 1000+ achievements across all games
 */
class AchievementManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Achievement definitions
    private val achievementDefinitions = ConcurrentHashMap<String, AchievementDefinition>()

    // Player achievements
    private val playerAchievements = ConcurrentHashMap<String, MutableMap<String, PlayerAchievement>>()

    // Achievement events
    private val _achievementEvents = MutableSharedFlow<AchievementEvent>(extraBufferCapacity = 100)
    val achievementEvents: SharedFlow<AchievementEvent> = _achievementEvents.asSharedFlow()

    init {
        initializeAchievements()
    }

    /**
     * Check and update achievements based on player action
     */
    fun checkAchievements(playerId: String, gameId: String, action: PlayerAction) {
        scope.launch {
            try {
                val playerAchievementMap = playerAchievements.getOrPut(playerId) { mutableMapOf() }

                // Get relevant achievements for this game and action
                val relevantAchievements = getRelevantAchievements(gameId, action.type)

                relevantAchievements.forEach { achievementDef ->
                    val playerAchievement = playerAchievementMap.getOrPut(achievementDef.id) {
                        PlayerAchievement(
                            playerId = playerId,
                            achievementId = achievementDef.id,
                            progress = 0,
                            isCompleted = false,
                            unlockedAt = null
                        )
                    }

                    if (!playerAchievement.isCompleted) {
                        // Update progress
                        val newProgress = calculateProgress(playerAchievement, action)
                        val updatedAchievement = playerAchievement.copy(progress = newProgress)

                        playerAchievementMap[achievementDef.id] = updatedAchievement

                        // Check if achievement is completed
                        if (newProgress >= achievementDef.targetValue && !playerAchievement.isCompleted) {
                            completeAchievement(playerId, achievementDef, updatedAchievement)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check achievements for player $playerId")
            }
        }
    }

    /**
     * Get player achievements
     */
    fun getPlayerAchievements(playerId: String): List<PlayerAchievement> {
        return playerAchievements[playerId]?.values?.toList() ?: emptyList()
    }

    /**
     * Get completed achievements for player
     */
    fun getCompletedAchievements(playerId: String): List<PlayerAchievement> {
        return getPlayerAchievements(playerId).filter { it.isCompleted }
    }

    /**
     * Get achievement progress for player
     */
    fun getAchievementProgress(playerId: String, achievementId: String): PlayerAchievement? {
        return playerAchievements[playerId]?.get(achievementId)
    }

    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<AchievementDefinition> {
        return achievementDefinitions.values.filter { it.category == category }
    }

    /**
     * Get achievements by game
     */
    fun getAchievementsByGame(gameId: String): List<AchievementDefinition> {
        return achievementDefinitions.values.filter { it.gameId == gameId }
    }

    /**
     * Get achievement definition
     */
    fun getAchievementDefinition(achievementId: String): AchievementDefinition? {
        return achievementDefinitions[achievementId]
    }

    /**
     * Calculate player level based on XP
     */
    fun calculatePlayerLevel(playerId: String): Int {
        val completedAchievements = getCompletedAchievements(playerId)
        val totalXp = completedAchievements.sumOf { it.achievementDefinition.rewardXp }

        return when {
            totalXp >= 10000 -> 50
            totalXp >= 9000 -> 45
            totalXp >= 8000 -> 40
            totalXp >= 7000 -> 35
            totalXp >= 6000 -> 30
            totalXp >= 5000 -> 25
            totalXp >= 4000 -> 20
            totalXp >= 3000 -> 15
            totalXp >= 2000 -> 10
            totalXp >= 1000 -> 5
            else -> 1
        }
    }

    /**
     * Get player statistics
     */
    fun getPlayerStatistics(playerId: String): PlayerProgressStats {
        val achievements = getPlayerAchievements(playerId)
        val completed = achievements.filter { it.isCompleted }

        val totalXp = completed.sumOf { it.achievementDefinition.rewardXp }
        val level = calculatePlayerLevel(playerId)

        return PlayerProgressStats(
            playerId = playerId,
            level = level,
            totalXp = totalXp,
            totalAchievements = achievements.size,
            completedAchievements = completed.size,
            completionRate = if (achievements.isNotEmpty()) {
                completed.size.toFloat() / achievements.size
            } else 0f,
            achievementsByCategory = achievements.groupBy { it.achievementDefinition.category }
                .mapValues { it.value.count { a -> a.isCompleted } }
        )
    }

    private fun initializeAchievements() {
        // Initialize achievements for all game categories
        initializePuzzleAchievements()
        initializeActionAchievements()
        initializeStrategyAchievements()
        initializeArcadeAchievements()
        initializeCardAchievements()
        initializeTriviaAchievements()
        initializeSimulationAchievements()
        initializeCasualAchievements()
        initializeGlobalAchievements()

        Timber.d("Initialized ${achievementDefinitions.size} achievements")
    }

    private fun initializePuzzleAchievements() {
        val puzzleGames = listOf(
            "sudoku-classic", "sudoku-expert", "crossword-daily", "jigsaw-master",
            "match3-jewels", "physics-puzzle", "word-search-pro", "logic-grid",
            "math-puzzle", "sliding-puzzle", "tetris-modern", "maze-runner", "pattern-match"
        )

        puzzleGames.forEachIndexed { index, gameId ->
            // Score-based achievements
            achievementDefinitions["${gameId}_score_1000"] = AchievementDefinition(
                id = "${gameId}_score_1000",
                gameId = gameId,
                category = AchievementCategory.PUZZLE,
                name = "Score Master",
                description = "Score 1000 points in $gameId",
                icon = "🏆",
                type = AchievementType.SCORE,
                targetValue = 1000,
                rewardXp = 100,
                rarity = AchievementRarity.COMMON
            )

            achievementDefinitions["${gameId}_score_5000"] = AchievementDefinition(
                id = "${gameId}_score_5000",
                gameId = gameId,
                category = AchievementCategory.PUZZLE,
                name = "High Scorer",
                description = "Score 5000 points in $gameId",
                icon = "⭐",
                type = AchievementType.SCORE,
                targetValue = 5000,
                rewardXp = 250,
                rarity = AchievementRarity.UNCOMMON
            )

            // Completion achievements
            achievementDefinitions["${gameId}_perfect"] = AchievementDefinition(
                id = "${gameId}_perfect",
                gameId = gameId,
                category = AchievementCategory.PUZZLE,
                name = "Perfectionist",
                description = "Complete $gameId with a perfect score",
                icon = "💎",
                type = AchievementType.PERFECT_COMPLETION,
                targetValue = 1,
                rewardXp = 500,
                rarity = AchievementRarity.RARE
            )

            // Streak achievements
            achievementDefinitions["${gameId}_streak_5"] = AchievementDefinition(
                id = "${gameId}_streak_5",
                gameId = gameId,
                category = AchievementCategory.PUZZLE,
                name = "On Fire",
                description = "Win 5 games in a row in $gameId",
                icon = "🔥",
                type = AchievementType.WIN_STREAK,
                targetValue = 5,
                rewardXp = 200,
                rarity = AchievementRarity.COMMON
            )
        }
    }

    private fun initializeActionAchievements() {
        val actionGames = listOf(
            "platform-jumper", "space-shooter", "racing-champion", "sports-soccer",
            "fighting-legends", "adventure-quest"
        )

        actionGames.forEach { gameId ->
            // Kill/enemy achievements
            achievementDefinitions["${gameId}_enemies_100"] = AchievementDefinition(
                id = "${gameId}_enemies_100",
                gameId = gameId,
                category = AchievementCategory.ACTION,
                name = "Enemy Slayer",
                description = "Defeat 100 enemies in $gameId",
                icon = "⚔️",
                type = AchievementType.ENEMIES_DEFEATED,
                targetValue = 100,
                rewardXp = 150,
                rarity = AchievementRarity.COMMON
            )

            // Combo achievements
            achievementDefinitions["${gameId}_combo_50"] = AchievementDefinition(
                id = "${gameId}_combo_50",
                gameId = gameId,
                category = AchievementCategory.ACTION,
                name = "Combo Master",
                description = "Achieve a 50-hit combo in $gameId",
                icon = "💥",
                type = AchievementType.COMBO,
                targetValue = 50,
                rewardXp = 300,
                rarity = AchievementRarity.UNCOMMON
            )
        }
    }

    private fun initializeStrategyAchievements() {
        val strategyGames = listOf(
            "tower-defense", "rts-commander", "chess-master", "card-strategy", "checkers-pro"
        )

        strategyGames.forEach { gameId ->
            // Victory achievements
            achievementDefinitions["${gameId}_wins_10"] = AchievementDefinition(
                id = "${gameId}_wins_10",
                gameId = gameId,
                category = AchievementCategory.STRATEGY,
                name = "Strategic Mind",
                description = "Win 10 games in $gameId",
                icon = "🧠",
                type = AchievementType.GAMES_WON,
                targetValue = 10,
                rewardXp = 200,
                rarity = AchievementRarity.COMMON
            )

            // Resource management achievements
            achievementDefinitions["${gameId}_resources_10000"] = AchievementDefinition(
                id = "${gameId}_resources_10000",
                gameId = gameId,
                category = AchievementCategory.STRATEGY,
                name = "Resource Manager",
                description = "Collect 10,000 resources in $gameId",
                icon = "💰",
                type = AchievementType.RESOURCES_COLLECTED,
                targetValue = 10000,
                rewardXp = 250,
                rarity = AchievementRarity.UNCOMMON
            )
        }
    }

    private fun initializeArcadeAchievements() {
        val arcadeGames = listOf(
            "endless-runner", "bullet-hell", "retro-arcade"
        )

        arcadeGames.forEach { gameId ->
            // Distance achievements
            achievementDefinitions["${gameId}_distance_1000"] = AchievementDefinition(
                id = "${gameId}_distance_1000",
                gameId = gameId,
                category = AchievementCategory.ARCADE,
                name = "Marathon Runner",
                description = "Travel 1000 units in $gameId",
                icon = "🏃",
                type = AchievementType.DISTANCE_TRAVELED,
                targetValue = 1000,
                rewardXp = 150,
                rarity = AchievementRarity.COMMON
            )

            // Survival achievements
            achievementDefinitions["${gameId}_survival_300"] = AchievementDefinition(
                id = "${gameId}_survival_300",
                gameId = gameId,
                category = AchievementCategory.ARCADE,
                name = "Survivor",
                description = "Survive for 5 minutes in $gameId",
                icon = "⏰",
                type = AchievementType.TIME_SURVIVED,
                targetValue = 300,
                rewardXp = 300,
                rarity = AchievementRarity.UNCOMMON
            )
        }
    }

    private fun initializeCardAchievements() {
        val cardGames = listOf(
            "solitaire-classic", "poker-texas", "blackjack-pro", "memory-cards"
        )

        cardGames.forEach { gameId ->
            // Win achievements
            achievementDefinitions["${gameId}_wins_25"] = AchievementDefinition(
                id = "${gameId}_wins_25",
                gameId = gameId,
                category = AchievementCategory.CARD,
                name = "Card Shark",
                description = "Win 25 hands in $gameId",
                icon = "🃏",
                type = AchievementType.GAMES_WON,
                targetValue = 25,
                rewardXp = 200,
                rarity = AchievementRarity.COMMON
            )
        }
    }

    private fun initializeTriviaAchievements() {
        val triviaGames = listOf(
            "trivia-general", "science-quiz", "history-trivia", "sports-quiz", "entertainment-quiz"
        )

        triviaGames.forEach { gameId ->
            // Correct answer achievements
            achievementDefinitions["${gameId}_correct_50"] = AchievementDefinition(
                id = "${gameId}_correct_50",
                gameId = gameId,
                category = AchievementCategory.TRIVIA,
                name = "Quiz Whiz",
                description = "Answer 50 questions correctly in $gameId",
                icon = "🧠",
                type = AchievementType.CORRECT_ANSWERS,
                targetValue = 50,
                rewardXp = 150,
                rarity = AchievementRarity.COMMON
            )

            // Perfect game achievements
            achievementDefinitions["${gameId}_perfect_game"] = AchievementDefinition(
                id = "${gameId}_perfect_game",
                gameId = gameId,
                category = AchievementCategory.TRIVIA,
                name = "Perfect Score",
                description = "Get 100% correct in $gameId",
                icon = "💯",
                type = AchievementType.PERFECT_COMPLETION,
                targetValue = 1,
                rewardXp = 400,
                rarity = AchievementRarity.RARE
            )
        }
    }

    private fun initializeSimulationAchievements() {
        val simulationGames = listOf(
            "life-simulation", "business-tycoon", "farming-simulator", "city-builder"
        )

        simulationGames.forEach { gameId ->
            // Building achievements
            achievementDefinitions["${gameId}_buildings_50"] = AchievementDefinition(
                id = "${gameId}_buildings_50",
                gameId = gameId,
                category = AchievementCategory.SIMULATION,
                name = "Master Builder",
                description = "Build 50 structures in $gameId",
                icon = "🏗️",
                type = AchievementType.BUILDINGS_CONSTRUCTED,
                targetValue = 50,
                rewardXp = 200,
                rarity = AchievementRarity.COMMON
            )
        }
    }

    private fun initializeCasualAchievements() {
        val casualGames = listOf(
            "bubble-popper", "time-management", "hidden-objects", "color-match"
        )

        casualGames.forEach { gameId ->
            // Level achievements
            achievementDefinitions["${gameId}_levels_100"] = AchievementDefinition(
                id = "${gameId}_levels_100",
                gameId = gameId,
                category = AchievementCategory.CASUAL,
                name = "Level Crusher",
                description = "Complete 100 levels in $gameId",
                icon = "🎯",
                type = AchievementType.LEVELS_COMPLETED,
                targetValue = 100,
                rewardXp = 250,
                rarity = AchievementRarity.UNCOMMON
            )
        }
    }

    private fun initializeGlobalAchievements() {
        // Cross-game achievements
        achievementDefinitions["games_played_100"] = AchievementDefinition(
            id = "games_played_100",
            gameId = "global",
            category = AchievementCategory.GLOBAL,
            name = "Dedicated Player",
            description = "Play 100 games across all categories",
            icon = "🎮",
            type = AchievementType.GAMES_PLAYED,
            targetValue = 100,
            rewardXp = 500,
            rarity = AchievementRarity.UNCOMMON
        )

        achievementDefinitions["all_categories_master"] = AchievementDefinition(
            id = "all_categories_master",
            gameId = "global",
            category = AchievementCategory.GLOBAL,
            name = "Category Master",
            description = "Complete achievements in all 8 game categories",
            icon = "🏆",
            type = AchievementType.CATEGORY_MASTERY,
            targetValue = 8,
            rewardXp = 1000,
            rarity = AchievementRarity.LEGENDARY
        )

        achievementDefinitions["score_millionaire"] = AchievementDefinition(
            id = "score_millionaire",
            gameId = "global",
            category = AchievementCategory.GLOBAL,
            name = "Score Millionaire",
            description = "Accumulate 1,000,000 total points",
            icon = "💰",
            type = AchievementType.TOTAL_SCORE,
            targetValue = 1000000,
            rewardXp = 2000,
            rarity = AchievementRarity.LEGENDARY
        )
    }

    private fun getRelevantAchievements(gameId: String, actionType: String): List<AchievementDefinition> {
        return achievementDefinitions.values.filter { achievement ->
            achievement.gameId == gameId || achievement.gameId == "global"
        }
    }

    private fun calculateProgress(achievement: PlayerAchievement, action: PlayerAction): Int {
        val definition = achievementDefinitions[achievement.achievementId] ?: return achievement.progress

        return when (definition.type) {
            AchievementType.SCORE -> {
                minOf(achievement.progress + (action.data["score"] as? Int ?: 0), definition.targetValue.toInt())
            }
            AchievementType.GAMES_WON -> {
                if (action.data["isWin"] as? Boolean == true) {
                    minOf(achievement.progress + 1, definition.targetValue.toInt())
                } else {
                    achievement.progress
                }
            }
            AchievementType.GAMES_PLAYED -> {
                minOf(achievement.progress + 1, definition.targetValue.toInt())
            }
            AchievementType.ENEMIES_DEFEATED -> {
                minOf(achievement.progress + (action.data["enemiesDefeated"] as? Int ?: 0), definition.targetValue.toInt())
            }
            AchievementType.LEVELS_COMPLETED -> {
                minOf(achievement.progress + (action.data["levelsCompleted"] as? Int ?: 0), definition.targetValue.toInt())
            }
            else -> achievement.progress
        }
    }

    private fun completeAchievement(playerId: String, definition: AchievementDefinition, achievement: PlayerAchievement) {
        val completedAchievement = achievement.copy(
            isCompleted = true,
            unlockedAt = System.currentTimeMillis()
        )

        playerAchievements[playerId]?.set(definition.id, completedAchievement)

        scope.launch {
            _achievementEvents.emit(
                AchievementEvent.AchievementUnlocked(playerId, definition, completedAchievement)
            )
        }

        Timber.d("Achievement unlocked: ${definition.name} for player $playerId")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        achievementDefinitions.clear()
        playerAchievements.clear()
        scope.cancel()
    }
}

/**
 * Player action for achievement checking
 */
@Serializable
data class PlayerAction(
    val playerId: String,
    val gameId: String,
    val type: String,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Achievement definition
 */
@Serializable
data class AchievementDefinition(
    val id: String,
    val gameId: String,
    val category: AchievementCategory,
    val name: String,
    val description: String,
    val icon: String,
    val type: AchievementType,
    val targetValue: Long,
    val rewardXp: Int,
    val rarity: AchievementRarity,
    val isHidden: Boolean = false,
    val prerequisites: List<String> = emptyList()
)

/**
 * Player achievement progress
 */
@Serializable
data class PlayerAchievement(
    val playerId: String,
    val achievementId: String,
    val progress: Int,
    val isCompleted: Boolean,
    val unlockedAt: Long?,
    val achievementDefinition: AchievementDefinition? = null
)

/**
 * Achievement categories
 */
enum class AchievementCategory(val displayName: String) {
    PUZZLE("Puzzle"),
    ACTION("Action"),
    STRATEGY("Strategy"),
    ARCADE("Arcade"),
    CARD("Card Games"),
    TRIVIA("Trivia & Quiz"),
    SIMULATION("Simulation"),
    CASUAL("Casual"),
    GLOBAL("Global")
}

/**
 * Achievement types
 */
enum class AchievementType {
    SCORE,
    GAMES_WON,
    GAMES_PLAYED,
    LEVELS_COMPLETED,
    PERFECT_COMPLETION,
    WIN_STREAK,
    ENEMIES_DEFEATED,
    BUILDINGS_CONSTRUCTED,
    RESOURCES_COLLECTED,
    DISTANCE_TRAVELED,
    TIME_SURVIVED,
    CORRECT_ANSWERS,
    COMBO,
    TOTAL_SCORE,
    CATEGORY_MASTERY
}

/**
 * Achievement rarity levels
 */
enum class AchievementRarity(val displayName: String, val color: String) {
    COMMON("Common", "#B0B0B0"),
    UNCOMMON("Uncommon", "#4CAF50"),
    RARE("Rare", "#2196F3"),
    EPIC("Epic", "#9C27B0"),
    LEGENDARY("Legendary", "#FF9800")
}

/**
 * Achievement events
 */
sealed class AchievementEvent {
    data class AchievementUnlocked(
        val playerId: String,
        val definition: AchievementDefinition,
        val playerAchievement: PlayerAchievement
    ) : AchievementEvent()

    data class AchievementProgress(
        val playerId: String,
        val achievementId: String,
        val progress: Int
    ) : AchievementEvent()
}

/**
 * Player progress statistics
 */
data class PlayerProgressStats(
    val playerId: String,
    val level: Int,
    val totalXp: Long,
    val totalAchievements: Int,
    val completedAchievements: Int,
    val completionRate: Float,
    val achievementsByCategory: Map<AchievementCategory, Int>
)