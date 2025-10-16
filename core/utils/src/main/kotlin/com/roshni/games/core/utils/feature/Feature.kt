package com.roshni.games.core.utils.feature

import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface representing a feature in the gaming platform
 */
interface Feature {

    /**
     * Unique identifier for this feature
     */
    val id: String

    /**
     * Human-readable name for this feature
     */
    val name: String

    /**
     * Detailed description of what this feature does
     */
    val description: String

    /**
     * The category/type of this feature
     */
    val category: FeatureCategory

    /**
     * Version number for tracking feature changes
     */
    val version: Int

    /**
     * Whether this feature is currently enabled
     */
    val enabled: Boolean

    /**
     * Current state of the feature
     */
    val state: StateFlow<FeatureState>

    /**
     * Feature dependencies that must be satisfied
     */
    val dependencies: List<FeatureDependency>

    /**
     * Tags for organizing and filtering features
     */
    val tags: List<String>

    /**
     * Configuration for this feature
     */
    val config: FeatureConfig

    /**
     * Creation timestamp
     */
    val createdAt: Long

    /**
     * Last modification timestamp
     */
    val modifiedAt: Long

    /**
     * Initialize the feature
     *
     * @param context The context for feature initialization
     * @return true if initialization was successful, false otherwise
     */
    suspend fun initialize(context: FeatureContext): Boolean

    /**
     * Enable the feature
     *
     * @param context The context for feature enablement
     * @return true if enablement was successful, false otherwise
     */
    suspend fun enable(context: FeatureContext): Boolean

    /**
     * Disable the feature
     *
     * @param context The context for feature disablement
     * @return true if disablement was successful, false otherwise
     */
    suspend fun disable(context: FeatureContext): Boolean

    /**
     * Execute the feature's main logic
     *
     * @param context The context for feature execution
     * @return The result of feature execution
     */
    suspend fun execute(context: FeatureContext): FeatureResult

    /**
     * Validate that this feature is properly configured
     *
     * @return Validation result containing any errors or warnings
     */
    suspend fun validate(): FeatureValidationResult

    /**
     * Get metadata about this feature for debugging and monitoring
     *
     * @return Feature metadata as a map
     */
    suspend fun getMetadata(): Map<String, Any>

    /**
     * Check if this feature can be executed with the given context
     *
     * @param context The context to check
     * @return true if the feature can be executed, false otherwise
     */
    suspend fun canExecute(context: FeatureContext): Boolean

    /**
     * Get the estimated execution time for this feature
     *
     * @return Estimated execution time in milliseconds, null if unknown
     */
    fun getEstimatedExecutionTimeMs(): Long?

    /**
     * Handle feature-specific events
     *
     * @param event The event to handle
     * @param context The context for event handling
     * @return true if the event was handled successfully, false otherwise
     */
    suspend fun handleEvent(event: FeatureEvent, context: FeatureContext): Boolean

    /**
     * Cleanup resources when feature is being destroyed
     */
    suspend fun cleanup()

    /**
     * Reset the feature to its initial state
     *
     * @param context The context for reset operation
     * @return true if reset was successful, false otherwise
     */
    suspend fun reset(context: FeatureContext): Boolean
}

/**
 * Categories of features in the gaming platform
 */
enum class FeatureCategory {
    GAMEPLAY,
    SOCIAL,
    ACCESSIBILITY,
    PARENTAL_CONTROLS,
    PERFORMANCE,
    ANALYTICS,
    SECURITY,
    UI,
    SYSTEM,
    INTEGRATION
}

/**
 * Current state of a feature
 */
enum class FeatureState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    ENABLING,
    ENABLED,
    DISABLING,
    DISABLED,
    ERROR,
    DEPENDENCY_MISSING,
    CONFIGURATION_INVALID
}

/**
 * Dependency information for a feature
 */
data class FeatureDependency(
    val featureId: String,
    val minVersion: Int? = null,
    val requiredState: FeatureState = FeatureState.ENABLED,
    val optional: Boolean = false
)

/**
 * Configuration for a feature
 */
data class FeatureConfig(
    val properties: Map<String, Any> = emptyMap(),
    val timeoutMs: Long? = null,
    val retryCount: Int = 3,
    val enabledByDefault: Boolean = false,
    val requiresUserConsent: Boolean = false,
    val permissions: List<String> = emptyList()
)

/**
 * Context for feature execution
 */
