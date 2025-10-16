package com.roshni.games.core.utils.notification

import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Notification scheduling configuration
 */
data class NotificationSchedule(
    val type: ScheduleType,
    val triggerTime: Long? = null,
    val interval: Long? = null, // milliseconds
    val cronExpression: String? = null,
    val conditions: List<ScheduleCondition> = emptyList(),
    val timeWindow: TimeWindow? = null,
    val repeatConfig: RepeatConfig? = null,
    val timezone: ZoneId = ZoneId.systemDefault(),
    val maxOccurrences: Int? = null,
    val endTime: Long? = null
)

/**
 * Types of notification scheduling
 */
enum class ScheduleType {
    IMMEDIATE,           // Send immediately
    DELAYED,            // Send after a delay
    SCHEDULED,          // Send at specific time
    RECURRING,          // Send at regular intervals
    CRON,               // Send based on cron expression
    CONDITIONAL,        // Send when conditions are met
    BATCH               // Send in batches
}

/**
 * Schedule conditions that must be met for delivery
 */
data class ScheduleCondition(
    val type: ConditionType,
    val field: String,
    val operator: ComparisonOperator,
    val value: Any,
    val description: String? = null
)

/**
 * Types of schedule conditions
 */
enum class ConditionType {
    USER_PRESENCE,      // User is active/inactive
    DEVICE_STATE,       // Device battery, connectivity, etc.
    GAME_STATE,         // Game progress, achievements, etc.
    TIME_BASED,         // Time of day, day of week, etc.
    LOCATION_BASED,     // Geographic location
    CUSTOM              // Custom condition
}

/**
 * Comparison operators for conditions
 */
enum class ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    IN,
    NOT_IN,
    EXISTS,
    NOT_EXISTS
}

/**
 * Time window for scheduling
 */
data class TimeWindow(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val includeHolidays: Boolean = false,
    val timezone: ZoneId = ZoneId.systemDefault()
)

/**
 * Repeat configuration for recurring notifications
 */
data class RepeatConfig(
    val frequency: RepeatFrequency,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val daysOfMonth: Set<Int>? = null,
    val monthsOfYear: Set<Int>? = null,
    val maxRepeats: Int? = null,
    val endDate: LocalDateTime? = null
)

/**
 * Repeat frequency options
 */
enum class RepeatFrequency {
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Notification action configuration
 */
data class NotificationAction(
    val id: String,
    val type: ActionType,
    val label: String,
    val data: Map<String, Any> = emptyMap(),
    val style: ActionStyle = ActionStyle.DEFAULT,
    val icon: String? = null,
    val deepLink: String? = null,
    val requiresAuth: Boolean = false,
    val conditions: List<ActionCondition> = emptyList()
)

/**
 * Types of notification actions
 */
enum class ActionType {
    OPEN_APP,           // Open the app
    OPEN_SCREEN,        // Open specific screen
    OPEN_GAME,          // Open specific game
    DISMISS,            // Dismiss notification
    REPLY,              // Reply to notification
    SHARE,              // Share content
    SAVE,               // Save to favorites/bookmarks
    RATE,               // Rate content
    PURCHASE,           // Make a purchase
    JOIN_GAME,          // Join a game session
    VIEW_PROFILE,       // View user profile
    ACCEPT_INVITATION,  // Accept invitation
    DECLINE_INVITATION, // Decline invitation
    CUSTOM              // Custom action
}

/**
 * Action styling options
 */
enum class ActionStyle {
    DEFAULT,            // Standard button style
    PRIMARY,            // Primary/prominent button
    DESTRUCTIVE,        // Destructive action (red/warning)
    POSITIVE,           // Positive action (green/confirm)
    CANCEL              // Cancel style
}

/**
 * Conditions that must be met for action to be available
 */
data class ActionCondition(
    val type: ConditionType,
    val field: String,
    val operator: ComparisonOperator,
    val value: Any,
    val errorMessage: String? = null
)

/**
 * Scheduled notification status
 */
data class ScheduledNotificationStatus(
    val notificationId: String,
    val scheduleId: String,
    val status: ScheduleStatus,
    val nextTriggerTime: Long? = null,
    val lastTriggerTime: Long? = null,
    val triggerCount: Int = 0,
    val errorCount: Int = 0,
    val lastError: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Schedule execution status
 */
enum class ScheduleStatus {
    PENDING,            // Waiting to be scheduled
    SCHEDULED,          // Scheduled and waiting
    ACTIVE,             // Currently active
    PAUSED,             // Temporarily paused
    COMPLETED,          // Finished all executions
    CANCELLED,          // Cancelled by user/system
    FAILED,             // Failed to execute
    EXPIRED             // Schedule has expired
}

/**
 * Schedule execution result
 */
data class ScheduleExecutionResult(
    val scheduleId: String,
    val notificationId: String,
    val executionTime: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val deliveryResults: List<NotificationDeliveryResult> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Schedule statistics for monitoring
 */
data class ScheduleStatistics(
    val scheduleId: String,
    val totalExecutions: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0,
    val averageExecutionTime: Long = 0L,
    val lastExecutionTime: Long? = null,
    val nextExecutionTime: Long? = null,
    val uptime: Float = 1.0f,
    val errorRate: Float = 0.0f
)

/**
 * Batch scheduling configuration
 */
data class BatchScheduleConfig(
    val batchSize: Int = 10,
    val maxWaitTime: Long = 300000L, // 5 minutes
    val triggerThreshold: Int = 5,
    val groupBy: String? = null,
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.ASCENDING
)

/**
 * Sort order for batch scheduling
 */
enum class SortOrder {
    ASCENDING,
    DESCENDING
}