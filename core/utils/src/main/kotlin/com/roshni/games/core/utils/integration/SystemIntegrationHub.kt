package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.workflow.WorkflowEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Core interface for the System Integration Hub
 */
interface SystemIntegrationHub {

    /**
     * Current status of the integration hub
     */
    val status: StateFlow<IntegrationHubStatus>

    /**
     * Overall system health
     */
    val systemHealth: StateFlow<SystemHealth>

    /**
     * Initialize the integration hub
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Register a component for integration
     */
    suspend fun registerComponent(component: IntegratedComponent): Result<Unit>

    /**
     * Unregister a component
     */
    suspend fun unregisterComponent(componentId: String): Result<Unit>

    /**
     * Create integration between components
     */
    suspend fun createIntegration(
        integrationId: String,
        sourceComponent: String,
        targetComponent: String,
        integrationType: IntegrationType,
        configuration: IntegrationConfiguration
    ): Result<Unit>

    /**
     * Remove integration
     */
    suspend fun removeIntegration(integrationId: String): Result<Unit>

    /**
     * Send event to component(s)
     */
    suspend fun sendEvent(
        event: CrossFeatureEvent,
        context: IntegrationContext? = null
    ): Result<EventProcessingResult>

    /**
     * Process data through integration flows
     */
    suspend fun processData(
        sourceComponent: String,
        data: Any,
        context: IntegrationContext
    ): Result<DataFlowResult>

    /**
     * Synchronize state between components
     */
    suspend fun synchronizeState(
        sourceComponent: String,
        targetComponent: String,
        stateKey: String,
        stateValue: Any
    ): Result<Unit>

    /**
     * Execute coordinated workflow across components
     */
    suspend fun executeWorkflow(
        workflowId: String,
        context: IntegrationContext,
        inputData: Map<String, Any> = emptyMap()
    ): Result<WorkflowExecutionResult>

    /**
     * Get integration metrics
     */
    fun getIntegrationMetrics(): IntegrationMetrics

    /**
     * Observe system events
     */
    fun observeSystemEvents(): Flow<SystemEvent>

    /**
     * Shutdown the integration hub
     */
    suspend fun shutdown(): Result<Unit>
}

/**
 * Default implementation of SystemIntegrationHub
 */
