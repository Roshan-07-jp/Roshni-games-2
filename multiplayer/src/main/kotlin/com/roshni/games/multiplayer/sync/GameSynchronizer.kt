package com.roshni.games.multiplayer.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Synchronizes game state across multiple players in real-time
 */
class GameSynchronizer(
    private val sessionId: String,
    private val playerId: String
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Game state
    private val _gameState = MutableSharedFlow<Map<String, Any>>(replay = 1)
    val gameState: SharedFlow<Map<String, Any>> = _gameState.asSharedFlow()

    // Player states
    private val playerStates = ConcurrentHashMap<String, PlayerState>()

    // Synchronization events
    private val _syncEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 100)
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    // Synchronization settings
    private val config = SyncConfig()

    // State validation
    private var lastValidatedState: String? = null
    private var validationFailures = 0

    init {
        startStateValidation()
    }

    /**
     * Update local player state
     */
    fun updatePlayerState(state: Map<String, Any>) {
        val playerState = PlayerState(
            playerId = playerId,
            state = state,
            timestamp = System.currentTimeMillis(),
            sequenceNumber = getNextSequenceNumber()
        )

        playerStates[playerId] = playerState

        // Broadcast state update
        scope.launch {
            _syncEvents.emit(SyncEvent.PlayerStateUpdated(playerId, state))
        }

        Timber.d("Updated player state for $playerId in session $sessionId")
    }

    /**
     * Handle remote player state update
     */
    fun handleRemotePlayerState(playerId: String, state: Map<String, Any>, timestamp: Long) {
        if (playerId == this.playerId) return // Ignore own updates

        val remoteState = PlayerState(
            playerId = playerId,
            state = state,
            timestamp = timestamp,
            sequenceNumber = getNextSequenceNumber()
        )

        playerStates[playerId] = remoteState

        // Validate state consistency
        validateStateConsistency()

        scope.launch {
            _syncEvents.emit(SyncEvent.RemoteStateReceived(playerId, state))
        }

        Timber.d("Received remote state from $playerId")
    }

    /**
     * Synchronize game state across all players
     */
    fun synchronizeGameState(targetState: Map<String, Any>) {
        scope.launch {
            try {
                // Validate target state
                if (!validateTargetState(targetState)) {
                    Timber.w("Target state validation failed")
                    return@launch
                }

                // Apply state changes
                _gameState.emit(targetState)

                // Notify all players
                _syncEvents.emit(SyncEvent.GameStateSynchronized(targetState))

                lastValidatedState = targetState.hashCode().toString()

                Timber.d("Game state synchronized for session $sessionId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to synchronize game state")
                _syncEvents.emit(SyncEvent.SyncError("Failed to synchronize: ${e.message}"))
            }
        }
    }

    /**
     * Handle player action and synchronize across players
     */
    fun handlePlayerAction(action: PlayerAction): Result<Unit> {
        return try {
            // Validate action
            if (!validatePlayerAction(action)) {
                return Result.failure(IllegalArgumentException("Invalid player action"))
            }

            // Apply action locally
            val newState = applyPlayerAction(action)

            // Broadcast action to other players
            scope.launch {
                _syncEvents.emit(SyncEvent.PlayerActionBroadcast(playerId, action))
            }

            // Update synchronized state
            synchronizeGameState(newState)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle player action")
            Result.failure(e)
        }
    }

    /**
     * Get current synchronized game state
     */
    fun getCurrentGameState(): Map<String, Any>? {
        return _gameState.replayCache.lastOrNull()
    }

    /**
     * Get player state
     */
    fun getPlayerState(playerId: String): PlayerState? {
        return playerStates[playerId]
    }

    /**
     * Get all player states
     */
    fun getAllPlayerStates(): Map<String, PlayerState> {
        return playerStates.toMap()
    }

    /**
     * Check if game state is synchronized
     */
    fun isStateSynchronized(): Boolean {
        return validationFailures == 0
    }

    /**
     * Force state resynchronization
     */
    fun forceResync(targetState: Map<String, Any>) {
        Timber.d("Forcing state resynchronization")

        scope.launch {
            _gameState.emit(targetState)
            _syncEvents.emit(SyncEvent.ForceResync(targetState))
            lastValidatedState = targetState.hashCode().toString()
            validationFailures = 0
        }
    }

    /**
     * Handle connection loss and recovery
     */
    fun handleConnectionLost() {
        Timber.d("Handling connection loss for session $sessionId")

        scope.launch {
            _syncEvents.emit(SyncEvent.ConnectionLost)
        }
    }

    /**
     * Handle connection recovery
     */
    fun handleConnectionRecovered() {
        Timber.d("Handling connection recovery for session $sessionId")

        scope.launch {
            // Request full state sync
            _syncEvents.emit(SyncEvent.RequestFullSync)
        }
    }

    private fun startStateValidation() {
        scope.launch {
            while (isActive) {
                try {
                    validateStateConsistency()
                    delay(config.validationInterval)
                } catch (e: Exception) {
                    Timber.e(e, "Error in state validation")
                }
            }
        }
    }

    private fun validateStateConsistency() {
        if (playerStates.size <= 1) return

        // Check for state divergence
        val states = playerStates.values.toList()
        val referenceState = states.first()

        var maxDivergence = 0.0
        var divergentPlayers = mutableListOf<String>()

        states.forEach { state ->
            if (state != referenceState) {
                val divergence = calculateStateDivergence(referenceState, state)
                if (divergence > config.maxAllowedDivergence) {
                    divergentPlayers.add(state.playerId)
                }
                maxDivergence = maxOf(maxDivergence, divergence)
            }
        }

        if (divergentPlayers.isNotEmpty()) {
            validationFailures++

            scope.launch {
                _syncEvents.emit(SyncEvent.StateDivergenceDetected(divergentPlayers, maxDivergence))
            }

            Timber.w("State divergence detected for players: $divergentPlayers")
        } else {
            validationFailures = 0
        }
    }

    private fun validateTargetState(state: Map<String, Any>): Boolean {
        // Implement state validation logic
        return state.isNotEmpty() // Basic validation
    }

    private fun validatePlayerAction(action: PlayerAction): Boolean {
        // Implement action validation logic
        return action.type.isNotEmpty() && action.playerId == playerId
    }

    private fun applyPlayerAction(action: PlayerAction): Map<String, Any> {
        val currentState = _gameState.replayCache.lastOrNull() ?: emptyMap()
        val newState = currentState.toMutableMap()

        // Apply action effects to state
        when (action.type) {
            "MOVE" -> {
                newState["playerX"] = action.data["x"] as? Float ?: 0f
                newState["playerY"] = action.data["y"] as? Float ?: 0f
            }
            "ATTACK" -> {
                newState["lastAttack"] = System.currentTimeMillis()
            }
            "USE_ITEM" -> {
                val itemId = action.data["itemId"] as? String ?: ""
                newState["usedItem"] = itemId
            }
        }

        return newState
    }

    private fun calculateStateDivergence(state1: PlayerState, state2: PlayerState): Double {
        // Simple divergence calculation based on timestamp difference
        val timeDiff = abs(state1.timestamp - state2.timestamp).toDouble()
        return (timeDiff / 1000.0) // Convert to seconds
    }

    private fun getNextSequenceNumber(): Int {
        return (playerStates[playerId]?.sequenceNumber ?: 0) + 1
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        playerStates.clear()
        scope.cancel()
    }
}

