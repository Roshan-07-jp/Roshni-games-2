package com.roshni.games.core.utils.integration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages cross-feature events for communication between features
 */
class CrossFeatureEventManager {

    private val _events = MutableSharedFlow<CrossFeatureEvent>(replay = 0, extraBufferCapacity = 100)
    val events: SharedFlow<CrossFeatureEvent> = _events.asSharedFlow()

    private val eventHandlers = ConcurrentHashMap<String, MutableList<EventHandler>>()
    private val eventFilters = ConcurrentHashMap<String, EventFilter>()

    /**
     * Publish an event to all registered handlers
     */
    suspend fun publishEvent(event: CrossFeatureEvent) {
        try {
            Timber.d("Publishing cross-feature event: ${event.eventType} from ${event.sourceFeature}")

            // Add timestamp if not present
            val enrichedEvent = if (event.timestamp == 0L) {
                event.copy(timestamp = System.currentTimeMillis())
            } else {
                event
            }

            _events.emit(enrichedEvent)

            // Process through registered handlers
            processEventWithHandlers(enrichedEvent)

        } catch (e: Exception) {
            Timber.e(e, "Failed to publish cross-feature event: ${event.eventType}")
        }
    }

    /**
     * Register an event handler for specific event types
     */
    fun registerHandler(
        handlerId: String,
        targetFeature: String,
        eventTypes: List<String>,
        priority: EventPriority = EventPriority.NORMAL,
        handler: suspend (CrossFeatureEvent, IntegrationContext) -> EventHandlerResult
    ) {
        val eventHandler = EventHandler(
            id = handlerId,
            targetFeature = targetFeature,
            eventTypes = eventTypes,
            priority = priority,
            handler = handler
        )

        eventTypes.forEach { eventType ->
            val handlers = eventHandlers.getOrPut(eventType) { mutableListOf() }
            handlers.add(eventHandler)
            // Sort by priority (higher priority first)
            handlers.sortByDescending { it.priority.level }
        }

        Timber.d("Registered event handler: $handlerId for events: $eventTypes")
    }

    /**
     * Unregister an event handler
     */
    fun unregisterHandler(handlerId: String, eventTypes: List<String>? = null) {
        val typesToRemove = eventTypes ?: eventHandlers.keys.toList()

        typesToRemove.forEach { eventType ->
            eventHandlers[eventType]?.removeAll { it.id == handlerId }
        }

        Timber.d("Unregistered event handler: $handlerId")
    }

    /**
     * Register an event filter
     */
    fun registerFilter(filterId: String, filter: EventFilter) {
        eventFilters[filterId] = filter
        Timber.d("Registered event filter: $filterId")
    }

    /**
     * Remove an event filter
     */
    fun removeFilter(filterId: String) {
        eventFilters.remove(filterId)
        Timber.d("Removed event filter: $filterId")
    }

    /**
     * Get events for a specific feature
     */
    fun getEventsForFeature(featureId: String): Flow<CrossFeatureEvent> {
        return events.filter { event ->
            event.targetFeatures.isEmpty() || event.targetFeatures.contains(featureId)
        }
    }

    /**
     * Get events by type
     */
    fun getEventsByType(eventType: String): Flow<CrossFeatureEvent> {
        return events.filter { it.eventType == eventType }
    }

    /**
     * Get events from a specific source feature
     */
    fun getEventsFromSource(sourceFeature: String): Flow<CrossFeatureEvent> {
        return events.filter { it.sourceFeature == sourceFeature }
    }

    /**
     * Process event with registered handlers
     */
    private suspend fun processEventWithHandlers(event: CrossFeatureEvent) {
        val handlers = eventHandlers[event.eventType] ?: return

        handlers.forEach { handler ->
            try {
                // Check if handler should process this event
                if (shouldHandlerProcessEvent(handler, event)) {
                    val context = IntegrationContext(
                        eventId = event.id,
                        sourceFeature = event.sourceFeature,
                        timestamp = event.timestamp,
                        priority = event.priority
                    )

                    val result = handler.handler(event, context)

                    Timber.d("Event handler ${handler.id} processed event ${event.eventType}: ${result.success}")

                    if (!result.success) {
                        Timber.w("Event handler ${handler.id} failed: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in event handler ${handler.id} for event ${event.eventType}")
            }
        }
    }

    /**
     * Check if handler should process the event
     */
    private fun shouldHandlerProcessEvent(handler: EventHandler, event: CrossFeatureEvent): Boolean {
        // Check target features
        if (event.targetFeatures.isNotEmpty() && handler.targetFeature !in event.targetFeatures) {
            return false
        }

        // Apply filters
        eventFilters.values.forEach { filter ->
            if (!filter.shouldProcess(event, handler)) {
                return false
            }
        }

        return true
    }
}

/**
 * Represents a cross-feature event
 */
data class CrossFeatureEvent(
    val id: String,
    val eventType: String,
    val sourceFeature: String,
    val targetFeatures: List<String> = emptyList(), // Empty means broadcast to all
    val payload: Map<String, Any> = emptyMap(),
    val priority: EventPriority = EventPriority.NORMAL,
    val timestamp: Long = 0,
    val correlationId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun create(
            eventType: String,
            sourceFeature: String,
            payload: Map<String, Any> = emptyMap(),
            targetFeatures: List<String> = emptyList(),
            priority: EventPriority = EventPriority.NORMAL,
            correlationId: String? = null
        ): CrossFeatureEvent {
            return CrossFeatureEvent(
                id = "event_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}",
                eventType = eventType,
                sourceFeature = sourceFeature,
                targetFeatures = targetFeatures,
                payload = payload,
                priority = priority,
                timestamp = System.currentTimeMillis(),
                correlationId = correlationId
            )
        }
    }
}

/**
 * Event handler configuration
 */
data class EventHandler(
    val id: String,
    val targetFeature: String,
    val eventTypes: List<String>,
    val priority: EventPriority,
    val handler: suspend (CrossFeatureEvent, IntegrationContext) -> EventHandlerResult
)

/**
 * Result of event handler execution
 */
data class EventHandlerResult(
    val success: Boolean,
    val error: String? = null,
    val processedData: Any? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(processedData: Any? = null, metadata: Map<String, Any> = emptyMap()): EventHandlerResult {
            return EventHandlerResult(
                success = true,
                processedData = processedData,
                metadata = metadata
            )
        }

        fun failure(error: String, metadata: Map<String, Any> = emptyMap()): EventHandlerResult {
            return EventHandlerResult(
                success = false,
                error = error,
                metadata = metadata
            )
        }
    }
}

/**
 * Interface for filtering events
 */
interface EventFilter {
    suspend fun shouldProcess(event: CrossFeatureEvent, handler: EventHandler): Boolean
    fun getFilterInfo(): FilterInfo
}

/**
 * Information about an event filter
 */
data class FilterInfo(
    val name: String,
    val description: String,
    val version: String
)

/**
 * Context for event processing
 */
data class IntegrationContext(
    val eventId: String,
    val sourceFeature: String,
    val timestamp: Long,
    val priority: EventPriority,
    val metadata: Map<String, Any> = emptyMap()
)