class SystemIntegrationHubImpl(
    private val featureManager: FeatureManager,
    private val ruleEngine: RuleEngine,
    private val workflowEngine: WorkflowEngine
) : SystemIntegrationHub {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(IntegrationHubStatus.INITIALIZING)
    override val status: StateFlow<IntegrationHubStatus> = _status.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealth())
    override val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()

    private val registeredComponents = MutableStateFlow<Map<String, IntegratedComponent>>(emptyMap())
    private val systemEvents = MutableStateFlow<List<SystemEvent>>(emptyList())

    // Component managers
    private lateinit var eventManager: CrossFeatureEventManager
    private lateinit var dataFlowManager: DataFlowManager
    private lateinit var featureCoordinator: FeatureIntegrationCoordinator

    override suspend fun initialize(): Result<Unit> = mutex.withLock {
        try {
            Timber.d("Initializing SystemIntegrationHub")

            _status.value = IntegrationHubStatus.INITIALIZING

            // Initialize component managers
            eventManager = CrossFeatureEventManager()
            dataFlowManager = DataFlowManager()
            featureCoordinator = FeatureIntegrationCoordinator(featureManager, eventManager, dataFlowManager)

            // Register core components
            registerCoreComponents()

            // Setup system health monitoring
            setupHealthMonitoring()

            // Setup event coordination
            setupEventCoordination()

            _status.value = IntegrationHubStatus.READY

            // Emit system ready event
            emitSystemEvent(SystemEvent.SystemReady("SystemIntegrationHub initialized successfully"))

            Timber.d("SystemIntegrationHub initialized successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SystemIntegrationHub")
            _status.value = IntegrationHubStatus.ERROR
            emitSystemEvent(SystemEvent.SystemError("Initialization failed: ${e.message}"))
            Result.failure(e)
        }
    }

    override suspend fun registerComponent(component: IntegratedComponent): Result<Unit> = mutex.withLock {
        try {
            if (registeredComponents.value.containsKey(component.id)) {
                return Result.failure(IllegalArgumentException("Component ${component.id} already registered"))
            }

            registeredComponents.value = registeredComponents.value + (component.id to component)

            // Setup component integrations
            setupComponentIntegrations(component)

            emitSystemEvent(SystemEvent.ComponentRegistered(component.id, component.type))

            Timber.d("Registered component: ${component.id}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to register component: ${component.id}")
            Result.failure(e)
        }
    }

    override suspend fun unregisterComponent(componentId: String): Result<Unit> = mutex.withLock {
        try {
            val component = registeredComponents.value[componentId] ?: return Result.failure(IllegalArgumentException("Component $componentId not found"))

            // Cleanup component integrations
            cleanupComponentIntegrations(component)

            registeredComponents.value = registeredComponents.value - componentId

            emitSystemEvent(SystemEvent.ComponentUnregistered(componentId))

            Timber.d("Unregistered component: $componentId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister component: $componentId")
            Result.failure(e)
        }
    }

    override suspend fun createIntegration(
        integrationId: String,
        sourceComponent: String,
        targetComponent: String,
        integrationType: IntegrationType,
        configuration: IntegrationConfiguration
    ): Result<Unit> = mutex.withLock {
        try {
            // Validate components exist
            val source = registeredComponents.value[sourceComponent]
                ?: return Result.failure(IllegalArgumentException("Source component $sourceComponent not found"))

            val target = registeredComponents.value[targetComponent]
                ?: return Result.failure(IllegalArgumentException("Target component $targetComponent not found"))

            // Create integration based on type
            when (integrationType) {
                IntegrationType.DIRECT -> createDirectIntegration(integrationId, source, target, configuration)
                IntegrationType.EVENT_DRIVEN -> createEventDrivenIntegration(integrationId, source, target, configuration)
                IntegrationType.DATA_FLOW -> createDataFlowIntegration(integrationId, source, target, configuration)
                IntegrationType.STATE_SYNC -> createStateSyncIntegration(integrationId, source, target, configuration)
                IntegrationType.WORKFLOW -> createWorkflowIntegration(integrationId, source, target, configuration)
                IntegrationType.RULE_BASED -> createRuleBasedIntegration(integrationId, source, target, configuration)
                else -> return Result.failure(IllegalArgumentException("Unsupported integration type: $integrationType"))
            }

            emitSystemEvent(SystemEvent.IntegrationCreated(integrationId, integrationType, sourceComponent, targetComponent))

            Timber.d("Created integration: $integrationId ($sourceComponent -> $targetComponent)")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create integration: $integrationId")
            Result.failure(e)
        }
    }

    override suspend fun removeIntegration(integrationId: String): Result<Unit> = mutex.withLock {
        try {
            // Remove from appropriate manager based on integration type
            // This would be implemented based on how integrations are stored

            emitSystemEvent(SystemEvent.IntegrationRemoved(integrationId))

            Timber.d("Removed integration: $integrationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to remove integration: $integrationId")
            Result.failure(e)
        }
    }

    override suspend fun sendEvent(
        event: CrossFeatureEvent,
        context: IntegrationContext?
    ): Result<EventProcessingResult> = mutex.withLock {
        try {
            val enrichedContext = context ?: IntegrationContext(
                eventId = event.id,
                sourceFeature = event.sourceFeature,
                timestamp = event.timestamp,
                priority = event.priority
            )

            eventManager.publishEvent(event)

            val result = EventProcessingResult(
                success = true,
                eventId = event.id,
                processedBy = listOf("SystemIntegrationHub"),
                processingTimeMs = System.currentTimeMillis() - event.timestamp
            )

            emitSystemEvent(SystemEvent.EventProcessed(event.eventType, event.sourceFeature))

            Result.success(result)

        } catch (e: Exception) {
            Timber.e(e, "Failed to send event: ${event.eventType}")
            Result.failure(e)
        }
    }

    override suspend fun processData(
        sourceComponent: String,
        data: Any,
        context: IntegrationContext
    ): Result<DataFlowResult> = mutex.withLock {
        try {
            // Find relevant data flows for the source component
            val relevantFlows = dataFlowManager.getDataFlowsForComponent(sourceComponent)

            if (relevantFlows.isEmpty()) {
                return Result.failure(IllegalStateException("No data flows found for component: $sourceComponent"))
            }

            val results = mutableListOf<DataFlowResult>()
            var overallSuccess = true

            for (flow in relevantFlows) {
                val result = dataFlowManager.processData(flow.id, data, context)
                results.add(result)

                if (!result.success) {
                    overallSuccess = false
                }
            }

            val combinedResult = if (overallSuccess) {
                DataFlowResult.success(
                    data = results.lastOrNull()?.data ?: data,
                    flowId = "combined_${sourceComponent}",
                    processingTimeMs = results.sumOf { it.processingTimeMs }
                )
            } else {
                DataFlowResult.failure("Some data flows failed")
            }

            emitSystemEvent(SystemEvent.DataProcessed(sourceComponent, results.size))

            Result.success(combinedResult)

        } catch (e: Exception) {
            Timber.e(e, "Failed to process data from component: $sourceComponent")
            Result.failure(e)
        }
    }

    override suspend fun synchronizeState(
        sourceComponent: String,
        targetComponent: String,
        stateKey: String,
        stateValue: Any
    ): Result<Unit> = mutex.withLock {
        try {
            featureCoordinator.synchronizeFeatureState(
                sourceFeature = sourceComponent,
                targetFeature = targetComponent,
                stateKey = stateKey,
                stateValue = stateValue
            )

            emitSystemEvent(SystemEvent.StateSynchronized(sourceComponent, targetComponent, stateKey))

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronize state between $sourceComponent and $targetComponent")
            Result.failure(e)
        }
    }

    override suspend fun executeWorkflow(
        workflowId: String,
        context: IntegrationContext,
        inputData: Map<String, Any>
    ): Result<WorkflowExecutionResult> = mutex.withLock {
        try {
            // Execute workflow through workflow engine
            val workflowContext = com.roshni.games.core.utils.workflow.WorkflowContext(
                workflowId = workflowId,
                inputData = inputData,
                metadata = context.metadata
            )

            val result = workflowEngine.executeWorkflow(workflowId, workflowContext)

            val workflowResult = WorkflowExecutionResult(
                success = result.success,
                workflowId = workflowId,
                outputData = result.outputData,
                executionTimeMs = result.executionTimeMs,
                stepsExecuted = result.stepsExecuted
            )

            emitSystemEvent(SystemEvent.WorkflowExecuted(workflowId, result.success))

            Result.success(workflowResult)

        } catch (e: Exception) {
            Timber.e(e, "Failed to execute workflow: $workflowId")
            Result.failure(e)
        }
    }

    override fun getIntegrationMetrics(): IntegrationMetrics {
        val components = registeredComponents.value
        val coordinationMetrics = featureCoordinator.coordinationMetrics.value

        return IntegrationMetrics(
            totalComponents = components.size,
            activeComponents = components.count { it.value.isActive },
            totalIntegrations = coordinationMetrics.totalIntegrations,
            activeIntegrations = coordinationMetrics.activeIntegrations,
            eventDrivenIntegrations = coordinationMetrics.eventDrivenIntegrations,
            dataFlowIntegrations = coordinationMetrics.dataFlowIntegrations,
            stateSyncIntegrations = coordinationMetrics.stateSyncIntegrations,
            totalEventsProcessed = 0, // Would be tracked
            totalDataFlowsProcessed = dataFlowManager.flowMetrics.value.totalProcessed,
            totalWorkflowsExecuted = 0, // Would be tracked
            averageProcessingTimeMs = dataFlowManager.flowMetrics.value.averageProcessingTimeMs
        )
    }

    override fun observeSystemEvents(): Flow<SystemEvent> = flow {
        systemEvents.collect { events ->
            events.forEach { emit(it) }
        }
    }

    override suspend fun shutdown(): Result<Unit> = mutex.withLock {
        try {
            Timber.d("Shutting down SystemIntegrationHub")

            _status.value = IntegrationHubStatus.SHUTTING_DOWN

            // Cleanup all components
            registeredComponents.value.values.forEach { component ->
                cleanupComponentIntegrations(component)
            }

            registeredComponents.value = emptyMap()

            _status.value = IntegrationHubStatus.SHUTDOWN

            emitSystemEvent(SystemEvent.SystemShutdown("SystemIntegrationHub shutdown complete"))

            Timber.d("SystemIntegrationHub shutdown complete")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Error during SystemIntegrationHub shutdown")
            _status.value = IntegrationHubStatus.ERROR
            Result.failure(e)
        }
    }

    /**
     * Register core system components
     */
    private suspend fun registerCoreComponents() {
        // Register FeatureManager as a component
        registerComponent(IntegratedComponent(
            id = "feature_manager",
            name = "Feature Manager",
            type = ComponentType.FEATURE_MANAGER,
            version = "1.0.0",
            isActive = true
        ))

        // Register RuleEngine as a component
        registerComponent(IntegratedComponent(
            id = "rule_engine",
            name = "Rule Engine",
            type = ComponentType.RULE_ENGINE,
            version = "1.0.0",
            isActive = true
        ))

        // Register WorkflowEngine as a component
        registerComponent(IntegratedComponent(
            id = "workflow_engine",
            name = "Workflow Engine",
            type = ComponentType.WORKFLOW_ENGINE,
            version = "1.0.0",
            isActive = true
        ))
    }

    /**
     * Setup component integrations
     */
    private suspend fun setupComponentIntegrations(component: IntegratedComponent) {
        when (component.type) {
            ComponentType.FEATURE_MANAGER -> setupFeatureManagerIntegration(component)
            ComponentType.RULE_ENGINE -> setupRuleEngineIntegration(component)
            ComponentType.WORKFLOW_ENGINE -> setupWorkflowEngineIntegration(component)
            else -> {
                // Generic setup for other component types
            }
        }
    }

    /**
     * Cleanup component integrations
     */
    private suspend fun cleanupComponentIntegrations(component: IntegratedComponent) {
        when (component.type) {
            ComponentType.FEATURE_MANAGER -> cleanupFeatureManagerIntegration(component)
            ComponentType.RULE_ENGINE -> cleanupRuleEngineIntegration(component)
            ComponentType.WORKFLOW_ENGINE -> cleanupWorkflowEngineIntegration(component)
            else -> {
                // Generic cleanup for other component types
            }
        }
    }

    /**
     * Setup FeatureManager integration
     */
    private suspend fun setupFeatureManagerIntegration(component: IntegratedComponent) {
        // Setup event handlers for feature lifecycle events
        eventManager.registerHandler(
            handlerId = "${component.id}_lifecycle_handler",
            targetFeature = component.id,
            eventTypes = listOf("FEATURE_ENABLED", "FEATURE_DISABLED", "FEATURE_EXECUTED"),
            priority = EventPriority.HIGH
        ) { event, context ->
            EventHandlerResult.success("Feature lifecycle event processed")
        }
    }

    /**
     * Setup RuleEngine integration
     */
    private suspend fun setupRuleEngineIntegration(component: IntegratedComponent) {
        // Setup rule evaluation events
        eventManager.registerHandler(
            handlerId = "${component.id}_rule_handler",
            targetFeature = component.id,
            eventTypes = listOf("RULE_EVALUATION_REQUESTED"),
            priority = EventPriority.NORMAL
        ) { event, context ->
            EventHandlerResult.success("Rule evaluation event processed")
        }
    }

    /**
     * Setup WorkflowEngine integration
     */
    private suspend fun setupWorkflowEngineIntegration(component: IntegratedComponent) {
        // Setup workflow execution events
        eventManager.registerHandler(
            handlerId = "${component.id}_workflow_handler",
            targetFeature = component.id,
            eventTypes = listOf("WORKFLOW_EXECUTION_REQUESTED"),
            priority = EventPriority.NORMAL
        ) { event, context ->
            EventHandlerResult.success("Workflow execution event processed")
        }
    }

    /**
     * Cleanup FeatureManager integration
     */
    private suspend fun cleanupFeatureManagerIntegration(component: IntegratedComponent) {
        eventManager.unregisterHandler("${component.id}_lifecycle_handler")
    }

    /**
     * Cleanup RuleEngine integration
     */
    private suspend fun cleanupRuleEngineIntegration(component: IntegratedComponent) {
        eventManager.unregisterHandler("${component.id}_rule_handler")
    }

    /**
     * Cleanup WorkflowEngine integration
     */
    private suspend fun cleanupWorkflowEngineIntegration(component: IntegratedComponent) {
        eventManager.unregisterHandler("${component.id}_workflow_handler")
    }

    /**
     * Create direct integration
     */
    private suspend fun createDirectIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        // Direct integration setup
    }

    /**
     * Create event-driven integration
     */
    private suspend fun createEventDrivenIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        // Event-driven integration setup
    }

    /**
     * Create data flow integration
     */
    private suspend fun createDataFlowIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        dataFlowManager.createDataFlow(
            flowId = integrationId,
            sourceComponent = source.id,
            targetComponent = target.id,
            direction = configuration.dataFlowDirection,
            priority = configuration.priority
        )
    }

    /**
     * Create state synchronization integration
     */
    private suspend fun createStateSyncIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        // State sync integration setup
    }

    /**
     * Create workflow-based integration
     */
    private suspend fun createWorkflowIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        // Workflow integration setup
    }

    /**
     * Create rule-based integration
     */
    private suspend fun createRuleBasedIntegration(
        integrationId: String,
        source: IntegratedComponent,
        target: IntegratedComponent,
        configuration: IntegrationConfiguration
    ) {
        // Rule-based integration setup
    }

    /**
     * Setup system health monitoring
     */
    private fun setupHealthMonitoring() {
        // Monitor component health periodically
        kotlinx.coroutines.GlobalScope.launch {
            while (_status.value == IntegrationHubStatus.READY) {
                try {
                    val components = registeredComponents.value
                    val healthyComponents = components.count { it.value.isActive }

                    _systemHealth.value = SystemHealth(
                        overallStatus = if (healthyComponents == components.size) HealthStatus.HEALTHY else HealthStatus.DEGRADED,
                        componentHealth = components.mapValues { it.value.isActive },
                        totalComponents = components.size,
                        healthyComponents = healthyComponents,
                        lastHealthCheck = System.currentTimeMillis()
                    )

                    kotlinx.coroutines.delay(30000) // Check every 30 seconds

                } catch (e: Exception) {
                    Timber.e(e, "Error in health monitoring")
                    kotlinx.coroutines.delay(60000) // Retry after 1 minute on error
                }
            }
        }
    }

    /**
     * Setup event coordination
     */
    private fun setupEventCoordination() {
        // Setup cross-component event coordination
        kotlinx.coroutines.GlobalScope.launch {
            eventManager.events.collect { event ->
                // Coordinate events across components
                processCrossComponentEvent(event)
            }
        }
    }

    /**
     * Process cross-component events
     */
    private suspend fun processCrossComponentEvent(event: CrossFeatureEvent) {
        // Process events that need coordination across multiple components
        when (event.eventType) {
            "SYSTEM_HEALTH_CHECK" -> handleSystemHealthCheck(event)
            "COMPONENT_STATE_CHANGE" -> handleComponentStateChange(event)
            else -> {
                // Other events are handled by specific component handlers
            }
        }
    }

    /**
     * Handle system health check events
     */
    private suspend fun handleSystemHealthCheck(event: CrossFeatureEvent) {
        // Trigger health check across all components
        emitSystemEvent(SystemEvent.HealthCheckRequested)
    }

    /**
     * Handle component state change events
     */
    private suspend fun handleComponentStateChange(event: CrossFeatureEvent) {
        // Handle component state changes that affect other components
        val componentId = event.payload["componentId"] as? String ?: return
        val newState = event.payload["newState"] as? String ?: return

        emitSystemEvent(SystemEvent.ComponentStateChanged(componentId, newState))
    }

    /**
     * Emit system event
     */
    private fun emitSystemEvent(event: SystemEvent) {
        val updatedEvents = systemEvents.value + event
        systemEvents.value = updatedEvents.takeLast(100) // Keep last 100 events
    }
}

