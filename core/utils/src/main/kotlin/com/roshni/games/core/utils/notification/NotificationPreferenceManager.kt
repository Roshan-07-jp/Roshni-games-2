package com.roshni.games.core.utils.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing user notification preferences
 */
interface NotificationPreferenceManager {

    /**
     * Global notification preferences
     */
    val globalPreferences: StateFlow<NotificationPreferences>

    /**
     * Do not disturb status
     */
    val doNotDisturbStatus: StateFlow<DoNotDisturbStatus>

    /**
     * Initialize the preference manager
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Shutdown the preference manager
     */
    suspend fun shutdown(): Result<Unit>

    /**
     * Get user notification preferences
     */
    suspend fun getUserPreferences(userId: String): Result<UserNotificationPreferences>

    /**
     * Update user notification preferences
     */
    suspend fun updateUserPreferences(
        userId: String,
        preferences: UserNotificationPreferences
    ): Result<Unit>

    /**
     * Get preferred channels for a user
     */
    suspend fun getPreferredChannels(userId: String): List<NotificationChannel>

    /**
     * Set preferred channels for a user
     */
    suspend fun setPreferredChannels(
        userId: String,
        channels: List<NotificationChannel>
    ): Result<Unit>

    /**
     * Get notification preferences for specific types
     */
    suspend fun getPreferencesForType(
        userId: String,
        type: NotificationType
    ): Result<TypeNotificationPreferences>

    /**
     * Update preferences for specific notification type
     */
    suspend fun updatePreferencesForType(
        userId: String,
        type: NotificationType,
        preferences: TypeNotificationPreferences
    ): Result<Unit>

    /**
     * Pause notifications for a user
     */
    suspend fun pauseUserNotifications(userId: String, duration: Long?): Result<Unit>

    /**
     * Resume notifications for a user
     */
    suspend fun resumeUserNotifications(userId: String): Result<Unit>

    /**
     * Enable do not disturb mode
     */
    suspend fun enableDoNotDisturb(duration: Long?): Result<Unit>

    /**
     * Disable do not disturb mode
     */
    suspend fun disableDoNotDisturb(): Result<Unit>

    /**
     * Check if do not disturb is active
     */
    suspend fun isDoNotDisturbActive(): Boolean

    /**
     * Get notification template by type
     */
    suspend fun getNotificationTemplate(type: NotificationType): Result<NotificationTemplate>

    /**
     * Save notification template
     */
    suspend fun saveNotificationTemplate(template: NotificationTemplate): Result<Unit>

    /**
     * Delete notification template
     */
    suspend fun deleteNotificationTemplate(templateId: String): Result<Unit>

    /**
     * Get all notification templates
     */
    suspend fun getAllNotificationTemplates(): Result<List<NotificationTemplate>>

    /**
     * Check if user should receive notification based on preferences
     */
    suspend fun shouldReceiveNotification(
        userId: String,
        notification: Notification
    ): Result<Boolean>

    /**
     * Get quiet hours for a user
     */
    suspend fun getQuietHours(userId: String): Result<List<TimeWindow>>

    /**
     * Set quiet hours for a user
     */
    suspend fun setQuietHours(userId: String, quietHours: List<TimeWindow>): Result<Unit>

    /**
     * Reset user preferences to defaults
     */
    suspend fun resetUserPreferences(userId: String): Result<Unit>

    /**
     * Export user preferences
     */
    suspend fun exportUserPreferences(userId: String): Result<String>

    /**
     * Import user preferences
     */
    suspend fun importUserPreferences(userId: String, preferencesJson: String): Result<Unit>
}

/**
 * Global notification preferences
 */
data class NotificationPreferences(
    val defaultChannels: List<String> = listOf("in_app", "push"),
    val defaultPriority: NotificationPriority = NotificationPriority.NORMAL,
    val enableAnalytics: Boolean = true,
    val enableLogging: Boolean = true,
    val rateLimitPerHour: Int = 100,
    val batchSize: Int = 10,
    val retryAttempts: Int = 3,
    val enableGrouping: Boolean = true,
    val groupByType: Boolean = true,
    val maxNotificationsPerDay: Int = 50
)

/**
 * User-specific notification preferences
 */
