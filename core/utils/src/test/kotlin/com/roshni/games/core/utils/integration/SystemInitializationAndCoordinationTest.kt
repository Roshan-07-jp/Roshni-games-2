package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.integration.HealthStatus
import com.roshni.games.core.utils.integration.IntegrationHubStatus
import com.roshni.games.core.utils.integration.IntegrationTestDataFactory
import com.roshni.games.core.utils.integration.IntegrationTestUtils
import com.roshni.games.core.utils.integration.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for system initialization and coordination
 */
class SystemInitializationAndCoordinationTest : SystemIntegrationTest() {

    @Test
    fun `test system integration hub initialization`() = runTest {
        Timber.d("Testing system integration hub initialization")

        // Verify initial state
        assertEquals(IntegrationHubStatus.READY, systemIntegrationHub.status.value)

        // Verify system health
        val health = systemIntegrationHub.systemHealth.value
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)

        // Verify core components are registered
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.totalComponents >= 3, "Should have at least 3 core components")

        Timber.d("System integration hub initialization test passed")
    }

    @Test
    fun `test core component registration and activation`() = runTest {
        Timber.d("Testing core component registration and activation")

        // Wait for system stabilization
        waitForSystemStabilization()

        // Verify all core components are active
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.activeComponents >= 3, "Should have at least 3 active components")

        // Verify system health remains good
        val health = systemIntegrationHub.systemHealth.value
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)

        Timber.d("Core component registration and activation test passed")
    }

    @Test
    fun `test feature manager integration with hub`() = runTest {
        Timber.d("Testing feature manager integration with hub")

        // Verify feature manager is registered as a component
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.totalComponents > 0)

        // Test feature registration through feature manager
        val testFeature = IntegrationTestDataFactory.createMockFeature(
            id = "integration_test_feature",
            name = "Integration Test Feature"
        )

        val registered = featureManager.registerFeature(testFeature)
        assertTrue(registered, "Feature should be registered successfully")

        // Verify feature is in registered features
        val retrievedFeature = featureManager.getFeature("integration_test_feature")
        assertNotNull(retrievedFeature, "Feature should be retrievable")

        Timber.d("Feature manager integration test passed")
    }

    @Test
    fun `test rule engine integration with hub`() = runTest {
        Timber.d("Testing rule engine integration with hub")

        // Test rule registration
        val testRule = IntegrationTestDataFactory.createMockRule(
            id = "integration_test_rule",
            name = "Integration Test Rule"
        )

        val registered = ruleEngine.registerRule(testRule)
        assertTrue(registered, "Rule should be registered successfully")

        // Test rule evaluation
        val context = IntegrationTestDataFactory.createTestRuleContext()
        val result = ruleEngine.evaluateRule("integration_test_rule", context)

        assertTrue(result.success, "Rule evaluation should succeed")

        Timber.d("Rule engine integration test passed")
    }

    @Test
    fun `test workflow engine integration with hub`() = runTest {
        Timber.d("Testing workflow engine integration with hub")

        // Test workflow registration
        val testWorkflow = IntegrationTestDataFactory.createMockWorkflow(
            id = "integration_test_workflow",
            name = "Integration Test Workflow"
        )

        val registered = workflowEngine.registerWorkflow(testWorkflow)
        assertTrue(registered, "Workflow should be registered successfully")

        // Test workflow execution through hub
        val context = IntegrationTestDataFactory.createTestIntegrationContext()
        val workflowResult = systemIntegrationHub.executeWorkflow(
            workflowId = "integration_test_workflow",
            context = context,
            inputData = mapOf("testInput" to "testValue")
        )

        assertTrue(workflowResult.isSuccess, "Workflow execution should succeed")
        val result = workflowResult.getOrThrow()
        assertTrue(result.success, "Workflow result should indicate success")

        Timber.d("Workflow engine integration test passed")
    }

    @Test
    fun `test system health monitoring and reporting`() = runTest {
        Timber.d("Testing system health monitoring and reporting")

        // Wait for system to stabilize
        waitForSystemStabilization()

        // Verify health monitoring is working
        val health = systemIntegrationHub.systemHealth.value
        assertNotNull(health.lastHealthCheck, "Health check timestamp should be set")
        assertTrue(health.lastHealthCheck > 0, "Health check timestamp should be valid")

        // Verify component health tracking
        assertTrue(health.totalComponents > 0, "Should track component count")
        assertEquals(health.totalComponents, health.healthyComponents, "All components should be healthy")

        Timber.d("System health monitoring test passed")
    }

    @Test
    fun `test system event generation and observation`() = runTest {
        Timber.d("Testing system event generation and observation")

        // Register a test component to generate events
        val testComponent = IntegrationTestDataFactory.createTestComponent(
            id = "event_test_component",
            name = "Event Test Component"
        )

        val registerResult = systemIntegrationHub.registerComponent(testComponent)
        assertTrue(registerResult.isSuccess, "Component should be registered successfully")

        // Wait for component registration event
        val events = IntegrationTestUtils.waitForSystemEvents(
            hub = systemIntegrationHub,
            eventCount = 1,
            eventFilter = { event ->
                event is SystemEvent.ComponentRegistered &&
                event.componentId == "event_test_component"
            }
        )

        assertTrue(events.isNotEmpty(), "Should receive component registration event")

        // Test event observation
        val allEvents = systemIntegrationHub.observeSystemEvents()
            .take(10)
            .toList()

        assertTrue(allEvents.isNotEmpty(), "Should observe system events")

        Timber.d("System event generation and observation test passed")
    }

    @Test
    fun `test cross-component coordination`() = runTest {
        Timber.d("Testing cross-component coordination")

        // Register multiple test components
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent("coord_component_1", "Coordination Component 1"),
            IntegrationTestDataFactory.createTestComponent("coord_component_2", "Coordination Component 2"),
            IntegrationTestDataFactory.createTestComponent("coord_component_3", "Coordination Component 3")
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create integrations between components
        val componentIds = components.map { it.id }
        IntegrationTestUtils.createIntegrations(
            hub = systemIntegrationHub,
            sourceComponents = componentIds,
            targetComponents = componentIds
        )

        // Verify coordination metrics
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.totalIntegrations > 0, "Should have created integrations")

        // Test coordinated workflow execution
        val context = IntegrationTestDataFactory.createTestIntegrationContext()
        val workflowResult = systemIntegrationHub.executeWorkflow(
            workflowId = "test_integration_workflow",
            context = context,
            inputData = mapOf("coordinationTest" to true)
        )

        assertTrue(workflowResult.isSuccess, "Coordinated workflow should execute successfully")

        Timber.d("Cross-component coordination test passed")
    }

    @Test
    fun `test system metrics collection and reporting`() = runTest {
        Timber.d("Testing system metrics collection and reporting")

        // Perform some operations to generate metrics
        val testComponent = IntegrationTestDataFactory.createTestComponent(
            id = "metrics_test_component",
            name = "Metrics Test Component"
        )

        systemIntegrationHub.registerComponent(testComponent)

        // Send some test events
        val events = listOf(
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "METRICS_TEST_EVENT_1",
                sourceFeature = "metrics_test_component"
            ),
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "METRICS_TEST_EVENT_2",
                sourceFeature = "metrics_test_component"
            )
        )

        IntegrationTestUtils.sendEventsAndVerify(systemIntegrationHub, events)

        // Verify metrics are collected
        val metrics = systemIntegrationHub.getIntegrationMetrics()

        // Should have processed events
        assertTrue(metrics.totalEventsProcessed >= 2, "Should have processed test events")

        // Verify average processing time is reasonable
        assertTrue(metrics.averageProcessingTimeMs >= 0, "Average processing time should be non-negative")

        Timber.d("System metrics collection test passed")
    }

    @Test
    fun `test system shutdown and cleanup coordination`() = runTest {
        Timber.d("Testing system shutdown and cleanup coordination")

        // Register some test components
        val testComponents = listOf(
            IntegrationTestDataFactory.createTestComponent("shutdown_component_1", "Shutdown Component 1"),
            IntegrationTestDataFactory.createTestComponent("shutdown_component_2", "Shutdown Component 2")
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, testComponents)

        // Verify components are active
        var metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.activeComponents >= 2, "Should have active components before shutdown")

        // Shutdown the integration hub
        val shutdownResult = systemIntegrationHub.shutdown()
        assertTrue(shutdownResult.isSuccess, "Shutdown should complete successfully")

        // Verify final state
        assertEquals(IntegrationHubStatus.SHUTDOWN, systemIntegrationHub.status.value)

        Timber.d("System shutdown and cleanup coordination test passed")
    }

    @Test
    fun `test error handling in system coordination`() = runTest {
        Timber.d("Testing error handling in system coordination")

        // Try to register a component with invalid configuration
        val invalidComponent = IntegrationTestDataFactory.createTestComponent(
            id = "", // Invalid empty ID
            name = "Invalid Component"
        )

        val invalidResult = systemIntegrationHub.registerComponent(invalidComponent)
        assertTrue(invalidResult.isFailure, "Should fail to register component with invalid ID")

        // Verify system remains healthy despite error
        val health = systemIntegrationHub.systemHealth.value
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)

        // Verify error is recorded in system events
        val errorEvents = IntegrationTestUtils.waitForSystemEvents(
            hub = systemIntegrationHub,
            eventCount = 1,
            eventFilter = { event -> event is SystemEvent.SystemError }
        )

        // Should have at least one error event (may be more from other operations)
        assertTrue(errorEvents.isNotEmpty(), "Should record system errors")

        Timber.d("Error handling in system coordination test passed")
    }

    @Test
    fun `test system recovery after component failure`() = runTest {
        Timber.d("Testing system recovery after component failure")

        // Register a test component
        val testComponent = IntegrationTestDataFactory.createTestComponent(
            id = "recovery_test_component",
            name = "Recovery Test Component"
        )

        val registerResult = systemIntegrationHub.registerComponent(testComponent)
        assertTrue(registerResult.isSuccess, "Component should be registered successfully")

        // Wait for system stabilization
        waitForSystemStabilization()

        // Verify system is healthy
        var health = systemIntegrationHub.systemHealth.value
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)

        // Unregister the component (simulating failure)
        val unregisterResult = systemIntegrationHub.unregisterComponent("recovery_test_component")
        assertTrue(unregisterResult.isSuccess, "Component should be unregistered successfully")

        // Verify system remains healthy after component removal
        health = systemIntegrationHub.systemHealth.value
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)

        Timber.d("System recovery after component failure test passed")
    }

    @Test
    fun `test concurrent component registration and coordination`() = runTest {
        Timber.d("Testing concurrent component registration and coordination")

        // Create multiple components for concurrent registration
        val components = (1..5).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "concurrent_component_$index",
                name = "Concurrent Component $index"
            )
        }

        // Register components concurrently
        val registrationJobs = components.map { component ->
            async {
                systemIntegrationHub.registerComponent(component)
            }
        }

        // Wait for all registrations to complete
        val registrationResults = registrationJobs.map { it.await() }

        // Verify all registrations succeeded
        registrationResults.forEach { result ->
            assertTrue(result.isSuccess, "All component registrations should succeed")
        }

        // Wait for system stabilization
        waitForSystemStabilization()

        // Verify all components are active
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(metrics.activeComponents >= components.size, "All registered components should be active")

        Timber.d("Concurrent component registration and coordination test passed")
    }

    @Test
    fun `test system performance under load`() = runTest {
        Timber.d("Testing system performance under load")

        // Create a larger number of components and interactions
        val componentCount = 10
        val interactionCount = 50

        val components = (1..componentCount).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "load_test_component_$index",
                name = "Load Test Component $index"
            )
        }

        // Register all components
        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create and send multiple events
        val events = (1..interactionCount).map { index ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "LOAD_TEST_EVENT_$index",
                sourceFeature = components[index % components.size].id
            )
        }

        // Measure performance of event processing
        val performanceMeasurement = IntegrationTestUtils.measureIntegrationPerformance(
            hub = systemIntegrationHub,
            operation = {
                runTest {
                    IntegrationTestUtils.sendEventsAndVerify(systemIntegrationHub, events.take(5))
                }
            },
            operationName = "event_processing_under_load",
            warmUpRuns = 2,
            measuredRuns = 5
        )

        // Verify performance is reasonable (adjust thresholds as needed)
        assertTrue(performanceMeasurement.averageExecutionTimeMs < 1000,
            "Average execution time should be reasonable: ${performanceMeasurement.averageExecutionTimeMs}ms")

        Timber.d("System performance under load test passed")
    }
}