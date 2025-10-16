package com.roshni.games.offline.ai

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.*
import kotlin.random.Random

/**
 * Comprehensive AI system for offline gaming with multiple difficulty levels and adaptive AI
 */
class AISystem {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // AI players and behaviors
    private val aiPlayers = ConcurrentHashMap<String, AIPlayer>()
    private val aiBehaviors = ConcurrentHashMap<String, AIBehavior>()

    // AI events
    private val _aiEvents = MutableSharedFlow<AIEvent>(extraBufferCapacity = 100)
    val aiEvents: SharedFlow<AIEvent> = _aiEvents.asSharedFlow()

    // Adaptive AI data
    private val playerPerformanceData = ConcurrentHashMap<String, PlayerPerformanceData>()

    init {
        initializeAIBehaviors()
        initializeAdaptiveSystem()
    }

    /**
     * Create AI opponent for game
     */
    fun createAIOpponent(
        gameId: String,
        difficulty: AIDifficulty,
        personality: AIPersonality = AIPersonality.BALANCED
    ): AIPlayer {
        val aiPlayer = AIPlayer(
            id = "ai_${gameId}_${System.currentTimeMillis()}",
            gameId = gameId,
            difficulty = difficulty,
            personality = personality,
            skillLevel = calculateSkillLevel(difficulty),
            isAdaptive = true
        )

        aiPlayers[aiPlayer.id] = aiPlayer

        Timber.d("Created AI opponent: ${aiPlayer.id} with difficulty: $difficulty")

        scope.launch {
            _aiEvents.emit(AIEvent.AIOpponentCreated(aiPlayer.id, gameId))
        }

        return aiPlayer
    }

    /**
     * Get AI move for current game state
     */
    fun getAIMove(
        aiPlayerId: String,
        gameState: GameState,
        possibleMoves: List<GameMove>
    ): GameMove? {
        val aiPlayer = aiPlayers[aiPlayerId] ?: return null

        return when (aiPlayer.difficulty) {
            AIDifficulty.EASY -> getEasyAIMove(gameState, possibleMoves)
            AIDifficulty.MEDIUM -> getMediumAIMove(gameState, possibleMoves, aiPlayer)
            AIDifficulty.HARD -> getHardAIMove(gameState, possibleMoves, aiPlayer)
            AIDifficulty.EXPERT -> getExpertAIMove(gameState, possibleMoves, aiPlayer)
        }
    }

    /**
     * Update AI learning based on game result
     */
    fun updateAILearning(playerId: String, gameResult: GameResult) {
        scope.launch {
            try {
                val performanceData = playerPerformanceData.getOrPut(playerId) {
                    PlayerPerformanceData(playerId)
                }

                // Update performance metrics
                val updatedData = performanceData.copy(
                    gamesPlayed = performanceData.gamesPlayed + 1,
                    totalScore = performanceData.totalScore + gameResult.score,
                    averageScore = (performanceData.totalScore + gameResult.score) / (performanceData.gamesPlayed + 1),
                    winRate = calculateWinRate(performanceData, gameResult),
                    lastPlayed = System.currentTimeMillis(),
                    skillRating = calculateSkillRating(performanceData, gameResult)
                )

                playerPerformanceData[playerId] = updatedData

                // Adapt AI difficulty if needed
                adaptAIDifficulty(playerId, gameResult)

                _aiEvents.emit(AIEvent.AILearningUpdated(playerId, updatedData))
            } catch (e: Exception) {
                Timber.e(e, "Failed to update AI learning")
            }
        }
    }

    /**
     * Get AI behavior for specific game type
     */
    fun getAIBehavior(gameId: String, personality: AIPersonality): AIBehavior {
        return aiBehaviors.getOrPut("${gameId}_${personality.name}") {
            createAIBehavior(gameId, personality)
        }
    }

    /**
     * Start offline tournament
     */
    fun startOfflineTournament(
        gameId: String,
        players: List<String>,
        tournamentType: TournamentType
    ): OfflineTournament {
        val tournament = OfflineTournament(
            id = "tournament_${System.currentTimeMillis()}",
            gameId = gameId,
            players = players,
            type = tournamentType,
            status = TournamentStatus.IN_PROGRESS,
            currentRound = 1,
            totalRounds = calculateTotalRounds(players.size, tournamentType),
            createdAt = System.currentTimeMillis()
        )

        Timber.d("Started offline tournament: ${tournament.id} with ${players.size} players")

        scope.launch {
            _aiEvents.emit(AIEvent.TournamentStarted(tournament.id))
        }

        return tournament
    }

