package com.roshni.games.core.utils.workflow

/**
 * Interface representing a complete workflow definition with states and transitions
 */
interface Workflow {

    /**
     * Unique identifier for this workflow
     */
    val id: String

    /**
     * Human-readable name for this workflow
     */
    val name: String

    /**
     * Detailed description of what this workflow does
     */
    val description: String

    /**
     * The category/type of this workflow
     */
    val category: String

    /**
     * Version number for tracking workflow changes
     */
    val version: Int

    /**
     * Whether this workflow is currently enabled
     */
    val enabled: Boolean

    /**
     * Tags for organizing and filtering workflows
     */
    val tags: List<String>

    /**
     * All states in this workflow
     */
    val states: List<WorkflowState>

    /**
     * All transitions in this workflow
     */
    val transitions: List<WorkflowTransition>

    /**
     * Global actions that can be executed during workflow lifecycle
     */
    val globalActions: List<WorkflowAction>

    /**
     * Default timeout for workflow execution in milliseconds
     */
    val defaultTimeoutMs: Long?

    /**
     * Creation timestamp
     */
    val createdAt: Long

    /**
     * Last modification timestamp
     */
    val modifiedAt: Long

    /**
     * Get the initial state for this workflow
     */
    fun getInitialState(): WorkflowState?

    /**
     * Get all terminal states for this workflow
     */
    fun getTerminalStates(): List<WorkflowState>

    /**
     * Get all error states for this workflow
     */
    fun getErrorStates(): List<WorkflowState>

    /**
     * Get all possible transitions from a given state
     */
    fun getTransitionsFrom(state: WorkflowState): List<WorkflowTransition>

    /**
     * Get all possible transitions to a given state
     */
    fun getTransitionsTo(state: WorkflowState): List<WorkflowTransition>

    /**
     * Find a state by its ID
     */
    fun findState(stateId: String): WorkflowState?

    /**
     * Find a transition by its ID
     */
    fun findTransition(transitionId: String): WorkflowTransition?

    /**
     * Validate that this workflow is properly configured
     */
    suspend fun validate(): Boolean

    /**
     * Get metadata about this workflow for debugging and monitoring
     */
    suspend fun getMetadata(): Map<String, Any>

    /**
     * Create a new execution context for this workflow
     */
    fun createContext(
        userId: String,
        sessionId: String? = null,
        initialVariables: Map<String, Any> = emptyMap(),
        ruleContext: com.roshni.games.core.utils.rules.RuleContext? = null
    ): WorkflowContext

    /**
     * Check if this workflow can be executed with the given context
     */
    suspend fun canExecute(context: WorkflowContext): Boolean

    /**
     * Get the estimated execution time for this workflow
     */
    fun getEstimatedExecutionTimeMs(): Long?
}

/**
 * Base implementation of the Workflow interface
 */
abstract class BaseWorkflow : Workflow {

    protected abstract val workflowStates: List<WorkflowState>
    protected abstract val workflowTransitions: List<WorkflowTransition>
    protected abstract val workflowGlobalActions: List<WorkflowAction>

    final override val states: List<WorkflowState>
        get() = workflowStates

    final override val transitions: List<WorkflowTransition>
        get() = workflowTransitions

    final override val globalActions: List<WorkflowAction>
        get() = workflowGlobalActions

    override fun getInitialState(): WorkflowState? {
        return states.find { it.isInitial }
    }

    override fun getTerminalStates(): List<WorkflowState> {
        return states.filter { it.isTerminal }
    }

    override fun getErrorStates(): List<WorkflowState> {
        return states.filter { it.isError }
    }

    override fun getTransitionsFrom(state: WorkflowState): List<WorkflowTransition> {
        return transitions.filter { it.fromState == state }
    }

    override fun getTransitionsTo(state: WorkflowState): List<WorkflowTransition> {
        return transitions.filter { it.toState == state }
    }

    override fun findState(stateId: String): WorkflowState? {
        return states.find { it.id == stateId }
    }

    override fun findTransition(transitionId: String): WorkflowTransition? {
        return transitions.find { it.id == transitionId }
    }

    override suspend fun validate(): Boolean {
        // Basic validation checks
        if (id.isBlank()) return false
        if (name.isBlank()) return false
        if (states.isEmpty()) return false
        if (getInitialState() == null) return false
        if (getTerminalStates().isEmpty()) return false

        // Validate state connectivity
        val initialState = getInitialState() ?: return false
        val reachableStates = getReachableStates(initialState)

        // All states should be reachable from initial state
        if (reachableStates.size != states.size) return false

        // All terminal states should be reachable
        val terminalStates = getTerminalStates()
        if (terminalStates.any { it !in reachableStates }) return false

        return true
    }

    override suspend fun getMetadata(): Map<String, Any> {
        return mapOf(
            "stateCount" to states.size,
            "transitionCount" to transitions.size,
            "terminalStateCount" to getTerminalStates().size,
            "hasTimeout" to (defaultTimeoutMs != null),
            "estimatedExecutionTimeMs" to (getEstimatedExecutionTimeMs() ?: 0)
        )
    }

    override fun createContext(
        userId: String,
        sessionId: String?,
        initialVariables: Map<String, Any>,
        ruleContext: com.roshni.games.core.utils.rules.RuleContext?
    ): WorkflowContext {
        val initialState = getInitialState() ?: throw IllegalStateException("Workflow has no initial state")

        return WorkflowContext(
            workflowId = id,
            executionId = generateExecutionId(),
            currentState = initialState,
            userId = userId,
            sessionId = sessionId,
            variables = initialVariables.toMutableMap(),
            ruleContext = ruleContext,
            timeoutMs = defaultTimeoutMs
        )
    }

    override suspend fun canExecute(context: WorkflowContext): Boolean {
        // Basic checks - can be overridden by subclasses
        return enabled && getInitialState() != null
    }

    override fun getEstimatedExecutionTimeMs(): Long? {
        // Simple estimation based on state count and transitions
        // Can be overridden by subclasses for more accurate estimates
        val avgStateTime = 5000L // 5 seconds average per state
        val avgTransitionTime = 1000L // 1 second average per transition

        return (states.size * avgStateTime) + (transitions.size * avgTransitionTime)
    }

    /**
     * Get all states reachable from the given state
     */
    private fun getReachableStates(fromState: WorkflowState): Set<WorkflowState> {
        val reachable = mutableSetOf<WorkflowState>()
        val toVisit = mutableListOf(fromState)

        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeAt(0)
            if (current in reachable) continue

            reachable.add(current)
            val outgoingTransitions = getTransitionsFrom(current)
            val nextStates = outgoingTransitions.map { it.toState }

            toVisit.addAll(nextStates.filter { it !in reachable })
        }

        return reachable
    }

    /**
     * Generate a unique execution ID
     */
    private fun generateExecutionId(): String {
        return "${id}_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
    }
}