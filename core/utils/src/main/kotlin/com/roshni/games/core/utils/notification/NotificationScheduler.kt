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
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing scheduled notifications
 */
interface NotificationScheduler {

    /**
     * Scheduled notifications status
     */
    val scheduledNotifications: StateFlow<List<ScheduledNotificationStatus>>

    /**
     * Scheduler health metrics
     */
    val schedulerHealth: StateFlow<SchedulerHealthMetrics>

    /**
     * Initialize the scheduler
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Shutdown the scheduler
     */
    suspend fun shutdown(): Result<Unit>

    /**
     * Schedule a notification
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
     * Update a scheduled notification
     */
    suspend fun updateScheduledNotification(
        notificationId: String,
        notification: Notification
    ): Result<Unit>

    /**
     * Get scheduled notification by ID
     */
    suspend fun getScheduledNotification(notificationId: String): Result<Notification>

    /**
     * Get all scheduled notifications
     */
    fun getScheduledNotifications(): Flow<List<ScheduledNotificationStatus>>

    /**
     * Get schedule execution history
     */
    fun getScheduleExecutionHistory(scheduleId: String): Flow<List<ScheduleExecutionResult>>

    /**
     * Pause scheduler
     */
    suspend fun pauseScheduler(): Result<Unit>

    /**
     * Resume scheduler
     */
    suspend fun resumeScheduler(): Result<Unit>

    /**
     * Check if scheduler is running
     */
    suspend fun isRunning(): Boolean

    /**
     * Get next execution time for a schedule
     */
    suspend fun getNextExecutionTime(scheduleId: String): Result<Long?>

    /**
     * Get scheduler statistics
     */
    suspend fun getSchedulerStatistics(): Result<SchedulerStatistics>

    /**
     * Cleanup old scheduled notifications
     */
    suspend fun cleanup(olderThan: Long): Result<Int>
}

/**
 * Scheduler health metrics
 */
data class SchedulerHealthMetrics(
    val isRunning: Boolean = false,
    val activeSchedules: Int = 0,
    val pendingExecutions: Int = 0,
    val averageExecutionTime: Long = 0L,
    val errorRate: Float = 0.0f,
    val lastHealthCheck: Long = System.currentTimeMillis(),
    val uptime: Long = 0L
)

/**
 * Scheduler statistics
 */
