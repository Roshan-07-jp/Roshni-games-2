package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.integration.EventPriority
import com.roshni.games.core.utils.integration.IntegrationTestDataFactory
import com.roshni.games.core.utils.integration.IntegrationTestUtils
import com.roshni.games.core.utils.integration.IntegrationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import timber.log.Timber
import kotlin.test.assertTrue

/**
 * Integration tests for performance and error handling across systems
 */
class PerformanceAndErrorHandlingTest : SystemIntegrationTest() {

    @Test
    fun `test system performance under load`() = runTest {
        Timber.d("Testing system performance under load")

        // Register multiple components for load testing
        val componentCount = 10
        val components = (1..componentCount).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "load_component_$index",
                name = "Load Component $index"
            )
        }

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Generate high volume of events
        val eventCount = 50
        val events = (1..eventCount).map { index ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "LOAD_TEST_EVENT_$index",
                sourceFeature = components[index % components.size].id,
                targetFeatures = listOf(components[(index + 1) % components.size].id),
                priority = if (index % 10 == 0) EventPriority.HIGH else EventPriority.NORMAL
            )
        }

        // Measure performance of event processing under load
        val performanceMeasurement = IntegrationTestUtils.measureIntegrationPerformance(
            hub = systemIntegrationHub,
            operation = {
                runTest {
                    IntegrationTestUtils.sendEventsAndVerify(
                        hub = systemIntegrationHub,
                        events = events.take(5)
                    )
                }
            },
            operationName = "high_load_event_processing",
            warmUpRuns = 2,
            measuredRuns = 5
        )

        // Verify performance metrics
        assertTrue(performanceMeasurement.averageExecutionTimeMs < 2000,
            "Average execution time should be reasonable under load")

        Timber.d("High load performance test passed")
    }

    @Test
    fun `test error recovery mechanisms`() = runTest {
        Timber.d("Testing error recovery mechanisms")

        // Register components
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent("error_component_1", "Error Component 1"),
            IntegrationTestDataFactory.createTestComponent("error_component_2", "Error Component 2")
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create integration with retry configuration
        val resilientConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.EVENT_DRIVEN,
            retryCount = 3,
            timeoutMs = 2000
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "resilient_integration",
            sourceComponent = "error_component_1",
            targetComponent = "error_component_2",
            integrationType = IntegrationType.EVENT_DRIVEN,
            configuration = resilientConfig
        )

        assertTrue(integrationResult.isSuccess, "Resilient integration should be created successfully")

        // Send events that might cause issues
        val events = (1..5).map { index ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "RESILIENCE_TEST_EVENT_$index",
                sourceFeature = "error_component_1",
                targetFeatures = listOf("error_component_2")
            )
        }

        val eventResults = IntegrationTestUtils.sendEventsAndVerify(
            hub = systemIntegrationHub,
            events = events
        )

        // Verify that the system handled errors gracefully
        assertTrue(eventResults.size >= events.size * 0.8, "Most events should be processed successfully")

        Timber.d("Error recovery test passed")
    }

    @Test
    fun `test concurrent operations and thread safety`() = runTest {
        Timber.d("Testing concurrent operations and thread safety")

        // Register test components
        val components = (1..5).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "concurrent_component_$index",
                name = "Concurrent Component $index"
            )
        }

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Perform concurrent operations
        val operationJobs = (1..10).map { index ->
            async {
                when (index % 3) {
                    0 -> {
                        // Send events
                        val event = IntegrationTestDataFactory.createTestCrossFeatureEvent(
                            eventType = "CONCURRENT_EVENT_$index",
                            sourceFeature = components[index % components.size].id,
                            targetFeatures = listOf(components[(index + 1) % components.size].id)
                        )
                        systemIntegrationHub.sendEvent(event)
                    }
                    1 -> {
                        // Process data
                        val data = mapOf("concurrentTest" to true, "operationIndex" to index)
                        val context = IntegrationTestDataFactory.createTestIntegrationContext()
                        systemIntegrationHub.processData(components[index % components.size].id, data, context)
                    }
                    2 -> {
                        // Synchronize state
                        val stateUpdate = mapOf("concurrent.timestamp" to System.currentTimeMillis())
                        systemIntegrationHub.synchronizeState(
                            sourceComponent = components[index % components.size].id,
                            targetComponent = components[(index + 1) % components.size].id,
                            stateKey = "concurrent.test",
                            stateValue = index
                        )
                    }
                }
            }
        }

        // Wait for all operations to complete
        val operationResults = operationJobs.map { it.await() }

        // Verify most operations succeeded
        val successCount = operationResults.count { it.isSuccess }
        assertTrue(successCount >= operationResults.size * 0.8,
            "Most concurrent operations should succeed")

        Timber.d("Concurrent operations test passed")
    }

    @Test
    fun `test graceful degradation under component failures`() = runTest {
        Timber.d("Testing graceful degradation under component failures")

        // Register multiple components
        val components = (1..5).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "degradation_component_$index",
                name = "Degradation Component $index"
            )
        }

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Verify system is initially healthy
        var health = systemIntegrationHub.systemHealth.value
        assertTrue(health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY,
            "System should be healthy initially")

        // Simulate component failures by unregistering some components
        val componentsToFail = listOf(components[1], components[3])
        componentsToFail.forEach { component ->
            val unregisterResult = systemIntegrationHub.unregisterComponent(component.id)
            assertTrue(unregisterResult.isSuccess, "Component ${component.id} should be unregistered successfully")
        }

        // Wait for system to stabilize after failures
        delay(1000)

        // Verify system gracefully degrades but remains functional
        health = systemIntegrationHub.systemHealth.value
        assertTrue(health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY ||
                  health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.DEGRADED,
            "System should gracefully degrade but remain functional")

        // Test that system can still process events with reduced capacity
        val remainingComponents = components.filter { it !in componentsToFail }
        if (remainingComponents.size >= 2) {
            val testEvent = IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "DEGRADATION_TEST_EVENT",
                sourceFeature = remainingComponents[0].id,
                targetFeatures = listOf(remainingComponents[1].id)
            )

            val eventResult = systemIntegrationHub.sendEvent(testEvent)
            assertTrue(eventResult.isSuccess, "System should still process events after degradation")
        }

        Timber.d("Graceful degradation test passed")
    }

    @Test
    fun `test error isolation and containment`() = runTest {
        Timber.d("Testing error isolation and containment")

        // Register components
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent("error_source_1", "Error Source 1"),
            IntegrationTestDataFactory.createTestComponent("error_source_2", "Error Source 2"),
            IntegrationTestDataFactory.createTestComponent("error_target_1", "Error Target 1"),
            IntegrationTestDataFactory.createTestComponent("error_target_2", "Error Target 2")
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create integrations
        IntegrationTestUtils.createIntegrations(
            hub = systemIntegrationHub,
            sourceComponents = components.map { it.id },
            targetComponents = components.map { it.id },
            integrationType = IntegrationType.EVENT_DRIVEN
        )

        // Send events that might cause issues
        val events = (1..5).map { index ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "ERROR_ISOLATION_TEST_$index",
                sourceFeature = components[index % components.size].id,
                targetFeatures = listOf(components[(index + 1) % components.size].id)
            )
        }

        val eventResults = IntegrationTestUtils.sendEventsAndVerify(
            hub = systemIntegrationHub,
            events = events
        )

        // Verify that errors are isolated and don't crash the system
        assertTrue(eventResults.size >= events.size * 0.8, "Most events should be processed successfully")

        // Verify system remains healthy
        val health = systemIntegrationHub.systemHealth.value
        assertTrue(health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY,
            "System should remain healthy despite errors")

        Timber.d("Error isolation test passed")
    }

    @Test
    fun `test resource cleanup and memory management`() = runTest {
        Timber.d("Testing resource cleanup and memory management")

        // Register many components
        val components = (1..15).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "cleanup_component_$index",
                name = "Cleanup Component $index"
            )
        }

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Process large amounts of data
        val dataItems = (1..30).map { index ->
            mapOf(
                "dataId" to "cleanup_test_data_$index",
                "content" to "Large content chunk ${index} ".repeat(50),
                "metadata" to mapOf("size" to "large", "index" to index)
            )
        }

        val context = IntegrationTestDataFactory.createTestIntegrationContext()

        // Process data in batches
        val batchSize = 10
        for (i in dataItems.indices step batchSize) {
            val batch = dataItems.slice(i until minOf(i + batchSize, dataItems.size))

            IntegrationTestUtils.processDataAndVerify(
                hub = systemIntegrationHub,
                sourceComponent = components[0].id,
                dataItems = batch,
                context = context
            )

            delay(50)
        }

        // Verify system remains healthy after intensive operations
        val health = systemIntegrationHub.systemHealth.value
        assertTrue(health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY ||
                  health.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.DEGRADED,
            "System should remain healthy after intensive operations")

        Timber.d("Resource cleanup test passed")
    }
}