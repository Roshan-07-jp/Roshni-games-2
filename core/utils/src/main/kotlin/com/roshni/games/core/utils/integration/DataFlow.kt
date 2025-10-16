package com.roshni.games.core.utils.integration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages data flow between integrated components
 */
class DataFlowManager {

    private val _activeFlows = MutableStateFlow<Map<String, DataFlow>>(emptyMap())
    val activeFlows: StateFlow<Map<String, DataFlow>> = _activeFlows.asStateFlow()

    private val _flowMetrics = MutableStateFlow<DataFlowMetrics>(DataFlowMetrics())
    val flowMetrics: StateFlow<DataFlowMetrics> = _flowMetrics.asStateFlow()

    /**
     * Create a new data flow between components
     */
    fun createDataFlow(
        flowId: String,
        sourceComponent: String,
        targetComponent: String,
        direction: DataFlowDirection,
        dataTransformer: DataTransformer? = null,
        priority: EventPriority = EventPriority.NORMAL
    ): DataFlow {
        val dataFlow = DataFlow(
            id = flowId,
            sourceComponent = sourceComponent,
            targetComponent = targetComponent,
            direction = direction,
            transformer = dataTransformer,
            priority = priority,
            createdAt = System.currentTimeMillis()
        )

        _activeFlows.value = _activeFlows.value + (flowId to dataFlow)

        Timber.d("Created data flow: $flowId from $sourceComponent to $targetComponent")
        return dataFlow
    }

    /**
     * Remove a data flow
     */
    fun removeDataFlow(flowId: String): Boolean {
        val removed = _activeFlows.value.containsKey(flowId)
        if (removed) {
            _activeFlows.value = _activeFlows.value - flowId
            Timber.d("Removed data flow: $flowId")
        }
        return removed
    }

    /**
     * Get a specific data flow by ID
     */
    fun getDataFlow(flowId: String): DataFlow? {
        return _activeFlows.value[flowId]
    }

    /**
     * Get all data flows for a component
     */
    fun getDataFlowsForComponent(componentId: String): List<DataFlow> {
        return _activeFlows.value.values.filter { flow ->
            flow.sourceComponent == componentId || flow.targetComponent == componentId
        }
    }

    /**
     * Process data through a flow
     */
    suspend fun processData(
        flowId: String,
        data: Any,
        context: IntegrationContext
    ): DataFlowResult {
        val flow = getDataFlow(flowId) ?: return DataFlowResult.failure("Flow $flowId not found")

        return try {
            val startTime = System.currentTimeMillis()

            // Transform data if transformer is available
            val transformedData = flow.transformer?.transform(data, context) ?: data

            // Update metrics
            updateFlowMetrics(flowId, true, System.currentTimeMillis() - startTime)

            DataFlowResult.success(
                data = transformedData,
                flowId = flowId,
                processingTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            Timber.e(e, "Error processing data through flow $flowId")
            updateFlowMetrics(flowId, false, 0)
            DataFlowResult.failure("Processing failed: ${e.message}")
        }
    }

    /**
     * Update flow metrics
     */
    private fun updateFlowMetrics(flowId: String, success: Boolean, processingTimeMs: Long) {
        val currentMetrics = _flowMetrics.value
        val updatedMetrics = currentMetrics.copy(
            totalFlows = currentMetrics.totalFlows,
            activeFlows = _activeFlows.value.size,
            totalProcessed = currentMetrics.totalProcessed + 1,
            successfulProcessed = currentMetrics.successfulProcessed + if (success) 1 else 0,
            failedProcessed = currentMetrics.failedProcessed + if (success) 0 else 1,
            totalProcessingTimeMs = currentMetrics.totalProcessingTimeMs + processingTimeMs,
            averageProcessingTimeMs = if (currentMetrics.totalProcessed > 0) {
                (currentMetrics.totalProcessingTimeMs + processingTimeMs) / (currentMetrics.totalProcessed + 1)
            } else {
                processingTimeMs.toDouble()
            }
        )
        _flowMetrics.value = updatedMetrics
    }

    /**
     * Get flow statistics
     */
    fun getFlowStatistics(): Map<String, FlowStatistics> {
        return _activeFlows.value.mapValues { (_, flow) ->
            FlowStatistics(
                flowId = flow.id,
                totalExecutions = 0, // Would be tracked per flow
                successRate = 0.0,   // Would be calculated from execution history
                averageProcessingTimeMs = 0.0
            )
        }
    }

    /**
     * Clear all flows
     */
    fun clearAllFlows() {
        _activeFlows.value = emptyMap()
        _flowMetrics.value = DataFlowMetrics()
        Timber.d("Cleared all data flows")
    }
}

/**
 * Represents a data flow configuration
 */
data class DataFlow(
    val id: String,
    val sourceComponent: String,
    val targetComponent: String,
    val direction: DataFlowDirection,
    val transformer: DataTransformer?,
    val priority: EventPriority,
    val createdAt: Long,
    val isActive: Boolean = true,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Interface for data transformation
 */
interface DataTransformer {
    suspend fun transform(data: Any, context: IntegrationContext): Any
    fun getTransformerInfo(): TransformerInfo
}

/**
 * Information about a data transformer
 */
data class TransformerInfo(
    val name: String,
    val version: String,
    val description: String,
    val supportedTypes: List<String>
)

/**
 * Result of data flow processing
 */
data class DataFlowResult(
    val success: Boolean,
    val data: Any?,
    val flowId: String?,
    val processingTimeMs: Long,
    val error: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(
            data: Any,
            flowId: String,
            processingTimeMs: Long,
            metadata: Map<String, Any> = emptyMap()
        ): DataFlowResult {
            return DataFlowResult(
                success = true,
                data = data,
                flowId = flowId,
                processingTimeMs = processingTimeMs,
                metadata = metadata
            )
        }

        fun failure(error: String, flowId: String? = null): DataFlowResult {
            return DataFlowResult(
                success = false,
                data = null,
                flowId = flowId,
                processingTimeMs = 0,
                error = error
            )
        }
    }
}

/**
 * Metrics for data flow management
 */
data class DataFlowMetrics(
    val totalFlows: Int = 0,
    val activeFlows: Int = 0,
    val totalProcessed: Long = 0,
    val successfulProcessed: Long = 0,
    val failedProcessed: Long = 0,
    val totalProcessingTimeMs: Long = 0,
    val averageProcessingTimeMs: Double = 0.0
)

/**
 * Statistics for individual flows
 */
data class FlowStatistics(
    val flowId: String,
    val totalExecutions: Long,
    val successRate: Double,
    val averageProcessingTimeMs: Double
)