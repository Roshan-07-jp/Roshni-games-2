package com.roshni.games.multiplayer.matchmaking

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Service for matchmaking and game session management
 */
class MatchmakingService {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active game sessions
    private val activeSessions = ConcurrentHashMap<String, GameSession>()

    // Players in matchmaking queue
    private val matchmakingQueue = ConcurrentHashMap<String, PlayerMatchmakingInfo>()

    // Matchmaking events
    private val _matchmakingEvents = MutableSharedFlow<MatchmakingEvent>(extraBufferCapacity = 100)
    val matchmakingEvents: SharedFlow<MatchmakingEvent> = _matchmakingEvents.asSharedFlow()

    // Matchmaking configuration
    private val config = MatchmakingConfig()

    init {
        // Start matchmaking processor
        startMatchmakingProcessor()
    }

    /**
     * Join matchmaking queue
     */
    fun joinMatchmaking(request: MatchmakingRequest): String {
        val playerInfo = PlayerMatchmakingInfo(
            playerId = request.playerId,
            gameId = request.gameId,
            skillLevel = request.skillLevel,
            region = request.region,
            preferredLatency = request.preferredLatency,
            joinTime = System.currentTimeMillis()
        )

        matchmakingQueue[request.playerId] = playerInfo

        Timber.d("Player ${request.playerId} joined matchmaking for game ${request.gameId}")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.PlayerJoinedQueue(request.playerId))
        }

        return request.playerId
    }

    /**
     * Leave matchmaking queue
     */
    fun leaveMatchmaking(playerId: String) {
        matchmakingQueue.remove(playerId)

        Timber.d("Player $playerId left matchmaking")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.PlayerLeftQueue(playerId))
        }
    }

    /**
     * Create game session
     */
    fun createGameSession(creator: SessionCreator): GameSession {
        val session = GameSession(
            id = generateSessionId(),
            gameId = creator.gameId,
            creatorId = creator.creatorId,
            maxPlayers = creator.maxPlayers,
            isPrivate = creator.isPrivate,
            settings = creator.settings,
            status = SessionStatus.WAITING_FOR_PLAYERS,
            createdAt = System.currentTimeMillis()
        )

        activeSessions[session.id] = session

        Timber.d("Created game session: ${session.id} for game: ${creator.gameId}")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.SessionCreated(session.id))
        }

        return session
    }

    /**
     * Join game session
     */
    fun joinGameSession(playerId: String, sessionId: String): Result<Unit> {
        val session = activeSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        if (session.status != SessionStatus.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Session is not accepting players"))
        }

        if (session.players.size >= session.maxPlayers) {
            return Result.failure(IllegalStateException("Session is full"))
        }

        // Add player to session
        val updatedSession = session.copy(
            players = session.players + playerId,
            status = if (session.players.size + 1 >= session.minPlayers) {
                SessionStatus.READY_TO_START
            } else {
                SessionStatus.WAITING_FOR_PLAYERS
            }
        )

        activeSessions[sessionId] = updatedSession

        // Remove player from matchmaking queue if present
        matchmakingQueue.remove(playerId)

        Timber.d("Player $playerId joined session $sessionId")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.PlayerJoinedSession(playerId, sessionId))
        }

        return Result.success(Unit)
    }

    /**
     * Leave game session
     */
    fun leaveGameSession(playerId: String, sessionId: String): Result<Unit> {
        val session = activeSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        val updatedPlayers = session.players - playerId
        val updatedSession = session.copy(
            players = updatedPlayers,
            status = when {
                updatedPlayers.isEmpty() -> SessionStatus.ENDED
                session.creatorId == playerId -> SessionStatus.ENDED // Creator left
                else -> session.status
            }
        )

        if (updatedSession.status == SessionStatus.ENDED) {
            activeSessions.remove(sessionId)
        } else {
            activeSessions[sessionId] = updatedSession
        }

        Timber.d("Player $playerId left session $sessionId")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.PlayerLeftSession(playerId, sessionId))
        }

        return Result.success(Unit)
    }

    /**
     * Get available game sessions for matchmaking
     */
    fun getAvailableSessions(gameId: String, playerSkill: Int): List<GameSession> {
        return activeSessions.values.filter { session ->
            session.gameId == gameId &&
            session.status == SessionStatus.WAITING_FOR_PLAYERS &&
            session.players.size < session.maxPlayers &&
            abs(session.averageSkill - playerSkill) <= config.skillTolerance
        }
    }

    /**
     * Start game session
     */
    fun startGameSession(sessionId: String, starterId: String): Result<Unit> {
        val session = activeSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        if (session.creatorId != starterId) {
            return Result.failure(IllegalArgumentException("Only session creator can start the game"))
        }

        if (session.players.size < session.minPlayers) {
            return Result.failure(IllegalStateException("Not enough players to start"))
        }

        val updatedSession = session.copy(
            status = SessionStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )

        activeSessions[sessionId] = updatedSession

        Timber.d("Game session started: $sessionId")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.SessionStarted(sessionId))
        }

        return Result.success(Unit)
    }

    /**
     * End game session
     */
    fun endGameSession(sessionId: String, results: GameResults): Result<Unit> {
        val session = activeSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        val updatedSession = session.copy(
            status = SessionStatus.COMPLETED,
            endedAt = System.currentTimeMillis(),
            results = results
        )

        // Keep completed sessions for a while for result viewing
        scope.launch {
            delay(300000) // 5 minutes
            activeSessions.remove(sessionId)
        }

        Timber.d("Game session ended: $sessionId")

        scope.launch {
            _matchmakingEvents.emit(MatchmakingEvent.SessionEnded(sessionId, results))
        }

        return Result.success(Unit)
    }

    /**
     * Get game session by ID
     */
    fun getGameSession(sessionId: String): GameSession? {
        return activeSessions[sessionId]
    }

    /**
     * Update player skill level
     */
    fun updatePlayerSkill(playerId: String, newSkillLevel: Int) {
        matchmakingQueue[playerId]?.let { playerInfo ->
            val updatedInfo = playerInfo.copy(skillLevel = newSkillLevel)
            matchmakingQueue[playerId] = updatedInfo

            Timber.d("Updated skill level for player $playerId: $newSkillLevel")
        }
    }

    /**
     * Get matchmaking statistics
     */
    fun getMatchmakingStats(): MatchmakingStats {
        val sessions = activeSessions.values
        val queuedPlayers = matchmakingQueue.values

        return MatchmakingStats(
            activeSessions = sessions.size,
            waitingForPlayers = sessions.count { it.status == SessionStatus.WAITING_FOR_PLAYERS },
            inProgress = sessions.count { it.status == SessionStatus.IN_PROGRESS },
            playersInQueue = queuedPlayers.size,
            averageWaitTime = calculateAverageWaitTime(queuedPlayers)
        )
    }

    private fun startMatchmakingProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    processMatchmaking()
                    delay(config.matchmakingInterval)
                } catch (e: Exception) {
                    Timber.e(e, "Error in matchmaking processor")
                }
            }
        }
    }

    private fun processMatchmaking() {
        val playersInQueue = matchmakingQueue.values.toList()

        // Group players by game and skill level
        val gameGroups = playersInQueue.groupBy { it.gameId }

        gameGroups.forEach { (gameId, players) ->
            // Sort players by skill level
            val sortedPlayers = players.sortedBy { it.skillLevel }

            // Try to create matches
            createMatchesForGame(gameId, sortedPlayers)
        }
    }

    private fun createMatchesForGame(gameId: String, players: List<PlayerMatchmakingInfo>) {
        if (players.size < 2) return // Need at least 2 players

        // Simple matchmaking: pair players with similar skill levels
        for (i in players.indices step 2) {
            val player1 = players[i]
            val player2 = players.getOrNull(i + 1)

            if (player2 != null) {
                val skillDifference = abs(player1.skillLevel - player2.skillLevel)

                if (skillDifference <= config.skillTolerance) {
                    // Create new session for these players
                    val creator = SessionCreator(
                        creatorId = player1.playerId,
                        gameId = gameId,
                        maxPlayers = 2,
                        isPrivate = false,
                        settings = mapOf("skillBased" to true)
                    )

                    val session = createGameSession(creator)

                    // Add both players to session
                    joinGameSession(player1.playerId, session.id)
                    joinGameSession(player2.playerId, session.id)

                    // Remove from queue
                    matchmakingQueue.remove(player1.playerId)
                    matchmakingQueue.remove(player2.playerId)

                    Timber.d("Created match between ${player1.playerId} and ${player2.playerId}")
                }
            }
        }
    }

    private fun calculateAverageWaitTime(players: Collection<PlayerMatchmakingInfo>): Long {
        if (players.isEmpty()) return 0

        val currentTime = System.currentTimeMillis()
        val totalWaitTime = players.sumOf { currentTime - it.joinTime }
        return totalWaitTime / players.size
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        activeSessions.clear()
        matchmakingQueue.clear()
        scope.cancel()
    }
}

