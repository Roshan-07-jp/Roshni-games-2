/**
 * Notification System Package
 *
 * This package contains a comprehensive multi-channel notification management system
 * designed for the Roshni Games platform. The system provides:
 *
 * ## Core Components
 *
 * ### Data Models
 * - [Notification] - Core notification data class with metadata and state tracking
 * - [NotificationChannel] - Sealed class for different delivery channels (Push, In-App, Email, SMS, Custom)
 * - [NotificationSchedule] - Scheduling configuration for delayed and recurring notifications
 * - [NotificationAction] - Interactive actions that can be performed on notifications
 *
 * ### Managers
 * - [NotificationManager] - Main interface for high-level notification operations
 * - [NotificationManagerImpl] - Implementation coordinating all notification subsystems
 * - [NotificationPreferenceManager] - User preference and configuration management
 * - [NotificationDeliveryManager] - Multi-channel delivery coordination
 * - [NotificationScheduler] - Scheduled and recurring notification handling
 * - [NotificationIntegrationManager] - System integration and event handling
 *
 * ## Features
 *
 * ### Multi-Channel Delivery
 * - Push notifications (FCM, APNS, Web Push)
 * - In-app notifications with customizable positioning
 * - Email notifications with HTML support
 * - SMS notifications with template support
 * - Custom channels for third-party integrations
 *
 * ### Scheduling & Automation
 * - Immediate, delayed, and scheduled delivery
 * - Recurring notifications with cron expressions
 * - Conditional delivery based on user state and preferences
 * - Batch processing for high-volume scenarios
 *
 * ### User Preferences
 * - Per-user notification preferences
 * - Channel-specific settings (sound, vibration, preview)
 * - Quiet hours and do-not-disturb modes
 * - Notification type filtering and blocking
 *
 * ### System Integration
 * - Game session event handling
 * - Achievement and progression notifications
 * - Social interaction notifications (friend requests, messages)
 * - Security and parental control event notifications
 * - Purchase and transaction notifications
 *
 * ### Analytics & Monitoring
 * - Delivery tracking and success rates
 * - Channel health monitoring
 * - User engagement metrics
 * - Performance and error tracking
 *
 * ## Usage Examples
 *
 * ### Send Immediate Notification
 * ```kotlin
 * val notification = Notification(
 *     id = "game-update-001",
 *     type = NotificationType.GAME_UPDATE,
 *     priority = NotificationPriority.NORMAL,
 *     title = "Game Update Available",
 *     message = "A new update is available for your favorite game!",
 *     channels = listOf(
 *         NotificationChannel.PushNotification(pushToken = "user-push-token"),
 *         NotificationChannel.InAppNotification()
 *     ),
 *     metadata = NotificationMetadata(
 *         userId = "user123",
 *         gameId = "game456",
 *         category = "update"
 *     )
 * )
 *
 * notificationManager.sendNotification(notification)
 * ```
 *
 * ### Schedule Recurring Notification
 * ```kotlin
 * val schedule = NotificationSchedule(
 *     type = ScheduleType.RECURRING,
 *     repeatConfig = RepeatConfig(
 *         frequency = RepeatFrequency.DAILY,
 *         interval = 1
 *     ),
 *     timeWindow = TimeWindow(
 *         startTime = LocalTime.of(9, 0),
 *         endTime = LocalTime.of(18, 0)
 *     )
 * )
 *
 * notificationManager.scheduleNotification(notification, schedule)
 * ```
 *
 * ### Handle System Events
 * ```kotlin
 * // Achievement unlocked
 * notificationIntegrationManager.onAchievementUnlocked(
 *     playerId = "player123",
 *     achievementId = "first-win",
 *     achievementData = mapOf(
 *         "name" to "First Victory",
 *         "description" to "Win your first game",
 *         "points" to 100
 *     )
 * )
 * ```
 *
 * ## Architecture
 *
 * The notification system follows a modular architecture with clear separation of concerns:
 *
 * - **Core Layer**: Data models and business logic
 * - **Manager Layer**: High-level APIs and coordination
 * - **Channel Layer**: Channel-specific delivery implementations
 * - **Integration Layer**: System event handling and external integrations
 * - **Preference Layer**: User settings and configuration management
 *
 * ## Best Practices
 *
 * - Always validate notifications before sending
 * - Respect user preferences and quiet hours
 * - Use appropriate priority levels for different notification types
 * - Monitor delivery success rates and channel health
 * - Provide actionable notifications with clear call-to-actions
 * - Test notifications across all supported channels
 *
 * ## Dependencies
 *
 * The notification system integrates with:
 * - [SystemIntegrationHub] for system event handling
 * - [SecurityService] for parental controls and security events
 * - [FeatureIntegrationManager] for cross-feature coordination
 * - Dagger Hilt for dependency injection
 * - Kotlin Coroutines for asynchronous operations
 * - Timber for logging
 */
package com.roshni.games.core.utils.notification