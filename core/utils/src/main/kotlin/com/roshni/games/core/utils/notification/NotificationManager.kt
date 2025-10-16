package com.roshni.games.core.utils.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for notification management system
 * Provides high-level operations for creating, managing, and delivering notifications
 */
interface NotificationManager {

    /**
     * Current notification statistics
     */
    val notificationStats: StateFlow<NotificationStats>

    /**
     * Available notification channels
     */
    val availableChannels: StateFlow<List<NotificationChannel>>

    /**
     * Channel health status
     */
    val channelHealth: StateFlow<Map<NotificationChannel, ChannelHealthMetrics>>

    /**
     * Initialize the notification manager
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Shutdown the notification manager
     */
    suspend fun shutdown(): Result<Unit>

    /**
     * Send a notification immediately
     */
    suspend fun sendNotification(
        notification: Notification,
        channels: List<NotificationChannel>? = null
    ): Result<List<NotificationDeliveryResult>>

    /**
     * Schedule a notification for future delivery
     */
    suspend fun scheduleNotification(
        notification: Notification,
        schedule: NotificationSchedule
    ): Result<String>

    /**
     * Cancel a scheduled notification
     */
    suspend fun cancelScheduledNotification(scheduleId: String): Result<Unit>

    /**
     * Update an existing notification
     */
    suspend fun updateNotification(
        notificationId: String,
        updates: NotificationUpdate
    ): Result<Unit>

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Get notification by ID
     */
    suspend fun getNotification(notificationId: String): Result<Notification>

    /**
     * Get notifications by state
     */
    fun getNotificationsByState(state: NotificationState): Flow<List<Notification>>

    /**
     * Get notifications by type
     */
    fun getNotificationsByType(type: NotificationType): Flow<List<Notification>>

    /**
     * Get notifications for a specific user
     */
    fun getNotificationsByUser(userId: String): Flow<List<Notification>>

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Mark notification as dismissed
     */
    suspend fun markAsDismissed(notificationId: String): Result<Unit>

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /**
     * Get unread notification count for a user
     */
    suspend fun getUnreadCount(userId: String): Result<Int>

    /**
     * Register a notification channel
     */
    suspend fun registerChannel(channel: NotificationChannel): Result<Unit>

    /**
     * Unregister a notification channel
     */
    suspend fun unregisterChannel(channelId: String): Result<Unit>

    /**
     * Update channel configuration
     */
    suspend fun updateChannelConfiguration(
        channelId: String,
        configuration: ChannelConfiguration
    ): Result<Unit>

    /**
     * Get channel delivery status
     */
    suspend fun getChannelStatus(channelId: String): Result<ChannelDeliveryStatus>

    /**
     * Test channel delivery
     */
    suspend fun testChannel(
        channel: NotificationChannel,
        testNotification: Notification
    ): Result<NotificationDeliveryResult>

    /**
     * Get scheduled notifications
     */
    fun getScheduledNotifications(): Flow<List<ScheduledNotificationStatus>>

    /**
     * Get schedule execution history
     */
    fun getScheduleExecutionHistory(scheduleId: String): Flow<List<ScheduleExecutionResult>>

    /**
     * Pause all notifications for a user
     */
    suspend fun pauseUserNotifications(userId: String, duration: Long? = null): Result<Unit>

    /**
     * Resume notifications for a user
     */
    suspend fun resumeUserNotifications(userId: String): Result<Unit>

    /**
     * Enable do not disturb mode
     */
    suspend fun enableDoNotDisturb(duration: Long? = null): Result<Unit>

    /**
     * Disable do not disturb mode
     */
    suspend fun disableDoNotDisturb(): Result<Unit>

    /**
     * Check if do not disturb is active
     */
    suspend fun isDoNotDisturbActive(): Boolean

    /**
     * Get notification delivery history
     */
    fun getDeliveryHistory(
        notificationId: String? = null,
        channel: NotificationChannel? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<NotificationDeliveryResult>>

    /**
     * Retry failed notification deliveries
     */
    suspend fun retryFailedDeliveries(
        maxRetries: Int = 3,
        olderThan: Long? = null
    ): Result<Int>

    /**
     * Clean up old notifications and delivery history
     */
    suspend fun cleanup(
        olderThan: Long = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L), // 30 days
        includeDelivered: Boolean = true,
        includeFailed: Boolean = true
    ): Result<Int>

    /**
     * Export notification data for analytics
     */
    suspend fun exportAnalyticsData(
        startTime: Long,
        endTime: Long,
        format: ExportFormat = ExportFormat.JSON
    ): Result<String>

    /**
     * Handle notification action execution
     */
    suspend fun executeAction(
        notificationId: String,
        actionId: String,
        actionData: Map<String, Any> = emptyMap()
    ): Result<Unit>

    /**
     * Validate notification before sending
     */
    suspend fun validateNotification(notification: Notification): Result<NotificationValidationResult>

    /**
     * Get notification template by type
     */
    suspend fun getNotificationTemplate(type: NotificationType): Result<NotificationTemplate>

    /**
     * Create notification from template
     */
    suspend fun createFromTemplate(
        template: NotificationTemplate,
        data: Map<String, Any>
    ): Result<Notification>
}

/**
 * Notification update data class
 */
data class NotificationUpdate(
    val title: String? = null,
    val message: String? = null,
    val priority: NotificationPriority? = null,
    val channels: List<NotificationChannel>? = null,
    val schedule: NotificationSchedule? = null,
    val actions: List<NotificationAction>? = null,
    val metadata: NotificationMetadata? = null,
    val state: NotificationState? = null
)

/**
 * Notification validation result
 */
data class NotificationValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

/**
 * Notification template for common notification types
 */
data class NotificationTemplate(
    val id: String,
    val type: NotificationType,
    val name: String,
    val titleTemplate: String,
    val messageTemplate: String,
    val defaultChannels: List<String> = emptyList(),
    val defaultPriority: NotificationPriority = NotificationPriority.NORMAL,
    val defaultActions: List<NotificationAction> = emptyList(),
    val requiredFields: Set<String> = emptySet(),
    val optionalFields: Set<String> = emptySet(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Export format options
 */
enum class ExportFormat {
    JSON,
    CSV,
    XML,
    PDF
}