/**
 * Matchmaking request
 */
@Serializable
data class MatchmakingRequest(
    val playerId: String,
    val gameId: String,
    val skillLevel: Int,
    val region: String,
    val preferredLatency: Int,
    val gameMode: String? = null
)

/**
 * Player matchmaking information
 */
data class PlayerMatchmakingInfo(
    val playerId: String,
    val gameId: String,
    val skillLevel: Int,
    val region: String,
    val preferredLatency: Int,
    val joinTime: Long
)

/**
 * Session creator information
 */
data class SessionCreator(
    val creatorId: String,
    val gameId: String,
    val maxPlayers: Int,
    val minPlayers: Int = 2,
    val isPrivate: Boolean = false,
    val settings: Map<String, Any> = emptyMap()
)

/**
 * Game session
 */
@Serializable
data class GameSession(
    val id: String,
    val gameId: String,
    val creatorId: String,
    val players: List<String> = emptyList(),
    val maxPlayers: Int,
    val minPlayers: Int = 2,
    val isPrivate: Boolean = false,
    val settings: Map<String, Any> = emptyMap(),
    val status: SessionStatus,
    val averageSkill: Int = 0,
    val createdAt: Long,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val results: GameResults? = null
)

/**
 * Session status
 */
enum class SessionStatus {
    WAITING_FOR_PLAYERS,
    READY_TO_START,
    IN_PROGRESS,
    COMPLETED,
    ENDED
}

