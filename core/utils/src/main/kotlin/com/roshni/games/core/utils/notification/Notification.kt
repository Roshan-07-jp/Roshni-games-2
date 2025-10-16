package com.roshni.games.core.utils.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * Core notification data class representing a notification in the system
 */
data class Notification(
    val id: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val title: String,
    val message: String,
    val data: Map<String, Any> = emptyMap(),
    val channels: List<NotificationChannel> = emptyList(),
    val schedule: NotificationSchedule? = null,
    val actions: List<NotificationAction> = emptyList(),
    val metadata: NotificationMetadata = NotificationMetadata(),
    val state: NotificationState = NotificationState.PENDING
)

/**
 * Notification types for categorization
 */
enum class NotificationType {
    GAME_UPDATE,
    ACHIEVEMENT_UNLOCKED,
    FRIEND_REQUEST,
    GAME_INVITATION,
    SYSTEM_ANNOUNCEMENT,
    PROMOTIONAL,
    SECURITY_ALERT,
    PARENTAL_CONTROL,
    GAME_SESSION_REMINDER,
    TOURNAMENT_INVITATION,
    LEADERBOARD_UPDATE,
    CUSTOM
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Notification state tracking
 */
enum class NotificationState {
    PENDING,
    SCHEDULED,
    DELIVERING,
    DELIVERED,
    READ,
    DISMISSED,
    FAILED,
    CANCELLED
}

/**
 * Notification metadata for tracking and analytics
 */
data class NotificationMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val tags: Set<String> = emptySet(),
    val category: String? = null,
    val source: String? = null,
    val userId: String? = null,
    val gameId: String? = null,
    val sessionId: String? = null
)

/**
 * Notification delivery result
 */
data class NotificationDeliveryResult(
    val notificationId: String,
    val channel: NotificationChannel,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val deliveryId: String? = null
)

/**
 * Notification statistics for analytics
 */
data class NotificationStats(
    val totalSent: Int = 0,
    val totalDelivered: Int = 0,
    val totalFailed: Int = 0,
    val totalRead: Int = 0,
    val averageDeliveryTime: Long = 0L,
    val channelStats: Map<NotificationChannel, ChannelStats> = emptyMap()
)

/**
 * Channel-specific statistics
 */
data class ChannelStats(
    val sent: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val read: Int = 0,
    val averageDeliveryTime: Long = 0L
)