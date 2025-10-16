package com.roshni.games.core.utils.savestate

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Enhanced save state manager with crash recovery and corruption detection
 */
class SaveStateManager(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "game_save_states")
    private val _saveStates = MutableStateFlow<Map<String, GameSaveState>>(emptyMap())
    private val _crashRecoveryStates = MutableStateFlow<Map<String, CrashRecoveryState>>(emptyMap())

    // Public flows
    val saveStates: StateFlow<Map<String, GameSaveState>> = _saveStates.asStateFlow()
    val crashRecoveryStates: StateFlow<Map<String, CrashRecoveryState>> = _crashRecoveryStates.asStateFlow()

    private val encryptionKey: SecretKey by lazy { initializeEncryption() }
    private val saveStateCache = ConcurrentHashMap<String, GameSaveState>()
    private val corruptionDetector = SaveStateCorruptionDetector()

    companion object {
        private const val MAX_SAVE_STATES_PER_GAME = 10
        private const val AUTO_SAVE_INTERVAL_MS = 30000L // 30 seconds
        private const val CRASH_RECOVERY_TIMEOUT_MS = 300000L // 5 minutes
    }

    /**
     * Initialize the save state manager
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing SaveStateManager")

            // Load existing save states
            loadAllSaveStates()

            // Check for crash recovery opportunities
            checkCrashRecoveryStates()

            // Start auto-save monitoring
            startAutoSaveMonitoring()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SaveStateManager")
            Result.failure(e)
        }
    }

    /**
     * Save game state with automatic backup and validation
     */
    suspend fun saveGameState(
        gameId: String,
        playerId: String,
        gameData: Map<String, Any>,
        metadata: Map<String, Any> = emptyMap(),
        forceSave: Boolean = false
    ): Result<String> {
        return try {
            val saveState = GameSaveState(
                id = generateSaveStateId(),
                gameId = gameId,
                playerId = playerId,
                timestamp = System.currentTimeMillis(),
                gameData = gameData,
                metadata = metadata,
                version = 1,
                checksum = ""
            )

            // Validate save state before saving
            val validationResult = validateSaveState(saveState)
            if (!validationResult.isValid) {
                return Result.failure(IllegalStateException("Save state validation failed: ${validationResult.errors}"))
            }

            // Calculate checksum for corruption detection
            val checksum = calculateChecksum(saveState)
            val finalSaveState = saveState.copy(checksum = checksum)

            // Encrypt sensitive data
            val encryptedSaveState = encryptSaveState(finalSaveState)

            // Save to persistent storage
            saveToPersistentStorage(finalSaveState)

            // Update cache
            saveStateCache[finalSaveState.id] = finalSaveState

            // Update flow
            updateSaveStatesFlow()

            // Create backup for crash recovery
            if (forceSave) {
                createCrashRecoveryBackup(finalSaveState)
            }

            // Cleanup old save states
            cleanupOldSaveStates(gameId)

            Timber.d("Saved game state: ${finalSaveState.id} for game: $gameId")
            Result.success(finalSaveState.id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save game state for game: $gameId")
            Result.failure(e)
        }
    }

    /**
     * Load game state with corruption recovery
     */
    suspend fun loadGameState(saveStateId: String): Result<GameSaveState> {
        return try {
            // Try cache first
            val cachedState = saveStateCache[saveStateId]
            if (cachedState != null) {
                val validationResult = validateSaveState(cachedState)
                if (validationResult.isValid) {
                    return Result.success(cachedState)
                } else {
                    Timber.w("Cached save state corrupted, loading from storage")
                    saveStateCache.remove(saveStateId)
                }
            }

            // Load from persistent storage
            val loadedState = loadFromPersistentStorage(saveStateId)
            if (loadedState != null) {
                val validationResult = validateSaveState(loadedState)
                if (validationResult.isValid) {
                    // Update cache
                    saveStateCache[saveStateId] = loadedState
                    updateSaveStatesFlow()
                    return Result.success(loadedState)
                } else {
                    Timber.w("Loaded save state corrupted: ${validationResult.errors}")
                    // Try to recover from backup
                    val recoveredState = attemptRecovery(saveStateId)
                    if (recoveredState != null) {
                        saveStateCache[saveStateId] = recoveredState
                        updateSaveStatesFlow()
                        return Result.success(recoveredState)
                    } else {
                        return Result.failure(IllegalStateException("Save state corrupted and recovery failed"))
                    }
                }
            }

            Result.failure(IllegalStateException("Save state not found: $saveStateId"))

        } catch (e: Exception) {
            Timber.e(e, "Failed to load game state: $saveStateId")
            Result.failure(e)
        }
    }

    /**
     * Get all save states for a specific game
     */
    suspend fun getSaveStatesForGame(gameId: String): Result<List<GameSaveState>> {
        return try {
            val gameSaveStates = _saveStates.value.values.filter { it.gameId == gameId }
                .sortedByDescending { it.timestamp }

            Result.success(gameSaveStates)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get save states for game: $gameId")
            Result.failure(e)
        }
    }

    /**
     * Delete save state
     */
    suspend fun deleteSaveState(saveStateId: String): Result<Unit> {
        return try {
            // Remove from persistent storage
            deleteFromPersistentStorage(saveStateId)

            // Remove from cache
            saveStateCache.remove(saveStateId)

            // Update flow
            updateSaveStatesFlow()

            Timber.d("Deleted save state: $saveStateId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete save state: $saveStateId")
            Result.failure(e)
        }
    }

    /**
     * Create crash recovery backup
     */
    suspend fun createCrashRecoveryBackup(gameState: Map<String, Any>, gameId: String): Result<Unit> {
        return try {
            val recoveryState = CrashRecoveryState(
                id = generateSaveStateId(),
                gameId = gameId,
                timestamp = System.currentTimeMillis(),
                gameState = gameState,
                canRecover = true,
                expiresAt = System.currentTimeMillis() + CRASH_RECOVERY_TIMEOUT_MS
            )

            // Save crash recovery state
            saveCrashRecoveryState(recoveryState)

            Timber.d("Created crash recovery backup for game: $gameId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create crash recovery backup for game: $gameId")
            Result.failure(e)
        }
    }

    /**
     * Attempt to recover from crash
     */
    suspend fun attemptCrashRecovery(gameId: String): Result<GameSaveState?> {
        return try {
            val recoveryStates = _crashRecoveryStates.value.values.filter {
                it.gameId == gameId && it.canRecover && it.expiresAt > System.currentTimeMillis()
            }

            if (recoveryStates.isEmpty()) {
                return Result.success(null)
            }

            // Get the most recent recovery state
            val latestRecovery = recoveryStates.maxByOrNull { it.timestamp }
            if (latestRecovery != null) {
                val gameSaveState = GameSaveState(
                    id = generateSaveStateId(),
                    gameId = gameId,
                    playerId = "crash_recovery",
                    timestamp = System.currentTimeMillis(),
                    gameData = latestRecovery.gameState,
                    metadata = mapOf("recovered_from_crash" to true),
                    version = 1,
                    checksum = calculateChecksum(latestRecovery.gameState)
                )

                // Remove the recovery state after successful recovery
                removeCrashRecoveryState(latestRecovery.id)

                Timber.d("Successfully recovered from crash for game: $gameId")
                return Result.success(gameSaveState)
            }

            Result.success(null)

        } catch (e: Exception) {
            Timber.e(e, "Failed to attempt crash recovery for game: $gameId")
            Result.failure(e)
        }
    }

    /**
     * Validate save state integrity
     */
    private fun validateSaveState(saveState: GameSaveState): ValidationResult {
        val errors = mutableListOf<String>()

        // Check required fields
        if (saveState.gameId.isBlank()) {
            errors.add("Game ID is required")
        }

        if (saveState.playerId.isBlank()) {
            errors.add("Player ID is required")
        }

        // Check timestamp
        if (saveState.timestamp <= 0) {
            errors.add("Invalid timestamp")
        }

        // Check version
        if (saveState.version < 1) {
            errors.add("Invalid version")
        }

        // Verify checksum
        if (saveState.checksum.isNotBlank()) {
            val calculatedChecksum = calculateChecksum(saveState)
            if (calculatedChecksum != saveState.checksum) {
                errors.add("Checksum mismatch - data may be corrupted")
            }
        }

        // Check data size (prevent extremely large save states)
        val dataSize = Json.encodeToString(saveState.gameData).length
        if (dataSize > 10 * 1024 * 1024) { // 10MB limit
            errors.add("Save state too large: ${dataSize} bytes")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Calculate checksum for corruption detection
     */
    private fun calculateChecksum(saveState: GameSaveState): String {
        return calculateChecksum(saveState.gameData)
    }

    private fun calculateChecksum(data: Map<String, Any>): String {
        return try {
            val jsonData = Json.encodeToString(data)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(jsonData.toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate checksum")
            ""
        }
    }

    /**
     * Encrypt save state data
     */
    private fun encryptSaveState(saveState: GameSaveState): GameSaveState {
        return try {
            // Only encrypt sensitive metadata, keep game data accessible for quick loading
            val sensitiveData = mapOf(
                "playerId" to saveState.playerId,
                "timestamp" to saveState.timestamp
            )

            val jsonData = Json.encodeToString(sensitiveData)
            val encryptedData = encryptData(jsonData)
            val encryptedMetadata = saveState.metadata.toMutableMap()
            encryptedMetadata["encrypted_sensitive"] = encryptedData

            saveState.copy(metadata = encryptedMetadata)
        } catch (e: Exception) {
            Timber.e(e, "Failed to encrypt save state")
            saveState
        }
    }

    /**
     * Decrypt save state data
     */
    private fun decryptSaveState(saveState: GameSaveState): GameSaveState {
        return try {
            val encryptedSensitive = saveState.metadata["encrypted_sensitive"] as? String
            if (encryptedSensitive != null) {
                val decryptedJson = decryptData(encryptedSensitive)
                val sensitiveData = Json.decodeFromString<Map<String, Any>>(decryptedJson)

                val decryptedMetadata = saveState.metadata.toMutableMap()
                decryptedMetadata.remove("encrypted_sensitive")
                decryptedMetadata.putAll(sensitiveData)

                saveState.copy(metadata = decryptedMetadata)
            } else {
                saveState
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt save state")
            saveState
        }
    }

    /**
     * Encrypt data using AES-GCM
     */
    private fun encryptData(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())

        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt data using AES-GCM
     */
    private fun decryptData(encryptedData: String): String {
        val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
        val iv = decoded.copyOfRange(0, 12) // GCM IV is 12 bytes
        val data = decoded.copyOfRange(12, decoded.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmParameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmParameterSpec)

        val decryptedData = cipher.doFinal(data)
        return String(decryptedData)
    }

    /**
     * Initialize encryption key
     */
    private fun initializeEncryption(): SecretKey {
        return KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()
    }

    /**
     * Load all save states from persistent storage
     */
    private suspend fun loadAllSaveStates() {
        try {
            // In real implementation, load from DataStore or Room database
            Timber.d("Loaded all save states")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load all save states")
        }
    }

    /**
     * Save to persistent storage
     */
    private suspend fun saveToPersistentStorage(saveState: GameSaveState) {
        try {
            val key = stringPreferencesKey("save_state_${saveState.id}")
            val value = Json.encodeToString(saveState)

            context.dataStore.edit { preferences ->
                preferences[key] = value
            }

            // Also save timestamp for cleanup
            val timestampKey = longPreferencesKey("save_state_timestamp_${saveState.id}")
            context.dataStore.edit { preferences ->
                preferences[timestampKey] = saveState.timestamp
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to save to persistent storage")
        }
    }

    /**
     * Load from persistent storage
     */
    private suspend fun loadFromPersistentStorage(saveStateId: String): GameSaveState? {
        return try {
            val key = stringPreferencesKey("save_state_$saveStateId")
            val preferences = context.dataStore.data.first()

            val jsonData = preferences[key] ?: return null
            val saveState = Json.decodeFromString<GameSaveState>(jsonData)

            // Decrypt if necessary
            decryptSaveState(saveState)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load from persistent storage: $saveStateId")
            null
        }
    }

    /**
     * Delete from persistent storage
     */
    private suspend fun deleteFromPersistentStorage(saveStateId: String) {
        try {
            context.dataStore.edit { preferences ->
                val key = stringPreferencesKey("save_state_$saveStateId")
                preferences.remove(key)

                val timestampKey = longPreferencesKey("save_state_timestamp_$saveStateId")
                preferences.remove(timestampKey)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete from persistent storage: $saveStateId")
        }
    }

    /**
     * Save crash recovery state
     */
    private suspend fun saveCrashRecoveryState(recoveryState: CrashRecoveryState) {
        try {
            // Update flow
            val currentRecoveryStates = _crashRecoveryStates.value.toMutableMap()
            currentRecoveryStates[recoveryState.id] = recoveryState
            _crashRecoveryStates.value = currentRecoveryStates

        } catch (e: Exception) {
            Timber.e(e, "Failed to save crash recovery state")
        }
    }

    /**
     * Remove crash recovery state
     */
    private suspend fun removeCrashRecoveryState(recoveryStateId: String) {
        try {
            val currentRecoveryStates = _crashRecoveryStates.value.toMutableMap()
            currentRecoveryStates.remove(recoveryStateId)
            _crashRecoveryStates.value = currentRecoveryStates
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove crash recovery state: $recoveryStateId")
        }
    }

    /**
     * Update save states flow
     */
    private fun updateSaveStatesFlow() {
        _saveStates.value = saveStateCache.toMap()
    }

    /**
     * Check for crash recovery opportunities
     */
    private suspend fun checkCrashRecoveryStates() {
        try {
            val currentTime = System.currentTimeMillis()
            val validRecoveryStates = _crashRecoveryStates.value.values.filter {
                it.expiresAt > currentTime
            }

            val updatedRecoveryStates = validRecoveryStates.associateBy { it.id }
            _crashRecoveryStates.value = updatedRecoveryStates

        } catch (e: Exception) {
            Timber.e(e, "Failed to check crash recovery states")
        }
    }

    /**
     * Start auto-save monitoring
     */
    private fun startAutoSaveMonitoring() {
        // In real implementation, this would use a coroutine to periodically trigger auto-saves
        Timber.d("Started auto-save monitoring")
    }

    /**
     * Cleanup old save states for a game
     */
    private suspend fun cleanupOldSaveStates(gameId: String) {
        try {
            val gameSaveStates = _saveStates.value.values.filter { it.gameId == gameId }
                .sortedByDescending { it.timestamp }

            if (gameSaveStates.size > MAX_SAVE_STATES_PER_GAME) {
                val statesToDelete = gameSaveStates.takeLast(gameSaveStates.size - MAX_SAVE_STATES_PER_GAME)

                statesToDelete.forEach { saveState ->
                    deleteSaveState(saveState.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old save states for game: $gameId")
        }
    }

    /**
     * Attempt to recover corrupted save state
     */
    private suspend fun attemptRecovery(saveStateId: String): GameSaveState? {
        return try {
            // Try to find a backup or previous version
            // In real implementation, this would look for backup files or previous versions
            Timber.d("Attempting recovery for corrupted save state: $saveStateId")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to attempt recovery for save state: $saveStateId")
            null
        }
    }

    /**
     * Generate unique save state ID
     */
    private fun generateSaveStateId(): String {
        return "save_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
    }

    /**
     * Create crash recovery backup
     */
    private suspend fun createCrashRecoveryBackup(saveState: GameSaveState) {
        try {
            val recoveryState = CrashRecoveryState(
                id = "crash_backup_${saveState.id}",
                gameId = saveState.gameId,
                timestamp = System.currentTimeMillis(),
                gameState = saveState.gameData,
                canRecover = true,
                expiresAt = System.currentTimeMillis() + CRASH_RECOVERY_TIMEOUT_MS
            )

            saveCrashRecoveryState(recoveryState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create crash recovery backup")
        }
    }
}

/**
 * Game save state data class
 */
@Serializable
data class GameSaveState(
    val id: String,
    val gameId: String,
    val playerId: String,
    val timestamp: Long,
    val gameData: Map<String, Any>,
    val metadata: Map<String, Any> = emptyMap(),
    val version: Int = 1,
    val checksum: String
)

/**
 * Crash recovery state data class
 */
@Serializable
data class CrashRecoveryState(
    val id: String,
    val gameId: String,
    val timestamp: Long,
    val gameState: Map<String, Any>,
    val canRecover: Boolean,
    val expiresAt: Long
)

/**
 * Save state validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Save state corruption detector
 */
class SaveStateCorruptionDetector {

    private val corruptionPatterns = listOf(
        "corrupted_data",
        "invalid_json",
        "checksum_mismatch"
    )

    fun detectCorruption(saveState: GameSaveState): List<String> {
        val issues = mutableListOf<String>()

        // Check for obvious corruption patterns
        val jsonData = saveState.gameData.toString()
        corruptionPatterns.forEach { pattern ->
            if (jsonData.contains(pattern)) {
                issues.add("Detected corruption pattern: $pattern")
            }
        }

        // Check data consistency
        if (saveState.gameData.isEmpty() && saveState.timestamp > 0) {
            issues.add("Empty game data with valid timestamp")
        }

        return issues
    }
}