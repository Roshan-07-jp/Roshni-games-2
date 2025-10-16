package com.roshni.games.core.utils.security.events

import com.roshni.games.core.utils.security.model.SecurityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Comprehensive security event handling system
 */
class SecurityEventHandler(
    private val scope: CoroutineScope,
    private val maxEventsStored: Int = 10000,
    private val enableEventPersistence: Boolean = true
) {

    private val mutex = Mutex()

    // Event storage
    private val _events = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val events: StateFlow<List<SecurityEvent>> = _events.asStateFlow()

    // Event broadcasting
    private val _eventFlow = MutableSharedFlow<SecurityEvent>(replay = 0)
    val eventFlow: Flow<SecurityEvent> = _eventFlow.asSharedFlow()

    // Event listeners
    private val eventListeners = mutableMapOf<String, suspend (SecurityEvent) -> Unit>()

    // Event statistics
    private val eventCounts = MutableStateFlow<Map<SecurityEvent.Type, Int>>(emptyMap())

    init {
        // Start event processing
        startEventProcessing()
    }

    /**
     * Record a security event
     */
    suspend fun recordEvent(event: SecurityEvent): Result<Unit> {
        return try {
            mutex.withLock {
                // Add timestamp if not present
                val eventWithTimestamp = when (event) {
                    is SecurityEvent.AuthenticationEvent -> event.copy(timestamp = LocalDateTime.now())
                    is SecurityEvent.AuthorizationEvent -> event.copy(timestamp = LocalDateTime.now())
                    is SecurityEvent.SessionEvent -> event.copy(timestamp = LocalDateTime.now())
                    is SecurityEvent.PermissionEvent -> event.copy(timestamp = LocalDateTime.now())
                    is SecurityEvent.SecurityAlert -> event.copy(timestamp = LocalDateTime.now())
                    is SecurityEvent.DataAccessEvent -> event.copy(timestamp = LocalDateTime.now())
                }

                // Add to storage
                val currentEvents = _events.value.toMutableList()
                currentEvents.add(0, eventWithTimestamp) // Add to beginning for chronological order

                // Trim if too many events
                if (currentEvents.size > maxEventsStored) {
                    currentEvents.subList(maxEventsStored, currentEvents.size).clear()
                }

                _events.value = currentEvents

                // Update statistics
                updateEventStatistics(eventWithTimestamp.type)

                // Broadcast event
                scope.launch {
                    _eventFlow.emit(eventWithTimestamp)
                }

                // Notify listeners
                notifyEventListeners(eventWithTimestamp)

                // Persist if enabled
                if (enableEventPersistence) {
                    persistEvent(eventWithTimestamp)
                }

                Timber.d("Security event recorded: ${eventWithTimestamp.type}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Event recording error")
            Result.failure(e)
        }
    }

    /**
     * Get events with filtering
     */
    suspend fun getEvents(
        userId: String? = null,
        eventType: SecurityEvent.Type? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null,
        limit: Int = 100
    ): List<SecurityEvent> {
        return try {
            mutex.withLock {
                _events.value.filter { event ->
                    (userId == null || event.userId == userId) &&
                    (eventType == null || event.type == eventType) &&
                    (fromDate == null || event.timestamp.isAfter(fromDate)) &&
                    (toDate == null || event.timestamp.isBefore(toDate))
                }.take(limit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Event retrieval error")
            emptyList()
        }
    }

    /**
     * Get event statistics
     */
    suspend fun getEventStatistics(
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null
    ): Map<String, Any> {
        return try {
            mutex.withLock {
                val events = _events.value.filter { event ->
                    (fromDate == null || event.timestamp.isAfter(fromDate)) &&
                    (toDate == null || event.timestamp.isBefore(toDate))
                }

                val typeCounts = events.groupBy { it.type }.mapValues { it.value.size }
                val userCounts = events.groupBy { it.userId }.mapValues { it.value.size }

                mapOf(
                    "totalEvents" to events.size,
                    "eventTypeBreakdown" to typeCounts,
                    "userEventCounts" to userCounts,
                    "timeRange" to mapOf(
                        "from" to (fromDate?.toString() ?: "beginning"),
                        "to" to (toDate?.toString() ?: "now")
                    ),
                    "mostActiveUsers" to userCounts.entries.sortedByDescending { it.value }.take(10)
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Event statistics error")
            emptyMap()
        }
    }

    /**
     * Register an event listener
     */
    fun registerEventListener(
        id: String,
        listener: suspend (SecurityEvent) -> Unit
    ) {
        eventListeners[id] = listener
        Timber.d("Event listener registered: $id")
    }

    /**
     * Unregister an event listener
     */
    fun unregisterEventListener(id: String) {
        eventListeners.remove(id)
        Timber.d("Event listener unregistered: $id")
    }

    /**
     * Clear all events (for testing or reset)
     */
    suspend fun clearEvents(): Result<Unit> {
        return try {
            mutex.withLock {
                _events.value = emptyList()
                eventCounts.value = emptyMap()
                Timber.d("All security events cleared")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Event clearing error")
            Result.failure(e)
        }
    }

    /**
     * Export events for analysis
     */
    suspend fun exportEvents(
        format: ExportFormat = ExportFormat.JSON,
        userId: String? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null
    ): String {
        return try {
            val events = getEvents(userId, null, fromDate, toDate)

            when (format) {
                ExportFormat.JSON -> {
                    // In a real implementation, use a JSON library
                    events.joinToString(",\n") { event ->
                        // Simplified JSON representation
                        "{\"type\":\"${event.type}\",\"userId\":\"${event.userId}\",\"timestamp\":\"${event.timestamp}\"}"
                    }
                }
                ExportFormat.CSV -> {
                    val header = "Type,UserId,Timestamp,Metadata\n"
                    val rows = events.joinToString("\n") { event ->
                        "${event.type},${event.userId},${event.timestamp},${event.metadata}"
                    }
                    header + rows
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Event export error")
            "Error: ${e.message}"
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun startEventProcessing() {
        scope.launch {
            // Process events in background if needed
            Timber.d("Security event processing started")
        }
    }

    private suspend fun updateEventStatistics(eventType: SecurityEvent.Type) {
        val currentCounts = eventCounts.value.toMutableMap()
        currentCounts[eventType] = (currentCounts[eventType] ?: 0) + 1
        eventCounts.value = currentCounts
    }

    private suspend fun notifyEventListeners(event: SecurityEvent) {
        eventListeners.values.forEach { listener ->
            try {
                scope.launch {
                    listener(event)
                }
            } catch (e: Exception) {
                Timber.e(e, "Event listener notification error")
            }
        }
    }

    private suspend fun persistEvent(event: SecurityEvent) {
        try {
            // In a real implementation, persist to database or file system
            // For now, just log the event
            Timber.d("Persisting security event: ${event.type} for user ${event.userId}")
        } catch (e: Exception) {
            Timber.e(e, "Event persistence error")
        }
    }

    /**
     * Export formats supported
     */
    enum class ExportFormat {
        JSON, CSV
    }
}

/**
 * Convenience functions for creating security events
 */
object SecurityEventFactory {

    fun authenticationEvent(
        userId: String,
        success: Boolean,
        method: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.AuthenticationEvent {
        return SecurityEvent.AuthenticationEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            success = success,
            method = method,
            timestamp = timestamp,
            metadata = metadata
        )
    }

    fun authorizationEvent(
        userId: String,
        permission: String,
        resource: String?,
        success: Boolean,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.AuthorizationEvent {
        return SecurityEvent.AuthorizationEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            permission = permission,
            resource = resource,
            success = success,
            timestamp = timestamp,
            metadata = metadata
        )
    }

    fun sessionEvent(
        userId: String,
        eventType: SecurityEvent.SessionEvent.Type,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.SessionEvent {
        return SecurityEvent.SessionEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            eventType = eventType,
            timestamp = timestamp,
            metadata = metadata
        )
    }

    fun permissionEvent(
        userId: String,
        eventType: SecurityEvent.PermissionEvent.Type,
        permissions: List<String>,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.PermissionEvent {
        return SecurityEvent.PermissionEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            eventType = eventType,
            permissions = permissions,
            timestamp = timestamp,
            metadata = metadata
        )
    }

    fun securityAlert(
        userId: String,
        severity: SecurityEvent.SecurityAlert.Severity,
        message: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.SecurityAlert {
        return SecurityEvent.SecurityAlert(
            id = UUID.randomUUID().toString(),
            userId = userId,
            severity = severity,
            message = message,
            timestamp = timestamp,
            metadata = metadata
        )
    }

    fun dataAccessEvent(
        userId: String,
        operation: String,
        resource: String,
        success: Boolean,
        timestamp: LocalDateTime = LocalDateTime.now(),
        metadata: Map<String, Any> = emptyMap()
    ): SecurityEvent.DataAccessEvent {
        return SecurityEvent.DataAccessEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            operation = operation,
            resource = resource,
            success = success,
            timestamp = timestamp,
            metadata = metadata
        )
    }
}