/**
 * Player state in multiplayer session
 */
@Serializable
data class PlayerState(
    val playerId: String,
    val state: Map<String, Any>,
    val timestamp: Long,
    val sequenceNumber: Int
)

/**
 * Player action
 */
@Serializable
data class PlayerAction(
    val playerId: String,
    val type: String,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Synchronization events
 */
sealed class SyncEvent {
    data class PlayerStateUpdated(val playerId: String, val state: Map<String, Any>) : SyncEvent()
    data class RemoteStateReceived(val playerId: String, val state: Map<String, Any>) : SyncEvent()
    data class GameStateSynchronized(val state: Map<String, Any>) : SyncEvent()
    data class PlayerActionBroadcast(val playerId: String, val action: PlayerAction) : SyncEvent()
    data class StateDivergenceDetected(val players: List<String>, val divergence: Double) : SyncEvent()
    data class SyncError(val message: String) : SyncEvent()
    data class ForceResync(val state: Map<String, Any>) : SyncEvent()
    data object ConnectionLost : SyncEvent()
    data object RequestFullSync : SyncEvent()
}

/**
 * Synchronization configuration
 */
data class SyncConfig(
    val validationInterval: Long = 1000L, // 1 second
    val maxAllowedDivergence: Double = 2.0, // 2 seconds
    val maxValidationFailures: Int = 3,
    val enableStateCompression: Boolean = true,
    val enableDeltaSync: Boolean = true,
    val syncRateLimit: Long = 100L // Minimum time between syncs in ms
)

/**
 * State synchronization utilities
 */
object SyncUtils {

    /**
     * Calculate state delta between two states
     */
    fun calculateDelta(oldState: Map<String, Any>, newState: Map<String, Any>): Map<String, Any> {
        val delta = mutableMapOf<String, Any>()

        // Find changed keys
        val allKeys = (oldState.keys + newState.keys).distinct()
        allKeys.forEach { key ->
            val oldValue = oldState[key]
            val newValue = newState[key]

            if (oldValue != newValue) {
                delta[key] = newValue as Any
            }
        }

        return delta
    }

    /**
     * Apply delta to base state
     */
    fun applyDelta(baseState: Map<String, Any>, delta: Map<String, Any>): Map<String, Any> {
        return baseState + delta
    }

    /**
     * Compress state for network transmission
     */
    fun compressState(state: Map<String, Any>): ByteArray {
        // Simple compression - in real implementation would use proper compression
        return kotlinx.serialization.json.Json.encodeToString(state).toByteArray()
    }

    /**
     * Decompress state from network transmission
     */
    fun decompressState(data: ByteArray): Map<String, Any> {
        return try {
            val json = String(data)
            kotlinx.serialization.json.Json.decodeFromString<Map<String, Any>>(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decompress state")
            emptyMap()
        }
    }
}