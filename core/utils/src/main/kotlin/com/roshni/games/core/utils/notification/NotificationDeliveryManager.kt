package com.roshni.games.core.utils.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing notification delivery across multiple channels
 */
interface NotificationDeliveryManager {

    /**
     * Available notification channels
     */
    val availableChannels: StateFlow<List<NotificationChannel>>

    /**
     * Channel delivery status
     */
    val channelStatus: StateFlow<Map<String, ChannelDeliveryStatus>>

    /**
     * Initialize the delivery manager
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Shutdown the delivery manager
     */
    suspend fun shutdown(): Result<Unit>

    /**
     * Deliver notification to specified channels
     */
    suspend fun deliverNotification(
        notification: Notification,
        channels: List<NotificationChannel>
    ): List<NotificationDeliveryResult>

    /**
     * Get notification by ID
     */
    suspend fun getNotification(notificationId: String): Result<Notification>

    /**
     * Update existing notification
     */
    suspend fun updateNotification(notification: Notification): Result<Unit>

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Get notifications by state
     */
    fun getNotificationsByState(state: NotificationState): Flow<List<Notification>>

    /**
     * Get notifications by type
     */
    fun getNotificationsByType(type: NotificationType): Flow<List<Notification>>

    /**
     * Get notifications by user
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
     * Mark all notifications as read for user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /**
     * Get unread count for user
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
     * Get available channels
     */
    suspend fun getAvailableChannels(): Result<List<NotificationChannel>>

    /**
     * Test channel delivery
     */
    suspend fun testChannel(
        channel: NotificationChannel,
        testNotification: Notification
    ): Result<NotificationDeliveryResult>

    /**
     * Get delivery history
     */
    fun getDeliveryHistory(
        notificationId: String? = null,
        channel: NotificationChannel? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<NotificationDeliveryResult>>

    /**
     * Retry failed deliveries
     */
    suspend fun retryFailedDeliveries(
        maxRetries: Int = 3,
        olderThan: Long? = null
    ): Result<Int>

    /**
     * Get notification statistics
     */
    suspend fun getNotificationStats(): Result<NotificationStats>

    /**
     * Cleanup old notifications and history
     */
    suspend fun cleanup(
        olderThan: Long,
        includeDelivered: Boolean = true,
        includeFailed: Boolean = true
    ): Result<Int>
}

/**
 * Implementation of NotificationDeliveryManager
 */
@Singleton
class NotificationDeliveryManagerImpl @Inject constructor(
    private val notificationPreferenceManager: NotificationPreferenceManager
) : NotificationDeliveryManager {

    private val _availableChannels = MutableStateFlow<List<NotificationChannel>>(emptyList())
    private val _channelStatus = MutableStateFlow<Map<String, ChannelDeliveryStatus>>(emptyMap())

    private val deliveredNotifications = mutableMapOf<String, Notification>()
    private val notificationStates = mutableMapOf<String, NotificationState>()
    private val deliveryHistory = mutableMapOf<String, MutableList<NotificationDeliveryResult>>()
    private val channelRegistry = mutableMapOf<String, NotificationChannel>()
    private val channelDeliveryStatus = mutableMapOf<String, ChannelDeliveryStatus>()

    private val mutex = Mutex()
    private var isInitialized = false

    override val availableChannels: StateFlow<List<NotificationChannel>> = _availableChannels.asStateFlow()
    override val channelStatus: StateFlow<Map<String, ChannelDeliveryStatus>> = _channelStatus.asStateFlow()

    init {
        // Initialize default channels
        initializeDefaultChannels()

        // Start channel monitoring
        startChannelMonitoring()
    }

    override suspend fun initialize(): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            mutex.withLock {
                if (isInitialized) {
                    Timber.d("NotificationDeliveryManager already initialized")
                    return@withContext Result.success(Unit)
                }

                Timber.d("Initializing NotificationDeliveryManager")

                // Initialize channels
                initializeChannels()

                // Load existing notifications (in real implementation)
                loadExistingNotifications()

                isInitialized = true
                Timber.d("NotificationDeliveryManager initialized successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NotificationDeliveryManager")
            Result.failure(e)
        }
    }

