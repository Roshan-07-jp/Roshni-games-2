package com.roshni.games.core.utils.workflow

/**
 * Represents a state in a workflow state machine
 *
 * @property id Unique identifier for this state
 * @property name Human-readable name for this state
 * @property description Detailed description of what this state represents
 * @property type The type/category of this state
 * @property isInitial Whether this is an initial state for the workflow
 * @property isTerminal Whether this is a terminal state (workflow ends here)
 * @property isError Whether this is an error state
 * @property metadata Additional metadata associated with this state
 * @property entryActions Actions to execute when entering this state
 * @property exitActions Actions to execute when exiting this state
 */
data class WorkflowState(
    val id: String,
    val name: String,
    val description: String = "",
    val type: StateType = StateType.NORMAL,
    val isInitial: Boolean = false,
    val isTerminal: Boolean = false,
    val isError: Boolean = false,
    val metadata: Map<String, Any> = emptyMap(),
    val entryActions: List<WorkflowAction> = emptyList(),
    val exitActions: List<WorkflowAction> = emptyList()
) {

    /**
     * Types of workflow states
     */
    enum class StateType {
        INITIAL,    // Starting state of a workflow
        NORMAL,     // Regular intermediate state
        DECISION,   // State that leads to conditional transitions
        PARALLEL,   // State that can transition to multiple other states
        TERMINAL,   // Final state that ends the workflow
        ERROR       // Error state for handling failures
    }

    /**
     * Check if this state can transition to another state
     */
    fun canTransitionTo(targetState: WorkflowState): Boolean {
        return when (type) {
            StateType.TERMINAL -> false
            StateType.ERROR -> targetState.type == StateType.TERMINAL || targetState.type == StateType.ERROR
            else -> targetState.type != StateType.INITIAL || isInitial
        }
    }

    /**
     * Get the timeout for this state in milliseconds
     */
    fun getTimeoutMs(): Long? {
        return metadata["timeoutMs"] as? Long
    }

    /**
     * Check if this state has a timeout configured
     */
    fun hasTimeout(): Boolean {
        return getTimeoutMs() != null
    }
}