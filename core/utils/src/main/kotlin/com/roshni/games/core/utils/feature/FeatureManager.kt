package com.roshni.games.core.utils.feature

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
 * Core interface for the feature manager that manages feature registration,
 * lifecycle, dependencies, and execution across the gaming platform.
 */
interface FeatureManager {

    /**
     * Current status of the feature manager
     */
    val status: StateFlow<FeatureManagerStatus>

    /**
     * All registered features
     */
    val registeredFeatures: StateFlow<List<Feature>>

    /**
     * Features that are currently enabled
     */
    val enabledFeatures: StateFlow<List<Feature>>

    /**
     * Register a new feature with the manager
     *
     * @param feature The feature to register
     * @return true if registration was successful, false otherwise
     */
    suspend fun registerFeature(feature: Feature): Boolean

    /**
     * Unregister a feature from the manager
     *
     * @param featureId The ID of the feature to unregister
     * @return true if unregistration was successful, false otherwise
     */
    suspend fun unregisterFeature(featureId: String): Boolean

    /**
     * Get a registered feature by its ID
     *
     * @param featureId The ID of the feature to retrieve
     * @return The feature if found, null otherwise
     */
    suspend fun getFeature(featureId: String): Feature?

    /**
     * Get all registered features
     *
     * @return List of all registered features
     */
    suspend fun getAllFeatures(): List<Feature>

    /**
     * Get features by category
     *
     * @param category The category to filter by
     * @return List of features in the specified category
     */
    suspend fun getFeaturesByCategory(category: FeatureCategory): List<Feature>

    /**
     * Get features by tags
     *
     * @param tags The tags to filter by
     * @return List of features that have any of the specified tags
     */
    suspend fun getFeaturesByTags(tags: List<String>): List<Feature>

    /**
     * Initialize the feature manager and all registered features
     *
     * @param context Initial context for feature manager initialization
     * @return true if initialization was successful, false otherwise
     */
    suspend fun initialize(context: FeatureManagerContext): Boolean

    /**
     * Enable a feature
     *
     * @param featureId The ID of the feature to enable
     * @param context Context for feature enablement
     * @return true if enablement was successful, false otherwise
     */
    suspend fun enableFeature(featureId: String, context: FeatureContext): Boolean

    /**
     * Disable a feature
     *
     * @param featureId The ID of the feature to disable
     * @param context Context for feature disablement
     * @return true if disablement was successful, false otherwise
     */
    suspend fun disableFeature(featureId: String, context: FeatureContext): Boolean

    /**
     * Execute a feature
     *
     * @param featureId The ID of the feature to execute
     * @param context Context for feature execution
     * @return The result of feature execution
     */
    suspend fun executeFeature(featureId: String, context: FeatureContext): FeatureResult

    /**
     * Execute multiple features in dependency order
     *
     * @param featureIds The IDs of the features to execute
     * @param context Context for feature execution
     * @return List of feature execution results
     */
    suspend fun executeFeatures(featureIds: List<String>, context: FeatureContext): List<FeatureResult>

    /**
     * Check if a feature can be enabled based on dependencies and rules
     *
     * @param featureId The ID of the feature to check
     * @param context Context for the check
     * @return true if the feature can be enabled, false otherwise
     */
    suspend fun canEnableFeature(featureId: String, context: FeatureContext): Boolean

    /**
     * Get features that depend on the specified feature
     *
     * @param featureId The ID of the feature to check dependencies for
     * @return List of features that depend on the specified feature
     */
    suspend fun getDependentFeatures(featureId: String): List<Feature>

    /**
     * Resolve and enable features based on dependencies
     *
     * @param featureIds The IDs of the features to resolve and enable
     * @param context Context for feature resolution
     * @return List of features that were successfully enabled
     */
    suspend fun resolveAndEnableFeatures(featureIds: List<String>, context: FeatureContext): List<Feature>

    /**
     * Validate all registered features
     *
     * @return Validation result containing any errors or warnings
     */
    suspend fun validateAllFeatures(): FeatureManagerValidationResult

    /**
     * Get feature execution statistics
     *
     * @param featureId The ID of the feature to get statistics for (null for all features)
     * @return Feature execution statistics
     */
    suspend fun getFeatureStatistics(featureId: String? = null): FeatureStatistics

    /**
     * Clear feature execution statistics
     */
    suspend fun clearStatistics()

