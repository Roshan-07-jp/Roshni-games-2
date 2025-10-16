package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.feature.CrossFeatureEvent
import com.roshni.games.core.utils.feature.FeatureContext
import com.roshni.games.core.utils.feature.FeatureResult
import com.roshni.games.core.utils.integration.DataFlowResult
import com.roshni.games.core.utils.integration.EventProcessingResult
import com.roshni.games.core.utils.integration.IntegrationContext
import com.roshni.games.core.utils.integration.IntegrationMetrics
import com.roshni.games.core.utils.integration.SystemIntegrationHub
import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleResult
import com.roshni.games.core.utils.workflow.WorkflowContext
import com.roshni.games.core.utils.workflow.WorkflowExecutionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Utility functions for integration testing
 */
object IntegrationTestUtils {

    /**
     * Wait for integration hub to reach a specific status
     */
    suspend fun waitForIntegrationHubStatus(
        hub: SystemIntegrationHub,
        targetStatus: IntegrationHubStatus,
        timeoutMs: Long = 5000
    ) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (hub.status.value == targetStatus) {
                Timber.d("Integration hub reached status: $targetStatus")
                return
            }
            delay(100)
        }

        throw AssertionError("Integration hub did not reach status $targetStatus within ${timeoutMs}ms")
    }

    /**
     * Wait for system health to become healthy
     */
    suspend fun waitForSystemHealth(
        hub: SystemIntegrationHub,
        timeoutMs: Long = 5000
    ) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val health = hub.systemHealth.value
            if (health.overallStatus == HealthStatus.HEALTHY) {
                Timber.d("System health is now healthy")
                return
            }
            delay(100)
        }

        throw AssertionError("System did not become healthy within ${timeoutMs}ms")
    }

    /**
     * Register multiple components and wait for them to be active
     */
    suspend fun registerComponentsAndWait(
        hub: SystemIntegrationHub,
        components: List<IntegratedComponent>,
        timeoutMs: Long = 5000
    ) {
        for (component in components) {
            val result = hub.registerComponent(component)
            assertTrue(result.isSuccess, "Failed to register component ${component.id}")
        }

        // Wait for all components to be active
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val metrics = hub.getIntegrationMetrics()
            if (metrics.activeComponents >= components.size) {
                Timber.d("All ${components.size} components are now active")
                return
            }
            delay(100)
        }

        throw AssertionError("Not all components became active within ${timeoutMs}ms")
    }

    /**
     * Create and register multiple integrations between components
     */
    suspend fun createIntegrations(
        hub: SystemIntegrationHub,
        sourceComponents: List<String>,
        targetComponents: List<String>,
        integrationType: com.roshni.games.core.utils.integration.IntegrationType = com.roshni.games.core.utils.integration.IntegrationType.EVENT_DRIVEN,
        configuration: com.roshni.games.core.utils.integration.IntegrationConfiguration = IntegrationTestDataFactory.createTestIntegrationConfiguration()
    ) {
        sourceComponents.forEachIndexed { index, source ->
            val target = targetComponents[index % targetComponents.size]
            val integrationId = "integration_${source}_${target}_${System.currentTimeMillis()}"

            val result = hub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = integrationType,
                configuration = configuration
            )

            assertTrue(result.isSuccess, "Failed to create integration $integrationId")
        }

        Timber.d("Created ${sourceComponents.size} integrations")
    }

    /**
     * Send events and verify they are processed correctly
     */
    suspend fun sendEventsAndVerify(
        hub: SystemIntegrationHub,
        events: List<CrossFeatureEvent>,
        expectedProcessedCount: Int? = null
    ): List<EventProcessingResult> {
        val results = mutableListOf<EventProcessingResult>()

        for (event in events) {
            val result = hub.sendEvent(event)
            assertTrue(result.isSuccess, "Failed to send event ${event.id}")
            results.add(result.getOrThrow())
        }

        if (expectedProcessedCount != null) {
            assertEquals(expectedProcessedCount, results.size, "Expected $expectedProcessedCount events to be processed")
        }

        Timber.d("Successfully sent and verified ${results.size} events")
        return results
    }

    /**
     * Process data through integration flows and verify results
     */
    suspend fun processDataAndVerify(
        hub: SystemIntegrationHub,
        sourceComponent: String,
        dataItems: List<Any>,
        context: IntegrationContext
    ): List<DataFlowResult> {
        val results = mutableListOf<DataFlowResult>()

        for (data in dataItems) {
            val result = hub.processData(sourceComponent, data, context)
            assertTrue(result.isSuccess, "Failed to process data from $sourceComponent")
            results.add(result.getOrThrow())
        }

        Timber.d("Successfully processed ${results.size} data items from $sourceComponent")
        return results
    }

    /**
     * Execute workflows and verify results
     */
    suspend fun executeWorkflowsAndVerify(
        hub: SystemIntegrationHub,
        workflowIds: List<String>,
        contexts: List<IntegrationContext>,
        inputDataList: List<Map<String, Any>> = emptyList()
    ): List<WorkflowExecutionResult> {
        val results = mutableListOf<WorkflowExecutionResult>()

        for (i in workflowIds.indices) {
            val workflowId = workflowIds[i]
            val context = contexts[i % contexts.size]
            val inputData = if (i < inputDataList.size) inputDataList[i] else emptyMap()

            val result = hub.executeWorkflow(workflowId, context, inputData)
            assertTrue(result.isSuccess, "Failed to execute workflow $workflowId")
            results.add(result.getOrThrow())
        }

        Timber.d("Successfully executed ${results.size} workflows")
        return results
    }

    /**
     * Synchronize state between components and verify
     */
    suspend fun synchronizeStateAndVerify(
        hub: SystemIntegrationHub,
        sourceComponent: String,
        targetComponent: String,
        stateUpdates: Map<String, Any>
    ) {
        for ((stateKey, stateValue) in stateUpdates) {
            val result = hub.synchronizeState(
                sourceComponent = sourceComponent,
                targetComponent = targetComponent,
                stateKey = stateKey,
                stateValue = stateValue
            )

            assertTrue(result.isSuccess, "Failed to synchronize state $stateKey between $sourceComponent and $targetComponent")
        }

        Timber.d("Successfully synchronized ${stateUpdates.size} state updates")
    }

    /**
     * Get integration metrics and perform basic validation
     */
    fun validateIntegrationMetrics(
        metrics: IntegrationMetrics,
        expectedMinComponents: Int = 1,
        expectedMinIntegrations: Int = 0
    ) {
        assertTrue(
            metrics.totalComponents >= expectedMinComponents,
            "Expected at least $expectedMinComponents components, but found ${metrics.totalComponents}"
        )

        assertTrue(
            metrics.totalIntegrations >= expectedMinIntegrations,
            "Expected at least $expectedMinIntegrations integrations, but found ${metrics.totalIntegrations}"
        )

        assertTrue(
            metrics.activeComponents <= metrics.totalComponents,
            "Active components (${metrics.activeComponents}) cannot exceed total components (${metrics.totalComponents})"
        )

        Timber.d("Integration metrics validation passed: $metrics")
    }

    /**
     * Wait for specific system events to occur
     */
    suspend fun waitForSystemEvents(
        hub: SystemIntegrationHub,
        eventCount: Int,
        timeoutMs: Long = 5000,
        eventFilter: ((com.roshni.games.core.utils.integration.SystemEvent) -> Boolean)? = null
    ): List<com.roshni.games.core.utils.integration.SystemEvent> {
        val startTime = System.currentTimeMillis()
        val events = mutableListOf<com.roshni.games.core.utils.integration.SystemEvent>()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentEvents = hub.observeSystemEvents()
                .take(100)
                .toList()

            val filteredEvents = if (eventFilter != null) {
                currentEvents.filter(eventFilter)
            } else {
                currentEvents
            }

            events.addAll(filteredEvents)

            if (events.size >= eventCount) {
                Timber.d("Collected ${events.size} system events")
                return events.take(eventCount)
            }

            delay(100)
        }

        Timber.d("Collected ${events.size} system events before timeout")
        return events
    }

    /**
     * Measure performance of integration operations
     */
    suspend fun measureIntegrationPerformance(
        hub: SystemIntegrationHub,
        operation: suspend () -> Unit,
        operationName: String,
        warmUpRuns: Int = 3,
        measuredRuns: Int = 10
    ): PerformanceMeasurement {
        val executionTimes = mutableListOf<Long>()

        // Warm-up runs
        repeat(warmUpRuns) {
            operation()
        }

        // Measured runs
        repeat(measuredRuns) {
            val startTime = System.nanoTime()
            operation()
            val endTime = System.nanoTime()
            executionTimes.add((endTime - startTime) / 1_000_000) // Convert to milliseconds
        }

        val averageTime = executionTimes.average()
        val minTime = executionTimes.minOrNull() ?: 0.0
        val maxTime = executionTimes.maxOrNull() ?: 0.0

        return PerformanceMeasurement(
            operationName = operationName,
            averageExecutionTimeMs = averageTime,
            minExecutionTimeMs = minTime,
            maxExecutionTimeMs = maxTime,
            executionCount = measuredRuns,
            standardDeviation = calculateStandardDeviation(executionTimes)
        )
    }

    /**
     * Create a comprehensive integration test report
     */
    fun createIntegrationTestReport(
        testName: String,
        metrics: IntegrationMetrics,
        performanceMeasurements: List<PerformanceMeasurement>,
        events: List<com.roshni.games.core.utils.integration.SystemEvent>,
        errors: List<String> = emptyList()
    ): IntegrationTestReport {
        return IntegrationTestReport(
            testName = testName,
            timestamp = System.currentTimeMillis(),
            metrics = metrics,
            performanceMeasurements = performanceMeasurements,
            systemEvents = events,
            errors = errors,
            summary = generateTestSummary(metrics, performanceMeasurements, events, errors)
        )
    }

    /**
     * Validate that all systems are properly coordinated
     */
    suspend fun validateSystemCoordination(
        hub: SystemIntegrationHub,
        expectedActiveComponents: Int,
        expectedIntegrations: Int,
        timeoutMs: Long = 5000
    ) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val metrics = hub.getIntegrationMetrics()
            val health = hub.systemHealth.value

            if (metrics.activeComponents >= expectedActiveComponents &&
                metrics.totalIntegrations >= expectedIntegrations &&
                health.overallStatus == HealthStatus.HEALTHY) {

                Timber.d("System coordination validated successfully")
                return
            }

            delay(100)
        }

        val finalMetrics = hub.getIntegrationMetrics()
        val finalHealth = hub.systemHealth.value

        throw AssertionError(
            "System coordination validation failed. " +
            "Expected active components: $expectedActiveComponents, actual: ${finalMetrics.activeComponents}. " +
            "Expected integrations: $expectedIntegrations, actual: ${finalMetrics.totalIntegrations}. " +
            "Health status: ${finalHealth.overallStatus}"
        )
    }

    /**
     * Calculate standard deviation for performance measurements
     */
    private fun calculateStandardDeviation(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0

        val average = values.average()
        val variance = values.sumOf { (it - average) * (it - average) } / values.size

        return kotlin.math.sqrt(variance)
    }

    /**
     * Generate a summary of the test results
     */
    private fun generateTestSummary(
        metrics: IntegrationMetrics,
        performanceMeasurements: List<PerformanceMeasurement>,
        events: List<com.roshni.games.core.utils.integration.SystemEvent>,
        errors: List<String>
    ): String {
        val sb = StringBuilder()

        sb.append("Integration Test Summary:\n")
        sb.append("- Components: ${metrics.totalComponents} total, ${metrics.activeComponents} active\n")
        sb.append("- Integrations: ${metrics.totalIntegrations}\n")
        sb.append("- Events processed: ${metrics.totalEventsProcessed}\n")
        sb.append("- Data flows processed: ${metrics.totalDataFlowsProcessed}\n")
        sb.append("- Workflows executed: ${metrics.totalWorkflowsExecuted}\n")

        if (performanceMeasurements.isNotEmpty()) {
            val avgPerformance = performanceMeasurements.map { it.averageExecutionTimeMs }.average()
            sb.append("- Average performance: ${"%.2f".format(avgPerformance)}ms\n")
        }

        sb.append("- System events: ${events.size}\n")
        sb.append("- Errors: ${errors.size}\n")

        return sb.toString()
    }
}

/**
 * Performance measurement data class
 */
data class PerformanceMeasurement(
    val operationName: String,
    val averageExecutionTimeMs: Double,
    val minExecutionTimeMs: Double,
    val maxExecutionTimeMs: Double,
    val executionCount: Int,
    val standardDeviation: Double
)

/**
 * Integration test report data class
 */
data class IntegrationTestReport(
    val testName: String,
    val timestamp: Long,
    val metrics: IntegrationMetrics,
    val performanceMeasurements: List<PerformanceMeasurement>,
    val systemEvents: List<com.roshni.games.core.utils.integration.SystemEvent>,
    val errors: List<String>,
    val summary: String
)