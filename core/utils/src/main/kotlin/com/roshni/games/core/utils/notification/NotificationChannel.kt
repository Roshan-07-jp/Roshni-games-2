package com.roshni.games.core.utils.notification

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Sealed class representing different notification delivery channels
 */
sealed class NotificationChannel(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val configuration: ChannelConfiguration = ChannelConfiguration()
) {

    /**
     * Push notification channel for mobile devices
     */
    data class PushNotification(
        val pushToken: String? = null,
        val platform: PushPlatform = PushPlatform.FCM,
        val badgeCount: Int = 0,
        val soundEnabled: Boolean = true,
        val vibrationEnabled: Boolean = true,
        val override val id: String = "push",
        val override val name: String = "Push Notifications",
        val override val description: String = "Receive push notifications on your device",
        val override val isEnabled: Boolean = true,
        val override val priority: Int = 1,
        val override val configuration: ChannelConfiguration = ChannelConfiguration()
    ) : NotificationChannel(id, name, description, isEnabled, priority, configuration)

    /**
     * In-app notification channel for notifications shown within the app
     */
    data class InAppNotification(
        val showInForeground: Boolean = true,
        val showInBackground: Boolean = false,
        val position: NotificationPosition = NotificationPosition.TOP,
        val duration: Long = 5000L, // milliseconds
        val allowDismiss: Boolean = true,
        val requireInteraction: Boolean = false,
        val override val id: String = "in_app",
        val override val name: String = "In-App Notifications",
        val override val description: String = "Show notifications within the app interface",
        val override val isEnabled: Boolean = true,
        val override val priority: Int = 2,
        val override val configuration: ChannelConfiguration = ChannelConfiguration()
    ) : NotificationChannel(id, name, description, isEnabled, priority, configuration)

    /**
     * Email notification channel
     */
    data class EmailNotification(
        val emailAddress: String,
        val templateId: String? = null,
        val useHtml: Boolean = true,
        val attachments: List<String> = emptyList(),
        val override val id: String = "email",
        val override val name: String = "Email Notifications",
        val override val description: String = "Receive notifications via email",
        val override val isEnabled: Boolean = true,
        val override val priority: Int = 3,
        val override val configuration: ChannelConfiguration = ChannelConfiguration()
    ) : NotificationChannel(id, name, description, isEnabled, priority, configuration)

    /**
     * SMS notification channel
     */
    data class SMSNotification(
        val phoneNumber: String,
        val countryCode: String = "+1",
        val templateId: String? = null,
        val override val id: String = "sms",
        val override val name: String = "SMS Notifications",
        val override val description: String = "Receive notifications via text message",
        val override val isEnabled: Boolean = true,
        val override val priority: Int = 4,
        val override val configuration: ChannelConfiguration = ChannelConfiguration()
    ) : NotificationChannel(id, name, description, isEnabled, priority, configuration)

    /**
     * Custom notification channel for third-party integrations
     */
    data class CustomChannel(
        val provider: String,
        val providerConfig: Map<String, Any> = emptyMap(),
        val webhookUrl: String? = null,
        val apiKey: String? = null,
        val customHeaders: Map<String, String> = emptyMap(),
        val override val id: String,
        val override val name: String,
        val override val description: String,
        val override val isEnabled: Boolean = true,
        val override val priority: Int = 5,
        val override val configuration: ChannelConfiguration = ChannelConfiguration()
    ) : NotificationChannel(id, name, description, isEnabled, priority, configuration)
}

/**
 * Push notification platforms
 */
enum class PushPlatform {
    FCM,    // Firebase Cloud Messaging
    APNS,   // Apple Push Notification Service
    HMS,    // Huawei Mobile Services
    WEB_PUSH
}

/**
 * In-app notification display positions
 */
enum class NotificationPosition {
    TOP,
    BOTTOM,
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * Channel configuration for delivery settings
 */
data class ChannelConfiguration(
    val retryAttempts: Int = 3,
    val retryDelay: Long = 1000L, // milliseconds
    val timeout: Long = 30000L, // milliseconds
    val rateLimitPerMinute: Int = 60,
    val rateLimitPerHour: Int = 1000,
    val batchSize: Int = 10,
    val enableAnalytics: Boolean = true,
    val enableLogging: Boolean = true,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Channel delivery status
 */
data class ChannelDeliveryStatus(
    val channel: NotificationChannel,
    val isAvailable: Boolean,
    val lastDeliveryTime: Long? = null,
    val successRate: Float = 0.0f,
    val averageDeliveryTime: Long = 0L,
    val errorCount: Int = 0,
    val lastError: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Channel capability information
 */
data class ChannelCapability(
    val channel: NotificationChannel,
    val supportsRichContent: Boolean = false,
    val supportsImages: Boolean = false,
    val supportsActions: Boolean = false,
    val supportsScheduling: Boolean = false,
    val supportsGrouping: Boolean = false,
    val maxTitleLength: Int = 100,
    val maxMessageLength: Int = 500,
    val supportedContentTypes: Set<String> = emptySet(),
    val rateLimits: RateLimitInfo = RateLimitInfo()
)

/**
 * Rate limiting information for channels
 */
data class RateLimitInfo(
    val requestsPerSecond: Int = 10,
    val requestsPerMinute: Int = 100,
    val requestsPerHour: Int = 1000,
    val burstLimit: Int = 20
)

/**
 * Channel health metrics
 */
data class ChannelHealthMetrics(
    val channel: NotificationChannel,
    val uptime: Float = 1.0f,
    val responseTime: Long = 0L,
    val errorRate: Float = 0.0f,
    val throughput: Int = 0, // messages per minute
    val queueSize: Int = 0,
    val lastHealthCheck: Long = System.currentTimeMillis()
)