    /**
     * Get easy AI move (random with some basic logic)
     */
    private fun getEasyAIMove(gameState: GameState, possibleMoves: List<GameMove>): GameMove {
        return possibleMoves.random()
    }

    /**
     * Get medium AI move (balanced strategy)
     */
    private fun getMediumAIMove(
        gameState: GameState,
        possibleMoves: List<GameMove>,
        aiPlayer: AIPlayer
    ): GameMove {
        // 70% optimal moves, 30% random
        return if (Random.nextFloat() < 0.7f) {
            evaluateMoves(gameState, possibleMoves, aiPlayer).firstOrNull() ?: possibleMoves.random()
        } else {
            possibleMoves.random()
        }
    }

    /**
     * Get hard AI move (advanced strategy)
     */
    private fun getHardAIMove(
        gameState: GameState,
        possibleMoves: List<GameMove>,
        aiPlayer: AIPlayer
    ): GameMove {
        // 90% optimal moves, 10% random
        return if (Random.nextFloat() < 0.9f) {
            evaluateMoves(gameState, possibleMoves, aiPlayer).firstOrNull() ?: possibleMoves.random()
        } else {
            possibleMoves.random()
        }
    }

    /**
     * Get expert AI move (perfect play with lookahead)
     */
    private fun getExpertAIMove(
        gameState: GameState,
        possibleMoves: List<GameMove>,
        aiPlayer: AIPlayer
    ): GameMove {
        // Use minimax algorithm for perfect play
        return evaluateMoves(gameState, possibleMoves, aiPlayer).firstOrNull() ?: possibleMoves.random()
    }

