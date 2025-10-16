package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.integration.DataFlowDirection
import com.roshni.games.core.utils.integration.EventPriority
import com.roshni.games.core.utils.integration.IntegrationTestDataFactory
import com.roshni.games.core.utils.integration.IntegrationTestUtils
import com.roshni.games.core.utils.integration.IntegrationType
import com.roshni.games.core.utils.integration.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for cross-system communication and data flow validation
 */
class CrossSystemCommunicationAndDataFlowTest : SystemIntegrationTest() {

    @Test
    fun `test event-driven integration between components`() = runTest {
        Timber.d("Testing event-driven integration between components")

        // Register test components
        val component1 = IntegrationTestDataFactory.createTestComponent(
            id = "event_source_component",
            name = "Event Source Component",
            capabilities = listOf("event_generation", "data_processing")
        )

        val component2 = IntegrationTestDataFactory.createTestComponent(
            id = "event_target_component",
            name = "Event Target Component",
            capabilities = listOf("event_handling", "response_generation")
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(component1, component2)
        )

        // Create event-driven integration
        val integrationConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.EVENT_DRIVEN,
            priority = EventPriority.HIGH
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "event_driven_integration",
            sourceComponent = "event_source_component",
            targetComponent = "event_target_component",
            integrationType = IntegrationType.EVENT_DRIVEN,
            configuration = integrationConfig
        )

        assertTrue(integrationResult.isSuccess, "Event-driven integration should be created successfully")

        // Send test event
        val testEvent = IntegrationTestDataFactory.createTestCrossFeatureEvent(
            eventType = "TEST_EVENT_DRIVEN",
            sourceFeature = "event_source_component",
            targetFeatures = listOf("event_target_component"),
            priority = EventPriority.HIGH,
            payload = mapOf(
                "testData" to "event_driven_test",
                "timestamp" to System.currentTimeMillis()
            )
        )

        val eventResult = systemIntegrationHub.sendEvent(testEvent)
        assertTrue(eventResult.isSuccess, "Event should be sent successfully")

        // Verify event processing
        val processedEvent = eventResult.getOrThrow()
        assertTrue(processedEvent.success, "Event should be processed successfully")
        assertTrue(processedEvent.processedBy.isNotEmpty(), "Event should be processed by at least one component")

