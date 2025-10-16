package com.roshni.games.offline.multiplayer

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * Manages local multiplayer connections via Bluetooth, WiFi Direct, and Hotseat mode
 */
class LocalMultiplayerManager(
    private val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // WiFi P2P manager for WiFi Direct
    private var wifiP2pManager: WifiP2pManager? = null
    private var wifiP2pChannel: WifiP2pManager.Channel? = null

    // Local multiplayer sessions
    private val localSessions = ConcurrentHashMap<String, LocalGameSession>()

    // Connection events
    private val _connectionEvents = MutableSharedFlow<LocalMultiplayerEvent>(extraBufferCapacity = 100)
    val connectionEvents: SharedFlow<LocalMultiplayerEvent> = _connectionEvents.asSharedFlow()

    // Device discovery
    private val discoveredDevices = mutableListOf<NetworkDevice>()

    init {
        initializeWifiP2P()
    }

    /**
     * Initialize WiFi P2P for WiFi Direct multiplayer
     */
    private fun initializeWifiP2P() {
        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager?.initialize(context, Looper.getMainLooper()) {
            Timber.d("WiFi P2P initialized")
        }
    }

    /**
     * Start discovering nearby devices for WiFi Direct
     */
    fun startDeviceDiscovery(): Result<Unit> {
        return try {
            val manager = wifiP2pManager ?: return Result.failure(IllegalStateException("WiFi P2P not initialized"))
            val channel = wifiP2pChannel ?: return Result.failure(IllegalStateException("WiFi P2P channel not initialized"))

            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Timber.d("Device discovery started")
                    scope.launch {
                        _connectionEvents.emit(LocalMultiplayerEvent.DeviceDiscoveryStarted)
                    }
                }

                override fun onFailure(reason: Int) {
                    Timber.e("Device discovery failed: $reason")
                    scope.launch {
                        _connectionEvents.emit(LocalMultiplayerEvent.DeviceDiscoveryFailed("Reason code: $reason"))
                    }
                }
            })

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start device discovery")
            Result.failure(e)
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDeviceDiscovery() {
        try {
            wifiP2pManager?.stopPeerDiscovery(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Timber.d("Device discovery stopped")
                }

                override fun onFailure(reason: Int) {
                    Timber.e("Failed to stop device discovery: $reason")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Error stopping device discovery")
        }
    }

    /**
     * Connect to device via WiFi Direct
     */
    fun connectToDevice(device: NetworkDevice): Result<Unit> {
        return try {
            // Implementation would use WifiP2pManager.connect()
            // For now, simulate connection
            val session = LocalGameSession(
                id = "local_${System.currentTimeMillis()}",
                hostDeviceId = getDeviceId(),
                connectedDevices = listOf(device.id),
                connectionType = ConnectionType.WIFI_DIRECT,
                status = SessionStatus.CONNECTING,
                createdAt = System.currentTimeMillis()
            )

            localSessions[session.id] = session

            Timber.d("Connecting to device: ${device.name}")

            scope.launch {
                _connectionEvents.emit(LocalMultiplayerEvent.DeviceConnected(device))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to device")
            Result.failure(e)
        }
    }

    /**
     * Create local game session for hotseat mode
     */
    fun createHotseatSession(
        gameId: String,
        playerNames: List<String>,
        maxPlayers: Int = 4
    ): LocalGameSession {
        val session = LocalGameSession(
            id = "hotseat_${System.currentTimeMillis()}",
            gameId = gameId,
            hostDeviceId = getDeviceId(),
            connectedDevices = listOf(getDeviceId()),
            players = playerNames.map { PlayerInfo(it, getDeviceId()) },
            connectionType = ConnectionType.HOTSEAT,
            status = SessionStatus.WAITING_FOR_PLAYERS,
            maxPlayers = maxPlayers,
            createdAt = System.currentTimeMillis()
        )

        localSessions[session.id] = session

        Timber.d("Created hotseat session: ${session.id} with ${playerNames.size} players")

        scope.launch {
            _connectionEvents.emit(LocalMultiplayerEvent.HotseatSessionCreated(session.id))
        }

        return session
    }

    /**
     * Join hotseat session
     */
    fun joinHotseatSession(sessionId: String, playerName: String): Result<Unit> {
        val session = localSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        if (session.status != SessionStatus.WAITING_FOR_PLAYERS) {
            return Result.failure(IllegalStateException("Session not accepting players"))
        }

        if (session.players.size >= session.maxPlayers) {
            return Result.failure(IllegalStateException("Session is full"))
        }

        val updatedPlayers = session.players + PlayerInfo(playerName, getDeviceId())
        val updatedSession = session.copy(
            players = updatedPlayers,
            status = if (updatedPlayers.size >= 2) SessionStatus.READY_TO_START else SessionStatus.WAITING_FOR_PLAYERS
        )

        localSessions[sessionId] = updatedSession

        Timber.d("Player $playerName joined hotseat session $sessionId")

        scope.launch {
            _connectionEvents.emit(LocalMultiplayerEvent.PlayerJoinedHotseat(sessionId, playerName))
        }

        return Result.success(Unit)
    }

    /**
     * Start local game session
     */
    fun startLocalGameSession(sessionId: String): Result<Unit> {
        val session = localSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        if (session.players.size < 2) {
            return Result.failure(IllegalStateException("Need at least 2 players"))
        }

        val updatedSession = session.copy(
            status = SessionStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )

        localSessions[sessionId] = updatedSession

        Timber.d("Started local game session: $sessionId")

        scope.launch {
            _connectionEvents.emit(LocalMultiplayerEvent.LocalGameStarted(sessionId))
        }

        return Result.success(Unit)
    }

    /**
     * Send game move in local session
     */
    fun sendLocalGameMove(sessionId: String, playerId: String, move: GameMove): Result<Unit> {
        val session = localSessions[sessionId]
        if (session == null) {
            return Result.failure(IllegalArgumentException("Session not found"))
        }

        if (session.status != SessionStatus.IN_PROGRESS) {
            return Result.failure(IllegalStateException("Session not in progress"))
        }

        // In real implementation, this would broadcast to all connected devices
        scope.launch {
            _connectionEvents.emit(LocalMultiplayerEvent.GameMoveReceived(sessionId, playerId, move))
        }

        return Result.success(Unit)
    }

    /**
     * End local game session
     */
    fun endLocalGameSession(sessionId: String, results: GameResults): Result<Unit> {
        val session = localSessions[sessionId]
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
            localSessions.remove(sessionId)
        }

        Timber.d("Ended local game session: $sessionId")

        scope.launch {
            _connectionEvents.emit(LocalMultiplayerEvent.LocalGameEnded(sessionId, results))
        }

        return Result.success(Unit)
    }

    /**
     * Get available local game sessions
     */
    fun getAvailableSessions(): List<LocalGameSession> {
        return localSessions.values.filter { session ->
            session.status == SessionStatus.WAITING_FOR_PLAYERS ||
            session.status == SessionStatus.READY_TO_START
        }
    }

    /**
     * Get discovered devices
     */
    fun getDiscoveredDevices(): List<NetworkDevice> {
        return discoveredDevices.toList()
    }

    /**
     * Disconnect from all local sessions
     */
    fun disconnectAll() {
        localSessions.values.forEach { session ->
            if (session.status == SessionStatus.IN_PROGRESS) {
                endLocalGameSession(session.id, GameResults(emptyList(), emptyMap(), 0, emptyMap()))
            }
        }

        discoveredDevices.clear()
        Timber.d("Disconnected from all local sessions")
    }

    private fun getDeviceId(): String {
        // In real implementation, this would get actual device ID
        return "device_${System.currentTimeMillis()}"
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnectAll()
        wifiP2pManager = null
        wifiP2pChannel = null
        scope.cancel()
    }
}