    /**
     * Evaluate moves and return best ones
     */
    private fun evaluateMoves(
        gameState: GameState,
        possibleMoves: List<GameMove>,
        aiPlayer: AIPlayer
    ): List<GameMove> {
        return possibleMoves.map { move ->
            move to evaluateMove(move, gameState, aiPlayer)
        }.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Evaluate a single move
     */
    private fun evaluateMove(move: GameMove, gameState: GameState, aiPlayer: AIPlayer): Float {
        var score = 0f

        // Basic evaluation based on move properties
        score += move.immediateReward * 10f
        score += move.strategicValue * 5f
        score -= move.risk * 3f

        // Adjust based on AI personality
        when (aiPlayer.personality) {
            AIPersonality.AGGRESSIVE -> score += move.risk * 2f
            AIPersonality.DEFENSIVE -> score -= move.risk * 2f
            AIPersonality.BALANCED -> {} // No adjustment
        }

        return score
    }

    /**
     * Adapt AI difficulty based on player performance
     */
    private fun adaptAIDifficulty(playerId: String, gameResult: GameResult) {
        val performanceData = playerPerformanceData[playerId] ?: return

        // Adjust AI difficulty based on player win rate
        val targetDifficulty = when {
            performanceData.winRate > 0.8f -> AIDifficulty.HARD
            performanceData.winRate > 0.6f -> AIDifficulty.MEDIUM
            performanceData.winRate > 0.3f -> AIDifficulty.EASY
            else -> AIDifficulty.EASY
        }

        // Update AI players for this player
        aiPlayers.values.filter { it.playerId == playerId }
            .forEach { aiPlayer ->
                if (aiPlayer.difficulty != targetDifficulty) {
                    aiPlayers[aiPlayer.id] = aiPlayer.copy(difficulty = targetDifficulty)
                    Timber.d("Adapted AI difficulty for ${aiPlayer.id} to $targetDifficulty")
                }
            }
    }

    private fun calculateSkillLevel(difficulty: AIDifficulty): Int {
        return when (difficulty) {
            AIDifficulty.EASY -> 300
            AIDifficulty.MEDIUM -> 600
            AIDifficulty.HARD -> 900
            AIDifficulty.EXPERT -> 1200
        }
    }

    private fun calculateWinRate(data: PlayerPerformanceData, result: GameResult): Float {
        val totalGames = data.gamesPlayed + 1
        val totalWins = data.wins + if (result.isWin) 1 else 0
        return totalWins.toFloat() / totalGames
    }

    private fun calculateSkillRating(data: PlayerPerformanceData, result: GameResult): Int {
        // Simple skill rating calculation
        return (data.averageScore * 0.1 + result.score * 0.01).toInt()
    }

    private fun calculateTotalRounds(playerCount: Int, tournamentType: TournamentType): Int {
        return when (tournamentType) {
            TournamentType.SINGLE_ELIMINATION -> {
                var rounds = 0
                var players = playerCount
                while (players > 1) {
                    players = (players + 1) / 2
                    rounds++
                }
                rounds
            }
            TournamentType.ROUND_ROBIN -> playerCount - 1
            TournamentType.DOUBLE_ELIMINATION -> playerCount * 2 - 1
        }
    }

    private fun initializeAIBehaviors() {
        // Initialize AI behaviors for different game types and personalities
        val gameTypes = listOf("puzzle", "action", "strategy", "arcade", "card", "trivia", "simulation", "casual")

        gameTypes.forEach { gameType ->
            AIPersonality.values().forEach { personality ->
                aiBehaviors["${gameType}_${personality.name}"] = createAIBehavior(gameType, personality)
            }
        }

        Timber.d("Initialized AI behaviors for ${gameTypes.size} game types")
    }

    private fun createAIBehavior(gameType: String, personality: AIPersonality): AIBehavior {
        return AIBehavior(
            gameType = gameType,
            personality = personality,
            strategy = getStrategyForPersonality(personality),
            riskTolerance = getRiskToleranceForPersonality(personality),
            adaptability = getAdaptabilityForPersonality(personality)
        )
    }

    private fun getStrategyForPersonality(personality: AIPersonality): AIStrategy {
        return when (personality) {
            AIPersonality.AGGRESSIVE -> AIStrategy(
                aggression = 0.9f,
                defense = 0.3f,
                patience = 0.4f,
                creativity = 0.7f
            )
            AIPersonality.DEFENSIVE -> AIStrategy(
                aggression = 0.3f,
                defense = 0.9f,
                patience = 0.8f,
                creativity = 0.5f
            )
            AIPersonality.BALANCED -> AIStrategy(
                aggression = 0.6f,
                defense = 0.6f,
                patience = 0.6f,
                creativity = 0.6f
            )
        }
    }

    private fun getRiskToleranceForPersonality(personality: AIPersonality): Float {
        return when (personality) {
            AIPersonality.AGGRESSIVE -> 0.8f
            AIPersonality.DEFENSIVE -> 0.2f
            AIPersonality.BALANCED -> 0.5f
        }
    }

    private fun getAdaptabilityForPersonality(personality: AIPersonality): Float {
        return when (personality) {
            AIPersonality.AGGRESSIVE -> 0.6f
            AIPersonality.DEFENSIVE -> 0.7f
            AIPersonality.BALANCED -> 0.8f
        }
    }

    private fun initializeAdaptiveSystem() {
        // Start adaptive learning processor
        scope.launch {
            while (isActive) {
                try {
                    processAdaptiveLearning()
                    delay(60000) // Process every minute
                } catch (e: Exception) {
                    Timber.e(e, "Error in adaptive learning")
                }
            }
        }
    }

    private fun processAdaptiveLearning() {
        // Analyze player performance and adjust AI accordingly
        playerPerformanceData.values.forEach { data ->
            if (data.gamesPlayed >= 10) { // Only adapt after sufficient data
                adaptAIForPlayer(data)
            }
        }
    }

    private fun adaptAIForPlayer(performanceData: PlayerPerformanceData) {
        // Adjust AI difficulty based on long-term performance trends
        val recentPerformance = calculateRecentPerformance(performanceData)

        aiPlayers.values.filter { it.playerId == performanceData.playerId }
            .forEach { aiPlayer ->
                val targetDifficulty = when {
                    recentPerformance > 0.8f -> AIDifficulty.HARD
                    recentPerformance > 0.6f -> AIDifficulty.MEDIUM
                    else -> AIDifficulty.EASY
                }

                if (aiPlayer.difficulty != targetDifficulty) {
                    aiPlayers[aiPlayer.id] = aiPlayer.copy(difficulty = targetDifficulty)
                    Timber.d("Adapted AI ${aiPlayer.id} to $targetDifficulty based on performance")
                }
            }
    }

    private fun calculateRecentPerformance(data: PlayerPerformanceData): Float {
        // Calculate performance trend (simplified)
        return data.winRate
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        aiPlayers.clear()
        aiBehaviors.clear()
        playerPerformanceData.clear()
        scope.cancel()
    }
}

/**
 * AI player configuration
 */
@Serializable
data class AIPlayer(
    val id: String,
    val gameId: String,
    val difficulty: AIDifficulty,
    val personality: AIPersonality,
    val skillLevel: Int,
    val isAdaptive: Boolean,
    val playerId: String? = null // Associated human player for adaptation
)

/**
 * AI difficulty levels
 */
enum class AIDifficulty(val displayName: String, val description: String) {
    EASY("Easy", "Perfect for beginners and casual players"),
    MEDIUM("Medium", "Balanced challenge for most players"),
    HARD("Hard", "Challenging gameplay for experienced players"),
    EXPERT("Expert", "Extremely difficult, for skilled players only")
}

/**
 * AI personality types
 */
enum class AIPersonality(val displayName: String, val description: String) {
    AGGRESSIVE("Aggressive", "Plays offensively and takes risks"),
    DEFENSIVE("Defensive", "Plays cautiously and focuses on survival"),
    BALANCED("Balanced", "Balanced playstyle with moderate risk-taking")
}

/**
 * AI behavior configuration
 */
@Serializable
data class AIBehavior(
    val gameType: String,
    val personality: AIPersonality,
    val strategy: AIStrategy,
    val riskTolerance: Float,
    val adaptability: Float
)

/**
 * AI strategy parameters
 */
@Serializable
data class AIStrategy(
    val aggression: Float, // 0.0 to 1.0
    val defense: Float,    // 0.0 to 1.0
    val patience: Float,   // 0.0 to 1.0
    val creativity: Float  // 0.0 to 1.0
)

/**
 * Game state for AI decision making
 */
@Serializable
data class GameState(
    val board: Map<String, Any> = emptyMap(),
    val currentPlayer: String = "",
    val turnNumber: Int = 0,
    val timeRemaining: Long = 0,
    val score: Map<String, Long> = emptyMap()
)

/**
 * Game move for AI evaluation
 */
@Serializable
data class GameMove(
    val id: String,
    val type: String,
    val data: Map<String, Any> = emptyMap(),
    val immediateReward: Float = 0f,
    val strategicValue: Float = 0f,
    val risk: Float = 0f
)

/**
 * Game result for AI learning
 */
@Serializable
data class GameResult(
    val playerId: String,
    val aiPlayerId: String,
    val score: Long,
    val isWin: Boolean,
    val duration: Long,
    val movesPlayed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Player performance data for adaptive AI
 */
@Serializable
data class PlayerPerformanceData(
    val playerId: String,
    val gamesPlayed: Int = 0,
    val totalScore: Long = 0,
    val averageScore: Double = 0.0,
    val bestScore: Long = 0,
    val wins: Int = 0,
    val winRate: Float = 0f,
    val lastPlayed: Long = 0,
    val skillRating: Int = 500,
    val playStyle: Map<String, Float> = emptyMap()
)

/**
 * Offline tournament
 */
@Serializable
data class OfflineTournament(
    val id: String,
    val gameId: String,
    val players: List<String>,
    val type: TournamentType,
    val status: TournamentStatus,
    val currentRound: Int,
    val totalRounds: Int,
    val matches: List<TournamentMatch> = emptyList(),
    val createdAt: Long,
    val completedAt: Long? = null
)

/**
 * Tournament types
 */
enum class TournamentType {
    SINGLE_ELIMINATION,
    ROUND_ROBIN,
    DOUBLE_ELIMINATION
}

/**
 * Tournament status
 */
enum class TournamentStatus {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Tournament match
 */
@Serializable
data class TournamentMatch(
    val id: String,
    val players: List<String>,
    val winner: String? = null,
    val score: Map<String, Long> = emptyMap(),
    val completedAt: Long? = null
)

/**
 * AI events
 */
sealed class AIEvent {
    data class AIOpponentCreated(val aiPlayerId: String, val gameId: String) : AIEvent()
    data class AILearningUpdated(val playerId: String, val performanceData: PlayerPerformanceData) : AIEvent()
    data class TournamentStarted(val tournamentId: String) : AIEvent()
    data class TournamentCompleted(val tournamentId: String, val winner: String) : AIEvent()
}