package com.roshni.games.service.backgroundsync.data.repository

import com.roshni.games.service.backgroundsync.data.datasource.SyncDataSource
import com.roshni.games.service.backgroundsync.data.model.SyncConfig
import com.roshni.games.service.backgroundsync.data.model.SyncOperation
import com.roshni.games.service.backgroundsync.data.model.SyncStats
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository interface for sync operations
 */
interface SyncRepository {

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
     * Clear old operations
     */
    suspend fun clearOldOperations(olderThanMillis: Long)

    /**
     * Check if device is online
     */
    fun isOnline(): Flow<Boolean>

    /**
     * Get operations by type
     */
    fun getOperationsByType(type: com.roshni.games.service.backgroundsync.data.model.SyncType): Flow<List<SyncOperation>>

    /**
     * Get high priority operations
     */
    fun getHighPriorityOperations(): Flow<List<SyncOperation>>
}

/**
 * Implementation of sync repository
 */
class SyncRepositoryImpl(
    private val dataSource: SyncDataSource
) : SyncRepository {

    override fun getSyncConfig(): Flow<SyncConfig> {
        return dataSource.getSyncConfig()
    }

    override suspend fun updateSyncConfig(config: SyncConfig) {
        Timber.d("Repository: Updating sync config")
        dataSource.updateSyncConfig(config)
    }

    override fun getPendingOperations(): Flow<List<SyncOperation>> {
        return dataSource.getPendingOperations()
    }

    override suspend fun enqueueOperation(operation: SyncOperation) {
        Timber.d("Repository: Enqueueing operation ${operation.id}")
        dataSource.enqueueOperation(operation)
    }

    override suspend fun dequeueOperation(operationId: String) {
        Timber.d("Repository: Dequeueing operation $operationId")
        dataSource.dequeueOperation(operationId)
    }

    override suspend fun updateOperationStatus(operationId: String, status: com.roshni.games.service.backgroundsync.data.model.SyncStatus) {
        Timber.d("Repository: Updating operation status $operationId")
        dataSource.updateOperationStatus(operationId, status)
    }

    override fun getSyncStats(): Flow<SyncStats> {
        return dataSource.getSyncStats()
    }

    override suspend fun clearOldOperations(olderThanMillis: Long) {
        Timber.d("Repository: Clearing old operations")
        dataSource.clearOldOperations(olderThanMillis)
    }

    override fun isOnline(): Flow<Boolean> {
        return dataSource.isOnline()
    }

    override fun getOperationsByType(type: com.roshni.games.service.backgroundsync.data.model.SyncType): Flow<List<SyncOperation>> {
        return dataSource.getPendingOperations().let { flow ->
            kotlinx.coroutines.flow.map { operations ->
                operations.filter { it.type == type }
            }
        }
    }

    override fun getHighPriorityOperations(): Flow<List<SyncOperation>> {
        return dataSource.getPendingOperations().let { flow ->
            kotlinx.coroutines.flow.map { operations ->
                operations.filter { it.priority == com.roshni.games.service.backgroundsync.data.model.SyncPriority.HIGH ||
                                   it.priority == com.roshni.games.service.backgroundsync.data.model.SyncPriority.CRITICAL }
                    .sortedByDescending { it.priority }
            }
        }
    }
}