data class SchedulerStatistics(
    val totalSchedules: Int = 0,
    val activeSchedules: Int = 0,
    val completedSchedules: Int = 0,
    val failedSchedules: Int = 0,
    val averageExecutionTime: Long = 0L,
    val totalExecutions: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0,
    val uptime: Long = 0L,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Implementation of NotificationScheduler
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    private val notificationManager: NotificationManager
) : NotificationScheduler {

    private val _scheduledNotifications = MutableStateFlow<List<ScheduledNotificationStatus>>(emptyList())
    private val _schedulerHealth = MutableStateFlow(SchedulerHealthMetrics())

    private val scheduledTasks = mutableMapOf<String, ScheduledTask>()
    private val executionHistory = mutableMapOf<String, MutableList<ScheduleExecutionResult>>()
    private val scheduleQueue = PriorityQueue<ScheduledTask>(compareBy { it.nextExecutionTime })

    private val mutex = Mutex()
    private var isInitialized = false
    private var isPaused = false
    private var schedulerStartTime = System.currentTimeMillis()

    override val scheduledNotifications: StateFlow<List<ScheduledNotificationStatus>> = _scheduledNotifications.asStateFlow()
    override val schedulerHealth: StateFlow<SchedulerHealthMetrics> = _schedulerHealth.asStateFlow()

    init {
        // Start scheduler loop
        startSchedulerLoop()
    }

    override suspend fun initialize(): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            mutex.withLock {
                if (isInitialized) {
                    Timber.d("NotificationScheduler already initialized")
                    return@withContext Result.success(Unit)
                }

                Timber.d("Initializing NotificationScheduler")

                // Load existing scheduled notifications (in real implementation)
                loadScheduledNotifications()

                // Start execution monitoring
                startExecutionMonitoring()

                isInitialized = true
                schedulerStartTime = System.currentTimeMillis()

                Timber.d("NotificationScheduler initialized successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NotificationScheduler")
            Result.failure(e)
        }
    }

    override suspend fun shutdown(): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            mutex.withLock {
                if (!isInitialized) {
                    return@withContext Result.success(Unit)
                }

                Timber.d("Shutting down NotificationScheduler")

                // Cancel all scheduled tasks
                cancelAllSchedules()

                // Save scheduled notifications (in real implementation)
                saveScheduledNotifications()

                isInitialized = false
                _scheduledNotifications.value = emptyList()

                Timber.d("NotificationScheduler shutdown successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to shutdown NotificationScheduler")
            Result.failure(e)
        }
    }

    override suspend fun scheduleNotification(
        notification: Notification,
        schedule: NotificationSchedule
    ): Result<String> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            if (isPaused) {
                return@withContext Result.failure(IllegalStateException("Scheduler is paused"))
            }

            // Generate schedule ID
            val scheduleId = java.util.UUID.randomUUID().toString()

            // Calculate next execution time
            val nextExecutionTime = calculateNextExecutionTime(schedule)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot calculate next execution time"))

            // Create scheduled task
            val scheduledTask = ScheduledTask(
                id = scheduleId,
                notification = notification,
                schedule = schedule,
                nextExecutionTime = nextExecutionTime,
                createdAt = System.currentTimeMillis()
            )

            // Add to tracking
            scheduledTasks[scheduleId] = scheduledTask
            scheduleQueue.add(scheduledTask)

            // Update status
            updateScheduledNotificationStatus(scheduledTask)

            Timber.d("Notification scheduled: $scheduleId, next execution: $nextExecutionTime")
            Result.success(scheduleId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule notification ${notification.id}")
            Result.failure(e)
        }
    }

    override suspend fun cancelScheduledNotification(scheduleId: String): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            val task = scheduledTasks.remove(scheduleId)
                ?: return@withContext Result.failure(NoSuchElementException("Scheduled notification not found: $scheduleId"))

            // Remove from queue
            scheduleQueue.remove(task)

            // Update status
            updateScheduledNotificationStatus(task.copy(status = ScheduleStatus.CANCELLED))

            Timber.d("Scheduled notification cancelled: $scheduleId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel scheduled notification $scheduleId")
            Result.failure(e)
        }
    }

    override suspend fun updateScheduledNotification(
        notificationId: String,
        notification: Notification
    ): Result<Unit> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized) {
                return@withContext Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            // Find the scheduled task
            val task = scheduledTasks.values.find { it.notification.id == notificationId }
                ?: return@withContext Result.failure(NoSuchElementException("Scheduled notification not found: $notificationId"))

            // Update notification
            val updatedTask = task.copy(notification = notification)

            scheduledTasks[task.id] = updatedTask

            // Recalculate next execution time if schedule changed
            if (notification.schedule != task.notification.schedule) {
                val nextExecutionTime = calculateNextExecutionTime(notification.schedule!!)
                if (nextExecutionTime != null) {
                    val recalculatedTask = updatedTask.copy(nextExecutionTime = nextExecutionTime)
                    scheduledTasks[task.id] = recalculatedTask
                    scheduleQueue.remove(task)
                    scheduleQueue.add(recalculatedTask)
                }
            }

            Timber.d("Scheduled notification updated: $notificationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update scheduled notification $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun getScheduledNotification(notificationId: String): Result<Notification> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            val task = scheduledTasks.values.find { it.notification.id == notificationId }
                ?: return@withContext Result.failure(NoSuchElementException("Scheduled notification not found: $notificationId"))

            Result.success(task.notification)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get scheduled notification $notificationId")
            Result.failure(e)
        }
    }

    override fun getScheduledNotifications(): Flow<List<ScheduledNotificationStatus>> = flow {
        if (!isInitialized) {
            emit(emptyList())
            return@flow
        }

        try {
            val statuses = scheduledTasks.values.map { task ->
                ScheduledNotificationStatus(
                    notificationId = task.notification.id,
                    scheduleId = task.id,
                    status = task.status,
                    nextTriggerTime = task.nextExecutionTime,
                    lastTriggerTime = task.lastExecutionTime,
                    triggerCount = task.executionCount,
                    errorCount = task.errorCount,
                    lastError = task.lastError
                )
            }

            emit(statuses)

            kotlinx.coroutines.delay(10000) // Update every 10 seconds

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
            val history = executionHistory[scheduleId] ?: emptyList()
            emit(history)

            kotlinx.coroutines.delay(5000)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get execution history for schedule $scheduleId")
            emit(emptyList())
        }
    }

    override suspend fun pauseScheduler(): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            isPaused = true

            Timber.d("NotificationScheduler paused")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to pause scheduler")
            Result.failure(e)
        }
    }

    override suspend fun resumeScheduler(): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            isPaused = false

            Timber.d("NotificationScheduler resumed")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to resume scheduler")
            Result.failure(e)
        }
    }

    override suspend fun isRunning(): Boolean {
        return isInitialized && !isPaused
    }

    override suspend fun getNextExecutionTime(scheduleId: String): Result<Long?> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            val task = scheduledTasks[scheduleId]
                ?: return Result.failure(NoSuchElementException("Schedule not found: $scheduleId"))

            Result.success(task.nextExecutionTime)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get next execution time for schedule $scheduleId")
            Result.failure(e)
        }
    }

    override suspend fun getSchedulerStatistics(): Result<SchedulerStatistics> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            val totalExecutions = executionHistory.values.sumOf { it.size }
            val successfulExecutions = executionHistory.values.sumOf { history ->
                history.count { it.success }
            }
            val failedExecutions = totalExecutions - successfulExecutions

            val stats = SchedulerStatistics(
                totalSchedules = scheduledTasks.size,
                activeSchedules = scheduledTasks.values.count { it.status == ScheduleStatus.SCHEDULED || it.status == ScheduleStatus.ACTIVE },
                completedSchedules = scheduledTasks.values.count { it.status == ScheduleStatus.COMPLETED },
                failedSchedules = scheduledTasks.values.count { it.status == ScheduleStatus.FAILED },
                totalExecutions = totalExecutions,
                successfulExecutions = successfulExecutions,
                failedExecutions = failedExecutions,
                uptime = System.currentTimeMillis() - schedulerStartTime
            )

            Result.success(stats)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get scheduler statistics")
            Result.failure(e)
        }
    }

    override suspend fun cleanup(olderThan: Long): Result<Int> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("NotificationScheduler not initialized"))
            }

            var cleanupCount = 0

            // Remove old completed/failed schedules
            val toRemove = scheduledTasks.filter { (_, task) ->
                (task.status == ScheduleStatus.COMPLETED || task.status == ScheduleStatus.FAILED) &&
                task.createdAt < olderThan
            }

            toRemove.forEach { (scheduleId, _) ->
                scheduledTasks.remove(scheduleId)
                executionHistory.remove(scheduleId)
                cleanupCount++
            }

            // Clean up old execution history
            executionHistory.values.forEach { history ->
                history.removeAll { it.executionTime < olderThan }
            }

            Timber.d("Cleaned up $cleanupCount old scheduled notifications")
            Result.success(cleanupCount)

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup scheduled notifications")
            Result.failure(e)
        }
    }

    // Private helper methods

    private fun startSchedulerLoop() {
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(1000) // Check every second

                    if (!isInitialized || isPaused) continue

                    // Check for tasks ready to execute
                    val now = System.currentTimeMillis()
                    val readyTasks = scheduleQueue.filter { it.nextExecutionTime <= now }

                    readyTasks.forEach { task ->
                        executeScheduledTask(task)
                    }

                    // Update scheduler health
                    updateSchedulerHealth()

                } catch (e: Exception) {
                    Timber.e(e, "Error in scheduler loop")
                }
            }
        }
    }

    private fun startExecutionMonitoring() {
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(30000) // Monitor every 30 seconds

                    if (!isInitialized) continue

                    // Update scheduled notification statuses
                    updateAllScheduledStatuses()

                } catch (e: Exception) {
                    Timber.e(e, "Error in execution monitoring")
                }
            }
        }
    }

    private suspend fun executeScheduledTask(task: ScheduledTask) {
        try {
            val startTime = System.currentTimeMillis()

            // Remove from queue
            scheduleQueue.remove(task)

            // Update task status
            val executingTask = task.copy(
                status = ScheduleStatus.ACTIVE,
                lastExecutionTime = startTime
            )
            scheduledTasks[task.id] = executingTask

            // Execute notification
            val result = notificationManager.sendNotification(executingTask.notification)

            val executionTime = System.currentTimeMillis() - startTime
            val success = result.isSuccess

            // Update task
            val updatedTask = executingTask.copy(
                status = if (success) ScheduleStatus.COMPLETED else ScheduleStatus.FAILED,
                executionCount = executingTask.executionCount + 1,
                lastExecutionTime = startTime,
                errorCount = if (success) executingTask.errorCount else executingTask.errorCount + 1,
                lastError = if (success) null else result.exceptionOrNull()?.message
            )

            scheduledTasks[task.id] = updatedTask

            // Calculate next execution time for recurring tasks
            if (success && task.schedule.repeatConfig != null) {
                val nextExecutionTime = calculateNextExecutionTime(task.schedule, startTime)
                if (nextExecutionTime != null) {
                    val recurringTask = updatedTask.copy(
                        status = ScheduleStatus.SCHEDULED,
                        nextExecutionTime = nextExecutionTime,
                        executionCount = updatedTask.executionCount
                    )
                    scheduledTasks[task.id] = recurringTask
                    scheduleQueue.add(recurringTask)
                }
            }

            // Record execution result
            val executionResult = ScheduleExecutionResult(
                scheduleId = task.id,
                notificationId = task.notification.id,
                executionTime = startTime,
                success = success,
                errorMessage = if (!success) result.exceptionOrNull()?.message else null,
                metadata = mapOf(
                    "executionTimeMs" to executionTime,
                    "attemptNumber" to updatedTask.executionCount
                )
            )

            executionHistory.getOrPut(task.id) { mutableListOf() }.add(executionResult)

            Timber.d("Scheduled task executed: ${task.id}, success: $success")

        } catch (e: Exception) {
            Timber.e(e, "Failed to execute scheduled task ${task.id}")

            // Update task with error
            val errorTask = task.copy(
                status = ScheduleStatus.FAILED,
                errorCount = task.errorCount + 1,
                lastError = e.message
            )
            scheduledTasks[task.id] = errorTask
        }
    }

    private fun calculateNextExecutionTime(schedule: NotificationSchedule, fromTime: Long? = null): Long? {
        val baseTime = fromTime ?: System.currentTimeMillis()

        return when (schedule.type) {
            ScheduleType.IMMEDIATE -> baseTime
            ScheduleType.DELAYED -> schedule.triggerTime
            ScheduleType.SCHEDULED -> schedule.triggerTime
            ScheduleType.RECURRING -> calculateRecurringTime(schedule, baseTime)
            ScheduleType.CRON -> calculateCronTime(schedule, baseTime)
            ScheduleType.CONDITIONAL -> calculateConditionalTime(schedule, baseTime)
            ScheduleType.BATCH -> calculateBatchTime(schedule, baseTime)
        }
    }

    private fun calculateRecurringTime(schedule: NotificationSchedule, fromTime: Long): Long? {
        val repeatConfig = schedule.repeatConfig ?: return null

        return when (repeatConfig.frequency) {
            RepeatFrequency.MINUTELY -> fromTime + (repeatConfig.interval * 60 * 1000L)
            RepeatFrequency.HOURLY -> fromTime + (repeatConfig.interval * 60 * 60 * 1000L)
            RepeatFrequency.DAILY -> fromTime + (repeatConfig.interval * 24 * 60 * 60 * 1000L)
            RepeatFrequency.WEEKLY -> fromTime + (repeatConfig.interval * 7 * 24 * 60 * 60 * 1000L)
            RepeatFrequency.MONTHLY -> {
                val nextMonth = LocalDateTime.now().plusMonths(repeatConfig.interval.toLong())
                nextMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            RepeatFrequency.YEARLY -> {
                val nextYear = LocalDateTime.now().plusYears(repeatConfig.interval.toLong())
                nextYear.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
    }

    private fun calculateCronTime(schedule: NotificationSchedule, fromTime: Long): Long? {
        // Simplified cron parsing - in real implementation would use a proper cron library
        val cronExpression = schedule.cronExpression ?: return null

        try {
            // Basic cron format: "minute hour day month dayOfWeek"
            val parts = cronExpression.split(" ")
            if (parts.size != 5) return null

            val now = LocalDateTime.now()
            var nextTime = now

            // Simple implementation - just add one hour for demo
            // Real implementation would properly parse cron expression
            nextTime = nextTime.plusHours(1)

            return nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse cron expression: $cronExpression")
            return null
        }
    }

    private fun calculateConditionalTime(schedule: NotificationSchedule, fromTime: Long): Long? {
        // Check if conditions are met
        val conditionsMet = checkScheduleConditions(schedule.conditions)

        return if (conditionsMet) {
            fromTime
        } else {
            // Check again in 5 minutes
            fromTime + (5 * 60 * 1000L)
        }
    }

    private fun calculateBatchTime(schedule: NotificationSchedule, fromTime: Long): Long? {
        // For batch scheduling, use the configured batch timing
        return fromTime + (schedule.timeWindow?.let { 30 * 60 * 1000L } ?: 60 * 1000L) // Default 1 minute
    }

    private fun checkScheduleConditions(conditions: List<ScheduleCondition>): Boolean {
        // Simplified condition checking
        return conditions.all { condition ->
            when (condition.type) {
                ConditionType.TIME_BASED -> checkTimeCondition(condition)
                ConditionType.USER_PRESENCE -> true // Would check actual user presence
                ConditionType.DEVICE_STATE -> true // Would check device state
                ConditionType.GAME_STATE -> true // Would check game state
                ConditionType.LOCATION_BASED -> true // Would check location
                ConditionType.CUSTOM -> true // Would check custom condition
            }
        }
    }

    private fun checkTimeCondition(condition: ScheduleCondition): Boolean {
        val now = LocalTime.now()

        return when (condition.operator) {
            ComparisonOperator.EQUALS -> true // Simplified
            ComparisonOperator.GREATER_THAN -> now > LocalTime.parse(condition.value as String)
            ComparisonOperator.LESS_THAN -> now < LocalTime.parse(condition.value as String)
            else -> true
        }
    }

    private fun updateScheduledNotificationStatus(task: ScheduledTask) {
        val status = ScheduledNotificationStatus(
            notificationId = task.notification.id,
            scheduleId = task.id,
            status = task.status,
            nextTriggerTime = task.nextExecutionTime,
            lastTriggerTime = task.lastExecutionTime,
            triggerCount = task.executionCount,
            errorCount = task.errorCount,
            lastError = task.lastError
        )

        val currentStatuses = _scheduledNotifications.value.toMutableList()
        val existingIndex = currentStatuses.indexOfFirst { it.scheduleId == task.id }

        if (existingIndex >= 0) {
            currentStatuses[existingIndex] = status
        } else {
            currentStatuses.add(status)
        }

        _scheduledNotifications.value = currentStatuses
    }

    private fun updateAllScheduledStatuses() {
        val statuses = scheduledTasks.values.map { task ->
            ScheduledNotificationStatus(
                notificationId = task.notification.id,
                scheduleId = task.id,
                status = task.status,
                nextTriggerTime = task.nextExecutionTime,
                lastTriggerTime = task.lastExecutionTime,
                triggerCount = task.executionCount,
                errorCount = task.errorCount,
                lastError = task.lastError
            )
        }

        _scheduledNotifications.value = statuses
    }

    private fun updateSchedulerHealth() {
        val activeSchedules = scheduledTasks.values.count { it.status == ScheduleStatus.SCHEDULED || it.status == ScheduleStatus.ACTIVE }
        val pendingExecutions = scheduleQueue.size
        val totalExecutions = executionHistory.values.sumOf { it.size }
        val successfulExecutions = executionHistory.values.sumOf { history -> history.count { it.success } }
        val averageExecutionTime = if (totalExecutions > 0) {
            executionHistory.values.flatten().map { it.metadata["executionTimeMs"] as? Long ?: 0L }.average().toLong()
        } else 0L

        val errorRate = if (totalExecutions > 0) {
            (totalExecutions - successfulExecutions).toFloat() / totalExecutions
        } else 0.0f

        val health = SchedulerHealthMetrics(
            isRunning = isRunning(),
            activeSchedules = activeSchedules,
            pendingExecutions = pendingExecutions,
            averageExecutionTime = averageExecutionTime,
            errorRate = errorRate,
            lastHealthCheck = System.currentTimeMillis(),
            uptime = System.currentTimeMillis() - schedulerStartTime
        )

        _schedulerHealth.value = health
    }

    private suspend fun cancelAllSchedules() {
        scheduledTasks.clear()
        scheduleQueue.clear()
    }

    private suspend fun loadScheduledNotifications() {
        // In real implementation, load from database
        scheduledTasks.clear()
        executionHistory.clear()
    }

    private suspend fun saveScheduledNotifications() {
        // In real implementation, save to database
    }
}

/**
 * Internal data class for tracking scheduled tasks
 */
private data class ScheduledTask(
    val id: String,
    val notification: Notification,
    val schedule: NotificationSchedule,
    val nextExecutionTime: Long,
    val createdAt: Long,
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,
    val executionCount: Int = 0,
    val errorCount: Int = 0,
    val lastExecutionTime: Long? = null,
    val lastError: String? = null
)