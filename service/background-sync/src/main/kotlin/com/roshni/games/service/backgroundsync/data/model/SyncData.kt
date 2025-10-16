package com.roshni.games.service.backgroundsync.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for background synchronization
 */

/**
 * Represents a sync operation
 */
@Serializable
data class SyncOperation(
    val id: String,
    val type: SyncType,
    val data: Map<String, Any>,
    val timestamp: Long,
    val priority: SyncPriority = SyncPriority.NORMAL,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

/**
 * Types of sync operations
 */
@Serializable
enum class SyncType {
    UPLOAD_SCORE,
    UPLOAD_ACHIEVEMENT,
    DOWNLOAD_GAMES,
    DOWNLOAD_ACHIEVEMENTS,
    SYNC_PROFILE,
    SYNC_SETTINGS
}

/**
 * Priority levels for sync operations
 */
@Serializable
enum class SyncPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

/**
 * Sync status
 */
@Serializable
sealed class SyncStatus {
    @Serializable
    data object Pending : SyncStatus()

    @Serializable
    data object InProgress : SyncStatus()

    @Serializable
    data class Completed(val timestamp: Long) : SyncStatus()

    @Serializable
    data class Failed(val error: String, val timestamp: Long) : SyncStatus()
}

/**
 * Sync configuration
 */
@Serializable
data class SyncConfig(
    val enabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val maxRetryAttempts: Int = 3,
    val syncIntervalMinutes: Long = 15,
    val maxOperationsPerBatch: Int = 50,
    val enableBackgroundSync: Boolean = true
)

/**
 * Sync statistics
 */
@Serializable
data class SyncStats(
    val totalOperations: Long = 0,
    val pendingOperations: Long = 0,
    val completedOperations: Long = 0,
    val failedOperations: Long = 0,
    val lastSyncTime: Long? = null,
    val averageSyncTime: Long = 0,
    val dataTransferred: Long = 0 // in bytes
)