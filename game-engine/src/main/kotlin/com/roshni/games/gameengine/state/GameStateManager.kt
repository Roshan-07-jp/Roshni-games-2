package com.roshni.games.gameengine.state

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roshni.games.gameengine.core.GameSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Game state management system for saving and loading game progress
 */
class GameStateManager(
    private val context: Context,
    private val gameId: String
) : GameSystem() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Game state database
    private lateinit var database: GameStateDatabase

    // Current game state
    private val _currentState = MutableStateFlow<GameState?>(null)
    val currentState: StateFlow<GameState?> = _currentState.asStateFlow()

    // Auto-save settings
    private var autoSaveEnabled = true
    private var autoSaveInterval = 30000L // 30 seconds
    private var lastAutoSave = 0L

    // State change listeners
    private val stateChangeListeners = mutableListOf<(GameState) -> Unit>()

    override fun initialize() {
        Timber.d("Initializing game state manager for game: $gameId")

        // Initialize database
        database = Room.databaseBuilder(
            context.applicationContext,
            GameStateDatabase::class.java,
            "game_state_$gameId.db"
        ).build()

        // Load last saved state
        scope.launch {
            loadLastSavedState()
        }

        Timber.d("Game state manager initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up game state manager")

        // Auto-save before cleanup
        if (autoSaveEnabled && _currentState.value != null) {
            scope.launch {
                saveGameState(_currentState.value!!)
            }
        }

        scope.cancel()
    }

    override fun update(deltaTime: Float) {
        // Check for auto-save
        if (autoSaveEnabled && _currentState.value != null) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAutoSave > autoSaveInterval) {
                scope.launch {
                    saveGameState(_currentState.value!!)
                    lastAutoSave = currentTime
                }
            }
        }
    }

    /**
     * Save current game state
     */
    suspend fun saveGameState(state: GameState, slotName: String = "autosave"): Result<Unit> {
        return try {
            val stateEntity = GameStateEntity(
                id = generateStateId(gameId, slotName),
                gameId = gameId,
                slotName = slotName,
                stateData = Json.encodeToString(state),
                timestamp = System.currentTimeMillis(),
                version = 1
            )

            database.gameStateDao().insertState(stateEntity)

            // Keep only last 10 saves per slot
            cleanupOldSaves(slotName)

            Timber.d("Saved game state: $slotName for game: $gameId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save game state: $slotName")
            Result.failure(e)
        }
    }

    /**
     * Load game state by slot name
     */
    suspend fun loadGameState(slotName: String = "autosave"): Result<GameState?> {
        return try {
            val stateEntity = database.gameStateDao().getStateBySlot(gameId, slotName)

            if (stateEntity != null) {
                val state = Json.decodeFromString<GameState>(stateEntity.stateData)
                _currentState.value = state

                // Notify listeners
                stateChangeListeners.forEach { it(state) }

                Timber.d("Loaded game state: $slotName for game: $gameId")
                Result.success(state)
            } else {
                Timber.d("No saved state found for slot: $slotName")
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load game state: $slotName")
            Result.failure(e)
        }
    }

    /**
     * Update current game state
     */
    fun updateCurrentState(update: (GameState) -> GameState) {
        _currentState.value?.let { current ->
            val newState = update(current)
            _currentState.value = newState

            // Notify listeners
            stateChangeListeners.forEach { it(newState) }
        }
    }

    /**
     * Set current game state
     */
    fun setCurrentState(state: GameState) {
        _currentState.value = state

        // Notify listeners
        stateChangeListeners.forEach { it(state) }
    }

    /**
     * Get all save slots for current game
     */
    suspend fun getSaveSlots(): Result<List<SaveSlotInfo>> {
        return try {
            val entities = database.gameStateDao().getStatesForGame(gameId)
            val slots = entities.map { entity ->
                SaveSlotInfo(
                    slotName = entity.slotName,
                    timestamp = entity.timestamp,
                    version = entity.version
                )
            }
            Result.success(slots)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get save slots")
            Result.failure(e)
        }
    }

    /**
     * Delete save slot
     */
    suspend fun deleteSaveSlot(slotName: String): Result<Unit> {
        return try {
            database.gameStateDao().deleteStateBySlot(gameId, slotName)
            Timber.d("Deleted save slot: $slotName for game: $gameId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete save slot: $slotName")
            Result.failure(e)
        }
    }

    /**
     * Enable/disable auto-save
     */
    fun setAutoSave(enabled: Boolean, interval: Long = 30000L) {
        autoSaveEnabled = enabled
        autoSaveInterval = interval
        Timber.d("Auto-save ${if (enabled) "enabled" else "disabled"} with interval: ${interval}ms")
    }

    /**
     * Add state change listener
     */
    fun addStateChangeListener(listener: (GameState) -> Unit) {
        stateChangeListeners.add(listener)
    }

    /**
     * Remove state change listener
     */
    fun removeStateChangeListener(listener: (GameState) -> Unit) {
        stateChangeListeners.remove(listener)
    }

    /**
     * Export game state to file
     */
    suspend fun exportGameState(slotName: String, filePath: String): Result<Unit> {
        return try {
            val state = loadGameState(slotName).getOrNull() ?: return Result.failure(IllegalStateException("No state to export"))

            val jsonData = Json.encodeToString(state)
            File(filePath).writeText(jsonData)

            Timber.d("Exported game state to: $filePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export game state")
            Result.failure(e)
        }
    }

    /**
     * Import game state from file
     */
    suspend fun importGameState(filePath: String, slotName: String): Result<Unit> {
        return try {
            val jsonData = File(filePath).readText()
            val state = Json.decodeFromString<GameState>(jsonData)

            saveGameState(state, slotName)

            Timber.d("Imported game state from: $filePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to import game state")
            Result.failure(e)
        }
    }

    private suspend fun loadLastSavedState() {
        try {
            val lastState = database.gameStateDao().getLastStateForGame(gameId)
            if (lastState != null) {
                val state = Json.decodeFromString<GameState>(lastState.stateData)
                _currentState.value = state
                Timber.d("Loaded last saved state for game: $gameId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load last saved state")
        }
    }

    private suspend fun cleanupOldSaves(slotName: String) {
        try {
            val entities = database.gameStateDao().getStatesForGame(gameId)
                .filter { it.slotName == slotName }
                .sortedByDescending { it.timestamp }

            // Keep only the last 10 saves
            if (entities.size > 10) {
                entities.drop(10).forEach { entity ->
                    database.gameStateDao().deleteState(entity.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old saves")
        }
    }

    private fun generateStateId(gameId: String, slotName: String): String {
        return "${gameId}_${slotName}_${System.currentTimeMillis()}"
    }
}

/**
 * Serializable game state
 */
@Serializable
data class GameState(
    val gameId: String,
    val playerId: String,
    val level: Int = 1,
    val score: Long = 0,
    val lives: Int = 3,
    val health: Float = 100f,
    val position: Position = Position(0f, 0f),
    val inventory: Map<String, Int> = emptyMap(),
    val achievements: Set<String> = emptySet(),
    val statistics: Map<String, Long> = emptyMap(),
    val customData: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 2D position
 */
@Serializable
data class Position(
    val x: Float,
    val y: Float
)

/**
 * Save slot information
 */
data class SaveSlotInfo(
    val slotName: String,
    val timestamp: Long,
    val version: Int
)

/**
 * Room database for game states
 */
@Database(entities = [GameStateEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class GameStateDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}

/**
 * Game state entity for Room database
 */
@kotlinx.serialization.Serializable
data class GameStateEntity(
    val id: String,
    val gameId: String,
    val slotName: String,
    val stateData: String,
    val timestamp: Long,
    val version: Int
)

/**
 * DAO for game state operations
 */
@androidx.room.Dao
interface GameStateDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertState(state: GameStateEntity)

    @androidx.room.Query("SELECT * FROM game_states WHERE gameId = :gameId AND slotName = :slotName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getStateBySlot(gameId: String, slotName: String): GameStateEntity?

    @androidx.room.Query("SELECT * FROM game_states WHERE gameId = :gameId ORDER BY timestamp DESC")
    suspend fun getStatesForGame(gameId: String): List<GameStateEntity>

    @androidx.room.Query("SELECT * FROM game_states WHERE gameId = :gameId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastStateForGame(gameId: String): GameStateEntity?

    @androidx.room.Query("DELETE FROM game_states WHERE id = :id")
    suspend fun deleteState(id: String)

    @androidx.room.Query("DELETE FROM game_states WHERE gameId = :gameId AND slotName = :slotName")
    suspend fun deleteStateBySlot(gameId: String, slotName: String)
}

/**
 * Type converters for Room
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): kotlinx.datetime.Instant? {
        return value?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
    }

    @androidx.room.TypeConverter
    fun toTimestamp(instant: kotlinx.datetime.Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
}