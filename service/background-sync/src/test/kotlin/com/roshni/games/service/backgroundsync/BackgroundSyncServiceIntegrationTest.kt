package com.roshni.games.service.backgroundsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roshni.games.core.utils.testing.TestConstants
import com.roshni.games.service.backgroundsync.data.datasource.LocalSyncDataSource
import com.roshni.games.service.backgroundsync.data.repository.SyncRepositoryImpl
import com.roshni.games.service.backgroundsync.domain.model.SyncConfigDomain
import com.roshni.games.service.backgroundsync.domain.model.SyncPriority
import com.roshni.games.service.backgroundsync.domain.model.SyncType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundSyncServiceIntegrationTest {

    private lateinit var dataSource: LocalSyncDataSource
    private lateinit var repository: com.roshni.games.service.backgroundsync.data.repository.SyncRepository
    private lateinit var syncService: BackgroundSyncService

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        dataSource = LocalSyncDataSource()
        repository = SyncRepositoryImpl(dataSource)
        syncService = BackgroundSyncService(repository)
    }

    @Test
    fun testSyncServiceInitialization() = runTest {
        // When
        val result = syncService.initialize()

        // Then
        assertTrue("Initialization should succeed", result.isSuccess)
    }

    @Test
    fun testSyncConfigOperations() = runTest {
        // Given
        val initialConfig = syncService.getSyncConfig().first()

        // When
        val newConfig = SyncConfigDomain(
            enabled = !initialConfig.enabled,
            syncOnWifiOnly = !initialConfig.syncOnWifiOnly,
            maxRetryAttempts = initialConfig.maxRetryAttempts + 1
        )

        val updateResult = syncService.updateSyncConfig(newConfig)

        // Then
        assertTrue("Config update should succeed", updateResult.isSuccess)

        val updatedConfig = syncService.getSyncConfig().first()
        assertEquals("Config should be updated", newConfig.enabled, updatedConfig.enabled)
        assertEquals("Config should be updated", newConfig.syncOnWifiOnly, updatedConfig.syncOnWifiOnly)
        assertEquals("Config should be updated", newConfig.maxRetryAttempts, updatedConfig.maxRetryAttempts)
    }

    @Test
    fun testScoreUploadEnqueue() = runTest {
        // When
        val result = syncService.enqueueScoreUpload(
            gameId = TestConstants.TEST_GAME_ID,
            playerId = TestConstants.TEST_PLAYER_ID,
            score = 10000,
            level = 5,
            metadata = mapOf("difficulty" to "hard")
        )

        // Then
        assertTrue("Score upload enqueue should succeed", result.isSuccess)
        assertNotNull("Should return operation ID", result.getOrNull())

        val pendingOperations = syncService.getPendingOperations().first()
        assertTrue("Should have pending operations", pendingOperations.isNotEmpty())

        val scoreOperation = pendingOperations.find {
            it.type == SyncType.UPLOAD_SCORE &&
            it.data["gameId"] == TestConstants.TEST_GAME_ID
        }
        assertNotNull("Should find score upload operation", scoreOperation)
    }

    @Test
    fun testAchievementUploadEnqueue() = runTest {
        // When
        val result = syncService.enqueueAchievementUpload(
            playerId = TestConstants.TEST_PLAYER_ID,
            achievementId = "test-achievement",
            metadata = mapOf("source" to "gameplay")
        )

        // Then
        assertTrue("Achievement upload enqueue should succeed", result.isSuccess)

        val pendingOperations = syncService.getPendingOperations().first()
        val achievementOperation = pendingOperations.find {
            it.type == SyncType.UPLOAD_ACHIEVEMENT &&
            it.data["achievementId"] == "test-achievement"
        }
        assertNotNull("Should find achievement upload operation", achievementOperation)
    }

    @Test
    fun testNetworkStateMonitoring() = runTest {
        // When
        val networkState = syncService.getNetworkState().first()

        // Then
        assertNotNull("Network state should not be null", networkState)
        // Note: In a real test environment, you might want to mock network state
    }

    @Test
    fun testSyncStats() = runTest {
        // Given
        syncService.enqueueScoreUpload("game1", "player1", 1000)

        // When
        val stats = syncService.getSyncStats().first()

        // Then
        assertNotNull("Sync stats should not be null", stats)
        assertTrue("Total operations should be at least 1", stats.totalOperations >= 1)
        assertTrue("Pending operations should be at least 1", stats.pendingOperations >= 1)
    }

    @Test
    fun testOperationFiltering() = runTest {
        // Given
        syncService.enqueueScoreUpload("game1", "player1", 1000)
        syncService.enqueueAchievementUpload("player1", "achievement1")

        // When
        val scoreOperations = syncService.getOperationsByType(SyncType.UPLOAD_SCORE).first()
        val achievementOperations = syncService.getOperationsByType(SyncType.UPLOAD_ACHIEVEMENT).first()

        // Then
        assertTrue("Should have score operations", scoreOperations.isNotEmpty())
        assertTrue("Should have achievement operations", achievementOperations.isNotEmpty())
        assertEquals("All score operations should be score type",
            SyncType.UPLOAD_SCORE, scoreOperations.first().type)
        assertEquals("All achievement operations should be achievement type",
            SyncType.UPLOAD_ACHIEVEMENT, achievementOperations.first().type)
    }

    @Test
    fun testHighPriorityOperations() = runTest {
        // Given
        syncService.enqueueScoreUpload("game1", "player1", 1000) // Normal priority
        syncService.enqueueAchievementUpload("player1", "achievement1") // High priority

        // When
        val highPriorityOps = syncService.getHighPriorityOperations().first()

        // Then
        // Achievement uploads are marked as HIGH priority in the service
        assertTrue("Should have high priority operations", highPriorityOps.isNotEmpty())
    }

    @Test
    fun testClearOldOperations() = runTest {
        // Given
        syncService.enqueueScoreUpload("game1", "player1", 1000)

        // When
        val clearResult = syncService.clearOldOperations(System.currentTimeMillis() + 1000) // Future time

        // Then
        assertTrue("Clear operation should succeed", clearResult.isSuccess)
    }
}