    /**
     * Observe feature state changes
     *
     * @param featureId Optional feature ID to filter (null for all features)
     * @return Flow of feature state changes
     */
    fun observeFeatureState(featureId: String? = null): Flow<FeatureStateChange>

    /**
     * Observe feature execution results
     *
     * @param featureId Optional feature ID to filter (null for all features)
     * @return Flow of feature execution results
     */
    fun observeFeatureResults(featureId: String? = null): Flow<FeatureResult>

    /**
     * Send an event to a specific feature
     *
     * @param featureId The ID of the feature to send the event to
     * @param event The event to send
     * @param context Context for event handling
     * @return true if the event was handled successfully, false otherwise
     */
    suspend fun sendEventToFeature(featureId: String, event: FeatureEvent, context: FeatureContext): Boolean

    /**
     * Send an event to all registered features
     *
     * @param event The event to send
     * @param context Context for event handling
     * @return List of features that handled the event successfully
     */
    suspend fun broadcastEvent(event: FeatureEvent, context: FeatureContext): List<String>

    /**
     * Export features configuration for backup or migration
     *
     * @return Exported features configuration as a map
     */
    suspend fun exportFeatures(): Map<String, Any>

    /**
     * Import features configuration
     *
     * @param configuration The configuration to import
     * @return true if import was successful, false otherwise
     */
    suspend fun importFeatures(configuration: Map<String, Any>): Boolean

    /**
     * Shutdown the feature manager and cleanup all features
     */
    suspend fun shutdown()
}

/**
 * Context for feature manager operations
 */
data class FeatureManagerContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val ruleEngine: RuleEngine? = null,
    val workflowEngine: WorkflowEngine? = null,
    val globalVariables: Map<String, Any> = emptyMap()
)

/**
 * Status of the feature manager
 */
data class FeatureManagerStatus(
    val isInitialized: Boolean = false,
    val registeredFeatureCount: Int = 0,
    val enabledFeatureCount: Int = 0,
    val errorCount: Long = 0,
    val lastActivityTime: Long? = null,
    val isShuttingDown: Boolean = false
)

/**
 * Result of feature state change observation
 */
data class FeatureStateChange(
    val featureId: String,
    val oldState: FeatureState,
    val newState: FeatureState,
    val timestamp: Long
)

/**
 * Statistics about feature execution performance and results
 */
data class FeatureStatistics(
    val totalExecutions: Long = 0,
    val successfulExecutions: Long = 0,
    val failedExecutions: Long = 0,
    val averageExecutionTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val lastExecutionTime: Long? = null,
    val featuresExecuted: Map<String, Long> = emptyMap(), // featureId -> execution count
    val categoryStatistics: Map<FeatureCategory, CategoryStats> = emptyMap()
) {

    data class CategoryStats(
        val executionCount: Long = 0,
        val averageExecutionTimeMs: Double = 0.0,
        val successRate: Double = 0.0
    )
}

/**
 * Result of feature manager validation
 */
data class FeatureManagerValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val featureValidationResults: Map<String, FeatureValidationResult> = emptyMap()
)

/**
 * Default implementation of the FeatureManager interface
 */
