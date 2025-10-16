package com.roshni.games.core.utils.notification

import com.roshni.games.core.utils.integration.SystemIntegrationHub
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationManager interface
 * Coordinates between delivery, scheduling, and preference management
 */
@Singleton
class NotificationManagerImpl @Inject constructor(
    private val notificationPreferenceManager: NotificationPreferenceManager,
    private val notificationDeliveryManager: NotificationDeliveryManager,
    private val notificationScheduler: NotificationScheduler,
    private val systemIntegrationHub: SystemIntegrationHub,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) : NotificationManager {

    private val _notificationStats = MutableStateFlow(NotificationStats())
    private val _availableChannels = MutableStateFlow<List<NotificationChannel>>(emptyList())
    private val _channelHealth = MutableStateFlow<Map<NotificationChannel, ChannelHealthMetrics>>(emptyMap())

    private val mutex = Mutex()
    private var isInitialized = false

    override val notificationStats: StateFlow<NotificationStats> = _notificationStats.asStateFlow()
    override val availableChannels: StateFlow<List<NotificationChannel>> = _availableChannels.asStateFlow()
    override val channelHealth: StateFlow<Map<NotificationChannel, ChannelHealthMetrics>> = _channelHealth.asStateFlow()

    init {
        // Start monitoring channel health
        startChannelHealthMonitoring()

        // Start statistics monitoring
        startStatisticsMonitoring()
    }

    override suspend fun initialize(): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            mutex.withLock {
                if (isInitialized) {
                    Timber.d("NotificationManager already initialized")
                    return@withContext Result.success(Unit)
                }

                Timber.d("Initializing NotificationManager")

                // Initialize sub-managers
                val results = listOf(
                    notificationPreferenceManager.initialize(),
                    notificationDeliveryManager.initialize(),
                    notificationScheduler.initialize()
                )

                // Check if all initializations succeeded
                val allSucceeded = results.all { it.isSuccess }

                if (allSucceeded) {
                    // Load available channels
                    loadAvailableChannels()

                    // Start monitoring integration with system
                    startSystemIntegration()

                    isInitialized = true
                    Timber.d("NotificationManager initialized successfully")
                    Result.success(Unit)
                } else {
                    val errors = results.filter { it.isFailure }.map { it.exceptionOrNull()?.message }
                    Timber.e("NotificationManager initialization failed: $errors")
                    Result.failure(IllegalStateException("NotificationManager initialization failed: $errors"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NotificationManager")
            Result.failure(e)
        }
    }

    override suspend fun shutdown(): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            mutex.withLock {
                if (!isInitialized) {
                    return@withContext Result.success(Unit)
                }

                Timber.d("Shutting down NotificationManager")

                // Shutdown sub-managers
                val results = listOf(
                    notificationDeliveryManager.shutdown(),
                    notificationScheduler.shutdown(),
                    notificationPreferenceManager.shutdown()
                )

                val allSucceeded = results.all { it.isSuccess }

                if (allSucceeded) {
                    isInitialized = false
                    _availableChannels.value = emptyList()
                    _channelHealth.value = emptyMap()
                    Timber.d("NotificationManager shutdown successfully")
                    Result.success(Unit)
                } else {
                    val errors = results.filter { it.isFailure }.map { it.exceptionOrNull()?.message }
                    Timber.e("NotificationManager shutdown failed: $errors")
                    Result.failure(IllegalStateException("NotificationManager shutdown failed: $errors"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to shutdown NotificationManager")
            Result.failure(e)
        }
    }

    override suspend fun sendNotification(
        notification: Notification,
        channels: List<NotificationChannel>?
    ): Result<List<NotificationDeliveryResult>> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Validate notification
            val validation = validateNotification(notification)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException("Invalid notification: ${validation.errors}"))
            }

            // Check user preferences
            val userPreferences = notificationPreferenceManager.getUserPreferences(notification.metadata.userId ?: "")
            if (userPreferences.isFailure) {
                Timber.w("Could not load user preferences, using defaults")
            }

            // Determine channels to use
            val channelsToUse = channels ?: notification.channels.ifEmpty {
                notificationPreferenceManager.getPreferredChannels(notification.metadata.userId ?: "")
            }

            if (channelsToUse.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No channels available for notification delivery"))
            }

            // Check if user has paused notifications
            if (userPreferences.isSuccess) {
                val preferences = userPreferences.getOrNull()
                if (preferences?.isPaused == true) {
                    Timber.d("Notifications paused for user ${notification.metadata.userId}")
                    return@withContext Result.success(emptyList())
                }
            }

            // Check do not disturb
            if (isDoNotDisturbActive() && notification.priority != NotificationPriority.URGENT) {
                Timber.d("Do not disturb active, skipping non-urgent notification")
                return@withContext Result.success(emptyList())
            }

            // Send notification via delivery manager
            val deliveryResults = notificationDeliveryManager.deliverNotification(notification, channelsToUse)

            // Update statistics
            updateStatistics(deliveryResults)

            // Log delivery results
            deliveryResults.forEach { result ->
                if (result.success) {
                    Timber.d("Notification ${notification.id} delivered via ${result.channel.id}")
                } else {
                    Timber.w("Notification ${notification.id} failed via ${result.channel.id}: ${result.errorMessage}")
                }
            }

            Result.success(deliveryResults)

        } catch (e: Exception) {
            Timber.e(e, "Failed to send notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun scheduleNotification(
        notification: Notification,
        schedule: NotificationSchedule
    ): Result<String> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Validate notification and schedule
            val validation = validateNotification(notification)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException("Invalid notification: ${validation.errors}"))
            }

            // Schedule via scheduler
            val scheduleId = notificationScheduler.scheduleNotification(notification, schedule)

            if (scheduleId.isSuccess) {
                Timber.d("Notification ${notification.id} scheduled with ID: ${scheduleId.getOrNull()}")
            } else {
                Timber.e("Failed to schedule notification ${notification.id}: ${scheduleId.exceptionOrNull()?.message}")
            }

            scheduleId

        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun cancelScheduledNotification(scheduleId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            val result = notificationScheduler.cancelScheduledNotification(scheduleId)

            if (result.isSuccess) {
                Timber.d("Scheduled notification cancelled: $scheduleId")
            } else {
                Timber.e("Failed to cancel scheduled notification $scheduleId: ${result.exceptionOrNull()?.message}")
            }

            result

        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel scheduled notification $scheduleId")
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(
        notificationId: String,
        updates: NotificationUpdate
    ): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Get current notification
            val currentNotification = getNotification(notificationId)
            if (currentNotification.isFailure) {
                return@withContext Result.failure(currentNotification.exceptionOrNull() ?: IllegalArgumentException("Notification not found"))
            }

            val notification = currentNotification.getOrNull()!!

            // Apply updates
            val updatedNotification = notification.copy(
                title = updates.title ?: notification.title,
                message = updates.message ?: notification.message,
                priority = updates.priority ?: notification.priority,
                channels = updates.channels ?: notification.channels,
                schedule = updates.schedule ?: notification.schedule,
                actions = updates.actions ?: notification.actions,
                metadata = updates.metadata ?: notification.metadata,
                state = updates.state ?: notification.state
            )

            // Update via delivery manager if already sent
            if (notification.state >= NotificationState.DELIVERED) {
                notificationDeliveryManager.updateNotification(updatedNotification)
            }

            // Update via scheduler if scheduled
            if (notification.schedule != null) {
                notificationScheduler.updateScheduledNotification(notificationId, updatedNotification)
            }

            Timber.d("Notification updated: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Cancel if scheduled
            notificationScheduler.cancelScheduledNotification(notificationId)

            // Delete from delivery manager
            notificationDeliveryManager.deleteNotification(notificationId)

            Timber.d("Notification deleted: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun getNotification(notificationId: String): Result<Notification> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Try to get from delivery manager first
            val deliveryNotification = notificationDeliveryManager.getNotification(notificationId)
            if (deliveryNotification.isSuccess) {
                return@withContext deliveryNotification
            }

            // Try scheduler if not found in delivery manager
            val scheduledNotification = notificationScheduler.getScheduledNotification(notificationId)
            if (scheduledNotification.isSuccess) {
                return@withContext Result.success(scheduledNotification.getOrNull()!!)
            }

            Result.failure(NoSuchElementException("Notification not found: $notificationId"))

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification $notificationId")
            Result.failure(e)
        }
    }

    override fun getNotificationsByState(state: NotificationState): Flow<List<Notification>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            notificationDeliveryManager.getNotificationsByState(state).combine(
                notificationScheduler.getScheduledNotifications()
            ) { delivered, scheduled ->
                delivered + scheduled.filter { it.state == state }.map { it.notification }
            }.collect { combined ->
                emit(combined)
            }
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
            notificationDeliveryManager.getNotificationsByType(type).combine(
                notificationScheduler.getScheduledNotifications()
            ) { delivered, scheduled ->
                delivered + scheduled.filter { it.notification.type == type }.map { it.notification }
            }.collect { combined ->
                emit(combined)
            }
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
            notificationDeliveryManager.getNotificationsByUser(userId).combine(
                notificationScheduler.getScheduledNotifications()
            ) { delivered, scheduled ->
                delivered + scheduled.filter { it.notification.metadata.userId == userId }.map { it.notification }
            }.collect { combined ->
                emit(combined)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications for user $userId")
            emit(emptyList())
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.markAsRead(notificationId)

            Timber.d("Notification marked as read: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun markAsDismissed(notificationId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.markAsDismissed(notificationId)

            Timber.d("Notification marked as dismissed: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as dismissed $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.markAllAsRead(userId)

            Timber.d("All notifications marked as read for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            val count = notificationDeliveryManager.getUnreadCount(userId)

            if (count.isSuccess) {
                Timber.d("Unread count for user $userId: ${count.getOrNull()}")
            }

            count

        } catch (e: Exception) {
            Timber.e(e, "Failed to get unread count for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun registerChannel(channel: NotificationChannel): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.registerChannel(channel)

            // Update available channels
            loadAvailableChannels()

            Timber.d("Channel registered: ${channel.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to register channel ${channel.id}")
            Result.failure(e)
        }
    }

    override suspend fun unregisterChannel(channelId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.unregisterChannel(channelId)

            // Update available channels
            loadAvailableChannels()

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
    ): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.updateChannelConfiguration(channelId, configuration)

            Timber.d("Channel configuration updated: $channelId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update channel configuration $channelId")
            Result.failure(e)
        }
    }

    override suspend fun getChannelStatus(channelId: String): Result<ChannelDeliveryStatus> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.getChannelStatus(channelId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get channel status $channelId")
            Result.failure(e)
        }
    }

    override suspend fun testChannel(
        channel: NotificationChannel,
        testNotification: Notification
    ): Result<NotificationDeliveryResult> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationDeliveryManager.testChannel(channel, testNotification)

        } catch (e: Exception) {
            Timber.e(e, "Failed to test channel ${channel.id}")
            Result.failure(e)
        }
    }

    override fun getScheduledNotifications(): Flow<List<ScheduledNotificationStatus>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            notificationScheduler.getScheduledNotifications().collect { scheduled ->
                emit(scheduled)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get scheduled notifications")
            emit(emptyList())
        }
    }

    override fun getScheduleExecutionHistory(scheduleId: String): Flow<List<ScheduleExecutionResult>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            notificationScheduler.getScheduleExecutionHistory(scheduleId).collect { history ->
                emit(history)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get schedule execution history for $scheduleId")
            emit(emptyList())
        }
    }

    override suspend fun pauseUserNotifications(userId: String, duration: Long?): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationPreferenceManager.pauseUserNotifications(userId, duration)

            Timber.d("Notifications paused for user $userId${duration?.let { " for ${it}ms" } ?: " indefinitely"}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to pause notifications for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun resumeUserNotifications(userId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationPreferenceManager.resumeUserNotifications(userId)

            Timber.d("Notifications resumed for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to resume notifications for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun enableDoNotDisturb(duration: Long?): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationPreferenceManager.enableDoNotDisturb(duration)

            Timber.d("Do not disturb enabled${duration?.let { " for ${it}ms" } ?: " indefinitely"}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to enable do not disturb")
            Result.failure(e)
        }
    }

    override suspend fun disableDoNotDisturb(): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            notificationPreferenceManager.disableDoNotDisturb()

            Timber.d("Do not disturb disabled")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to disable do not disturb")
            Result.failure(e)
        }
    }

    override suspend fun isDoNotDisturbActive(): Boolean {
        return try {
            if (!isInitialized) {
                return false
            }

            notificationPreferenceManager.isDoNotDisturbActive()

        } catch (e: Exception) {
            Timber.e(e, "Failed to check do not disturb status")
            false
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
            notificationDeliveryManager.getDeliveryHistory(notificationId, channel, startTime, endTime).collect { history ->
                emit(history)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get delivery history")
            emit(emptyList())
        }
    }

    override suspend fun retryFailedDeliveries(maxRetries: Int, olderThan: Long?): Result<Int> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            val retryCount = notificationDeliveryManager.retryFailedDeliveries(maxRetries, olderThan)

            if (retryCount.isSuccess) {
                Timber.d("Retried ${retryCount.getOrNull()} failed deliveries")
            }

            retryCount

        } catch (e: Exception) {
            Timber.e(e, "Failed to retry failed deliveries")
            Result.failure(e)
        }
    }

    override suspend fun cleanup(
        olderThan: Long,
        includeDelivered: Boolean,
        includeFailed: Boolean
    ): Result<Int> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            val cleanupCount = notificationDeliveryManager.cleanup(olderThan, includeDelivered, includeFailed)

            if (cleanupCount.isSuccess) {
                Timber.d("Cleaned up ${cleanupCount.getOrNull()} old notifications")
            }

            cleanupCount

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup notifications")
            Result.failure(e)
        }
    }

    override suspend fun exportAnalyticsData(
        startTime: Long,
        endTime: Long,
        format: ExportFormat
    ): Result<String> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Get data from all sources
            val deliveryHistory = notificationDeliveryManager.getDeliveryHistory(
                startTime = startTime,
                endTime = endTime
            )

            val scheduledHistory = notificationScheduler.getScheduleExecutionHistory()

            // Combine and format data
            val analyticsData = mapOf(
                "deliveryHistory" to deliveryHistory,
                "scheduledHistory" to scheduledHistory,
                "statistics" to notificationStats.value,
                "channelHealth" to channelHealth.value,
                "exportInfo" to mapOf(
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "format" to format.name,
                    "exportedAt" to System.currentTimeMillis()
                )
            )

            // Format based on requested format
            val formattedData = when (format) {
                ExportFormat.JSON -> kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    kotlinx.serialization.json.Json.encodeToJsonElement(analyticsData)
                )
                ExportFormat.CSV -> convertToCsv(analyticsData)
                ExportFormat.XML -> convertToXml(analyticsData)
                ExportFormat.PDF -> convertToPdf(analyticsData)
            }

            Result.success(formattedData)

        } catch (e: Exception) {
            Timber.e(e, "Failed to export analytics data")
            Result.failure(e)
        }
    }

    override suspend fun executeAction(
        notificationId: String,
        actionId: String,
        actionData: Map<String, Any>
    ): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Get notification to find the action
            val notification = getNotification(notificationId)
            if (notification.isFailure) {
                return@withContext Result.failure(notification.exceptionOrNull() ?: IllegalArgumentException("Notification not found"))
            }

            val action = notification.getOrNull()!!.actions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Action not found: $actionId"))

            // Execute action based on type
            when (action.type) {
                ActionType.OPEN_APP -> systemIntegrationHub.openApp(actionData)
                ActionType.OPEN_SCREEN -> systemIntegrationHub.openScreen(action.deepLink ?: "", actionData)
                ActionType.OPEN_GAME -> systemIntegrationHub.openGame(actionData["gameId"] as? String ?: "")
                ActionType.DISMISS -> markAsDismissed(notificationId)
                ActionType.REPLY -> handleReplyAction(notificationId, action, actionData)
                ActionType.SHARE -> handleShareAction(notification, action, actionData)
                ActionType.SAVE -> handleSaveAction(notificationId, action, actionData)
                ActionType.RATE -> handleRateAction(notificationId, action, actionData)
                ActionType.PURCHASE -> handlePurchaseAction(notificationId, action, actionData)
                ActionType.JOIN_GAME -> handleJoinGameAction(notificationId, action, actionData)
                ActionType.VIEW_PROFILE -> handleViewProfileAction(action, actionData)
                ActionType.ACCEPT_INVITATION -> handleAcceptInvitationAction(notificationId, action, actionData)
                ActionType.DECLINE_INVITATION -> handleDeclineInvitationAction(notificationId, action, actionData)
                ActionType.CUSTOM -> handleCustomAction(notificationId, action, actionData)
            }

            Timber.d("Action executed: $actionId for notification $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to execute action $actionId for notification $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun validateNotification(notification: Notification): Result<NotificationValidationResult> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            val suggestions = mutableListOf<String>()

            // Basic validation
            if (notification.title.isBlank()) {
                errors.add("Notification title cannot be empty")
            }

            if (notification.message.isBlank()) {
                errors.add("Notification message cannot be empty")
            }

            if (notification.channels.isEmpty()) {
                warnings.add("No channels specified for notification delivery")
                suggestions.add("Consider adding at least one delivery channel")
            }

            // Channel validation
            notification.channels.forEach { channel ->
                when (channel) {
                    is NotificationChannel.EmailNotification -> {
                        if (!isValidEmail(channel.emailAddress)) {
                            errors.add("Invalid email address for channel ${channel.id}")
                        }
                    }
                    is NotificationChannel.SMSNotification -> {
                        if (channel.phoneNumber.isBlank()) {
                            errors.add("Phone number cannot be empty for SMS channel ${channel.id}")
                        }
                    }
                    is NotificationChannel.PushNotification -> {
                        if (channel.pushToken.isNullOrBlank()) {
                            warnings.add("No push token specified for push channel ${channel.id}")
                        }
                    }
                }
            }

            // Schedule validation
            if (notification.schedule != null) {
                when (notification.schedule.type) {
                    ScheduleType.SCHEDULED -> {
                        if (notification.schedule.triggerTime == null) {
                            errors.add("Trigger time must be specified for scheduled notifications")
                        }
                    }
                    ScheduleType.RECURRING -> {
                        if (notification.schedule.interval == null) {
                            errors.add("Interval must be specified for recurring notifications")
                        }
                    }
                    ScheduleType.CRON -> {
                        if (notification.schedule.cronExpression.isNullOrBlank()) {
                            errors.add("Cron expression must be specified for cron notifications")
                        }
                    }
                }
            }

            // Priority validation
            if (notification.priority == NotificationPriority.URGENT && notification.channels.isEmpty()) {
                warnings.add("Urgent notifications should specify delivery channels")
            }

            val isValid = errors.isEmpty()

            NotificationValidationResult(
                isValid = isValid,
                errors = errors,
                warnings = warnings,
                suggestions = suggestions
            ).let { Result.success(it) }

        } catch (e: Exception) {
            Timber.e(e, "Failed to validate notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun getNotificationTemplate(type: NotificationType): Result<NotificationTemplate> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Get template from preference manager
            notificationPreferenceManager.getNotificationTemplate(type)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification template for type $type")
            Result.failure(e)
        }
    }

    override suspend fun createFromTemplate(
        template: NotificationTemplate,
        data: Map<String, Any>
    ): Result<Notification> = withContext(ioDispatcher) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationManager not initialized"))
            }

            // Validate required fields
            val missingFields = template.requiredFields.filter { it !in data }
            if (missingFields.isNotEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Missing required fields: $missingFields"))
            }

            // Create notification from template
            val notificationId = UUID.randomUUID().toString()
            val title = replaceTemplateVariables(template.titleTemplate, data)
            val message = replaceTemplateVariables(template.messageTemplate, data)

            val notification = Notification(
                id = notificationId,
                type = template.type,
                priority = template.defaultPriority,
                title = title,
                message = message,
                channels = template.defaultChannels.mapNotNull { channelId ->
                    _availableChannels.value.find { it.id == channelId }
                },
                actions = template.defaultActions,
                metadata = NotificationMetadata(
                    category = template.type.name,
                    source = "template:${template.id}"
                )
            )

            Result.success(notification)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create notification from template ${template.id}")
            Result.failure(e)
        }
    }

    // Private helper methods

    private fun startChannelHealthMonitoring() {
        applicationScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(30000) // Check every 30 seconds

                    if (!isInitialized) continue

                    val healthMap = mutableMapOf<NotificationChannel, ChannelHealthMetrics>()
                    _availableChannels.value.forEach { channel ->
                        val status = notificationDeliveryManager.getChannelStatus(channel.id)
                        if (status.isSuccess) {
                            val channelStatus = status.getOrNull()!!
                            healthMap[channel] = ChannelHealthMetrics(
                                channel = channel,
                                uptime = if (channelStatus.isAvailable) 1.0f else 0.0f,
                                responseTime = channelStatus.averageDeliveryTime,
                                errorRate = if (channelStatus.successRate > 0) 1.0f - channelStatus.successRate else 0.0f,
                                lastHealthCheck = System.currentTimeMillis()
                            )
                        }
                    }

                    _channelHealth.value = healthMap

                } catch (e: Exception) {
                    Timber.e(e, "Failed to monitor channel health")
                }
            }
        }
    }

    private fun startStatisticsMonitoring() {
        applicationScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(60000) // Update every minute

                    if (!isInitialized) continue

                    // Update statistics from delivery manager
                    val stats = notificationDeliveryManager.getNotificationStats()
                    if (stats.isSuccess) {
                        _notificationStats.value = stats.getOrNull()!!
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to monitor statistics")
                }
            }
        }
    }

    private suspend fun loadAvailableChannels() {
        try {
            val channels = notificationDeliveryManager.getAvailableChannels()
            if (channels.isSuccess) {
                _availableChannels.value = channels.getOrNull()!!
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load available channels")
        }
    }

    private fun startSystemIntegration() {
        applicationScope.launch {
            try {
                // Monitor system events that might affect notifications
                systemIntegrationHub.systemEvents.collect { event ->
                    when (event) {
                        is SystemIntegrationHub.SystemEvent.ConnectivityChanged -> {
                            if (!event.isConnected) {
                                Timber.d("Connectivity lost, adjusting notification delivery")
                                // Could pause non-critical notifications
                            }
                        }
                        is SystemIntegrationHub.SystemEvent.BatteryLevelChanged -> {
                            if (event.level < 0.15f) { // 15% battery
                                Timber.d("Low battery, optimizing notification delivery")
                                // Could reduce notification frequency
                            }
                        }
                        is SystemIntegrationHub.SystemEvent.UserPresenceChanged -> {
                            if (!event.isPresent) {
                                Timber.d("User not present, adjusting notification strategy")
                                // Could delay notifications or use different channels
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to monitor system integration")
            }
        }
    }

    private fun updateStatistics(deliveryResults: List<NotificationDeliveryResult>) {
        val currentStats = _notificationStats.value
        val newStats = currentStats.copy(
            totalSent = currentStats.totalSent + deliveryResults.size,
            totalDelivered = currentStats.totalDelivered + deliveryResults.count { it.success },
            totalFailed = currentStats.totalFailed + deliveryResults.count { !it.success }
        )
        _notificationStats.value = newStats
    }

    private fun replaceTemplateVariables(template: String, data: Map<String, Any>): String {
        var result = template
        data.forEach { (key, value) ->
            result = result.replace("{{$key}}", value.toString())
        }
        return result
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Action handlers (these would be implemented based on specific requirements)

    private suspend fun handleReplyAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle reply functionality
        Timber.d("Handling reply action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleShareAction(
        notification: Notification,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle sharing functionality
        Timber.d("Handling share action for notification ${notification.id}")
        return Result.success(Unit)
    }

    private suspend fun handleSaveAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle save functionality
        Timber.d("Handling save action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleRateAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle rating functionality
        Timber.d("Handling rate action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handlePurchaseAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle purchase functionality
        Timber.d("Handling purchase action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleJoinGameAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle joining game functionality
        Timber.d("Handling join game action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleViewProfileAction(
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle view profile functionality
        Timber.d("Handling view profile action")
        return Result.success(Unit)
    }

    private suspend fun handleAcceptInvitationAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle accepting invitation functionality
        Timber.d("Handling accept invitation action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleDeclineInvitationAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle declining invitation functionality
        Timber.d("Handling decline invitation action for notification $notificationId")
        return Result.success(Unit)
    }

    private suspend fun handleCustomAction(
        notificationId: String,
        action: NotificationAction,
        actionData: Map<String, Any>
    ): Result<Unit> {
        // Implementation would handle custom action functionality
        Timber.d("Handling custom action for notification $notificationId")
        return Result.success(Unit)
    }

    private fun convertToCsv(data: Map<String, Any>): String {
        // Simple CSV conversion implementation
        return "CSV format not fully implemented in this example"
    }

    private fun convertToXml(data: Map<String, Any>): String {
        // Simple XML conversion implementation
        return "<xml>XML format not fully implemented in this example</xml>"
    }

    private fun convertToPdf(data: Map<String, Any>): String {
        // PDF conversion would require additional libraries
        return "PDF format not implemented in this example"
    }
}