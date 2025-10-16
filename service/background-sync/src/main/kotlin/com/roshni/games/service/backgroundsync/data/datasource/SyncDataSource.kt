package com.roshni.games.service.backgroundsync.data.datasource

import com.roshni.games.service.backgroundsync.data.model.SyncConfig
import com.roshni.games.service.backgroundsync.data.model.SyncOperation
import com.roshni.games.service.backgroundsync.data.model.SyncStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Data source interface for sync operations
 */
interface SyncDataSource {

    /**
     * Get sync configuration
     */
    fun getSyncConfig(): Flow<SyncConfig>

    /**
     * Update sync configuration
     */
    suspend fun updateSyncConfig(config: SyncConfig)

    /**
     * Get pending sync operations
     */
    fun getPendingOperations(): Flow<List<SyncOperation>>

    /**
     * Add a sync operation to the queue
     */
    suspend fun enqueueOperation(operation: SyncOperation)

    /**
     * Remove a sync operation from the queue
     */
    suspend fun dequeueOperation(operationId: String)

    /**
     * Update sync operation status
     */
    suspend fun updateOperationStatus(operationId: String, status: com.roshni.games.service.backgroundsync.data.model.SyncStatus)

    /**
     * Get sync statistics
     */
    fun getSyncStats(): Flow<SyncStats>

    /**
     * Update sync statistics
     */
    suspend fun updateSyncStats(stats: SyncStats)

    /**
     * Clear all completed operations older than specified time
     */
    suspend fun clearOldOperations(olderThanMillis: Long)

    /**
     * Check if device is online and ready for sync
     */
    fun isOnline(): Flow<Boolean>
}

/**
 * Local implementation of sync data source
 */
class LocalSyncDataSource : SyncDataSource {

    private val _syncConfig = MutableStateFlow(
        SyncConfig(
            enabled = true,
            syncOnWifiOnly = false,
            maxRetryAttempts = 3,
            syncIntervalMinutes = 15,
            maxOperationsPerBatch = 50,
            enableBackgroundSync = true
        )
    )

    private val _pendingOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    private val _syncStats = MutableStateFlow(SyncStats())
    private val _isOnline = MutableStateFlow(true)

    override fun getSyncConfig(): Flow<SyncConfig> = _syncConfig.asStateFlow()

    override suspend fun updateSyncConfig(config: SyncConfig) {
        Timber.d("Updating sync config: $config")
        _syncConfig.value = config
    }

    override fun getPendingOperations(): Flow<List<SyncOperation>> = _pendingOperations.asStateFlow()

    override suspend fun enqueueOperation(operation: SyncOperation) {
        Timber.d("Enqueueing sync operation: ${operation.id} - ${operation.type}")
        val currentOperations = _pendingOperations.value.toMutableList()
        currentOperations.add(operation)
        _pendingOperations.value = currentOperations

        // Update stats
        updateStats { it.copy(totalOperations = it.totalOperations + 1, pendingOperations = it.pendingOperations + 1) }
    }

    override suspend fun dequeueOperation(operationId: String) {
        Timber.d("Dequeueing sync operation: $operationId")
        val currentOperations = _pendingOperations.value.toMutableList()
        currentOperations.removeAll { it.id == operationId }
        _pendingOperations.value = currentOperations

        // Update stats
        updateStats { it.copy(pendingOperations = it.pendingOperations - 1) }
    }

    override suspend fun updateOperationStatus(operationId: String, status: com.roshni.games.service.backgroundsync.data.model.SyncStatus) {
        Timber.d("Updating operation status: $operationId - $status")

        when (status) {
            is com.roshni.games.service.backgroundsync.data.model.SyncStatus.Completed -> {
                updateStats { it.copy(
                    completedOperations = it.completedOperations + 1,
                    pendingOperations = it.pendingOperations - 1,
                    lastSyncTime = status.timestamp
                )}
            }
            is com.roshni.games.service.backgroundsync.data.model.SyncStatus.Failed -> {
                updateStats { it.copy(
                    failedOperations = it.failedOperations + 1,
                    pendingOperations = it.pendingOperations - 1
                )}
            }
            else -> { /* Handle other states if needed */ }
        }
    }

    override fun getSyncStats(): Flow<SyncStats> = _syncStats.asStateFlow()

    override suspend fun updateSyncStats(stats: SyncStats) {
        _syncStats.value = stats
    }

    override suspend fun clearOldOperations(olderThanMillis: Long) {
        val currentTime = System.currentTimeMillis()
        val currentOperations = _pendingOperations.value.toMutableList()
        val operationsToRemove = currentOperations.filter {
            (currentTime - it.timestamp) > olderThanMillis
        }

        currentOperations.removeAll(operationsToRemove)
        _pendingOperations.value = currentOperations

        Timber.d("Cleared ${operationsToRemove.size} old operations")
    }

    override fun isOnline(): Flow<Boolean> = _isOnline.asStateFlow()

    private suspend fun updateStats(update: (SyncStats) -> SyncStats) {
        _syncStats.value = update(_syncStats.value)
    }

    /**
     * Simulate network status changes
     */
    fun simulateNetworkStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
        Timber.d("Network status changed: $isOnline")
    }
}