    override suspend fun shutdown(): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            mutex.withLock {
                if (!isInitialized) {
                    return@withContext Result.success(Unit)
                }

                Timber.d("Shutting down NotificationDeliveryManager")

                // Save notifications (in real implementation)
                saveNotifications()

                // Clear in-memory data
                deliveredNotifications.clear()
                notificationStates.clear()
                deliveryHistory.clear()

                isInitialized = false
                Timber.d("NotificationDeliveryManager shutdown successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to shutdown NotificationDeliveryManager")
            Result.failure(e)
        }
    }

    override suspend fun deliverNotification(
        notification: Notification,
        channels: List<NotificationChannel>
    ): List<NotificationDeliveryResult> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized) {
                throw IllegalStateException("NotificationDeliveryManager not initialized")
            }

            val results = mutableListOf<NotificationDeliveryResult>()

            // Store notification
            deliveredNotifications[notification.id] = notification
            notificationStates[notification.id] = NotificationState.DELIVERING

            // Deliver to each channel
            channels.forEach { channel ->
                val result = deliverToChannel(notification, channel)
                results.add(result)

                // Store delivery result
                deliveryHistory.getOrPut(notification.id) { mutableListOf() }.add(result)

                // Update channel status
                updateChannelStatus(channel, result)
            }

            // Update notification state based on results
            val allFailed = results.all { !it.success }
            val hasSuccess = results.any { it.success }

            notificationStates[notification.id] = when {
                allFailed -> NotificationState.FAILED
                hasSuccess -> NotificationState.DELIVERED
                else -> NotificationState.PENDING
            }

            Timber.d("Notification ${notification.id} delivered to ${channels.size} channels")
            results

        } catch (e: Exception) {
            Timber.e(e, "Failed to deliver notification ${notification.id}")
            listOf(NotificationDeliveryResult(
                notificationId = notification.id,
                channel = channels.firstOrNull() ?: NotificationChannel.InAppNotification(),
                success = false,
                errorMessage = e.message
            ))
        }
    }

    override suspend fun getNotification(notificationId: String): Result<Notification> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val notification = deliveredNotifications[notificationId]
                ?: return Result.failure(NoSuchElementException("Notification not found: $notificationId"))

            Result.success(notification)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(notification: Notification): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            deliveredNotifications[notification.id] = notification
            notificationStates[notification.id] = notification.state

            Timber.d("Notification updated: ${notification.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            deliveredNotifications.remove(notificationId)
            notificationStates.remove(notificationId)
            deliveryHistory.remove(notificationId)

            Timber.d("Notification deleted: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification $notificationId")
            Result.failure(e)
        }
    }

    override fun getNotificationsByState(state: NotificationState): Flow<List<Notification>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            val notifications = deliveredNotifications.values.filter { notification ->
                notificationStates[notification.id] == state
            }

            emit(notifications)

            // Continue monitoring for changes
            kotlinx.coroutines.delay(5000) // Poll every 5 seconds

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications by state $state")
            emit(emptyList())
        }
    }

    override fun getNotificationsByType(type: NotificationType): Flow<List<Notification>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            val notifications = deliveredNotifications.values.filter { notification ->
                notification.type == type
            }

            emit(notifications)

            kotlinx.coroutines.delay(5000)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications by type $type")
            emit(emptyList())
        }
    }

    override fun getNotificationsByUser(userId: String): Flow<List<Notification>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            val notifications = deliveredNotifications.values.filter { notification ->
                notification.metadata.userId == userId
            }

            emit(notifications)

            kotlinx.coroutines.delay(5000)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications for user $userId")
            emit(emptyList())
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            notificationStates[notificationId] = NotificationState.READ

            Timber.d("Notification marked as read: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun markAsDismissed(notificationId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            notificationStates[notificationId] = NotificationState.DISMISSED

            Timber.d("Notification marked as dismissed: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as dismissed $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            deliveredNotifications.values.forEach { notification ->
                if (notification.metadata.userId == userId) {
                    notificationStates[notification.id] = NotificationState.READ
                }
            }

            Timber.d("All notifications marked as read for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val count = deliveredNotifications.values.count { notification ->
                notification.metadata.userId == userId &&
                notificationStates[notification.id] != NotificationState.READ &&
                notificationStates[notification.id] != NotificationState.DISMISSED
            }

            Result.success(count)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get unread count for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun registerChannel(channel: NotificationChannel): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            channelRegistry[channel.id] = channel

            // Update available channels
            updateAvailableChannels()

            Timber.d("Channel registered: ${channel.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to register channel ${channel.id}")
            Result.failure(e)
        }
    }

    override suspend fun unregisterChannel(channelId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            channelRegistry.remove(channelId)
            channelDeliveryStatus.remove(channelId)

            // Update available channels
            updateAvailableChannels()

            Timber.d("Channel unregistered: $channelId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister channel $channelId")
            Result.failure(e)
        }
    }

    override suspend fun updateChannelConfiguration(
        channelId: String,
        configuration: ChannelConfiguration
    ): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val channel = channelRegistry[channelId]
                ?: return Result.failure(NoSuchElementException("Channel not found: $channelId"))

            // Update channel configuration
            val updatedChannel = when (channel) {
                is NotificationChannel.PushNotification -> channel.copy(configuration = configuration)
                is NotificationChannel.InAppNotification -> channel.copy(configuration = configuration)
                is NotificationChannel.EmailNotification -> channel.copy(configuration = configuration)
                is NotificationChannel.SMSNotification -> channel.copy(configuration = configuration)
                is NotificationChannel.CustomChannel -> channel.copy(configuration = configuration)
            }

            channelRegistry[channelId] = updatedChannel

            // Update available channels
            updateAvailableChannels()

            Timber.d("Channel configuration updated: $channelId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update channel configuration $channelId")
            Result.failure(e)
        }
    }

    override suspend fun getChannelStatus(channelId: String): Result<ChannelDeliveryStatus> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val status = channelDeliveryStatus[channelId]
                ?: return Result.failure(NoSuchElementException("Channel status not found: $channelId"))

            Result.success(status)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get channel status $channelId")
            Result.failure(e)
        }
    }

    override suspend fun getAvailableChannels(): Result<List<NotificationChannel>> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            Result.success(_availableChannels.value)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get available channels")
            Result.failure(e)
        }
    }

    override suspend fun testChannel(
        channel: NotificationChannel,
        testNotification: Notification
    ): Result<NotificationDeliveryResult> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val result = deliverToChannel(testNotification, channel)

            Timber.d("Channel test completed for ${channel.id}: ${if (result.success) "SUCCESS" else "FAILED"}")
            Result.success(result)

        } catch (e: Exception) {
            Timber.e(e, "Failed to test channel ${channel.id}")
            Result.failure(e)
        }
    }

    override fun getDeliveryHistory(
        notificationId: String?,
        channel: NotificationChannel?,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<NotificationDeliveryResult>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            val history = if (notificationId != null) {
                deliveryHistory[notificationId] ?: emptyList()
            } else {
                deliveryHistory.values.flatten()
            }

            val filteredHistory = history.filter { result ->
                (notificationId == null || result.notificationId == notificationId) &&
                (channel == null || result.channel.id == channel.id) &&
                (startTime == null || result.timestamp >= startTime) &&
                (endTime == null || result.timestamp <= endTime)
            }

            emit(filteredHistory)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get delivery history")
            emit(emptyList())
        }
    }

    override suspend fun retryFailedDeliveries(
        maxRetries: Int,
        olderThan: Long?
    ): Result<Int> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            var retryCount = 0
            val cutoffTime = olderThan ?: System.currentTimeMillis()

            deliveryHistory.values.flatten().forEach { result ->
                if (!result.success &&
                    result.timestamp < cutoffTime &&
                    (result.errorMessage?.contains("retry") == true || retryCount < maxRetries)) {

                    // In real implementation, would retry the delivery
                    retryCount++
                }
            }

            Timber.d("Retried $retryCount failed deliveries")
            Result.success(retryCount)

        } catch (e: Exception) {
            Timber.e(e, "Failed to retry failed deliveries")
            Result.failure(e)
        }
    }

    override suspend fun getNotificationStats(): Result<NotificationStats> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            val allResults = deliveryHistory.values.flatten()

            val stats = NotificationStats(
                totalSent = allResults.size,
                totalDelivered = allResults.count { it.success },
                totalFailed = allResults.count { !it.success },
                totalRead = notificationStates.values.count { it == NotificationState.READ }
            )

            Result.success(stats)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification statistics")
            Result.failure(e)
        }
    }

    override suspend fun cleanup(
        olderThan: Long,
        includeDelivered: Boolean,
        includeFailed: Boolean
    ): Result<Int> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationDeliveryManager not initialized"))
            }

            var cleanupCount = 0

            deliveredNotifications.entries.removeAll { (id, notification) ->
                val shouldRemove = notification.metadata.createdAt < olderThan &&
                    (includeDelivered && notificationStates[id] == NotificationState.DELIVERED ||
                     includeFailed && notificationStates[id] == NotificationState.FAILED)

                if (shouldRemove) cleanupCount++
                shouldRemove
            }

            // Clean up delivery history
            deliveryHistory.entries.removeAll { (id, results) ->
                results.removeAll { it.timestamp < olderThan }
                results.isEmpty()
            }

            Timber.d("Cleaned up $cleanupCount old notifications")
            Result.success(cleanupCount)

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup notifications")
            Result.failure(e)
        }
    }

    // Private helper methods

    private fun initializeDefaultChannels() {
        // Register default channels
        val pushChannel = NotificationChannel.PushNotification()
        val inAppChannel = NotificationChannel.InAppNotification()

        channelRegistry[pushChannel.id] = pushChannel
        channelRegistry[inAppChannel.id] = inAppChannel

        updateAvailableChannels()
    }

    private suspend fun initializeChannels() {
        // Initialize channel delivery mechanisms
        channelRegistry.values.forEach { channel ->
            val status = ChannelDeliveryStatus(
                channel = channel,
                isAvailable = true,
                successRate = 1.0f
            )
            channelDeliveryStatus[channel.id] = status
        }

        updateChannelStatusFlow()
    }

    private fun startChannelMonitoring() {
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(30000) // Monitor every 30 seconds

                    if (!isInitialized) continue

                    // Update channel health status
                    channelRegistry.values.forEach { channel ->
                        updateChannelHealth(channel)
                    }

                    updateChannelStatusFlow()

                } catch (e: Exception) {
                    Timber.e(e, "Failed to monitor channels")
                }
            }
        }
    }

    private suspend fun deliverToChannel(
        notification: Notification,
        channel: NotificationChannel
    ): NotificationDeliveryResult {
        return try {
            val startTime = System.currentTimeMillis()

            // Simulate delivery based on channel type
            val success = when (channel) {
                is NotificationChannel.PushNotification -> deliverPushNotification(notification, channel)
                is NotificationChannel.InAppNotification -> deliverInAppNotification(notification, channel)
                is NotificationChannel.EmailNotification -> deliverEmailNotification(notification, channel)
                is NotificationChannel.SMSNotification -> deliverSMSNotification(notification, channel)
                is NotificationChannel.CustomChannel -> deliverCustomNotification(notification, channel)
            }

            val deliveryTime = System.currentTimeMillis() - startTime

            NotificationDeliveryResult(
                notificationId = notification.id,
                channel = channel,
                success = success,
                timestamp = System.currentTimeMillis(),
                deliveryId = UUID.randomUUID().toString()
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to deliver to channel ${channel.id}")
            NotificationDeliveryResult(
                notificationId = notification.id,
                channel = channel,
                success = false,
                timestamp = System.currentTimeMillis(),
                errorMessage = e.message
            )
        }
    }

    private suspend fun deliverPushNotification(
        notification: Notification,
        channel: NotificationChannel.PushNotification
    ): Boolean {
        // Simulate push notification delivery
        kotlinx.coroutines.delay(100) // Simulate network delay

        // In real implementation, would use FCM/APNS/Web Push APIs
        return true
    }

    private suspend fun deliverInAppNotification(
        notification: Notification,
        channel: NotificationChannel.InAppNotification
    ): Boolean {
        // In-app notifications are always "delivered" since they're shown immediately
        return true
    }

    private suspend fun deliverEmailNotification(
        notification: Notification,
        channel: NotificationChannel.EmailNotification
    ): Boolean {
        // Simulate email delivery
        kotlinx.coroutines.delay(500) // Simulate email processing

        // In real implementation, would use email service (SendGrid, SES, etc.)
        return true
    }

    private suspend fun deliverSMSNotification(
        notification: Notification,
        channel: NotificationChannel.SMSNotification
    ): Boolean {
        // Simulate SMS delivery
        kotlinx.coroutines.delay(200) // Simulate SMS processing

        // In real implementation, would use SMS service (Twilio, AWS SNS, etc.)
        return true
    }

    private suspend fun deliverCustomNotification(
        notification: Notification,
        channel: NotificationChannel.CustomChannel
    ): Boolean {
        // Simulate custom channel delivery
        kotlinx.coroutines.delay(300) // Simulate custom processing

        // In real implementation, would use webhook or custom API
        return true
    }

    private fun updateChannelStatus(channel: NotificationChannel, result: NotificationDeliveryResult) {
        val currentStatus = channelDeliveryStatus[channel.id]
        if (currentStatus != null) {
            val totalDeliveries = currentStatus.errorCount + 1
            val successCount = if (result.success) currentStatus.errorCount + 1 else currentStatus.errorCount
            val newSuccessRate = successCount.toFloat() / totalDeliveries

            val updatedStatus = currentStatus.copy(
                lastDeliveryTime = result.timestamp,
                successRate = newSuccessRate,
                errorCount = if (result.success) currentStatus.errorCount else currentStatus.errorCount + 1,
                lastError = if (!result.success) result.errorMessage else currentStatus.lastError
            )

            channelDeliveryStatus[channel.id] = updatedStatus
        }
    }

    private fun updateChannelHealth(channel: NotificationChannel) {
        val status = channelDeliveryStatus[channel.id]
        if (status != null) {
            // Simple health check based on success rate and recent activity
            val isHealthy = status.successRate > 0.8f &&
                           (status.lastDeliveryTime == null ||
                            System.currentTimeMillis() - status.lastDeliveryTime < 300000) // 5 minutes

            val updatedStatus = status.copy(isAvailable = isHealthy)
            channelDeliveryStatus[channel.id] = updatedStatus
        }
    }

    private fun updateAvailableChannels() {
        _availableChannels.value = channelRegistry.values.toList()
    }

    private fun updateChannelStatusFlow() {
        _channelStatus.value = channelDeliveryStatus.toMap()
    }

    private suspend fun loadExistingNotifications() {
        // In real implementation, load from database
        deliveredNotifications.clear()
        notificationStates.clear()
        deliveryHistory.clear()
    }

    private suspend fun saveNotifications() {
        // In real implementation, save to database
    }
}