package com.roshni.games.core.utils.integration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages state synchronization between integrated components
 */
class StateSynchronizationManager {

    private val mutex = Mutex()
    private val componentStates = ConcurrentHashMap<String, MutableStateFlow<ComponentState>>()
    private val stateSyncRules = ConcurrentHashMap<String, StateSyncRule>()
    private val stateHistory = ConcurrentHashMap<String, MutableList<StateChangeRecord>>()

    private val _globalState = MutableStateFlow<Map<String, Any>>(emptyMap())
    val globalState: StateFlow<Map<String, Any>> = _globalState.asStateFlow()

    private val _syncMetrics = MutableStateFlow(StateSyncMetrics())
    val syncMetrics: StateFlow<StateSyncMetrics> = _syncMetrics.asStateFlow()

    /**
     * Register component state for synchronization
     */
    fun registerComponentState(
        componentId: String,
        initialState: Map<String, Any> = emptyMap()
    ): ComponentState {
        val stateFlow = MutableStateFlow(ComponentState(componentId, initialState))
        componentStates[componentId] = stateFlow

        // Initialize state history
        stateHistory[componentId] = mutableListOf()

        Timber.d("Registered state for component: $componentId")
        return stateFlow.value
    }

    /**
     * Update component state
     */
    suspend fun updateComponentState(
        componentId: String,
        updates: Map<String, Any>,
        source: String = "system"
    ): Result<Unit> = mutex.withLock {
        try {
            val currentStateFlow = componentStates[componentId]
                ?: return Result.failure(IllegalArgumentException("Component $componentId not registered"))

            val currentState = currentStateFlow.value
            val newState = currentState.copy(
                data = currentState.data + updates,
                lastModified = System.currentTimeMillis(),
                lastModifiedBy = source,
                version = currentState.version + 1
            )

            currentStateFlow.value = newState

            // Record state change
            recordStateChange(componentId, currentState, newState, source)

            // Apply synchronization rules
            applySyncRules(componentId, newState)

            // Update global state
            updateGlobalState()

            // Update metrics
            updateSyncMetrics()

            Timber.d("Updated state for component $componentId: $updates")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update state for component: $componentId")
            Result.failure(e)
        }
    }

    /**
     * Get component state
     */
    fun getComponentState(componentId: String): ComponentState? {
        return componentStates[componentId]?.value
    }

    /**
     * Observe component state changes
     */
    fun observeComponentState(componentId: String): Flow<ComponentState>? {
        return componentStates[componentId]?.asStateFlow()
    }

    /**
     * Observe specific state key across components
     */
    fun observeStateKey(key: String): Flow<Map<String, Any>> = flow {
        val componentStatesFlows = componentStates.values.map { it }

        combine(componentStatesFlows) { states ->
            states.associate { state ->
                state.componentId to (state.data[key] ?: "NOT_FOUND")
            }
        }.collect { combinedState ->
            emit(combinedState)
        }
    }

    /**
     * Add state synchronization rule
     */
    fun addSyncRule(ruleId: String, rule: StateSyncRule) {
        stateSyncRules[ruleId] = rule
        Timber.d("Added state sync rule: $ruleId")
    }

    /**
     * Remove state synchronization rule
     */
    fun removeSyncRule(ruleId: String) {
        stateSyncRules.remove(ruleId)
        Timber.d("Removed state sync rule: $ruleId")
    }

