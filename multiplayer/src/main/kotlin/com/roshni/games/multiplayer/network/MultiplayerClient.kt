package com.roshni.games.multiplayer.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import timber.log.Timber
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket client for real-time multiplayer communication
 */
class MultiplayerClient(
    private val serverUri: URI,
    private val playerId: String,
    private val authToken: String? = null
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocketClient: WebSocketClient? = null

    // Connection state
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    // Message streams
    private val _gameMessages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 100)
    val gameMessages: SharedFlow<GameMessage> = _gameMessages.asSharedFlow()

    private val _systemMessages = MutableSharedFlow<SystemMessage>(extraBufferCapacity = 50)
    val systemMessages: SharedFlow<SystemMessage> = _systemMessages.asSharedFlow()

    // Message handlers
    private val messageHandlers = ConcurrentHashMap<MessageType, MutableList<MessageHandler<*>>>()

    // Connection settings
    private val reconnectAttempts = 5
    private val reconnectDelay = 2000L
    private var currentReconnectAttempt = 0

    init {
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }

    /**
     * Connect to multiplayer server
     */
    fun connect(): Boolean {
        Timber.d("Connecting to multiplayer server: $serverUri")

        return try {
            webSocketClient = object : WebSocketClient(serverUri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Timber.d("Connected to multiplayer server")
                    currentReconnectAttempt = 0
                    scope.launch {
                        _connectionState.emit(ConnectionState.CONNECTED)
                    }

                    // Send authentication
                    sendAuthentication()
                }

                override fun onMessage(message: String?) {
                    message?.let { handleTextMessage(it) }
                }

                override fun onMessage(bytes: ByteBuffer?) {
                    bytes?.let { handleBinaryMessage(it) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Timber.d("Disconnected from multiplayer server: $reason")
                    scope.launch {
                        _connectionState.emit(ConnectionState.DISCONNECTED)
                    }

                    // Attempt to reconnect
                    if (currentReconnectAttempt < reconnectAttempts) {
                        scheduleReconnect()
                    }
                }

                override fun onError(ex: Exception?) {
                    Timber.e(ex, "WebSocket error")
                    scope.launch {
                        _connectionState.emit(ConnectionState.ERROR(ex?.message ?: "Unknown error"))
                    }
                }
            }

            webSocketClient?.connect()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to multiplayer server")
            scope.launch {
                _connectionState.tryEmit(ConnectionState.ERROR(e.message ?: "Connection failed"))
            }
            false
        }
    }

    /**
     * Disconnect from multiplayer server
     */
    fun disconnect() {
        Timber.d("Disconnecting from multiplayer server")
        webSocketClient?.close()
        webSocketClient = null
        scope.launch {
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }

    /**
     * Send game message to server
     */
    fun sendGameMessage(message: GameMessage) {
        sendMessage(MessageType.GAME, message)
    }

    /**
     * Send system message to server
     */
    fun sendSystemMessage(message: SystemMessage) {
        sendMessage(MessageType.SYSTEM, message)
    }

    /**
     * Register message handler
     */
    fun <T : Any> registerMessageHandler(
        messageType: MessageType,
        handler: MessageHandler<T>
    ) {
        val handlers = messageHandlers.getOrPut(messageType) { mutableListOf() }
        @Suppress("UNCHECKED_CAST")
        handlers.add(handler as MessageHandler<*>)
    }

    /**
     * Unregister message handler
     */
    fun unregisterMessageHandler(
        messageType: MessageType,
        handler: MessageHandler<*>
    ) {
        messageHandlers[messageType]?.remove(handler)
    }

    /**
     * Join multiplayer game session
     */
    fun joinGameSession(gameId: String, sessionId: String) {
        val joinMessage = SystemMessage.JoinGameSession(
            playerId = playerId,
            gameId = gameId,
            sessionId = sessionId,
            timestamp = System.currentTimeMillis()
        )
        sendSystemMessage(joinMessage)
    }

    /**
     * Leave multiplayer game session
     */
    fun leaveGameSession(sessionId: String) {
        val leaveMessage = SystemMessage.LeaveGameSession(
            playerId = playerId,
            sessionId = sessionId,
            timestamp = System.currentTimeMillis()
        )
        sendSystemMessage(leaveMessage)
    }

    /**
     * Send game state update
     */
    fun sendGameStateUpdate(sessionId: String, gameState: Map<String, Any>) {
        val stateMessage = GameMessage.GameStateUpdate(
            sessionId = sessionId,
            playerId = playerId,
            gameState = gameState,
            timestamp = System.currentTimeMillis()
        )
        sendGameMessage(stateMessage)
    }

    /**
     * Send player action
     */
    fun sendPlayerAction(sessionId: String, action: PlayerAction) {
        val actionMessage = GameMessage.PlayerAction(
            sessionId = sessionId,
            playerId = playerId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        sendGameMessage(actionMessage)
    }

    private fun sendAuthentication() {
        val authMessage = SystemMessage.Authentication(
            playerId = playerId,
            authToken = authToken,
            timestamp = System.currentTimeMillis()
        )
        sendSystemMessage(authMessage)
    }

    private fun sendMessage(type: MessageType, payload: Any) {
        try {
            val message = NetworkMessage(
                type = type,
                payload = Json.encodeToString(payload),
                timestamp = System.currentTimeMillis()
            )

            val jsonMessage = Json.encodeToString(message)
            webSocketClient?.send(jsonMessage)

            Timber.d("Sent message: $type")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message: $type")
        }
    }

    private fun handleTextMessage(message: String) {
        try {
            val networkMessage = Json.decodeFromString<NetworkMessage>(message)

            when (networkMessage.type) {
                MessageType.GAME -> {
                    val gameMessage = Json.decodeFromString<GameMessage>(networkMessage.payload)
                    scope.launch {
                        _gameMessages.emit(gameMessage)
                    }

                    // Notify handlers
                    notifyMessageHandlers(networkMessage.type, gameMessage)
                }
                MessageType.SYSTEM -> {
                    val systemMessage = Json.decodeFromString<SystemMessage>(networkMessage.payload)
                    scope.launch {
                        _systemMessages.emit(systemMessage)
                    }

                    // Notify handlers
                    notifyMessageHandlers(networkMessage.type, systemMessage)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle message: $message")
        }
    }

    private fun handleBinaryMessage(bytes: ByteBuffer) {
        // Handle binary messages if needed
        Timber.d("Received binary message: ${bytes.remaining()} bytes")
    }

    private fun notifyMessageHandlers(type: MessageType, message: Any) {
        messageHandlers[type]?.forEach { handler ->
            try {
                @Suppress("UNCHECKED_CAST")
                (handler as MessageHandler<Any>).onMessage(message)
            } catch (e: Exception) {
                Timber.e(e, "Error in message handler")
            }
        }
    }

    private fun scheduleReconnect() {
        currentReconnectAttempt++
        Timber.d("Scheduling reconnect attempt $currentReconnectAttempt/$reconnectAttempts")

        scope.launch {
            delay(reconnectDelay * currentReconnectAttempt)
            if (_connectionState.replayCache.lastOrNull() != ConnectionState.CONNECTED) {
                connect()
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Network message wrapper
 */
@Serializable
data class NetworkMessage(
    val type: MessageType,
    val payload: String,
    val timestamp: Long
)

/**
 * Message types
 */
enum class MessageType {
    SYSTEM, GAME
}

/**
 * Message handler interface
 */
interface MessageHandler<T> {
    fun onMessage(message: T)
}

/**
 * Game messages for gameplay communication
 */
@Serializable
sealed class GameMessage {
    @Serializable
    data class GameStateUpdate(
        val sessionId: String,
        val playerId: String,
        val gameState: Map<String, Any>,
        val timestamp: Long
    ) : GameMessage()

    @Serializable
    data class PlayerAction(
        val sessionId: String,
        val playerId: String,
        val action: PlayerAction,
        val timestamp: Long
    ) : GameMessage()

    @Serializable
    data class GameEvent(
        val sessionId: String,
        val eventType: String,
        val eventData: Map<String, Any>,
        val timestamp: Long
    ) : GameMessage()
}

/**
 * System messages for connection and session management
 */
@Serializable
sealed class SystemMessage {
    @Serializable
    data class Authentication(
        val playerId: String,
        val authToken: String?,
        val timestamp: Long
    ) : SystemMessage()

    @Serializable
    data class JoinGameSession(
        val playerId: String,
        val gameId: String,
        val sessionId: String,
        val timestamp: Long
    ) : SystemMessage()

    @Serializable
    data class LeaveGameSession(
        val playerId: String,
        val sessionId: String,
        val timestamp: Long
    ) : SystemMessage()

    @Serializable
    data class SessionUpdate(
        val sessionId: String,
        val players: List<String>,
        val status: String,
        val timestamp: Long
    ) : SystemMessage()

    @Serializable
    data class Error(
        val code: String,
        val message: String,
        val timestamp: Long
    ) : SystemMessage()
}

/**
 * Player actions
 */
@Serializable
data class PlayerAction(
    val type: String,
    val data: Map<String, Any>
)

/**
 * Connection state with reason
 */
data class ConnectionStateWithReason(
    val state: ConnectionState,
    val reason: String? = null
)