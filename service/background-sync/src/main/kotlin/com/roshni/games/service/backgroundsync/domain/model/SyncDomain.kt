package com.roshni.games.service.backgroundsync.domain.model

import com.roshni.games.service.backgroundsync.data.model.SyncConfig
import com.roshni.games.service.backgroundsync.data.model.SyncOperation
import com.roshni.games.service.backgroundsync.data.model.SyncStats
import kotlinx.datetime.Instant

/**
 * Domain models for background synchronization
 */

/**
 * Domain model for sync operation
 */
data class SyncOperationDomain(
    val id: String,
    val type: SyncType,
    val data: Map<String, Any>,
    val timestamp: Instant,
    val priority: SyncPriority = SyncPriority.NORMAL,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

/**
 * Domain enum for sync types
 */
enum class SyncType {
    UPLOAD_SCORE,
    UPLOAD_ACHIEVEMENT,
    DOWNLOAD_GAMES,
    DOWNLOAD_ACHIEVEMENTS,
    SYNC_PROFILE,
    SYNC_SETTINGS
}

/**
 * Domain enum for sync priority
 */
enum class SyncPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

/**
 * Domain model for sync status
 */
sealed class SyncStatus {
    data object Pending : SyncStatus()
    data object InProgress : SyncStatus()
    data class Completed(val timestamp: Instant) : SyncStatus()
    data class Failed(val error: String, val timestamp: Instant) : SyncStatus()
}

/**
 * Domain model for sync configuration
 */
data class SyncConfigDomain(
    val enabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val maxRetryAttempts: Int = 3,
    val syncIntervalMinutes: Long = 15,
    val maxOperationsPerBatch: Int = 50,
    val enableBackgroundSync: Boolean = true
)

/**
 * Domain model for sync statistics
 */
data class SyncStatsDomain(
    val totalOperations: Long = 0,
    val pendingOperations: Long = 0,
    val completedOperations: Long = 0,
    val failedOperations: Long = 0,
    val lastSyncTime: Instant? = null,
    val averageSyncTime: Long = 0,
    val dataTransferred: Long = 0 // in bytes
)

/**
 * Sync result
 */
sealed class SyncResult {
    data class Success(val operationId: String, val bytesTransferred: Long) : SyncResult()
    data class Error(val operationId: String, val error: String) : SyncResult()
    data class Retry(val operationId: String, val retryAfterMillis: Long) : SyncResult()
}

/**
 * Network state
 */
data class NetworkState(
    val isOnline: Boolean,
    val isWifi: Boolean = false,
    val isMetered: Boolean = false
)

/**
 * Extension functions for converting between data and domain models
 */
fun SyncOperation.toDomain(): SyncOperationDomain {
    return SyncOperationDomain(
        id = id,
        type = type.toDomain(),
        data = data,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        priority = priority.toDomain(),
        retryCount = retryCount,
        maxRetries = maxRetries
    )
}

fun com.roshni.games.service.backgroundsync.data.model.SyncType.toDomain(): SyncType {
    return when (this) {
        com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_SCORE -> SyncType.UPLOAD_SCORE
        com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_ACHIEVEMENT -> SyncType.UPLOAD_ACHIEVEMENT
        com.roshni.games.service.backgroundsync.data.model.SyncType.DOWNLOAD_GAMES -> SyncType.DOWNLOAD_GAMES
        com.roshni.games.service.backgroundsync.data.model.SyncType.DOWNLOAD_ACHIEVEMENTS -> SyncType.DOWNLOAD_ACHIEVEMENTS
        com.roshni.games.service.backgroundsync.data.model.SyncType.SYNC_PROFILE -> SyncType.SYNC_PROFILE
        com.roshni.games.service.backgroundsync.data.model.SyncType.SYNC_SETTINGS -> SyncType.SYNC_SETTINGS
    }
}

fun com.roshni.games.service.backgroundsync.data.model.SyncPriority.toDomain(): SyncPriority {
    return when (this) {
        com.roshni.games.service.backgroundsync.data.model.SyncPriority.LOW -> SyncPriority.LOW
        com.roshni.games.service.backgroundsync.data.model.SyncPriority.NORMAL -> SyncPriority.NORMAL
        com.roshni.games.service.backgroundsync.data.model.SyncPriority.HIGH -> SyncPriority.HIGH
        com.roshni.games.service.backgroundsync.data.model.SyncPriority.CRITICAL -> SyncPriority.CRITICAL
    }
}

fun com.roshni.games.service.backgroundsync.data.model.SyncStatus.toDomain(): SyncStatus {
    return when (this) {
        is com.roshni.games.service.backgroundsync.data.model.SyncStatus.Pending -> SyncStatus.Pending
        is com.roshni.games.service.backgroundsync.data.model.SyncStatus.InProgress -> SyncStatus.InProgress
        is com.roshni.games.service.backgroundsync.data.model.SyncStatus.Completed ->
            SyncStatus.Completed(Instant.fromEpochMilliseconds(this.timestamp))
        is com.roshni.games.service.backgroundsync.data.model.SyncStatus.Failed ->
            SyncStatus.Failed(this.error, Instant.fromEpochMilliseconds(this.timestamp))
    }
}

fun SyncOperationDomain.toData(): SyncOperation {
    return SyncOperation(
        id = id,
        type = type.toData(),
        data = data,
        timestamp = timestamp.toEpochMilliseconds(),
        priority = priority.toData(),
        retryCount = retryCount,
        maxRetries = maxRetries
    )
}

fun SyncType.toData(): com.roshni.games.service.backgroundsync.data.model.SyncType {
    return when (this) {
        SyncType.UPLOAD_SCORE -> com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_SCORE
        SyncType.UPLOAD_ACHIEVEMENT -> com.roshni.games.service.backgroundsync.data.model.SyncType.UPLOAD_ACHIEVEMENT
        SyncType.DOWNLOAD_GAMES -> com.roshni.games.service.backgroundsync.data.model.SyncType.DOWNLOAD_GAMES
        SyncType.DOWNLOAD_ACHIEVEMENTS -> com.roshni.games.service.backgroundsync.data.model.SyncType.DOWNLOAD_ACHIEVEMENTS
        SyncType.SYNC_PROFILE -> com.roshni.games.service.backgroundsync.data.model.SyncType.SYNC_PROFILE
        SyncType.SYNC_SETTINGS -> com.roshni.games.service.backgroundsync.data.model.SyncType.SYNC_SETTINGS
    }
}

fun SyncPriority.toData(): com.roshni.games.service.backgroundsync.data.model.SyncPriority {
    return when (this) {
        SyncPriority.LOW -> com.roshni.games.service.backgroundsync.data.model.SyncPriority.LOW
        SyncPriority.NORMAL -> com.roshni.games.service.backgroundsync.data.model.SyncPriority.NORMAL
        SyncPriority.HIGH -> com.roshni.games.service.backgroundsync.data.model.SyncPriority.HIGH
        SyncPriority.CRITICAL -> com.roshni.games.service.backgroundsync.data.model.SyncPriority.CRITICAL
    }
}

fun SyncStatus.toData(): com.roshni.games.service.backgroundsync.data.model.SyncStatus {
    return when (this) {
        is SyncStatus.Pending -> com.roshni.games.service.backgroundsync.data.model.SyncStatus.Pending
        is SyncStatus.InProgress -> com.roshni.games.service.backgroundsync.data.model.SyncStatus.InProgress
        is SyncStatus.Completed -> com.roshni.games.service.backgroundsync.data.model.SyncStatus.Completed(this.timestamp.toEpochMilliseconds())
        is SyncStatus.Failed -> com.roshni.games.service.backgroundsync.data.model.SyncStatus.Failed(this.error, this.timestamp.toEpochMilliseconds())
    }
}

fun SyncConfig.toDomain(): SyncConfigDomain {
    return SyncConfigDomain(
        enabled = enabled,
        syncOnWifiOnly = syncOnWifiOnly,
        maxRetryAttempts = maxRetryAttempts,
        syncIntervalMinutes = syncIntervalMinutes,
        maxOperationsPerBatch = maxOperationsPerBatch,
        enableBackgroundSync = enableBackgroundSync
    )
}

fun SyncConfigDomain.toData(): SyncConfig {
    return SyncConfig(
        enabled = enabled,
        syncOnWifiOnly = syncOnWifiOnly,
        maxRetryAttempts = maxRetryAttempts,
        syncIntervalMinutes = syncIntervalMinutes,
        maxOperationsPerBatch = maxOperationsPerBatch,
        enableBackgroundSync = enableBackgroundSync
    )
}

fun SyncStats.toDomain(): SyncStatsDomain {
    return SyncStatsDomain(
        totalOperations = totalOperations,
        pendingOperations = pendingOperations,
        completedOperations = completedOperations,
        failedOperations = failedOperations,
        lastSyncTime = lastSyncTime?.let { Instant.fromEpochMilliseconds(it) },
        averageSyncTime = averageSyncTime,
        dataTransferred = dataTransferred
    )
}

fun SyncStatsDomain.toData(): SyncStats {
    return SyncStats(
        totalOperations = totalOperations,
        pendingOperations = pendingOperations,
        completedOperations = completedOperations,
        failedOperations = failedOperations,
        lastSyncTime = lastSyncTime?.toEpochMilliseconds(),
        averageSyncTime = averageSyncTime,
        dataTransferred = dataTransferred
    )
}