data class UserNotificationPreferences(
    val userId: String,
    val isEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val pauseUntil: Long? = null,
    val preferredChannels: List<String> = emptyList(),
    val blockedTypes: Set<NotificationType> = emptySet(),
    val priorityOverrides: Map<NotificationType, NotificationPriority> = emptyMap(),
    val quietHours: List<TimeWindow> = emptyList(),
    val typePreferences: Map<NotificationType, TypeNotificationPreferences> = emptyMap(),
    val customSettings: Map<String, Any> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Preferences for specific notification types
 */
data class TypeNotificationPreferences(
    val type: NotificationType,
    val isEnabled: Boolean = true,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val channels: List<String> = emptyList(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showPreview: Boolean = true,
    val groupWithSimilar: Boolean = true,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Do not disturb status
 */
data class DoNotDisturbStatus(
    val isActive: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val duration: Long? = null,
    val reason: String? = null
)

/**
 * Implementation of NotificationPreferenceManager
 */
@Singleton
class NotificationPreferenceManagerImpl @Inject constructor() : NotificationPreferenceManager {

    private val _globalPreferences = MutableStateFlow(NotificationPreferences())
    private val _doNotDisturbStatus = MutableStateFlow(DoNotDisturbStatus())

    private val userPreferences = mutableMapOf<String, UserNotificationPreferences>()
    private val notificationTemplates = mutableMapOf<NotificationType, NotificationTemplate>()
    private val quietHoursMap = mutableMapOf<String, List<TimeWindow>>()

    private var isInitialized = false

    override val globalPreferences: StateFlow<NotificationPreferences> = _globalPreferences.asStateFlow()
    override val doNotDisturbStatus: StateFlow<DoNotDisturbStatus> = _doNotDisturbStatus.asStateFlow()

    init {
        // Initialize default templates
        initializeDefaultTemplates()

        // Start do not disturb monitoring
        startDoNotDisturbMonitoring()
    }

    override suspend fun initialize(): Result<Unit> {
        return try {
            if (isInitialized) {
                Timber.d("NotificationPreferenceManager already initialized")
                return Result.success(Unit)
            }

            Timber.d("Initializing NotificationPreferenceManager")

            // Load preferences from storage (in real implementation)
            loadGlobalPreferences()
            loadUserPreferences()
            loadQuietHours()

            isInitialized = true
            Timber.d("NotificationPreferenceManager initialized successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NotificationPreferenceManager")
            Result.failure(e)
        }
    }

    override suspend fun shutdown(): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.success(Unit)
            }

            Timber.d("Shutting down NotificationPreferenceManager")

            // Save preferences to storage (in real implementation)
            saveGlobalPreferences()
            saveUserPreferences()
            saveQuietHours()

            isInitialized = false
            Timber.d("NotificationPreferenceManager shutdown successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to shutdown NotificationPreferenceManager")
            Result.failure(e)
        }
    }

    override suspend fun getUserPreferences(userId: String): Result<UserNotificationPreferences> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val preferences = userPreferences[userId] ?: createDefaultUserPreferences(userId)
            Result.success(preferences)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get user preferences for $userId")
            Result.failure(e)
        }
    }

    override suspend fun updateUserPreferences(
        userId: String,
        preferences: UserNotificationPreferences
    ): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            userPreferences[userId] = preferences.copy(lastUpdated = System.currentTimeMillis())

            Timber.d("User preferences updated for $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user preferences for $userId")
            Result.failure(e)
        }
    }

    override suspend fun getPreferredChannels(userId: String): List<NotificationChannel> {
        return try {
            if (!isInitialized) {
                return emptyList()
            }

            val preferences = getUserPreferences(userId)
            if (preferences.isFailure) {
                return emptyList()
            }

            val userPrefs = preferences.getOrNull()!!
            val preferredChannelIds = if (userPrefs.preferredChannels.isNotEmpty()) {
                userPrefs.preferredChannels
            } else {
                globalPreferences.value.defaultChannels
            }

            // Convert channel IDs to actual channel objects
            // In real implementation, this would come from a channel registry
            preferredChannelIds.mapNotNull { channelId ->
                when (channelId) {
                    "push" -> NotificationChannel.PushNotification()
                    "in_app" -> NotificationChannel.InAppNotification()
                    "email" -> null // Would need email address
                    "sms" -> null // Would need phone number
                    else -> null
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to get preferred channels for $userId")
            emptyList()
        }
    }

    override suspend fun setPreferredChannels(
        userId: String,
        channels: List<NotificationChannel>
    ): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val preferences = getUserPreferences(userId)
            if (preferences.isFailure) {
                return preferences
            }

            val userPrefs = preferences.getOrNull()!!
            val channelIds = channels.map { it.id }

            val updatedPreferences = userPrefs.copy(
                preferredChannels = channelIds,
                lastUpdated = System.currentTimeMillis()
            )

            updateUserPreferences(userId, updatedPreferences)

        } catch (e: Exception) {
            Timber.e(e, "Failed to set preferred channels for $userId")
            Result.failure(e)
        }
    }

    override suspend fun getPreferencesForType(
        userId: String,
        type: NotificationType
    ): Result<TypeNotificationPreferences> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val userPrefs = getUserPreferences(userId)
            if (userPrefs.isFailure) {
                return Result.failure(userPrefs.exceptionOrNull()!!)
            }

            val preferences = userPrefs.getOrNull()!!
            val typePrefs = preferences.typePreferences[type] ?: createDefaultTypePreferences(type)

            Result.success(typePrefs)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get preferences for type $type for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun updatePreferencesForType(
        userId: String,
        type: NotificationType,
        preferences: TypeNotificationPreferences
    ): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val userPrefs = getUserPreferences(userId)
            if (userPrefs.isFailure) {
                return userPrefs
            }

            val currentPrefs = userPrefs.getOrNull()!!
            val updatedTypePreferences = currentPrefs.typePreferences.toMutableMap()
            updatedTypePreferences[type] = preferences

            val updatedUserPrefs = currentPrefs.copy(
                typePreferences = updatedTypePreferences,
                lastUpdated = System.currentTimeMillis()
            )

            updateUserPreferences(userId, updatedUserPrefs)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update preferences for type $type for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun pauseUserNotifications(userId: String, duration: Long?): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val preferences = getUserPreferences(userId)
            if (preferences.isFailure) {
                return preferences
            }

            val userPrefs = preferences.getOrNull()!!
            val pauseUntil = duration?.let { System.currentTimeMillis() + it }

            val updatedPreferences = userPrefs.copy(
                isPaused = true,
                pauseUntil = pauseUntil,
                lastUpdated = System.currentTimeMillis()
            )

            updateUserPreferences(userId, updatedPreferences)

        } catch (e: Exception) {
            Timber.e(e, "Failed to pause notifications for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun resumeUserNotifications(userId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val preferences = getUserPreferences(userId)
            if (preferences.isFailure) {
                return preferences
            }

            val userPrefs = preferences.getOrNull()!!

            val updatedPreferences = userPrefs.copy(
                isPaused = false,
                pauseUntil = null,
                lastUpdated = System.currentTimeMillis()
            )

            updateUserPreferences(userId, updatedPreferences)

        } catch (e: Exception) {
            Timber.e(e, "Failed to resume notifications for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun enableDoNotDisturb(duration: Long?): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val endTime = duration?.let { System.currentTimeMillis() + it }

            val status = DoNotDisturbStatus(
                isActive = true,
                startTime = System.currentTimeMillis(),
                endTime = endTime,
                duration = duration,
                reason = "Manual activation"
            )

            _doNotDisturbStatus.value = status

            Timber.d("Do not disturb enabled${duration?.let { " for ${it}ms" } ?: " indefinitely"}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to enable do not disturb")
            Result.failure(e)
        }
    }

    override suspend fun disableDoNotDisturb(): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val status = DoNotDisturbStatus(isActive = false)
            _doNotDisturbStatus.value = status

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

            val status = _doNotDisturbStatus.value

            if (!status.isActive) {
                return false
            }

            // Check if duration has expired
            if (status.endTime != null && System.currentTimeMillis() > status.endTime) {
                // Auto-disable expired do not disturb
                disableDoNotDisturb()
                return false
            }

            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to check do not disturb status")
            false
        }
    }

    override suspend fun getNotificationTemplate(type: NotificationType): Result<NotificationTemplate> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val template = notificationTemplates[type]
                ?: return Result.failure(NoSuchElementException("Template not found for type $type"))

            Result.success(template)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification template for type $type")
            Result.failure(e)
        }
    }

    override suspend fun saveNotificationTemplate(template: NotificationTemplate): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            notificationTemplates[template.type] = template

            Timber.d("Notification template saved: ${template.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save notification template ${template.id}")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotificationTemplate(templateId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val template = notificationTemplates.values.find { it.id == templateId }
                ?: return Result.failure(NoSuchElementException("Template not found: $templateId"))

            notificationTemplates.remove(template.type)

            Timber.d("Notification template deleted: $templateId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification template $templateId")
            Result.failure(e)
        }
    }

    override suspend fun getAllNotificationTemplates(): Result<List<NotificationTemplate>> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val templates = notificationTemplates.values.toList()
            Result.success(templates)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get all notification templates")
            Result.failure(e)
        }
    }

    override suspend fun shouldReceiveNotification(
        userId: String,
        notification: Notification
    ): Result<Boolean> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val userPrefs = getUserPreferences(userId)
            if (userPrefs.isFailure) {
                return Result.success(true) // Default to allowing if preferences unavailable
            }

            val preferences = userPrefs.getOrNull()!!

            // Check if notifications are enabled for user
            if (!preferences.isEnabled) {
                return Result.success(false)
            }

            // Check if notifications are paused
            if (preferences.isPaused) {
                if (preferences.pauseUntil != null && System.currentTimeMillis() > preferences.pauseUntil) {
                    // Auto-resume expired pause
                    resumeUserNotifications(userId)
                } else {
                    return Result.success(false)
                }
            }

            // Check if notification type is blocked
            if (preferences.blockedTypes.contains(notification.type)) {
                return Result.success(false)
            }

            // Check quiet hours
            if (isInQuietHours(userId, notification)) {
                return Result.success(false)
            }

            // Check do not disturb
            if (isDoNotDisturbActive() && notification.priority != NotificationPriority.URGENT) {
                return Result.success(false)
            }

            // Check type-specific preferences
            val typePrefs = preferences.typePreferences[notification.type]
            if (typePrefs != null && !typePrefs.isEnabled) {
                return Result.success(false)
            }

            Result.success(true)

        } catch (e: Exception) {
            Timber.e(e, "Failed to check if user $userId should receive notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun getQuietHours(userId: String): Result<List<TimeWindow>> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val quietHours = quietHoursMap[userId] ?: emptyList()
            Result.success(quietHours)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get quiet hours for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun setQuietHours(userId: String, quietHours: List<TimeWindow>): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            quietHoursMap[userId] = quietHours

            Timber.d("Quiet hours set for user $userId: ${quietHours.size} time windows")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to set quiet hours for user $userId")
            Result.failure(e)
        }
    }

    override suspend fun resetUserPreferences(userId: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val defaultPreferences = createDefaultUserPreferences(userId)
            userPreferences[userId] = defaultPreferences
            quietHoursMap.remove(userId)

            Timber.d("User preferences reset for $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to reset user preferences for $userId")
            Result.failure(e)
        }
    }

    override suspend fun exportUserPreferences(userId: String): Result<String> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val preferences = getUserPreferences(userId)
            if (preferences.isFailure) {
                return Result.failure(preferences.exceptionOrNull()!!)
            }

            val exportData = mapOf(
                "userId" to userId,
                "preferences" to preferences.getOrNull()!!,
                "quietHours" to (quietHoursMap[userId] ?: emptyList<TimeWindow>()),
                "exportedAt" to System.currentTimeMillis()
            )

            val json = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                kotlinx.serialization.json.Json.encodeToJsonElement(exportData)
            )

            Result.success(json)

        } catch (e: Exception) {
            Timber.e(e, "Failed to export user preferences for $userId")
            Result.failure(e)
        }
    }

    override suspend fun importUserPreferences(userId: String, preferencesJson: String): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationPreferenceManager not initialized"))
            }

            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(preferencesJson)
            val jsonMap = jsonElement as? Map<String, kotlinx.serialization.json.JsonElement>
                ?: return Result.failure(IllegalArgumentException("Invalid preferences JSON format"))

            // Parse preferences (simplified implementation)
            // In real implementation, would properly deserialize

            Timber.d("User preferences imported for $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to import user preferences for $userId")
            Result.failure(e)
        }
    }

    // Private helper methods

    private fun initializeDefaultTemplates() {
        notificationTemplates[NotificationType.GAME_UPDATE] = NotificationTemplate(
            id = "game_update_template",
            type = NotificationType.GAME_UPDATE,
            name = "Game Update",
            titleTemplate = "New Update Available",
            messageTemplate = "A new update is available for {gameName}. Update now to get the latest features!",
            defaultChannels = listOf("push", "in_app"),
            defaultPriority = NotificationPriority.NORMAL,
            requiredFields = setOf("gameName"),
            optionalFields = setOf("version", "updateSize", "changelog")
        )

        notificationTemplates[NotificationType.ACHIEVEMENT_UNLOCKED] = NotificationTemplate(
            id = "achievement_template",
            type = NotificationType.ACHIEVEMENT_UNLOCKED,
            name = "Achievement Unlocked",
            titleTemplate = "Achievement Unlocked!",
            messageTemplate = "Congratulations! You've unlocked the {achievementName} achievement.",
            defaultChannels = listOf("in_app"),
            defaultPriority = NotificationPriority.NORMAL,
            requiredFields = setOf("achievementName"),
            optionalFields = setOf("achievementDescription", "points", "rarity")
        )

        notificationTemplates[NotificationType.FRIEND_REQUEST] = NotificationTemplate(
            id = "friend_request_template",
            type = NotificationType.FRIEND_REQUEST,
            name = "Friend Request",
            titleTemplate = "New Friend Request",
            messageTemplate = "{senderName} wants to be your friend.",
            defaultChannels = listOf("push", "in_app"),
            defaultPriority = NotificationPriority.NORMAL,
            requiredFields = setOf("senderName"),
            optionalFields = setOf("senderMessage", "mutualFriends")
        )
    }

    private fun startDoNotDisturbMonitoring() {
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(60000) // Check every minute

                    if (!isInitialized) continue

                    val status = _doNotDisturbStatus.value
                    if (status.isActive && status.endTime != null) {
                        if (System.currentTimeMillis() > status.endTime) {
                            disableDoNotDisturb()
                        }
                    }

                    // Check user pause expirations
                    userPreferences.values.forEach { prefs ->
                        if (prefs.isPaused && prefs.pauseUntil != null) {
                            if (System.currentTimeMillis() > prefs.pauseUntil) {
                                resumeUserNotifications(prefs.userId)
                            }
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to monitor do not disturb status")
                }
            }
        }
    }

    private fun createDefaultUserPreferences(userId: String): UserNotificationPreferences {
        return UserNotificationPreferences(
            userId = userId,
            preferredChannels = globalPreferences.value.defaultChannels,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun createDefaultTypePreferences(type: NotificationType): TypeNotificationPreferences {
        return TypeNotificationPreferences(
            type = type,
            channels = globalPreferences.value.defaultChannels
        )
    }

    private fun isInQuietHours(userId: String, notification: Notification): Boolean {
        val quietHours = quietHoursMap[userId] ?: return false

        if (quietHours.isEmpty()) return false

        val now = java.time.LocalTime.now()
        val today = java.time.LocalDate.now()

        return quietHours.any { window ->
            val windowStart = window.startTime
            val windowEnd = window.endTime

            // Check if current time is within any quiet hour window
            when {
                windowStart <= windowEnd -> {
                    // Same day window
                    now in windowStart..windowEnd
                }
                else -> {
                    // Overnight window (e.g., 22:00 to 06:00)
                    now >= windowStart || now <= windowEnd
                }
            }
        }
    }

    private suspend fun loadGlobalPreferences() {
        // In real implementation, load from SharedPreferences/DataStore
        _globalPreferences.value = NotificationPreferences()
    }

    private suspend fun saveGlobalPreferences() {
        // In real implementation, save to SharedPreferences/DataStore
    }

    private suspend fun loadUserPreferences() {
        // In real implementation, load from database/SharedPreferences
        userPreferences.clear()
    }

    private suspend fun saveUserPreferences() {
        // In real implementation, save to database/SharedPreferences
    }

    private suspend fun loadQuietHours() {
        // In real implementation, load from database/SharedPreferences
        quietHoursMap.clear()
    }

    private suspend fun saveQuietHours() {
        // In real implementation, save to database/SharedPreferences
    }
}