data class FeatureContext(
    val featureId: String,
    val executionId: String,
    val userId: String? = null,
    val sessionId: String? = null,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val ruleContext: com.roshni.games.core.utils.rules.RuleContext? = null,
    val workflowContext: com.roshni.games.core.utils.workflow.WorkflowContext? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * Result of feature execution
 */
data class FeatureResult(
    val success: Boolean,
    val data: Map<String, Any> = emptyMap(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of feature validation
 */
data class FeatureValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val dependencyValidationResults: Map<String, FeatureValidationResult> = emptyMap()
)

/**
 * Events that can be sent to features
 */
sealed class FeatureEvent {
    data class UserAction(val action: String, val data: Map<String, Any> = emptyMap()) : FeatureEvent()
    data class SystemEvent(val eventType: String, val data: Map<String, Any> = emptyMap()) : FeatureEvent()
    data class ConfigurationChange(val changes: Map<String, Any>) : FeatureEvent()
    data class DependencyStateChange(val featureId: String, val newState: FeatureState) : FeatureEvent()
    object LifecycleEvent : FeatureEvent()
}

/**
 * Base implementation of the Feature interface
 */
abstract class BaseFeature : Feature {

    final override val state = kotlinx.coroutines.flow.MutableStateFlow(FeatureState.UNINITIALIZED)

    protected abstract val featureDependencies: List<FeatureDependency>
    protected abstract val featureTags: List<String>
    protected abstract val featureConfig: FeatureConfig

    final override val dependencies: List<FeatureDependency>
        get() = featureDependencies

    final override val tags: List<String>
        get() = featureTags

    final override val config: FeatureConfig
        get() = featureConfig

    override suspend fun initialize(context: FeatureContext): Boolean {
        return try {
            state.value = FeatureState.INITIALIZING

            // Validate dependencies
            val dependencyValidation = validateDependencies()
            if (!dependencyValidation.isValid) {
                state.value = FeatureState.DEPENDENCY_MISSING
                return false
            }

            // Validate configuration
            val configValidation = validateConfiguration()
            if (!configValidation.isValid) {
                state.value = FeatureState.CONFIGURATION_INVALID
                return false
            }

            // Perform feature-specific initialization
            val initSuccess = performInitialization(context)

            if (initSuccess) {
                state.value = if (enabled) FeatureState.ENABLED else FeatureState.READY
                true
            } else {
                state.value = FeatureState.ERROR
                false
            }

        } catch (e: Exception) {
            state.value = FeatureState.ERROR
            false
        }
    }

    override suspend fun enable(context: FeatureContext): Boolean {
        return try {
            if (state.value == FeatureState.ENABLED) return true

            state.value = FeatureState.ENABLING

            // Check if we can enable this feature
            if (!canEnable(context)) {
                state.value = FeatureState.ERROR
                return false
            }

            // Perform feature-specific enable logic
            val enableSuccess = performEnable(context)

            if (enableSuccess) {
                state.value = FeatureState.ENABLED
                true
            } else {
                state.value = FeatureState.ERROR
                false
            }

        } catch (e: Exception) {
            state.value = FeatureState.ERROR
            false
        }
    }

    override suspend fun disable(context: FeatureContext): Boolean {
        return try {
            if (state.value == FeatureState.DISABLED) return true

            state.value = FeatureState.DISABLING

            // Perform feature-specific disable logic
            val disableSuccess = performDisable(context)

            if (disableSuccess) {
                state.value = FeatureState.DISABLED
                true
            } else {
                state.value = FeatureState.ERROR
                false
            }

        } catch (e: Exception) {
            state.value = FeatureState.ERROR
            false
        }
    }

    override suspend fun execute(context: FeatureContext): FeatureResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Check if feature can be executed
            if (!canExecute(context)) {
                return FeatureResult(
                    success = false,
                    errors = listOf("Feature cannot be executed in current state: ${state.value}"),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Perform feature-specific execution
            val result = performExecute(context)

            FeatureResult(
                success = result.success,
                data = result.data,
                errors = result.errors,
                warnings = result.warnings,
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            FeatureResult(
                success = false,
                errors = listOf("Feature execution failed: ${e.message}"),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun validate(): FeatureValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Basic validation
        if (id.isBlank()) errors.add("Feature ID cannot be blank")
        if (name.isBlank()) errors.add("Feature name cannot be blank")
        if (version < 1) errors.add("Feature version must be >= 1")

        // Validate dependencies
        val dependencyValidation = validateDependencies()
        if (!dependencyValidation.isValid) {
            errors.addAll(dependencyValidation.errors)
        }
        warnings.addAll(dependencyValidation.warnings)

        // Validate configuration
        val configValidation = validateConfiguration()
        if (!configValidation.isValid) {
            errors.addAll(configValidation.errors)
        }
        warnings.addAll(configValidation.warnings)

        return FeatureValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    override suspend fun getMetadata(): Map<String, Any> {
        return mapOf(
            "state" to state.value.name,
            "enabled" to enabled,
            "dependencyCount" to dependencies.size,
            "tagCount" to tags.size,
            "estimatedExecutionTimeMs" to (getEstimatedExecutionTimeMs() ?: 0)
        )
    }

    override suspend fun canExecute(context: FeatureContext): Boolean {
        return state.value == FeatureState.ENABLED &&
               validateDependencies().isValid &&
               validateConfiguration().isValid
    }

    override fun getEstimatedExecutionTimeMs(): Long? {
        // Default implementation - can be overridden by subclasses
        return config.timeoutMs
    }

    override suspend fun handleEvent(event: FeatureEvent, context: FeatureContext): Boolean {
        return when (event) {
            is FeatureEvent.ConfigurationChange -> handleConfigurationChange(event.changes, context)
            is FeatureEvent.DependencyStateChange -> handleDependencyStateChange(event.featureId, event.newState, context)
            is FeatureEvent.UserAction -> handleUserAction(event.action, event.data, context)
            is FeatureEvent.SystemEvent -> handleSystemEvent(event.eventType, event.data, context)
            is FeatureEvent.LifecycleEvent -> handleLifecycleEvent(context)
        }
    }

    override suspend fun cleanup() {
        try {
            if (state.value == FeatureState.ENABLED) {
                performCleanup()
            }
            state.value = FeatureState.UNINITIALIZED
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }

    override suspend fun reset(context: FeatureContext): Boolean {
        return try {
            performReset(context)
            state.value = FeatureState.READY
            true
        } catch (e: Exception) {
            state.value = FeatureState.ERROR
            false
        }
    }

    /**
     * Validate feature dependencies
     */
    protected open suspend fun validateDependencies(): FeatureValidationResult {
        // Default implementation - can be overridden by subclasses
        return FeatureValidationResult(isValid = true)
    }

    /**
     * Validate feature configuration
     */
    protected open suspend fun validateConfiguration(): FeatureValidationResult {
        // Default implementation - can be overridden by subclasses
        return FeatureValidationResult(isValid = true)
    }

    /**
     * Check if feature can be enabled
     */
    protected open suspend fun canEnable(context: FeatureContext): Boolean {
        return state.value in listOf(FeatureState.READY, FeatureState.DISABLED) &&
               validateDependencies().isValid
    }

    /**
     * Perform feature-specific initialization
     */
    protected abstract suspend fun performInitialization(context: FeatureContext): Boolean

    /**
     * Perform feature-specific enable logic
     */
    protected abstract suspend fun performEnable(context: FeatureContext): Boolean

    /**
     * Perform feature-specific disable logic
     */
    protected abstract suspend fun performDisable(context: FeatureContext): Boolean

    /**
     * Perform feature-specific execution
     */
    protected abstract suspend fun performExecute(context: FeatureContext): FeatureResult

    /**
     * Perform feature-specific cleanup
     */
    protected abstract suspend fun performCleanup()

    /**
     * Perform feature-specific reset
     */
    protected abstract suspend fun performReset(context: FeatureContext): Boolean

    /**
     * Handle configuration changes
     */
    protected open suspend fun handleConfigurationChange(changes: Map<String, Any>, context: FeatureContext): Boolean {
        return true
    }

    /**
     * Handle dependency state changes
     */
    protected open suspend fun handleDependencyStateChange(featureId: String, newState: FeatureState, context: FeatureContext): Boolean {
        return true
    }

    /**
     * Handle user actions
     */
    protected open suspend fun handleUserAction(action: String, data: Map<String, Any>, context: FeatureContext): Boolean {
        return true
    }

    /**
     * Handle system events
     */
    protected open suspend fun handleSystemEvent(eventType: String, data: Map<String, Any>, context: FeatureContext): Boolean {
        return true
    }

    /**
     * Handle lifecycle events
     */
    protected open suspend fun handleLifecycleEvent(context: FeatureContext): Boolean {
        return true
    }
}