    /**
     * Synchronize state between components
     */
    suspend fun synchronizeBetweenComponents(
        sourceComponent: String,
        targetComponent: String,
        stateKeys: List<String>,
        syncMode: SyncMode = SyncMode.BIDIRECTIONAL
    ): Result<Unit> = mutex.withLock {
        try {
            val sourceState = getComponentState(sourceComponent)
                ?: return Result.failure(IllegalArgumentException("Source component $sourceComponent not found"))

            val targetState = getComponentState(targetComponent)
                ?: return Result.failure(IllegalArgumentException("Target component $targetComponent not found"))

            val syncedKeys = mutableListOf<String>()
            val syncResults = mutableListOf<String>()

            for (key in stateKeys) {
                val sourceValue = sourceState.data[key]
                val targetValue = targetState.data[key]

                when (syncMode) {
                    SyncMode.SOURCE_TO_TARGET -> {
                        if (sourceValue != null && sourceValue != targetValue) {
                            updateComponentState(targetComponent, mapOf(key to sourceValue), "sync_$sourceComponent")
                            syncedKeys.add(key)
                            syncResults.add("Synced $key from $sourceComponent to $targetComponent")
                        }
                    }
                    SyncMode.TARGET_TO_SOURCE -> {
                        if (targetValue != null && targetValue != sourceValue) {
                            updateComponentState(sourceComponent, mapOf(key to targetValue), "sync_$targetComponent")
                            syncedKeys.add(key)
                            syncResults.add("Synced $key from $targetComponent to $sourceComponent")
                        }
                    }
                    SyncMode.BIDIRECTIONAL -> {
                        if (sourceValue != null && sourceValue != targetValue) {
                            // Use source as authoritative for conflicts
                            updateComponentState(targetComponent, mapOf(key to sourceValue), "sync_bidirectional")
                            syncedKeys.add(key)
                            syncResults.add("Bidirectional sync $key: $sourceComponent -> $targetComponent")
                        }
                    }
                }
            }

            if (syncedKeys.isNotEmpty()) {
                Timber.d("Synchronized state between $sourceComponent and $targetComponent: $syncedKeys")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronize state between $sourceComponent and $targetComponent")
            Result.failure(e)
        }
    }

    /**
     * Get state change history for component
     */
    fun getStateHistory(componentId: String, limit: Int = 50): List<StateChangeRecord> {
        return stateHistory[componentId]?.takeLast(limit) ?: emptyList()
    }

    /**
     * Clear state history for component
     */
    fun clearStateHistory(componentId: String) {
        stateHistory[componentId]?.clear()
        Timber.d("Cleared state history for component: $componentId")
    }

    /**
     * Get synchronization conflicts
     */
    fun getSyncConflicts(): List<SyncConflict> {
        val conflicts = mutableListOf<SyncConflict>()

        // Analyze state history for conflicts
        stateHistory.values.forEach { history ->
            history.groupBy { it.stateKey }.forEach { (key, changes) ->
                if (changes.size > 1) {
                    val recentChanges = changes.takeLast(2)
                    if (recentChanges.size == 2) {
                        val (first, second) = recentChanges
                        if (first.newValue != second.oldValue) {
                            conflicts.add(SyncConflict(
                                stateKey = key,
                                componentId = first.componentId,
                                conflictingValues = listOf(first.newValue, second.newValue),
                                timestamp = second.timestamp
                            ))
                        }
                    }
                }
            }
        }

        return conflicts
    }

    /**
     * Record state change
     */
    private fun recordStateChange(
        componentId: String,
        oldState: ComponentState,
        newState: ComponentState,
        source: String
    ) {
        val history = stateHistory[componentId] ?: return

        // Find changed keys
        val changedKeys = newState.data.keys.filter { key ->
            oldState.data[key] != newState.data[key]
        }

        changedKeys.forEach { key ->
            history.add(StateChangeRecord(
                componentId = componentId,
                stateKey = key,
                oldValue = oldState.data[key],
                newValue = newState.data[key]!!,
                timestamp = newState.lastModified,
                source = source
            ))
        }

        // Keep only last 1000 changes per component
        if (history.size > 1000) {
            history.removeAt(0)
        }
    }

    /**
     * Apply synchronization rules
     */
    private suspend fun applySyncRules(componentId: String, newState: ComponentState) {
        stateSyncRules.values.forEach { rule ->
            try {
                if (rule.shouldApply(componentId, newState)) {
                    val actions = rule.apply(componentId, newState)
                    actions.forEach { action ->
                        executeSyncAction(action)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error applying sync rule ${rule.ruleId}")
            }
        }
    }

    /**
     * Execute synchronization action
     */
    private suspend fun executeSyncAction(action: StateSyncAction) {
        when (action) {
            is StateSyncAction.PropagateToComponent -> {
                updateComponentState(
                    componentId = action.targetComponentId,
                    updates = mapOf(action.stateKey to action.value),
                    source = "sync_rule_${action.ruleId}"
                )
            }
            is StateSyncAction.UpdateGlobalState -> {
                _globalState.value = _globalState.value + (action.key to action.value)
            }
            is StateSyncAction.TriggerEvent -> {
                // This would trigger an event through the event manager
                Timber.d("Sync action triggered event: ${action.eventType}")
            }
        }
    }

    /**
     * Update global state
     */
    private fun updateGlobalState() {
        val combinedState = componentStates.values.associate { stateFlow ->
            stateFlow.value.componentId to stateFlow.value.data
        }
        _globalState.value = combinedState
    }

    /**
     * Update synchronization metrics
     */
    private fun updateSyncMetrics() {
        val totalComponents = componentStates.size
        val totalStateKeys = componentStates.values.sumOf { it.value.data.size }
        val totalHistoryEntries = stateHistory.values.sumOf { it.size }

        _syncMetrics.value = _syncMetrics.value.copy(
            totalComponents = totalComponents,
            totalStateKeys = totalStateKeys,
            totalStateChanges = totalHistoryEntries,
            lastSyncTime = System.currentTimeMillis()
        )
    }
}

/**
 * Component state representation
 */
data class ComponentState(
    val componentId: String,
    val data: Map<String, Any>,
    val lastModified: Long = System.currentTimeMillis(),
    val lastModifiedBy: String = "system",
    val version: Long = 1,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * State synchronization rule
 */
interface StateSyncRule {
    val ruleId: String
    val description: String

    suspend fun shouldApply(componentId: String, newState: ComponentState): Boolean
    suspend fun apply(componentId: String, newState: ComponentState): List<StateSyncAction>
}

/**
 * State synchronization action
 */
sealed class StateSyncAction {
    data class PropagateToComponent(
        val ruleId: String,
        val targetComponentId: String,
        val stateKey: String,
        val value: Any
    ) : StateSyncAction()

    data class UpdateGlobalState(
        val ruleId: String,
        val key: String,
        val value: Any
    ) : StateSyncAction()

    data class TriggerEvent(
        val ruleId: String,
        val eventType: String,
        val payload: Map<String, Any>
    ) : StateSyncAction()
}

/**
 * State change record for history tracking
 */
data class StateChangeRecord(
    val componentId: String,
    val stateKey: String,
    val oldValue: Any?,
    val newValue: Any,
    val timestamp: Long,
    val source: String
)

/**
 * Synchronization conflict information
 */
data class SyncConflict(
    val stateKey: String,
    val componentId: String,
    val conflictingValues: List<Any>,
    val timestamp: Long,
    val resolution: ConflictResolution? = null
)

/**
 * Conflict resolution strategy
 */
enum class ConflictResolution {
    USE_SOURCE,
    USE_TARGET,
    MERGE,
    MANUAL
}

/**
 * Synchronization mode
 */
enum class SyncMode {
    SOURCE_TO_TARGET,
    TARGET_TO_SOURCE,
    BIDIRECTIONAL
}

/**
 * State synchronization metrics
 */
data class StateSyncMetrics(
    val totalComponents: Int = 0,
    val totalStateKeys: Int = 0,
    val totalStateChanges: Int = 0,
    val totalConflicts: Int = 0,
    val resolvedConflicts: Int = 0,
    val lastSyncTime: Long = 0,
    val averageSyncLatencyMs: Double = 0.0
)

/**
 * Example implementation of a state sync rule
 */
class GlobalStateSyncRule : StateSyncRule {
    override val ruleId: String = "global_state_sync"
    override val description: String = "Synchronizes specific component state to global state"

    private val globalKeys = setOf("user_preferences", "system_settings", "app_config")

    override suspend fun shouldApply(componentId: String, newState: ComponentState): Boolean {
        return newState.data.keys.any { it in globalKeys }
    }

    override suspend fun apply(componentId: String, newState: ComponentState): List<StateSyncAction> {
        return newState.data.filter { it.key in globalKeys }.map { (key, value) ->
            StateSyncAction.UpdateGlobalState(
                ruleId = ruleId,
                key = "$componentId.$key",
                value = value
            )
        }
    }
}