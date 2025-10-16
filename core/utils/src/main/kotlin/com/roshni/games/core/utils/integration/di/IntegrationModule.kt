package com.roshni.games.core.utils.integration.di

import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.integration.CrossFeatureEventManager
import com.roshni.games.core.utils.integration.DataFlowManager
import com.roshni.games.core.utils.integration.FeatureIntegrationCoordinator
import com.roshni.games.core.utils.integration.StateSynchronizationManager
import com.roshni.games.core.utils.integration.SystemIntegrationHub
import com.roshni.games.core.utils.integration.SystemIntegrationHubImpl
import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.workflow.WorkflowEngine
import org.koin.dsl.module

/**
 * Dependency injection module for integration components
 */
val integrationModule = module {

    // Core managers
    single<CrossFeatureEventManager> { CrossFeatureEventManager() }
    single<DataFlowManager> { DataFlowManager() }
    single<StateSynchronizationManager> { StateSynchronizationManager() }

    // Coordinators
    single<FeatureIntegrationCoordinator> { (featureManager: FeatureManager, eventManager: CrossFeatureEventManager, dataFlowManager: DataFlowManager) ->
        FeatureIntegrationCoordinator(featureManager, eventManager, dataFlowManager)
    }

    // Main integration hub
    single<SystemIntegrationHub> { (featureManager: FeatureManager, ruleEngine: RuleEngine, workflowEngine: WorkflowEngine) ->
        SystemIntegrationHubImpl(featureManager, ruleEngine, workflowEngine)
    }
}

/**
 * Integration component configuration
 */
data class IntegrationConfig(
    val enableEventCoordination: Boolean = true,
    val enableDataFlowProcessing: Boolean = true,
    val enableStateSynchronization: Boolean = true,
    val enableWorkflowIntegration: Boolean = true,
    val maxConcurrentEvents: Int = 100,
    val maxDataFlows: Int = 50,
    val stateSyncIntervalMs: Long = 30000,
    val enableMetricsCollection: Boolean = true,
    val enableHealthMonitoring: Boolean = true,
    val customProperties: Map<String, Any> = emptyMap()
)

/**
 * Integration module builder for custom configuration
 */
class IntegrationModuleBuilder {

    private var config = IntegrationConfig()

    fun enableEventCoordination(enable: Boolean = true) = apply {
        config = config.copy(enableEventCoordination = enable)
    }

    fun enableDataFlowProcessing(enable: Boolean = true) = apply {
        config = config.copy(enableDataFlowProcessing = enable)
    }

    fun enableStateSynchronization(enable: Boolean = true) = apply {
        config = config.copy(enableStateSynchronization = enable)
    }

    fun enableWorkflowIntegration(enable: Boolean = true) = apply {
        config = config.copy(enableWorkflowIntegration = enable)
    }

    fun setMaxConcurrentEvents(max: Int) = apply {
        config = config.copy(maxConcurrentEvents = max)
    }

    fun setMaxDataFlows(max: Int) = apply {
        config = config.copy(maxDataFlows = max)
    }

    fun setStateSyncInterval(intervalMs: Long) = apply {
        config = config.copy(stateSyncIntervalMs = intervalMs)
    }

    fun enableMetricsCollection(enable: Boolean = true) = apply {
        config = config.copy(enableMetricsCollection = enable)
    }

    fun enableHealthMonitoring(enable: Boolean = true) = apply {
        config = config.copy(enableHealthMonitoring = enable)
    }

    fun setCustomProperty(key: String, value: Any) = apply {
        val updatedProps = config.customProperties + (key to value)
        config = config.copy(customProperties = updatedProps)
    }

    fun build(): IntegrationConfig = config
}

/**
 * Integration module factory for creating custom configured modules
 */
object IntegrationModuleFactory {

    fun createModule(config: IntegrationConfig = IntegrationConfig()): org.koin.dsl.Module {
        return module {

            // Core managers with configuration
            single<CrossFeatureEventManager> {
                CrossFeatureEventManager().apply {
                    if (config.enableEventCoordination) {
                        // Configure event manager
                    }
                }
            }

            single<DataFlowManager> {
                DataFlowManager().apply {
                    if (config.enableDataFlowProcessing) {
                        // Configure data flow manager
                    }
                }
            }

            single<StateSynchronizationManager> {
                StateSynchronizationManager().apply {
                    if (config.enableStateSynchronization) {
                        // Configure state sync manager
                    }
                }
            }

            // Coordinators
            single<FeatureIntegrationCoordinator> { (featureManager: FeatureManager, eventManager: CrossFeatureEventManager, dataFlowManager: DataFlowManager) ->
                FeatureIntegrationCoordinator(featureManager, eventManager, dataFlowManager).apply {
                    if (config.enableWorkflowIntegration) {
                        // Configure workflow integration
                    }
                }
            }

            // Main integration hub
            single<SystemIntegrationHub> { (featureManager: FeatureManager, ruleEngine: RuleEngine, workflowEngine: WorkflowEngine) ->
                SystemIntegrationHubImpl(featureManager, ruleEngine, workflowEngine).apply {
                    // Apply configuration
                }
            }

            // Configuration as singleton
            single<IntegrationConfig> { config }
        }
    }

    fun builder(): IntegrationModuleBuilder = IntegrationModuleBuilder()
}

/**
 * Integration component qualifiers for dependency injection
 */
object IntegrationQualifiers {
    const val EVENT_MANAGER = "event_manager"
    const val DATA_FLOW_MANAGER = "data_flow_manager"
    const val STATE_SYNC_MANAGER = "state_sync_manager"
    const val FEATURE_COORDINATOR = "feature_coordinator"
    const val INTEGRATION_HUB = "integration_hub"
}

/**
 * Extension functions for easy module creation
 */
fun createIntegrationModule(config: IntegrationConfig = IntegrationConfig()): org.koin.dsl.Module {
    return IntegrationModuleFactory.createModule(config)
}

fun integrationModuleBuilder(): IntegrationModuleBuilder {
    return IntegrationModuleBuilder()
}