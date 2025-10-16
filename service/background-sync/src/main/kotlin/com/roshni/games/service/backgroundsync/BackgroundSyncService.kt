package com.roshni.games.service.backgroundsync

import com.roshni.games.service.backgroundsync.data.model.SyncOperation
import com.roshni.games.service.backgroundsync.data.model.SyncStatus
import com.roshni.games.service.backgroundsync.domain.model.NetworkState
import com.roshni.games.service.backgroundsync.domain.model.SyncConfigDomain
import com.roshni.games.service.backgroundsync.domain.model.SyncOperationDomain
import com.roshni.games.service.backgroundsync.domain.model.SyncPriority
import com.roshni.games.service.backgroundsync.domain.model.SyncResult
import com.roshni.games.service.backgroundsync.domain.model.SyncStatsDomain
import com.roshni.games.service.backgroundsync.domain.model.SyncType
import com.roshni.games.service.backgroundsync.domain.model.toData
import com.roshni.games.service.backgroundsync.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID

/**
 * Main service class for background synchronization
 */
class BackgroundSyncService(
    private val syncRepository: com.roshni.games.service.backgroundsync.data.repository.SyncRepository
) {

    /**
     * Initialize the sync service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing BackgroundSyncService")

            // Clear old operations on startup
            clearOldOperations(System.currentTimeMillis() - (24 * 60 * 60 * 1000)) // 24 hours

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize BackgroundSyncService")
            Result.failure(e)
        }
    }

    /**
     * Get sync configuration
     */
    fun getSyncConfig(): Flow<SyncConfigDomain> {
        return syncRepository.getSyncConfig().map { it.toDomain() }
    }

    /**
     * Update sync configuration
     */
    suspend fun updateSyncConfig(config: SyncConfigDomain): Result<Unit> {
        return try {
            syncRepository.updateSyncConfig(config.toData())
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update sync config")
            Result.failure(e)
        }
    }

    /**
     * Get sync statistics
     */
    fun getSyncStats(): Flow<SyncStatsDomain> {
        return syncRepository.getSyncStats().map { it.toDomain() }
    }

    /**
     * Check if device is online and ready for sync
     */
    fun getNetworkState(): Flow<NetworkState> {
        return syncRepository.isOnline().map { isOnline ->
            NetworkState(isOnline = isOnline)
        }
    }

    /**
     * Enqueue a sync operation
     */
    suspend fun enqueueSyncOperation(
        type: SyncType,
        data: Map<String, Any>,
        priority: SyncPriority = SyncPriority.NORMAL
    ): Result<String> {
        return try {
            val operation = SyncOperation(
                id = UUID.randomUUID().toString(),
                type = type.toData(),
                data = data,
                timestamp = System.currentTimeMillis(),
                priority = priority.toData()
            )

            syncRepository.enqueueOperation(operation)
            Timber.d("Enqueued sync operation: ${operation.id} - $type")

            Result.success(operation.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to enqueue sync operation")
            Result.failure(e)
        }
    }

    /**
     * Enqueue score upload
     */
    suspend fun enqueueScoreUpload(
        gameId: String,
        playerId: String,
        score: Long,
        level: Int = 1,
        metadata: Map<String, Any> = emptyMap()
    ): Result<String> {
        val data = mapOf(
            "gameId" to gameId,
            "playerId" to playerId,
            "score" to score,
            "level" to level,
            "metadata" to metadata
        )

        return enqueueSyncOperation(
            type = SyncType.UPLOAD_SCORE,
            data = data,
            priority = SyncPriority.NORMAL
        )
    }

    /**
     * Enqueue achievement upload
     */
    suspend fun enqueueAchievementUpload(
        playerId: String,
        achievementId: String,
        metadata: Map<String, Any> = emptyMap()
    ): Result<String> {
        val data = mapOf(
            "playerId" to playerId,
            "achievementId" to achievementId,
            "metadata" to metadata
        )

        return enqueueSyncOperation(
            type = SyncType.UPLOAD_ACHIEVEMENT,
            data = data,
            priority = SyncPriority.HIGH
        )
    }

    /**
     * Enqueue game download
     */
    suspend fun enqueueGameDownload(
        gameId: String,
        version: String
    ): Result<String> {
        val data = mapOf(
            "gameId" to gameId,
            "version" to version
        )

        return enqueueSyncOperation(
            type = SyncType.DOWNLOAD_GAMES,
            data = data,
            priority = SyncPriority.NORMAL
        )
    }

    /**
     * Get pending operations
     */
    fun getPendingOperations(): Flow<List<SyncOperationDomain>> {
        return syncRepository.getPendingOperations().map { operations ->
            operations.map { it.toDomain() }
        }
    }

    /**
     * Get operations by type
     */
    fun getOperationsByType(type: SyncType): Flow<List<SyncOperationDomain>> {
        return syncRepository.getOperationsByType(type.toData()).map { operations ->
            operations.map { it.toDomain() }
        }
    }

    /**
     * Get high priority operations
     */
    fun getHighPriorityOperations(): Flow<List<SyncOperationDomain>> {
        return syncRepository.getHighPriorityOperations().map { operations ->
            operations.map { it.toDomain() }
        }
    }

    /**
     * Process pending operations (should be called by WorkManager)
     */
    suspend fun processPendingOperations(): Result<Int> {
        return try {
            val config = syncRepository.getSyncConfig().first()
            if (!config.enabled) {
                Timber.d("Sync is disabled, skipping processing")
                return Result.success(0)
            }

            val networkState = getNetworkState().first()
            if (!networkState.isOnline) {
                Timber.d("Device is offline, skipping sync")
                return Result.success(0)
            }

            if (config.syncOnWifiOnly && !networkState.isWifi) {
                Timber.d("Sync on WiFi only and not on WiFi, skipping sync")
                return Result.success(0)
            }

            val pendingOps = syncRepository.getPendingOperations().first()
            val operationsToProcess = pendingOps.take(config.maxOperationsPerBatch)

            var processedCount = 0
            operationsToProcess.forEach { operation ->
                try {
                    syncRepository.updateOperationStatus(
                        operation.id,
                        SyncStatus.InProgress
                    )

                    // Process the operation (in real implementation, this would call the appropriate API)
                    val result = processOperation(operation)

                    when (result) {
                        is SyncResult.Success -> {
                            syncRepository.updateOperationStatus(
                                operation.id,
                                SyncStatus.Completed(System.currentTimeMillis())
                            )
                            syncRepository.dequeueOperation(operation.id)
                            processedCount++
                        }
                        is SyncResult.Error -> {
                            syncRepository.updateOperationStatus(
                                operation.id,
                                SyncStatus.Failed(result.error, System.currentTimeMillis())
                            )
                            // Could implement retry logic here
                        }
                        is SyncResult.Retry -> {
                            // Update retry count and reschedule
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to process operation ${operation.id}")
                    syncRepository.updateOperationStatus(
                        operation.id,
                        SyncStatus.Failed(e.message ?: "Unknown error", System.currentTimeMillis())
                    )
                }
            }

            Timber.d("Processed $processedCount operations")
            Result.success(processedCount)

        } catch (e: Exception) {
            Timber.e(e, "Failed to process pending operations")
            Result.failure(e)
        }
    }

    /**
     * Clear old operations
     */
    suspend fun clearOldOperations(olderThanMillis: Long): Result<Unit> {
        return try {
            syncRepository.clearOldOperations(olderThanMillis)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear old operations")
            Result.failure(e)
        }
    }

    /**
     * Process a single sync operation
     */
    private suspend fun processOperation(operation: SyncOperation): SyncResult {
        return try {
            // In a real implementation, this would call the appropriate API based on operation type
            // For now, we'll simulate the processing

            when (operation.type) {
                com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_SCORE -> {
                    // Simulate API call
                    kotlinx.coroutines.delay(100)
                    SyncResult.Success(operation.id, 256)
                }
                com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_ACHIEVEMENT -> {
                    // Simulate API call
                    kotlinx.coroutines.delay(150)
                    SyncResult.Success(operation.id, 128)
                }
                com.roshni.games.service.backgroundsync.data.model.SyncType.DOWNLOAD_GAMES -> {
                    // Simulate API call
                    kotlinx.coroutines.delay(500)
                    SyncResult.Success(operation.id, 1024)
                }
                else -> {
                    SyncResult.Error(operation.id, "Unsupported operation type")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to process operation ${operation.id}")
            SyncResult.Error(operation.id, e.message ?: "Unknown error")
        }
    }
}