class FeatureManagerImpl(
    private val ruleEngine: RuleEngine,
    private val workflowEngine: WorkflowEngine
) : FeatureManager {

    private val mutex = Mutex()
    private val _status = MutableStateFlow(FeatureManagerStatus())
    private val _registeredFeatures = MutableStateFlow<List<Feature>>(emptyList())
    private val _enabledFeatures = MutableStateFlow<List<Feature>>(emptyList())

    private val featureStateChanges = MutableStateFlow<List<FeatureStateChange>>(emptyList())
    private val featureResults = MutableStateFlow<List<FeatureResult>>(emptyList())

    private val executionStatistics = mutableMapOf<String, MutableList<Long>>()
    private var totalExecutions = 0L
    private var successfulExecutions = 0L
    private var failedExecutions = 0L

    override val status: StateFlow<FeatureManagerStatus> = _status.asStateFlow()
    override val registeredFeatures: StateFlow<List<Feature>> = _registeredFeatures.asStateFlow()
    override val enabledFeatures: StateFlow<List<Feature>> = _enabledFeatures.asStateFlow()

    override suspend fun registerFeature(feature: Feature): Boolean = mutex.withLock {
        try {
            // Check if feature is already registered
            if (_registeredFeatures.value.any { it.id == feature.id }) {
                Timber.w("Feature ${feature.id} is already registered")
                return false
            }

            // Validate feature before registration
            val validation = feature.validate()
            if (!validation.isValid) {
                Timber.e("Feature ${feature.id} validation failed: ${validation.errors}")
                return false
            }

            // Create initial context for feature
            val context = FeatureContext(
                featureId = feature.id,
                executionId = generateExecutionId(),
                ruleContext = com.roshni.games.core.utils.rules.RuleContext()
            )

            // Initialize feature if it's enabled by default
            if (feature.config.enabledByDefault) {
                val initSuccess = feature.initialize(context)
                if (!initSuccess) {
                    Timber.e("Failed to initialize feature ${feature.id}")
                    return false
                }

                if (feature.enabled) {
                    val enableSuccess = feature.enable(context)
                    if (!enableSuccess) {
                        Timber.e("Failed to enable feature ${feature.id}")
                        return false
                    }
                }
            }

            // Add to registered features
            val updatedFeatures = _registeredFeatures.value + feature
            _registeredFeatures.value = updatedFeatures

            // Update enabled features if necessary
            if (feature.enabled) {
                _enabledFeatures.value = _enabledFeatures.value + feature
            }

            // Update status
            updateStatus()

            Timber.d("Feature ${feature.id} registered successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to register feature ${feature.id}")
            false
        }
    }

    override suspend fun unregisterFeature(featureId: String): Boolean = mutex.withLock {
        try {
            val feature = getFeature(featureId) ?: return false

            // Check for dependent features
            val dependentFeatures = getDependentFeatures(featureId)
            if (dependentFeatures.isNotEmpty()) {
                Timber.w("Cannot unregister feature $featureId - it has dependent features: ${dependentFeatures.map { it.id }}")
                return false
            }

            // Cleanup feature
            feature.cleanup()

            // Remove from registered features
            val updatedFeatures = _registeredFeatures.value.filter { it.id != featureId }
            _registeredFeatures.value = updatedFeatures

            // Remove from enabled features if necessary
            val updatedEnabledFeatures = _enabledFeatures.value.filter { it.id != featureId }
            _enabledFeatures.value = updatedEnabledFeatures

            // Update status
            updateStatus()

            Timber.d("Feature $featureId unregistered successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister feature $featureId")
            false
        }
    }

    override suspend fun getFeature(featureId: String): Feature? {
        return _registeredFeatures.value.find { it.id == featureId }
    }

    override suspend fun getAllFeatures(): List<Feature> {
        return _registeredFeatures.value
    }

    override suspend fun getFeaturesByCategory(category: FeatureCategory): List<Feature> {
        return _registeredFeatures.value.filter { it.category == category }
    }

    override suspend fun getFeaturesByTags(tags: List<String>): List<Feature> {
        return _registeredFeatures.value.filter { feature ->
            tags.any { tag -> feature.tags.contains(tag) }
        }
    }

    override suspend fun initialize(context: FeatureManagerContext): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing FeatureManager")

            _status.value = _status.value.copy(isInitialized = false)

            // Initialize all registered features
            val featuresToInitialize = _registeredFeatures.value.filter { it.state.value == FeatureState.UNINITIALIZED }
            var allInitialized = true

            for (feature in featuresToInitialize) {
                val featureContext = FeatureContext(
                    featureId = feature.id,
                    executionId = generateExecutionId(),
                    userId = context.userId,
                    ruleContext = context.ruleEngine?.let { com.roshni.games.core.utils.rules.RuleContext() }
                )

                val initialized = feature.initialize(featureContext)
                if (!initialized) {
                    Timber.e("Failed to initialize feature ${feature.id}")
                    allInitialized = false
                }
            }

            if (allInitialized) {
                _status.value = _status.value.copy(isInitialized = true)
                Timber.d("FeatureManager initialized successfully")
                true
            } else {
                Timber.e("FeatureManager initialization failed")
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize FeatureManager")
            false
        }
    }

    override suspend fun enableFeature(featureId: String, context: FeatureContext): Boolean = mutex.withLock {
        try {
            val feature = getFeature(featureId) ?: return false

            // Check if feature can be enabled
            if (!canEnableFeature(featureId, context)) {
                Timber.w("Feature $featureId cannot be enabled")
                return false
            }

            // Enable dependencies first
            val dependenciesEnabled = enableDependencies(feature, context)
            if (!dependenciesEnabled) {
                Timber.e("Failed to enable dependencies for feature $featureId")
                return false
            }

            // Enable the feature
            val enabled = feature.enable(context)
            if (enabled) {
                // Update enabled features list
                if (feature !in _enabledFeatures.value) {
                    _enabledFeatures.value = _enabledFeatures.value + feature
                }

                updateStatus()
                Timber.d("Feature $featureId enabled successfully")
            }

            enabled

        } catch (e: Exception) {
            Timber.e(e, "Failed to enable feature $featureId")
            false
        }
    }

    override suspend fun disableFeature(featureId: String, context: FeatureContext): Boolean = mutex.withLock {
        try {
            val feature = getFeature(featureId) ?: return false

            // Check for dependent features that would be affected
            val dependentFeatures = getDependentFeatures(featureId)
            val enabledDependents = dependentFeatures.filter { it.enabled }

            if (enabledDependents.isNotEmpty()) {
                Timber.w("Disabling feature $featureId will affect dependent features: ${enabledDependents.map { it.id }}")
                // Could optionally disable dependents first, but for now we'll just warn
            }

            // Disable the feature
            val disabled = feature.disable(context)
            if (disabled) {
                // Update enabled features list
                _enabledFeatures.value = _enabledFeatures.value.filter { it.id != featureId }

                updateStatus()
                Timber.d("Feature $featureId disabled successfully")
            }

            disabled

        } catch (e: Exception) {
            Timber.e(e, "Failed to disable feature $featureId")
            false
        }
    }

    override suspend fun executeFeature(featureId: String, context: FeatureContext): FeatureResult = mutex.withLock {
        try {
            val feature = getFeature(featureId) ?: return FeatureResult(
                success = false,
                errors = listOf("Feature $featureId not found"),
                executionTimeMs = 0
            )

            // Record execution start
            val executionId = context.executionId
            val startTime = System.currentTimeMillis()

            // Execute the feature
            val result = feature.execute(context)

            // Record execution statistics
            val executionTime = System.currentTimeMillis() - startTime
            recordExecution(featureId, result.success, executionTime)

            // Store result for observation
            val updatedResults = featureResults.value + result
            featureResults.value = updatedResults

            Timber.d("Feature $featureId executed with result: ${result.success}")
            result

        } catch (e: Exception) {
            val result = FeatureResult(
                success = false,
                errors = listOf("Feature execution failed: ${e.message}"),
                executionTimeMs = System.currentTimeMillis() - System.currentTimeMillis()
            )

            Timber.e(e, "Failed to execute feature $featureId")
            result
        }
    }

    override suspend fun executeFeatures(featureIds: List<String>, context: FeatureContext): List<FeatureResult> {
        // Sort features by dependency order
        val sortedFeatureIds = sortFeaturesByDependencies(featureIds)

        val results = mutableListOf<FeatureResult>()
        for (featureId in sortedFeatureIds) {
            val result = executeFeature(featureId, context.copy(featureId = featureId))
            results.add(result)

            // If a feature failed and it's not optional, stop execution
            if (!result.success) {
                val feature = getFeature(featureId)
                val isOptional = feature?.dependencies?.any { it.optional } ?: false
                if (!isOptional) {
                    Timber.w("Stopping feature execution due to failure of required feature $featureId")
                    break
                }
            }
        }

        return results
    }

    override suspend fun canEnableFeature(featureId: String, context: FeatureContext): Boolean {
        val feature = getFeature(featureId) ?: return false

        // Check if feature dependencies are satisfied
        for (dependency in feature.dependencies) {
            if (dependency.optional) continue

            val dependentFeature = getFeature(dependency.featureId) ?: return false
            if (dependentFeature.state.value != dependency.requiredState) {
                return false
            }
        }

        // Check with rule engine if available
        if (context.ruleContext != null) {
            try {
                val ruleResult = ruleEngine.evaluateRule(
                    "feature_enablement_${featureId}",
                    context.ruleContext
                )
                if (!ruleResult.success) {
                    return false
                }
            } catch (e: Exception) {
                Timber.w(e, "Rule engine evaluation failed for feature $featureId")
            }
        }

        return true
    }

    override suspend fun getDependentFeatures(featureId: String): List<Feature> {
        return _registeredFeatures.value.filter { feature ->
            feature.dependencies.any { it.featureId == featureId }
        }
    }

    override suspend fun resolveAndEnableFeatures(featureIds: List<String>, context: FeatureContext): List<Feature> {
        val resolvedFeatures = mutableListOf<Feature>()
        val featuresToProcess = sortFeaturesByDependencies(featureIds).toMutableList()

        while (featuresToProcess.isNotEmpty()) {
            val featureId = featuresToProcess.first()
            val feature = getFeature(featureId) ?: continue

            val canEnable = canEnableFeature(featureId, context)
            if (canEnable) {
                val enabled = enableFeature(featureId, context)
                if (enabled) {
                    resolvedFeatures.add(feature)
                }
                featuresToProcess.remove(featureId)
            } else {
                // Check if all dependencies are satisfied
                val unsatisfiedDependencies = feature.dependencies.filter { dependency ->
                    val dependentFeature = getFeature(dependency.featureId)
                    dependentFeature?.state.value != dependency.requiredState
                }

                if (unsatisfiedDependencies.isEmpty()) {
                    // Dependencies are satisfied but rule engine blocked it
                    featuresToProcess.remove(featureId)
                } else {
                    // Move to end of list to try again later
                    featuresToProcess.remove(featureId)
                    featuresToProcess.add(featureId)
                }
            }
        }

        return resolvedFeatures
    }

    override suspend fun validateAllFeatures(): FeatureManagerValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val featureValidationResults = mutableMapOf<String, FeatureValidationResult>()

        for (feature in _registeredFeatures.value) {
            try {
                val validation = feature.validate()
                featureValidationResults[feature.id] = validation

                if (!validation.isValid) {
                    errors.add("Feature ${feature.id} validation failed: ${validation.errors}")
                }
                warnings.addAll(validation.warnings)

            } catch (e: Exception) {
                errors.add("Feature ${feature.id} validation threw exception: ${e.message}")
            }
        }

        return FeatureManagerValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            featureValidationResults = featureValidationResults
        )
    }

    override suspend fun getFeatureStatistics(featureId: String?): FeatureStatistics {
        return if (featureId != null) {
            // Get statistics for specific feature
            val executionTimes = executionStatistics[featureId] ?: emptyList()
            val executions = executionTimes.size.toLong()
            val successful = executionTimes.count { it > 0 } // Assuming positive time means success
            val failed = executions - successful
            val averageTime = if (executionTimes.isNotEmpty()) {
                executionTimes.average()
            } else 0.0

            FeatureStatistics(
                totalExecutions = executions,
                successfulExecutions = successful,
                failedExecutions = failed,
                averageExecutionTimeMs = averageTime,
                totalExecutionTimeMs = executionTimes.sum(),
                lastExecutionTime = executionTimes.lastOrNull()?.let { System.currentTimeMillis() - it },
                featuresExecuted = mapOf(featureId to executions)
            )
        } else {
            // Get statistics for all features
            val categoryStats = mutableMapOf<FeatureCategory, FeatureStatistics.CategoryStats>()

            _registeredFeatures.value.groupBy { it.category }.forEach { (category, features) ->
                val categoryExecutions = features.sumOf { executionStatistics[it.id]?.size ?: 0 }.toLong()
                val categoryExecutionTimes = features.flatMap { executionStatistics[it.id] ?: emptyList() }
                val categorySuccessful = categoryExecutionTimes.count { it > 0 }

                categoryStats[category] = FeatureStatistics.CategoryStats(
                    executionCount = categoryExecutions,
                    averageExecutionTimeMs = if (categoryExecutionTimes.isNotEmpty()) {
                        categoryExecutionTimes.average()
                    } else 0.0,
                    successRate = if (categoryExecutions > 0) {
                        categorySuccessful.toDouble() / categoryExecutions
                    } else 0.0
                )
            }

            FeatureStatistics(
                totalExecutions = totalExecutions,
                successfulExecutions = successfulExecutions,
                failedExecutions = failedExecutions,
                averageExecutionTimeMs = if (executionStatistics.values.flatten().isNotEmpty()) {
                    executionStatistics.values.flatten().average()
                } else 0.0,
                totalExecutionTimeMs = executionStatistics.values.flatten().sum(),
                featuresExecuted = executionStatistics.mapValues { it.value.size.toLong() },
                categoryStatistics = categoryStats
            )
        }
    }

    override suspend fun clearStatistics() {
        executionStatistics.clear()
        totalExecutions = 0
        successfulExecutions = 0
        failedExecutions = 0
    }

    override fun observeFeatureState(featureId: String?): Flow<FeatureStateChange> = flow {
        // This would be implemented with proper state observation
        // For now, return empty flow
    }

    override fun observeFeatureResults(featureId: String?): Flow<FeatureResult> = flow {
        // This would be implemented with proper result observation
        // For now, return empty flow
    }

    override suspend fun sendEventToFeature(featureId: String, event: FeatureEvent, context: FeatureContext): Boolean {
        val feature = getFeature(featureId) ?: return false
        return feature.handleEvent(event, context)
    }

    override suspend fun broadcastEvent(event: FeatureEvent, context: FeatureContext): List<String> {
        val successfulFeatures = mutableListOf<String>()

        for (feature in _registeredFeatures.value) {
            try {
                val handled = feature.handleEvent(event, context.copy(featureId = feature.id))
                if (handled) {
                    successfulFeatures.add(feature.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Feature ${feature.id} failed to handle broadcast event")
            }
        }

        return successfulFeatures
    }

    override suspend fun exportFeatures(): Map<String, Any> {
        return mapOf(
            "features" to _registeredFeatures.value.map { feature ->
                mapOf(
                    "id" to feature.id,
                    "name" to feature.name,
                    "category" to feature.category.name,
                    "enabled" to feature.enabled,
                    "version" to feature.version,
                    "config" to mapOf(
                        "timeoutMs" to feature.config.timeoutMs,
                        "retryCount" to feature.config.retryCount,
                        "enabledByDefault" to feature.config.enabledByDefault
                    )
                )
            },
            "statistics" to getFeatureStatistics(),
            "exportedAt" to System.currentTimeMillis()
        )
    }

    override suspend fun importFeatures(configuration: Map<String, Any>): Boolean {
        try {
            // This would implement feature import logic
            // For now, return true as placeholder
            Timber.d("Feature import completed")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to import features")
            false
        }
    }

    override suspend fun shutdown() {
        try {
            Timber.d("Shutting down FeatureManager")

            _status.value = _status.value.copy(isShuttingDown = true)

            // Cleanup all features
            for (feature in _registeredFeatures.value) {
                try {
                    feature.cleanup()
                } catch (e: Exception) {
                    Timber.e(e, "Error cleaning up feature ${feature.id}")
                }
            }

            _registeredFeatures.value = emptyList()
            _enabledFeatures.value = emptyList()
            _status.value = FeatureManagerStatus()

            Timber.d("FeatureManager shutdown complete")

        } catch (e: Exception) {
            Timber.e(e, "Error during FeatureManager shutdown")
        }
    }

    /**
     * Enable dependencies for a feature
     */
    private suspend fun enableDependencies(feature: Feature, context: FeatureContext): Boolean {
        for (dependency in feature.dependencies) {
            if (dependency.optional) continue

            val dependentFeature = getFeature(dependency.featureId) ?: return false
            if (dependentFeature.state.value != dependency.requiredState) {
                val enabled = enableFeature(dependency.featureId, context)
                if (!enabled) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Sort features by dependency order (topological sort)
     */
    private fun sortFeaturesByDependencies(featureIds: List<String>): List<String> {
        val features = featureIds.mapNotNull { getFeature(it) }
        val sorted = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(featureId: String) {
            if (featureId in visiting) {
                throw IllegalStateException("Circular dependency detected involving feature $featureId")
            }
            if (featureId in visited) return

            visiting.add(featureId)

            val feature = getFeature(featureId) ?: return
            for (dependency in feature.dependencies) {
                if (dependency.featureId in featureIds) {
                    visit(dependency.featureId)
                }
            }

            visiting.remove(featureId)
            visited.add(featureId)
            sorted.add(featureId)
        }

        for (featureId in featureIds) {
            if (featureId !in visited) {
                visit(featureId)
            }
        }

        return sorted
    }

    /**
     * Record feature execution statistics
     */
    private fun recordExecution(featureId: String, success: Boolean, executionTimeMs: Long) {
        if (executionStatistics[featureId] == null) {
            executionStatistics[featureId] = mutableListOf()
        }

        executionStatistics[featureId]?.add(executionTimeMs)
        totalExecutions++

        if (success) {
            successfulExecutions++
        } else {
            failedExecutions++
        }
    }

    /**
     * Update manager status
     */
    private fun updateStatus() {
        _status.value = _status.value.copy(
            registeredFeatureCount = _registeredFeatures.value.size,
            enabledFeatureCount = _enabledFeatures.value.size,
            lastActivityTime = System.currentTimeMillis()
        )
    }

    /**
     * Generate a unique execution ID
     */
    private fun generateExecutionId(): String {
        return "feature_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
    }
}