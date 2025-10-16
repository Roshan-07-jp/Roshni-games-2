package com.roshni.games.core.utils.workflow

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Context information for workflow execution
 *
 * @property workflowId The ID of the workflow being executed
 * @property executionId Unique identifier for this workflow execution instance
 * @property currentState The current state of the workflow
 * @property previousState The previous state (null if in initial state)
 * @property startTime When the workflow execution started
 * @property lastTransitionTime When the last state transition occurred
 * @property userId The ID of the user this workflow is executing for
 * @property sessionId The current session ID (if applicable)
 * @property variables Workflow variables that can be used in conditions and actions
 * @property events Events that have occurred during workflow execution
 * @property metadata Additional context-specific metadata
 * @property ruleContext The underlying rule context for rule engine integration
 * @property timeoutMs Timeout for the entire workflow execution
 */
data class WorkflowContext(
    val workflowId: String,
    val executionId: String,
    val currentState: WorkflowState,
    val previousState: WorkflowState? = null,
    val startTime: Long = System.currentTimeMillis(),
    val lastTransitionTime: Long = startTime,
    val userId: String,
    val sessionId: String? = null,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val events: MutableSet<String> = mutableSetOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf(),
    val ruleContext: RuleContext? = null,
    val timeoutMs: Long? = null
) {

    /**
     * Get the elapsed time since workflow execution started
     */
    fun getElapsedTimeMs(): Long {
        return System.currentTimeMillis() - startTime
    }

    /**
     * Get the time spent in the current state
     */
    fun getCurrentStateDurationMs(): Long {
        return System.currentTimeMillis() - lastTransitionTime
    }

    /**
     * Check if the workflow has timed out
     */
    fun isTimedOut(): Boolean {
        if (timeoutMs == null) return false
        return getElapsedTimeMs() > timeoutMs
    }

    /**
     * Check if the current state has timed out
     */
    fun isCurrentStateTimedOut(): Boolean {
        val stateTimeout = currentState.getTimeoutMs()
        if (stateTimeout == null) return false
        return getCurrentStateDurationMs() > stateTimeout
    }

    /**
     * Set a workflow variable
     */
    fun setVariable(key: String, value: Any) {
        variables[key] = value
    }

    /**
     * Get a workflow variable
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getVariable(key: String): T? {
        return variables[key] as? T
    }

    /**
     * Get a workflow variable with a default value
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getVariable(key: String, defaultValue: T): T {
        return variables[key] as? T ?: defaultValue
    }

    /**
     * Add an event to the workflow context
     */
    fun addEvent(event: String) {
        events.add(event)
    }

    /**
     * Check if an event has occurred
     */
    fun hasEvent(event: String): Boolean {
        return events.contains(event)
    }

    /**
     * Check if any of the specified events have occurred
     */
    fun hasAnyEvent(vararg events: String): Boolean {
        return events.any { hasEvent(it) }
    }

    /**
     * Check if all of the specified events have occurred
     */
    fun hasAllEvents(vararg events: String): Boolean {
        return events.all { hasEvent(it) }
    }

    /**
     * Create a copy of this context with updated state information
     */
    fun withStateTransition(newState: WorkflowState): WorkflowContext {
        return copy(
            currentState = newState,
            previousState = currentState,
            lastTransitionTime = System.currentTimeMillis()
        )
    }

    /**
     * Create a copy of this context with additional metadata
     */
    fun withMetadata(key: String, value: Any): WorkflowContext {
        val newMetadata = metadata.toMutableMap()
        newMetadata[key] = value
        return copy(metadata = newMetadata)
    }

    /**
     * Create a copy of this context with an additional event
     */
    fun withEvent(event: String): WorkflowContext {
        val newEvents = events.toMutableSet()
        newEvents.add(event)
        return copy(events = newEvents)
    }

    /**
     * Create a copy of this context with updated variables
     */
    fun withVariable(key: String, value: Any): WorkflowContext {
        val newVariables = variables.toMutableMap()
        newVariables[key] = value
        return copy(variables = newVariables)
    }

    /**
     * Convert to a RuleContext for rule engine integration
     */
    fun toRuleContext(): RuleContext? {
        if (ruleContext != null) return ruleContext

        return RuleContext(
            userId = userId,
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
    }

    /**
     * Create a snapshot of the current context for logging/debugging
     */
    fun createSnapshot(): Map<String, Any> {
        return mapOf(
            "workflowId" to workflowId,
            "executionId" to executionId,
            "currentState" to currentState.id,
            "previousState" to (previousState?.id ?: "none"),
            "elapsedTimeMs" to getElapsedTimeMs(),
            "currentStateDurationMs" to getCurrentStateDurationMs(),
            "userId" to userId,
            "sessionId" to (sessionId ?: "none"),
            "variableCount" to variables.size,
            "eventCount" to events.size,
            "metadataCount" to metadata.size,
            "isTimedOut" to isTimedOut(),
            "isCurrentStateTimedOut" to isCurrentStateTimedOut()
        )
    }
}