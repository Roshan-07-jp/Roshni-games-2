package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.feature.Feature
import com.roshni.games.core.utils.feature.FeatureContext
import com.roshni.games.core.utils.feature.FeatureManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Manages integration and coordination between features
 */
class FeatureIntegrationCoordinator(
    private val featureManager: FeatureManager,
    private val eventManager: CrossFeatureEventManager,
    private val dataFlowManager: DataFlowManager
) {

    private val mutex = Mutex()
    private val _integrationState = MutableStateFlow<Map<String, FeatureIntegration>>(emptyMap())
    val integrationState: StateFlow<Map<String, FeatureIntegration>> = _integrationState.asStateFlow()

    private val _coordinationMetrics = MutableStateFlow(CoordinationMetrics())
    val coordinationMetrics: StateFlow<CoordinationMetrics> = _coordinationMetrics.asStateFlow()

    /**
     * Create integration between features
     */
    suspend fun createFeatureIntegration(
        integrationId: String,
        sourceFeature: String,
        targetFeature: String,
        integrationType: IntegrationType,
        configuration: IntegrationConfiguration
    ): Result<FeatureIntegration> = mutex.withLock {
        try {
            // Validate features exist
            val source = featureManager.getFeature(sourceFeature)
                ?: return Result.failure(IllegalArgumentException("Source feature $sourceFeature not found"))

            val target = featureManager.getFeature(targetFeature)
                ?: return Result.failure(IllegalArgumentException("Target feature $targetFeature not found"))

            // Check if integration already exists
            if (_integrationState.value.containsKey(integrationId)) {
                return Result.failure(IllegalArgumentException("Integration $integrationId already exists"))
            }

            // Create the integration
            val integration = FeatureIntegration(
                id = integrationId,
                sourceFeature = sourceFeature,
                targetFeature = targetFeature,
                integrationType = integrationType,
                configuration = configuration,
                createdAt = System.currentTimeMillis()
            )

            // Setup the integration based on type
            setupIntegration(integration)

            // Store the integration
            _integrationState.value = _integrationState.value + (integrationId to integration)

            // Update metrics
            updateCoordinationMetrics()

            Timber.d("Created feature integration: $integrationId ($sourceFeature -> $targetFeature)")
            Result.success(integration)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create feature integration: $integrationId")
            Result.failure(e)
        }
    }

    /**
     * Remove feature integration
     */
    suspend fun removeFeatureIntegration(integrationId: String): Boolean = mutex.withLock {
        val integration = _integrationState.value[integrationId] ?: return false

        try {
            // Cleanup integration
            cleanupIntegration(integration)

            // Remove from state
            _integrationState.value = _integrationState.value - integrationId

            // Update metrics
            updateCoordinationMetrics()

            Timber.d("Removed feature integration: $integrationId")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to remove feature integration: $integrationId")
            false
        }
    }

    /**
     * Get integration by ID
     */
    fun getFeatureIntegration(integrationId: String): FeatureIntegration? {
        return _integrationState.value[integrationId]
    }

    /**
     * Get all integrations for a feature
     */
    fun getIntegrationsForFeature(featureId: String): List<FeatureIntegration> {
        return _integrationState.value.values.filter { integration ->
            integration.sourceFeature == featureId || integration.targetFeature == featureId
        }
    }

    /**
     * Coordinate feature interaction
     */
    suspend fun coordinateFeatureInteraction(
        sourceFeature: String,
        targetFeature: String,
        interactionType: String,
        payload: Map<String, Any>,
        context: IntegrationContext
    ): Result<InteractionResult> = mutex.withLock {
        try {
            val integrations = getIntegrationsForFeature(sourceFeature).filter {
                it.targetFeature == targetFeature && it.isActive
            }

            if (integrations.isEmpty()) {
                return Result.failure(IllegalStateException("No active integration found between $sourceFeature and $targetFeature"))
            }

            val results = mutableListOf<InteractionResult>()
            var overallSuccess = true

            for (integration in integrations) {
                val result = when (integration.integrationType) {
                    IntegrationType.DIRECT -> handleDirectInteraction(integration, interactionType, payload, context)
                    IntegrationType.EVENT_DRIVEN -> handleEventDrivenInteraction(integration, interactionType, payload, context)
                    IntegrationType.DATA_FLOW -> handleDataFlowInteraction(integration, interactionType, payload, context)
                    IntegrationType.STATE_SYNC -> handleStateSyncInteraction(integration, interactionType, payload, context)
                    else -> InteractionResult.failure("Unsupported integration type: ${integration.integrationType}")
                }

                results.add(result)
                if (!result.success) {
                    overallSuccess = false
                }
            }

            val combinedResult = if (overallSuccess) {
                InteractionResult.success(results.flatMap { it.data }, "All integrations succeeded")
            } else {
                InteractionResult.partialSuccess(results.filter { it.success }.flatMap { it.data }, "Some integrations failed")
            }

            // Update metrics
            updateCoordinationMetrics()

            Result.success(combinedResult)

        } catch (e: Exception) {
            Timber.e(e, "Failed to coordinate feature interaction between $sourceFeature and $targetFeature")
            Result.failure(e)
        }
    }

    /**
     * Synchronize state between features
     */
    suspend fun synchronizeFeatureState(
        sourceFeature: String,
        targetFeature: String,
        stateKey: String,
        stateValue: Any
    ): Result<Unit> = mutex.withLock {
        try {
            val source = featureManager.getFeature(sourceFeature)
                ?: return Result.failure(IllegalArgumentException("Source feature $sourceFeature not found"))

            val target = featureManager.getFeature(targetFeature)
                ?: return Result.failure(IllegalArgumentException("Target feature $targetFeature not found"))

            // Create state sync event
            val event = CrossFeatureEvent.create(
                eventType = "STATE_SYNC",
                sourceFeature = sourceFeature,
                payload = mapOf(
                    "stateKey" to stateKey,
                    "stateValue" to stateValue,
                    "targetFeature" to targetFeature
                ),
                priority = EventPriority.NORMAL
            )

            eventManager.publishEvent(event)

            Timber.d("Synchronized state between $sourceFeature and $targetFeature: $stateKey")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronize state between $sourceFeature and $targetFeature")
            Result.failure(e)
        }
    }

    /**
     * Setup integration based on type
     */
    private suspend fun setupIntegration(integration: FeatureIntegration) {
        when (integration.integrationType) {
            IntegrationType.EVENT_DRIVEN -> setupEventDrivenIntegration(integration)
            IntegrationType.DATA_FLOW -> setupDataFlowIntegration(integration)
            IntegrationType.STATE_SYNC -> setupStateSyncIntegration(integration)
            else -> {
                // Other types don't need special setup
            }
        }
    }

    /**
     * Cleanup integration
     */
    private suspend fun cleanupIntegration(integration: FeatureIntegration) {
        when (integration.integrationType) {
            IntegrationType.EVENT_DRIVEN -> cleanupEventDrivenIntegration(integration)
            IntegrationType.DATA_FLOW -> cleanupDataFlowIntegration(integration)
            IntegrationType.STATE_SYNC -> cleanupStateSyncIntegration(integration)
            else -> {
                // Other types don't need special cleanup
            }
        }
    }

    /**
     * Setup event-driven integration
     */
    private suspend fun setupEventDrivenIntegration(integration: FeatureIntegration) {
        // Register event handlers for the integration
        eventManager.registerHandler(
            handlerId = "integration_${integration.id}_handler",
            targetFeature = integration.targetFeature,
            eventTypes = integration.configuration.eventTypes,
            priority = integration.configuration.priority
        ) { event, context ->
            handleEventDrivenInteraction(integration, event.eventType, event.payload, context)
        }
    }

    /**
     * Setup data flow integration
     */
    private suspend fun setupDataFlowIntegration(integration: FeatureIntegration) {
        // Create data flow for the integration
        dataFlowManager.createDataFlow(
            flowId = "integration_${integration.id}_flow",
            sourceComponent = integration.sourceFeature,
            targetComponent = integration.targetFeature,
            direction = DataFlowDirection.UNIDIRECTIONAL,
            priority = integration.configuration.priority
        )
    }

    /**
     * Setup state synchronization integration
     */
    private suspend fun setupStateSyncIntegration(integration: FeatureIntegration) {
        // Setup state monitoring for synchronization
        // This would involve observing feature state changes
    }

    /**
     * Cleanup event-driven integration
     */
    private suspend fun cleanupEventDrivenIntegration(integration: FeatureIntegration) {
        eventManager.unregisterHandler(
            handlerId = "integration_${integration.id}_handler",
            eventTypes = integration.configuration.eventTypes
        )
    }

    /**
     * Cleanup data flow integration
     */
    private suspend fun cleanupDataFlowIntegration(integration: FeatureIntegration) {
        dataFlowManager.removeDataFlow("integration_${integration.id}_flow")
    }

    /**
     * Cleanup state synchronization integration
     */
    private suspend fun cleanupStateSyncIntegration(integration: FeatureIntegration) {
        // Cleanup state monitoring
    }

    /**
     * Handle direct interaction
     */
    private suspend fun handleDirectInteraction(
        integration: FeatureIntegration,
        interactionType: String,
        payload: Map<String, Any>,
        context: IntegrationContext
    ): InteractionResult {
        return try {
            val sourceFeature = featureManager.getFeature(integration.sourceFeature) ?: return InteractionResult.failure("Source feature not found")
            val targetFeature = featureManager.getFeature(integration.targetFeature) ?: return InteractionResult.failure("Target feature not found")

            // Create feature context for interaction
            val featureContext = FeatureContext(
                featureId = integration.targetFeature,
                executionId = context.eventId,
                globalVariables = payload
            )

            // Execute target feature with payload from source
            val result = featureManager.executeFeature(integration.targetFeature, featureContext)

            if (result.success) {
                InteractionResult.success(listOf(result), "Direct interaction successful")
            } else {
                InteractionResult.failure("Direct interaction failed: ${result.errors}")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in direct interaction for integration ${integration.id}")
            InteractionResult.failure("Direct interaction error: ${e.message}")
        }
    }

    /**
     * Handle event-driven interaction
     */
    private suspend fun handleEventDrivenInteraction(
        integration: FeatureIntegration,
        interactionType: String,
        payload: Map<String, Any>,
        context: IntegrationContext
    ): EventHandlerResult {
        return try {
            // Process through the target feature's event handler
            val handled = featureManager.sendEventToFeature(
                featureId = integration.targetFeature,
                event = com.roshni.games.core.utils.feature.FeatureEvent(
                    type = interactionType,
                    payload = payload,
                    timestamp = context.timestamp
                ),
                context = FeatureContext(
                    featureId = integration.targetFeature,
                    executionId = context.eventId
                )
            )

            if (handled) {
                EventHandlerResult.success("Event-driven interaction successful")
            } else {
                EventHandlerResult.failure("Event-driven interaction not handled")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in event-driven interaction for integration ${integration.id}")
            EventHandlerResult.failure("Event-driven interaction error: ${e.message}")
        }
    }

    /**
     * Handle data flow interaction
     */
    private suspend fun handleDataFlowInteraction(
        integration: FeatureIntegration,
        interactionType: String,
        payload: Map<String, Any>,
        context: IntegrationContext
    ): InteractionResult {
        return try {
            val flowId = "integration_${integration.id}_flow"
            val result = dataFlowManager.processData(flowId, payload, context)

            if (result.success) {
                InteractionResult.success(listOf(result.data), "Data flow interaction successful")
            } else {
                InteractionResult.failure("Data flow interaction failed: ${result.error}")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in data flow interaction for integration ${integration.id}")
            InteractionResult.failure("Data flow interaction error: ${e.message}")
        }
    }

    /**
     * Handle state synchronization interaction
     */
    private suspend fun handleStateSyncInteraction(
        integration: FeatureIntegration,
        interactionType: String,
        payload: Map<String, Any>,
        context: IntegrationContext
    ): InteractionResult {
        return try {
            val stateKey = payload["stateKey"] as? String ?: return InteractionResult.failure("State key not provided")
            val stateValue = payload["stateValue"] ?: return InteractionResult.failure("State value not provided")

            // Synchronize state with target feature
            synchronizeFeatureState(
                sourceFeature = integration.sourceFeature,
                targetFeature = integration.targetFeature,
                stateKey = stateKey,
                stateValue = stateValue
            )

            InteractionResult.success(listOf(stateValue), "State synchronization successful")

        } catch (e: Exception) {
            Timber.e(e, "Error in state sync interaction for integration ${integration.id}")
            InteractionResult.failure("State sync interaction error: ${e.message}")
        }
    }

    /**
     * Update coordination metrics
     */
    private fun updateCoordinationMetrics() {
        val integrations = _integrationState.value
        val activeIntegrations = integrations.count { it.value.isActive }

        _coordinationMetrics.value = _coordinationMetrics.value.copy(
            totalIntegrations = integrations.size,
            activeIntegrations = activeIntegrations,
            eventDrivenIntegrations = integrations.count { it.value.integrationType == IntegrationType.EVENT_DRIVEN },
            dataFlowIntegrations = integrations.count { it.value.integrationType == IntegrationType.DATA_FLOW },
            stateSyncIntegrations = integrations.count { it.value.integrationType == IntegrationType.STATE_SYNC }
        )
    }
}

/**
 * Represents a feature integration configuration
 */
data class FeatureIntegration(
    val id: String,
    val sourceFeature: String,
    val targetFeature: String,
    val integrationType: IntegrationType,
    val configuration: IntegrationConfiguration,
    val createdAt: Long,
    val isActive: Boolean = true,
    val lastActivity: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Configuration for feature integration
 */
data class IntegrationConfiguration(
    val eventTypes: List<String> = emptyList(),
    val dataFlowDirection: DataFlowDirection = DataFlowDirection.UNIDIRECTIONAL,
    val priority: EventPriority = EventPriority.NORMAL,
    val retryCount: Int = 3,
    val timeoutMs: Long = 30000,
    val enableStateSync: Boolean = false,
    val customProperties: Map<String, Any> = emptyMap()
)

/**
 * Result of feature interaction
 */
data class InteractionResult(
    val success: Boolean,
    val data: List<Any?>,
    val message: String,
    val partialSuccess: Boolean = false,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun success(data: List<Any?>, message: String): InteractionResult {
            return InteractionResult(
                success = true,
                data = data,
                message = message
            )
        }

        fun partialSuccess(data: List<Any?>, message: String): InteractionResult {
            return InteractionResult(
                success = true,
                data = data,
                message = message,
                partialSuccess = true
            )
        }

        fun failure(message: String, errors: List<String> = emptyList()): InteractionResult {
            return InteractionResult(
                success = false,
                data = emptyList(),
                message = message,
                errors = errors
            )
        }
    }
}

/**
 * Metrics for coordination
 */
data class CoordinationMetrics(
    val totalIntegrations: Int = 0,
    val activeIntegrations: Int = 0,
    val eventDrivenIntegrations: Int = 0,
    val dataFlowIntegrations: Int = 0,
    val stateSyncIntegrations: Int = 0,
    val totalInteractions: Long = 0,
    val successfulInteractions: Long = 0,
    val failedInteractions: Long = 0
)