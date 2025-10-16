package com.roshni.games.core.utils.savestate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class SaveStateManagerTest {

    private lateinit var saveStateManager: SaveStateManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        saveStateManager = SaveStateManager(context)
    }

    @Test
    fun testInitialization() = runTest {
        // When
        val result = saveStateManager.initialize()

        // Then
        assertTrue("Initialization should succeed", result.isSuccess)
    }

    @Test
    fun testSaveGameState() = runTest {
        // Given
        val gameId = "test_game_1"
        val playerId = "test_player_1"
        val gameData = mapOf(
            "level" to 5,
            "score" to 10000,
            "inventory" to listOf("sword", "shield")
        )

        // When
        val result = saveStateManager.saveGameState(gameId, playerId, gameData)

        // Then
        assertTrue("Save should succeed", result.isSuccess)
        assertNotNull("Should return save state ID", result.getOrNull())

        val saveStateId = result.getOrNull()!!
        val loadResult = saveStateManager.loadGameState(saveStateId)

        assertTrue("Load should succeed", loadResult.isSuccess)
        val loadedState = loadResult.getOrNull()!!
        assertEquals("Game ID should match", gameId, loadedState.gameId)
        assertEquals("Player ID should match", playerId, loadedState.playerId)
        assertEquals("Game data should match", gameData, loadedState.gameData)
    }

    @Test
    fun testSaveStateValidation() = runTest {
        // Given
        val gameId = "test_game_2"
        val playerId = "test_player_2"
        val gameData = mapOf("level" to 1)

        // When
        val result = saveStateManager.saveGameState(gameId, playerId, gameData)

        // Then
        assertTrue("Save should succeed", result.isSuccess)

        val saveStateId = result.getOrNull()!!
        val loadResult = saveStateManager.loadGameState(saveStateId)

        assertTrue("Load should succeed", loadResult.isSuccess)
        val loadedState = loadResult.getOrNull()!!

        // Verify checksum is calculated and valid
        assertTrue("Checksum should not be empty", loadedState.checksum.isNotEmpty())
    }

    @Test
    fun testGetSaveStatesForGame() = runTest {
        // Given
        val gameId = "test_game_3"
        val playerId = "test_player_3"
        val gameData1 = mapOf("level" to 1, "score" to 100)
        val gameData2 = mapOf("level" to 2, "score" to 200)

        // When
        val saveResult1 = saveStateManager.saveGameState(gameId, playerId, gameData1)
        val saveResult2 = saveStateManager.saveGameState(gameId, playerId, gameData2)

        // Then
        assertTrue("First save should succeed", saveResult1.isSuccess)
        assertTrue("Second save should succeed", saveResult2.isSuccess)

        val getResult = saveStateManager.getSaveStatesForGame(gameId)
        assertTrue("Get should succeed", getResult.isSuccess)

        val saveStates = getResult.getOrNull()!!
        assertEquals("Should have 2 save states", 2, saveStates.size)

        // Should be sorted by timestamp descending (newest first)
        assertTrue("Should be sorted by timestamp", saveStates[0].timestamp >= saveStates[1].timestamp)
    }

    @Test
    fun testDeleteSaveState() = runTest {
        // Given
        val gameId = "test_game_4"
        val playerId = "test_player_4"
        val gameData = mapOf("level" to 1)

        val saveResult = saveStateManager.saveGameState(gameId, playerId, gameData)
        assertTrue("Save should succeed", saveResult.isSuccess)

        val saveStateId = saveResult.getOrNull()!!

        // When
        val deleteResult = saveStateManager.deleteSaveState(saveStateId)

        // Then
        assertTrue("Delete should succeed", deleteResult.isSuccess)

        val loadResult = saveStateManager.loadGameState(saveStateId)
        assertTrue("Load should fail after deletion", loadResult.isFailure)
    }

    @Test
    fun testCrashRecoveryBackup() = runTest {
        // Given
        val gameId = "test_game_5"
        val gameState = mapOf(
            "level" to 3,
            "score" to 5000,
            "health" to 75
        )

        // When
        val backupResult = saveStateManager.createCrashRecoveryBackup(gameState, gameId)

        // Then
        assertTrue("Backup creation should succeed", backupResult.isSuccess)

        val recoveryResult = saveStateManager.attemptCrashRecovery(gameId)
        assertTrue("Recovery should succeed", recoveryResult.isSuccess)

        val recoveredState = recoveryResult.getOrNull()
        assertNotNull("Should recover game state", recoveredState)
        assertEquals("Game ID should match", gameId, recoveredState?.gameId)
        assertEquals("Game state should match", gameState, recoveredState?.gameData)
    }

    @Test
    fun testSaveStateWithMetadata() = runTest {
        // Given
        val gameId = "test_game_6"
        val playerId = "test_player_6"
        val gameData = mapOf("level" to 1)
        val metadata = mapOf(
            "difficulty" to "hard",
            "game_mode" to "adventure",
            "session_duration" to 3600
        )

        // When
        val result = saveStateManager.saveGameState(gameId, playerId, gameData, metadata)

        // Then
        assertTrue("Save should succeed", result.isSuccess)

        val saveStateId = result.getOrNull()!!
        val loadResult = saveStateManager.loadGameState(saveStateId)

        assertTrue("Load should succeed", loadResult.isSuccess)
        val loadedState = loadResult.getOrNull()!!

        assertEquals("Metadata should match", metadata, loadedState.metadata)
    }

    @Test
    fun testConcurrentSaveStates() = runTest {
        // Given
        val gameId = "test_game_7"
        val playerId = "test_player_7"

        // When - Save multiple states concurrently
        val results = (1..5).map { index ->
            val gameData = mapOf("level" to index, "score" to index * 100)
            saveStateManager.saveGameState(gameId, playerId, gameData)
        }

        // Then
        results.forEach { result ->
            assertTrue("All saves should succeed", result.isSuccess)
        }

        val getResult = saveStateManager.getSaveStatesForGame(gameId)
        assertTrue("Get should succeed", getResult.isSuccess)

        val saveStates = getResult.getOrNull()!!
        assertEquals("Should have 5 save states", 5, saveStates.size)
    }

    @Test
    fun testSaveStateLimits() = runTest {
        // Given
        val gameId = "test_game_8"
        val playerId = "test_player_8"

        // When - Save more than the maximum allowed states
        val results = (1..15).map { index ->
            val gameData = mapOf("level" to index, "score" to index * 100)
            saveStateManager.saveGameState(gameId, playerId, gameData)
        }

        // Then
        results.forEach { result ->
            assertTrue("All saves should succeed", result.isSuccess)
        }

        val getResult = saveStateManager.getSaveStatesForGame(gameId)
        assertTrue("Get should succeed", getResult.isSuccess)

        val saveStates = getResult.getOrNull()!!
        // Should be limited to maximum allowed states
        assertTrue("Should not exceed maximum states", saveStates.size <= 10)
    }
}