/**
 * Game results
 */
@Serializable
data class GameResults(
    val winnerIds: List<String>,
    val playerScores: Map<String, Long>,
    val duration: Long,
    val statistics: Map<String, Any>
)

/**
 * Matchmaking events
 */
sealed class MatchmakingEvent {
    data class PlayerJoinedQueue(val playerId: String) : MatchmakingEvent()
    data class PlayerLeftQueue(val playerId: String) : MatchmakingEvent()
    data class SessionCreated(val sessionId: String) : MatchmakingEvent()
    data class PlayerJoinedSession(val playerId: String, val sessionId: String) : MatchmakingEvent()
    data class PlayerLeftSession(val playerId: String, val sessionId: String) : MatchmakingEvent()
    data class SessionStarted(val sessionId: String) : MatchmakingEvent()
    data class SessionEnded(val sessionId: String, val results: GameResults) : MatchmakingEvent()
}

/**
 * Matchmaking configuration
 */
data class MatchmakingConfig(
    val matchmakingInterval: Long = 5000L, // 5 seconds
    val skillTolerance: Int = 200, // Skill difference tolerance
    val maxWaitTime: Long = 60000L, // 1 minute max wait
    val regionWeight: Float = 0.3f,
    val latencyWeight: Float = 0.4f,
    val skillWeight: Float = 0.3f
)

/**
 * Matchmaking statistics
 */
data class MatchmakingStats(
    val activeSessions: Int,
    val waitingForPlayers: Int,
    val inProgress: Int,
    val playersInQueue: Int,
    val averageWaitTime: Long
)