/**
 * Status of the integration hub
 */
enum class IntegrationHubStatus {
    INITIALIZING,
    READY,
    ERROR,
    SHUTTING_DOWN,
    SHUTDOWN
}

/**
 * System health information
 */
data class SystemHealth(
    val overallStatus: HealthStatus = HealthStatus.UNKNOWN,
    val componentHealth: Map<String, Boolean> = emptyMap(),
    val totalComponents: Int = 0,
    val healthyComponents: Int = 0,
    val lastHealthCheck: Long = 0
)

/**
 * Health status enumeration
 */
enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN
}

/**
 * Integrated component information
 */
data class IntegratedComponent(
    val id: String,
    val name: String,
    val type: ComponentType,
    val version: String,
    val isActive: Boolean = true,
    val capabilities: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Component type enumeration
 */
enum class ComponentType {
    FEATURE_MANAGER,
    RULE_ENGINE,
    WORKFLOW_ENGINE,
    DATA_PROCESSOR,
    EVENT_HANDLER,
    STATE_MANAGER,
    CUSTOM
}

/**
 * Event processing result
 */
data class EventProcessingResult(
    val success: Boolean,
    val eventId: String,
    val processedBy: List<String>,
    val processingTimeMs: Long,
    val error: String? = null
)

/**
 * Workflow execution result
 */
data class WorkflowExecutionResult(
    val success: Boolean,
    val workflowId: String,
    val outputData: Map<String, Any>,
    val executionTimeMs: Long,
    val stepsExecuted: Int,
    val errors: List<String> = emptyList()
)

/**
 * Integration metrics
 */
data class IntegrationMetrics(
    val totalComponents: Int,
    val activeComponents: Int,
    val totalIntegrations: Int,
    val activeIntegrations: Int,
    val eventDrivenIntegrations: Int,
    val dataFlowIntegrations: Int,
    val stateSyncIntegrations: Int,
    val totalEventsProcessed: Long,
    val totalDataFlowsProcessed: Long,
    val totalWorkflowsExecuted: Long,
    val averageProcessingTimeMs: Double
)

/**
 * System events for monitoring and debugging
 */
sealed class SystemEvent {
    data class SystemReady(val message: String) : SystemEvent()
    data class SystemError(val error: String) : SystemEvent()
    data class SystemShutdown(val message: String) : SystemEvent()
    data class ComponentRegistered(val componentId: String, val componentType: ComponentType) : SystemEvent()
    data class ComponentUnregistered(val componentId: String) : SystemEvent()
    data class ComponentStateChanged(val componentId: String, val newState: String) : SystemEvent()
    data class IntegrationCreated(val integrationId: String, val type: IntegrationType, val source: String, val target: String) : SystemEvent()
    data class IntegrationRemoved(val integrationId: String) : SystemEvent()
    data class EventProcessed(val eventType: String, val sourceComponent: String) : SystemEvent()
    data class DataProcessed(val sourceComponent: String, val flowCount: Int) : SystemEvent()
    data class StateSynchronized(val sourceComponent: String, val targetComponent: String, val stateKey: String) : SystemEvent()
    data class WorkflowExecuted(val workflowId: String, val success: Boolean) : SystemEvent()
    data object HealthCheckRequested : SystemEvent()
}