/**
 * Local game session for offline multiplayer
 */
@Serializable
data class LocalGameSession(
    val id: String,
    val gameId: String? = null,
    val hostDeviceId: String,
    val connectedDevices: List<String>,
    val players: List<PlayerInfo> = emptyList(),
    val connectionType: ConnectionType,
    val status: SessionStatus,
    val maxPlayers: Int = 4,
    val minPlayers: Int = 2,
    val createdAt: Long,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val results: GameResults? = null
)

/**
 * Player information for local multiplayer
 */
@Serializable
data class PlayerInfo(
    val name: String,
    val deviceId: String,
    val isReady: Boolean = false,
    val isHost: Boolean = false
)

/**
 * Network device for WiFi Direct
 */
@Serializable
data class NetworkDevice(
    val id: String,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0
)

/**
 * Connection types
 */
enum class ConnectionType {
    WIFI_DIRECT,
    BLUETOOTH,
    HOTSEAT,
    LOCAL_NETWORK
}

/**
 * Session status
 */
enum class SessionStatus {
    WAITING_FOR_PLAYERS,
    READY_TO_START,
    IN_PROGRESS,
    COMPLETED,
    DISCONNECTED
}

/**
 * Game move for local multiplayer
 */
@Serializable
data class GameMove(
    val playerId: String,
    val moveType: String,
    val moveData: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Game results for local multiplayer
 */
@Serializable
data class GameResults(
    val winnerIds: List<String>,
    val playerScores: Map<String, Long>,
    val duration: Long,
    val statistics: Map<String, Any>
)

/**
 * Local multiplayer events
 */
sealed class LocalMultiplayerEvent {
    data object DeviceDiscoveryStarted : LocalMultiplayerEvent()
    data class DeviceDiscoveryFailed(val reason: String) : LocalMultiplayerEvent()
    data class DeviceConnected(val device: NetworkDevice) : LocalMultiplayerEvent()
    data class DeviceDisconnected(val device: NetworkDevice) : LocalMultiplayerEvent()
    data class HotseatSessionCreated(val sessionId: String) : LocalMultiplayerEvent()
    data class PlayerJoinedHotseat(val sessionId: String, val playerName: String) : LocalMultiplayerEvent()
    data class LocalGameStarted(val sessionId: String) : LocalMultiplayerEvent()
    data class GameMoveReceived(val sessionId: String, val playerId: String, val move: GameMove) : LocalMultiplayerEvent()
    data class LocalGameEnded(val sessionId: String, val results: GameResults) : LocalMultiplayerEvent()
}