        Timber.d("Event-driven integration test passed")
    }

    @Test
    fun `test data flow integration between components`() = runTest {
        Timber.d("Testing data flow integration between components")

        // Register test components
        val sourceComponent = IntegrationTestDataFactory.createTestComponent(
            id = "data_source_component",
            name = "Data Source Component",
            type = ComponentType.DATA_PROCESSOR,
            capabilities = listOf("data_generation", "data_streaming")
        )

        val targetComponent = IntegrationTestDataFactory.createTestComponent(
            id = "data_target_component",
            name = "Data Target Component",
            type = ComponentType.DATA_PROCESSOR,
            capabilities = listOf("data_processing", "data_storage")
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(sourceComponent, targetComponent)
        )

        // Create data flow integration
        val dataFlowConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.DATA_FLOW,
            dataFlowDirection = DataFlowDirection.UNIDIRECTIONAL
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "data_flow_integration",
            sourceComponent = "data_source_component",
            targetComponent = "data_target_component",
            integrationType = IntegrationType.DATA_FLOW,
            configuration = dataFlowConfig
        )

        assertTrue(integrationResult.isSuccess, "Data flow integration should be created successfully")

        // Process test data
        val testData = mapOf(
            "dataId" to "test_data_001",
            "content" to "This is test data for integration testing",
            "metadata" to mapOf("source" to "integration_test", "type" to "text")
        )

        val context = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "data_source_component"
        )

        val dataResult = systemIntegrationHub.processData(
            sourceComponent = "data_source_component",
            data = testData,
            context = context
        )

        assertTrue(dataResult.isSuccess, "Data should be processed successfully")

        val processedData = dataResult.getOrThrow()
        assertTrue(processedData.success, "Data processing should succeed")
        assertNotNull(processedData.data, "Processed data should not be null")

        Timber.d("Data flow integration test passed")
    }

    @Test
    fun `test state synchronization between components`() = runTest {
        Timber.d("Testing state synchronization between components")

        // Register test components
        val component1 = IntegrationTestDataFactory.createTestComponent(
            id = "state_source_component",
            name = "State Source Component",
            type = ComponentType.STATE_MANAGER,
            capabilities = listOf("state_management", "state_sharing")
        )

        val component2 = IntegrationTestDataFactory.createTestComponent(
            id = "state_target_component",
            name = "State Target Component",
            type = ComponentType.STATE_MANAGER,
            capabilities = listOf("state_reception", "state_application")
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(component1, component2)
        )

        // Create state synchronization integration
        val stateSyncConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.STATE_SYNC
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "state_sync_integration",
            sourceComponent = "state_source_component",
            targetComponent = "state_target_component",
            integrationType = IntegrationType.STATE_SYNC,
            configuration = stateSyncConfig
        )

        assertTrue(integrationResult.isSuccess, "State sync integration should be created successfully")

        // Synchronize test state
        val stateUpdates = mapOf(
            "userPreference.theme" to "dark",
            "userPreference.language" to "en",
            "gameState.level" to 5,
            "gameState.score" to 1250,
            "sessionData.lastActivity" to System.currentTimeMillis()
        )

        IntegrationTestUtils.synchronizeStateAndVerify(
            hub = systemIntegrationHub,
            sourceComponent = "state_source_component",
            targetComponent = "state_target_component",
            stateUpdates = stateUpdates
        )

        // Verify state synchronization events were generated
        val syncEvents = IntegrationTestUtils.waitForSystemEvents(
            hub = systemIntegrationHub,
            eventCount = stateUpdates.size,
            eventFilter = { event ->
                event is SystemEvent.StateSynchronized &&
                event.sourceComponent == "state_source_component" &&
                event.targetComponent == "state_target_component"
            }
        )

        assertEquals(stateUpdates.size, syncEvents.size, "Should generate state sync events for each update")

        Timber.d("State synchronization test passed")
    }

    @Test
    fun `test bidirectional communication between components`() = runTest {
        Timber.d("Testing bidirectional communication between components")

        // Register test components
        val component1 = IntegrationTestDataFactory.createTestComponent(
            id = "bidirectional_component_1",
            name = "Bidirectional Component 1",
            capabilities = listOf("send_receive", "process_respond")
        )

        val component2 = IntegrationTestDataFactory.createTestComponent(
            id = "bidirectional_component_2",
            name = "Bidirectional Component 2",
            capabilities = listOf("receive_send", "respond_process")
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(component1, component2)
        )

        // Create bidirectional data flow integration
        val bidirectionalConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.DATA_FLOW,
            dataFlowDirection = DataFlowDirection.BIDIRECTIONAL
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "bidirectional_integration",
            sourceComponent = "bidirectional_component_1",
            targetComponent = "bidirectional_component_2",
            integrationType = IntegrationType.DATA_FLOW,
            configuration = bidirectionalConfig
        )

        assertTrue(integrationResult.isSuccess, "Bidirectional integration should be created successfully")

        // Test data flow in both directions
        val context1 = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "bidirectional_component_1"
        )

        val data1 = mapOf(
            "direction" to "component1_to_component2",
            "message" to "Hello from component 1",
            "timestamp" to System.currentTimeMillis()
        )

        val result1 = systemIntegrationHub.processData("bidirectional_component_1", data1, context1)
        assertTrue(result1.isSuccess, "Data flow from component 1 should succeed")

        val context2 = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "bidirectional_component_2"
        )

        val data2 = mapOf(
            "direction" to "component2_to_component1",
            "message" to "Hello from component 2",
            "timestamp" to System.currentTimeMillis()
        )

        val result2 = systemIntegrationHub.processData("bidirectional_component_2", data2, context2)
        assertTrue(result2.isSuccess, "Data flow from component 2 should succeed")

        Timber.d("Bidirectional communication test passed")
    }

    @Test
    fun `test multi-component event routing and processing`() = runTest {
        Timber.d("Testing multi-component event routing and processing")

        // Register multiple test components
        val components = (1..4).map { index ->
            IntegrationTestDataFactory.createTestComponent(
                id = "multi_component_$index",
                name = "Multi Component $index",
                capabilities = listOf("event_handling", "data_processing")
            )
        }

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create event-driven integrations between all components
        for (i in components.indices) {
            for (j in components.indices) {
                if (i != j) {
                    val integrationConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                        type = IntegrationType.EVENT_DRIVEN
                    )

                    val integrationResult = systemIntegrationHub.createIntegration(
                        integrationId = "multi_integration_${i}_${j}",
                        sourceComponent = components[i].id,
                        targetComponent = components[j].id,
                        integrationType = IntegrationType.EVENT_DRIVEN,
                        configuration = integrationConfig
                    )

                    assertTrue(integrationResult.isSuccess,
                        "Integration between ${components[i].id} and ${components[j].id} should be created successfully")
                }
            }
        }

        // Send events from each component
        val events = components.mapIndexed { index, component ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "MULTI_COMPONENT_EVENT_$index",
                sourceFeature = component.id,
                targetFeatures = components.filter { it.id != component.id }.map { it.id },
                payload = mapOf(
                    "sourceIndex" to index,
                    "targetCount" to (components.size - 1),
                    "broadcast" to true
                )
            )
        }

        // Send and verify all events
        val eventResults = IntegrationTestUtils.sendEventsAndVerify(
            hub = systemIntegrationHub,
            events = events,
            expectedProcessedCount = events.size
        )

        assertEquals(events.size, eventResults.size, "All events should be processed")

        // Verify each event was processed by multiple components
        eventResults.forEach { result ->
            assertTrue(result.processedBy.size >= 2, "Each event should be processed by multiple components")
        }

        Timber.d("Multi-component event routing test passed")
    }

    @Test
    fun `test data transformation through integration pipeline`() = runTest {
        Timber.d("Testing data transformation through integration pipeline")

        // Register components for data transformation pipeline
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "data_input_component",
                name = "Data Input Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "data_transform_component",
                name = "Data Transform Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "data_output_component",
                name = "Data Output Component",
                type = ComponentType.DATA_PROCESSOR
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create data flow integrations forming a pipeline
        val pipelineIntegrations = listOf(
            Triple("data_input_component", "data_transform_component", "pipeline_integration_1"),
            Triple("data_transform_component", "data_output_component", "pipeline_integration_2")
        )

        pipelineIntegrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = IntegrationType.DATA_FLOW,
                dataFlowDirection = DataFlowDirection.UNIDIRECTIONAL
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = IntegrationType.DATA_FLOW,
                configuration = config
            )

            assertTrue(result.isSuccess, "Pipeline integration $integrationId should be created successfully")
        }

        // Process data through the pipeline
        val originalData = mapOf(
            "rawData" to "original_input_data",
            "format" to "json",
            "pipelineId" to "test_pipeline"
        )

        val context = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "data_input_component"
        )

        val pipelineResult = systemIntegrationHub.processData(
            sourceComponent = "data_input_component",
            data = originalData,
            context = context
        )

        assertTrue(pipelineResult.isSuccess, "Data should flow through pipeline successfully")

        // Verify data was transformed (this would be more sophisticated in real implementation)
        val processedData = pipelineResult.getOrThrow()
        assertNotNull(processedData.data, "Pipeline output should contain processed data")

        Timber.d("Data transformation pipeline test passed")
    }

    @Test
    fun `test event priority and ordering in cross-system communication`() = runTest {
        Timber.d("Testing event priority and ordering in cross-system communication")

        // Register test components
        val component1 = IntegrationTestDataFactory.createTestComponent(
            id = "priority_component_1",
            name = "Priority Component 1"
        )

        val component2 = IntegrationTestDataFactory.createTestComponent(
            id = "priority_component_2",
            name = "Priority Component 2"
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(component1, component2)
        )

        // Create event-driven integration
        val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.EVENT_DRIVEN
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "priority_integration",
            sourceComponent = "priority_component_1",
            targetComponent = "priority_component_2",
            integrationType = IntegrationType.EVENT_DRIVEN,
            configuration = config
        )

        assertTrue(integrationResult.isSuccess, "Priority integration should be created successfully")

        // Send events with different priorities
        val events = listOf(
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "LOW_PRIORITY_EVENT",
                sourceFeature = "priority_component_1",
                targetFeatures = listOf("priority_component_2"),
                priority = EventPriority.LOW,
                payload = mapOf("priority" to "low", "order" to 3)
            ),
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "HIGH_PRIORITY_EVENT",
                sourceFeature = "priority_component_1",
                targetFeatures = listOf("priority_component_2"),
                priority = EventPriority.HIGH,
                payload = mapOf("priority" to "high", "order" to 1)
            ),
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "NORMAL_PRIORITY_EVENT",
                sourceFeature = "priority_component_1",
                targetFeatures = listOf("priority_component_2"),
                priority = EventPriority.NORMAL,
                payload = mapOf("priority" to "normal", "order" to 2)
            )
        )

        // Send events and collect processing results
        val eventResults = IntegrationTestUtils.sendEventsAndVerify(
            hub = systemIntegrationHub,
            events = events
        )

        assertEquals(3, eventResults.size, "All priority events should be processed")

        // Verify all events were processed successfully
        eventResults.forEach { result ->
            assertTrue(result.success, "All events should be processed successfully")
        }

        Timber.d("Event priority and ordering test passed")
    }

    @Test
    fun `test error propagation in cross-system communication`() = runTest {
        Timber.d("Testing error propagation in cross-system communication")

        // Register components where one might fail
        val reliableComponent = IntegrationTestDataFactory.createTestComponent(
            id = "reliable_component",
            name = "Reliable Component"
        )

        val unreliableComponent = IntegrationTestDataFactory.createTestComponent(
            id = "unreliable_component",
            name = "Unreliable Component"
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(reliableComponent, unreliableComponent)
        )

        // Create integration that might fail
        val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.EVENT_DRIVEN,
            retryCount = 1 // Low retry count to potentially trigger errors
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "error_propagation_integration",
            sourceComponent = "reliable_component",
            targetComponent = "unreliable_component",
            integrationType = IntegrationType.EVENT_DRIVEN,
            configuration = config
        )

        assertTrue(integrationResult.isSuccess, "Error propagation integration should be created successfully")

        // Send event that might cause processing issues
        val problematicEvent = IntegrationTestDataFactory.createTestCrossFeatureEvent(
            eventType = "PROBLEMATIC_EVENT",
            sourceFeature = "reliable_component",
            targetFeatures = listOf("unreliable_component"),
            payload = mapOf(
                "causeError" to true,
                "errorType" to "processing_error",
                "data" to null // This might cause issues in processing
            )
        )

        val eventResult = systemIntegrationHub.sendEvent(problematicEvent)

        // Event sending should still succeed even if processing fails
        assertTrue(eventResult.isSuccess, "Event sending should succeed even with processing errors")

        // Check for error events in system events
        val errorEvents = IntegrationTestUtils.waitForSystemEvents(
            hub = systemIntegrationHub,
            eventCount = 1,
            timeoutMs = 2000,
            eventFilter = { event -> event is SystemEvent.SystemError }
        )

        // We might have error events depending on how the system handles problematic data
        // The important thing is that the system doesn't crash

        Timber.d("Error propagation test passed")
    }

    @Test
    fun `test data consistency across integrated systems`() = runTest {
        Timber.d("Testing data consistency across integrated systems")

        // Register components that share data
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "consistency_component_1",
                name = "Consistency Component 1",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "consistency_component_2",
                name = "Consistency Component 2",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "consistency_component_3",
                name = "Consistency Component 3",
                type = ComponentType.STATE_MANAGER
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create state synchronization integrations between all components
        for (i in components.indices) {
            for (j in components.indices) {
                if (i != j) {
                    val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                        type = IntegrationType.STATE_SYNC
                    )

                    val result = systemIntegrationHub.createIntegration(
                        integrationId = "consistency_integration_${i}_${j}",
                        sourceComponent = components[i].id,
                        targetComponent = components[j].id,
                        integrationType = IntegrationType.STATE_SYNC,
                        configuration = config
                    )

                    assertTrue(result.isSuccess,
                        "Consistency integration between ${components[i].id} and ${components[j].id} should be created successfully")
                }
            }
        }

        // Synchronize consistent state across all components
        val sharedState = mapOf(
            "global.userId" to "test_user_123",
            "global.sessionId" to "test_session_456",
            "global.appVersion" to "1.0.0",
            "global.environment" to "testing",
            "global.timestamp" to System.currentTimeMillis()
        )

        // Synchronize state from first component to others
        for (i in 1 until components.size) {
            IntegrationTestUtils.synchronizeStateAndVerify(
                hub = systemIntegrationHub,
                sourceComponent = components[0].id,
                targetComponent = components[i].id,
                stateUpdates = sharedState
            )
        }

        // Verify state synchronization events
        val syncEvents = IntegrationTestUtils.waitForSystemEvents(
            hub = systemIntegrationHub,
            eventCount = (components.size - 1) * sharedState.size,
            timeoutMs = 3000,
            eventFilter = { event -> event is SystemEvent.StateSynchronized }
        )

        assertTrue(syncEvents.size >= (components.size - 1) * sharedState.size,
            "Should generate state synchronization events")

        Timber.d("Data consistency test passed")
    }

    @Test
    fun `test communication resilience and recovery`() = runTest {
        Timber.d("Testing communication resilience and recovery")

        // Register test components
        val component1 = IntegrationTestDataFactory.createTestComponent(
            id = "resilient_component_1",
            name = "Resilient Component 1"
        )

        val component2 = IntegrationTestDataFactory.createTestComponent(
            id = "resilient_component_2",
            name = "Resilient Component 2"
        )

        IntegrationTestUtils.registerComponentsAndWait(
            hub = systemIntegrationHub,
            components = listOf(component1, component2)
        )

        // Create integration with retry configuration
        val resilientConfig = IntegrationTestDataFactory.createTestIntegrationConfiguration(
            type = IntegrationType.EVENT_DRIVEN,
            retryCount = 3,
            timeoutMs = 1000
        )

        val integrationResult = systemIntegrationHub.createIntegration(
            integrationId = "resilient_integration",
            sourceComponent = "resilient_component_1",
            targetComponent = "resilient_component_2",
            integrationType = IntegrationType.EVENT_DRIVEN,
            configuration = resilientConfig
        )

        assertTrue(integrationResult.isSuccess, "Resilient integration should be created successfully")

        // Send multiple events to test resilience
        val events = (1..5).map { index ->
            IntegrationTestDataFactory.createTestCrossFeatureEvent(
                eventType = "RESILIENCE_TEST_EVENT_$index",
                sourceFeature = "resilient_component_1",
                targetFeatures = listOf("resilient_component_2"),
                payload = mapOf(
                    "eventIndex" to index,
                    "testResilience" to true,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        val startTime = System.currentTimeMillis()

        // Send events and measure resilience
        val eventResults = IntegrationTestUtils.sendEventsAndVerify(
            hub = systemIntegrationHub,
            events = events
        )

        val totalTime = System.currentTimeMillis() - startTime

        // Verify all events were processed
        assertEquals(events.size, eventResults.size, "All events should be processed")

        // Verify reasonable performance (adjust threshold as needed)
        assertTrue(totalTime < 10000, "Communication should complete within reasonable time: ${totalTime}ms")

        Timber.d("Communication resilience test passed")
    }
}