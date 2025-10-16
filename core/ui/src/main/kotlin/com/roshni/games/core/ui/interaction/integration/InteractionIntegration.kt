package com.roshni.games.core.ui.interaction.integration

import com.roshni.games.core.navigation.controller.NavigationFlowController
import com.roshni.games.core.ui.interaction.InteractionResponse
import com.roshni.games.core.ui.interaction.InteractionResponseSystem
import com.roshni.games.core.ui.interaction.NavigationAction
import com.roshni.games.core.ui.ux.engine.UXEnhancementEngine
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Integration layer that coordinates between Interaction Response System,
 * UX Enhancement Engine, and Navigation Flow Controller
 */
interface InteractionIntegration {

    /**
     * Current status of the integration
     */
    val status: StateFlow<IntegrationStatus>

    /**
     * Whether the integration is initialized and ready
     */
    val isReady: Boolean

    /**
     * Initialize the integration layer
     */
    suspend fun initialize(
        interactionSystem: InteractionResponseSystem,
        uxEngine: UXEnhancementEngine,
        navigationController: NavigationFlowController,
        context: UXContext
    ): Boolean

    /**
     * Process an interaction through the complete pipeline
     */
    suspend fun processInteraction(
        interaction: UserInteraction,
        context: UXContext
    ): IntegratedInteractionResult

    /**
     * Execute navigation actions from interaction response
     */
    suspend fun executeNavigationActions(
        actions: List<NavigationAction>,
        context: UXContext
    ): NavigationExecutionResult

    /**
     * Synchronize user context across all systems
     */
    suspend fun synchronizeUserContext(context: UXContext)

    /**
     * Get integration statistics
     */
    suspend fun getIntegrationStatistics(): IntegrationStatistics

    /**
     * Reset all integrated systems
     */
    suspend fun reset()

    /**
     * Shutdown the integration layer
     */
    suspend fun shutdown()

    /**
     * Observe integration events
     */
    fun observeIntegrationEvents(): Flow<IntegrationEvent>

    /**
     * Observe system coordination
     */
    fun observeSystemCoordination(): Flow<SystemCoordinationEvent>
}

/**
 * Status of the integration layer
 */
data class IntegrationStatus(
    val isInitialized: Boolean = false,
    val interactionSystemReady: Boolean = false,
    val uxEngineReady: Boolean = false,
    val navigationControllerReady: Boolean = false,
    val totalInteractionsProcessed: Long = 0,
    val totalCoordinations: Long = 0,
    val averageIntegrationTimeMs: Double = 0.0,
    val errorCount: Long = 0,
    val lastActivityTime: Long? = null
)

/**
 * Result of integrated interaction processing
 */
data class IntegratedInteractionResult(
    val interaction: UserInteraction,
    val interactionResponse: InteractionResponse,
    val uxEnhancements: List<com.roshni.games.core.ui.ux.model.UXEnhancement>,
    val navigationResults: List<com.roshni.games.core.navigation.model.NavigationResult>,
    val processingTimeMs: Long,
    val success: Boolean,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Result of navigation action execution
 */
data class NavigationExecutionResult(
    val executedActions: List<NavigationAction>,
    val navigationResults: List<com.roshni.games.core.navigation.model.NavigationResult>,
    val successCount: Int,
    val failureCount: Int,
    val totalExecutionTimeMs: Long,
    val timestamp: Long
)

/**
 * Statistics about system integration
 */
data class IntegrationStatistics(
    val totalInteractionsProcessed: Long = 0,
    val totalNavigationActions: Long = 0,
    val totalUXEnhancements: Long = 0,
    val averageIntegrationLatencyMs: Double = 0.0,
    val systemCoordinationEfficiency: Double = 0.0,
    val crossSystemCommunicationSuccess: Double = 0.0,
    val userExperienceConsistency: Double = 0.0,
    val errorRecoveryRate: Double = 0.0
)

/**
 * Events from the integration layer
 */
sealed class IntegrationEvent {
    data class InteractionProcessed(
        val interactionId: String,
        val systemsInvolved: List<String>,
        val processingTimeMs: Long,
        val timestamp: Long
    ) : IntegrationEvent()

    data class NavigationTriggered(
        val source: String,
        val destination: String,
        val triggerReason: String,
        val timestamp: Long
    ) : IntegrationEvent()

    data class EnhancementApplied(
        val interactionId: String,
        val enhancementTypes: List<String>,
        val applicationContext: String,
        val timestamp: Long
    ) : IntegrationEvent()

    data class SystemCoordination(
        val coordinatedSystems: List<String>,
        val coordinationType: String,
        val success: Boolean,
        val timestamp: Long
    ) : IntegrationEvent()

    data class ErrorOccurred(
        val system: String,
        val errorType: String,
        val errorMessage: String,
        val recoveryAttempted: Boolean,
        val timestamp: Long
    ) : IntegrationEvent()
}

/**
 * System coordination events
 */
data class SystemCoordinationEvent(
    val eventType: CoordinationEventType,
    val sourceSystem: String,
    val targetSystem: String,
    val coordinationData: Map<String, Any>,
    val success: Boolean,
    val timestamp: Long
)

/**
 * Types of coordination events
 */
enum class CoordinationEventType {
    CONTEXT_SYNCHRONIZATION, STATE_SHARING, ACTION_COORDINATION,
    ERROR_PROPAGATION, PERFORMANCE_OPTIMIZATION, USER_EVENT_ROUTING
}

/**
 * Default implementation of the Interaction Integration layer
 */
class InteractionIntegrationImpl : InteractionIntegration {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(IntegrationStatus())
    override val status: StateFlow<IntegrationStatus> = _status.asStateFlow()

    private val integrationEvents = MutableStateFlow<List<IntegrationEvent>>(emptyList())
    private val systemCoordinationEvents = MutableStateFlow<List<SystemCoordinationEvent>>(emptyList())

    private lateinit var interactionSystem: InteractionResponseSystem
    private lateinit var uxEngine: UXEnhancementEngine
    private lateinit var navigationController: NavigationFlowController

    private var interactionCount = 0L
    private var coordinationCount = 0L
    private var totalIntegrationTime = 0L

    override val isReady: Boolean
        get() = _status.value.isInitialized &&
                _status.value.interactionSystemReady &&
                _status.value.uxEngineReady &&
                _status.value.navigationControllerReady

    override suspend fun initialize(
        interactionSystem: InteractionResponseSystem,
        uxEngine: UXEnhancementEngine,
        navigationController: NavigationFlowController,
        context: UXContext
    ): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing Interaction Integration Layer")

            _status.value = _status.value.copy(isInitialized = false)

            this.interactionSystem = interactionSystem
            this.uxEngine = uxEngine
            this.navigationController = navigationController

            // Initialize interaction system
            val interactionInitialized = interactionSystem.initialize(uxEngine, navigationController, context)

            // Check if all systems are ready
            val interactionReady = interactionSystem.isReady
            val uxReady = uxEngine.isReady
            val navigationReady = navigationController.navigationState.value.isInitialized

            _status.value = _status.value.copy(
                isInitialized = interactionInitialized,
                interactionSystemReady = interactionReady,
                uxEngineReady = uxReady,
                navigationControllerReady = navigationReady,
                lastActivityTime = System.currentTimeMillis()
            )

            if (interactionInitialized && interactionReady && uxReady && navigationReady) {
                Timber.d("Interaction Integration Layer initialized successfully")
                true
            } else {
                Timber.w("Interaction Integration Layer initialized with some systems not ready")
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Interaction Integration Layer")
            _status.value = _status.value.copy(
                isInitialized = false,
                errorCount = _status.value.errorCount + 1
            )
            false
        }
    }

    override suspend fun processInteraction(
        interaction: UserInteraction,
        context: UXContext
    ): IntegratedInteractionResult = mutex.withLock {
        val startTime = System.currentTimeMillis()

        try {
            // Process through interaction response system
            val interactionResponse = interactionSystem.processInteraction(interaction, context)

            // Process through UX enhancement engine
            val enhancedInteraction = uxEngine.processInteraction(interaction, context)

            // Execute any navigation actions
            val navigationResults = if (interactionResponse.hasNavigationActions()) {
                executeNavigationActions(interactionResponse.navigationActions, context).navigationResults
            } else {
                emptyList()
            }

            val totalTime = System.currentTimeMillis() - startTime

            // Update statistics
            interactionCount++
            totalIntegrationTime += totalTime
            updateStatus()

            val result = IntegratedInteractionResult(
                interaction = interaction,
                interactionResponse = interactionResponse,
                uxEnhancements = enhancedInteraction.enhancements,
                navigationResults = navigationResults,
                processingTimeMs = totalTime,
                success = true,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf(
                    "systemsInvolved" to listOf("interaction", "ux", "navigation"),
                    "enhancementCount" to enhancedInteraction.enhancements.size,
                    "navigationActionCount" to interactionResponse.navigationActions.size
                )
            )

            // Record integration event
            val event = IntegrationEvent.InteractionProcessed(
                interactionId = interaction.id,
                systemsInvolved = listOf("InteractionResponseSystem", "UXEnhancementEngine", "NavigationFlowController"),
                processingTimeMs = totalTime,
                timestamp = System.currentTimeMillis()
            )

            integrationEvents.value = (integrationEvents.value + event).takeLast(100)

            Timber.d("Processed integrated interaction ${interaction.id} in ${totalTime}ms")

            result

        } catch (e: Exception) {
            Timber.e(e, "Failed to process integrated interaction ${interaction.id}")

            _status.value = _status.value.copy(errorCount = _status.value.errorCount + 1)

            IntegratedInteractionResult(
                interaction = interaction,
                interactionResponse = InteractionResponse(
                    interactionId = interaction.id,
                    immediateReactions = emptyList(),
                    personalizedReactions = emptyList(),
                    uxEnhancements = emptyList(),
                    navigationActions = emptyList(),
                    context = context,
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf("error" to (e.message ?: "Unknown error"))
                ),
                uxEnhancements = emptyList(),
                navigationResults = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime,
                success = false,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    override suspend fun executeNavigationActions(
        actions: List<NavigationAction>,
        context: UXContext
    ): NavigationExecutionResult = mutex.withLock {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<com.roshni.games.core.navigation.model.NavigationResult>()
        var successCount = 0
        var failureCount = 0

        try {
            for (action in actions) {
                try {
                    when (action) {
                        is NavigationAction.ImmediateNavigation -> {
                            val result = navigationController.navigate(
                                destination = action.destination,
                                arguments = action.arguments,
                                navOptions = null,
                                context = context.let { uxContext ->
                                    com.roshni.games.core.navigation.model.NavigationContext(
                                        currentDestination = uxContext.screenName,
                                        targetDestination = action.destination,
                                        arguments = action.arguments,
                                        userId = uxContext.userId,
                                        sessionId = uxContext.sessionId
                                    )
                                }
                            )
                            results.add(result)
                            if (result.isSuccess()) successCount++ else failureCount++

                            // Record navigation event
                            val event = IntegrationEvent.NavigationTriggered(
                                source = context.screenName,
                                destination = action.destination,
                                triggerReason = "immediate_navigation",
                                timestamp = System.currentTimeMillis()
                            )
                            integrationEvents.value = (integrationEvents.value + event).takeLast(100)
                        }

                        is NavigationAction.ContextualNavigation -> {
                            // For contextual navigation, we might delay execution or use different logic
                            val result = navigationController.navigate(
                                destination = action.destination,
                                arguments = emptyMap(),
                                navOptions = null,
                                context = context.let { uxContext ->
                                    com.roshni.games.core.navigation.model.NavigationContext(
                                        currentDestination = uxContext.screenName,
                                        targetDestination = action.destination,
                                        arguments = emptyMap(),
                                        userId = uxContext.userId,
                                        sessionId = uxContext.sessionId
                                    )
                                }
                            )
                            results.add(result)
                            if (result.isSuccess()) successCount++ else failureCount++
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute navigation action ${action.id}")
                    failureCount++

                    // Record error event
                    val errorEvent = IntegrationEvent.ErrorOccurred(
                        system = "NavigationFlowController",
                        errorType = "NavigationExecutionError",
                        errorMessage = e.message ?: "Unknown error",
                        recoveryAttempted = true,
                        timestamp = System.currentTimeMillis()
                    )
                    integrationEvents.value = (integrationEvents.value + errorEvent).takeLast(100)
                }
            }

            val totalTime = System.currentTimeMillis() - startTime

            NavigationExecutionResult(
                executedActions = actions,
                navigationResults = results,
                successCount = successCount,
                failureCount = failureCount,
                totalExecutionTimeMs = totalTime,
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to execute navigation actions")

            NavigationExecutionResult(
                executedActions = actions,
                navigationResults = results,
                successCount = successCount,
                failureCount = failureCount + 1,
                totalExecutionTimeMs = System.currentTimeMillis() - startTime,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    override suspend fun synchronizeUserContext(context: UXContext) {
        mutex.withLock {
            try {
                // Synchronize context across all systems
                interactionSystem.updateUserBehaviorModel(context)
                uxEngine.updateUserContext(context)

                // Update navigation context if needed
                navigationController.updateContext { currentContext ->
                    currentContext.copy(
                        userId = context.userId,
                        sessionId = context.sessionId,
                        userPreferences = context.userPreferences.let { prefs ->
                            mapOf(
                                "theme" to prefs.theme.name,
                                "animationSpeed" to prefs.animationSpeed.name,
                                "soundEnabled" to prefs.soundEnabled.toString(),
                                "hapticFeedbackEnabled" to prefs.hapticFeedbackEnabled.toString()
                            )
                        }
                    )
                }

                // Record coordination event
                val coordinationEvent = SystemCoordinationEvent(
                    eventType = CoordinationEventType.CONTEXT_SYNCHRONIZATION,
                    sourceSystem = "InteractionIntegration",
                    targetSystem = "AllSystems",
                    coordinationData = mapOf(
                        "userId" to (context.userId ?: "anonymous"),
                        "screenName" to context.screenName,
                        "timestamp" to context.timestamp
                    ),
                    success = true,
                    timestamp = System.currentTimeMillis()
                )

                systemCoordinationEvents.value = (systemCoordinationEvents.value + coordinationEvent).takeLast(100)

                coordinationCount++

                Timber.d("Synchronized user context across all systems")

            } catch (e: Exception) {
                Timber.e(e, "Failed to synchronize user context")

                // Record error event
                val errorEvent = IntegrationEvent.ErrorOccurred(
                    system = "InteractionIntegration",
                    errorType = "ContextSynchronizationError",
                    errorMessage = e.message ?: "Unknown error",
                    recoveryAttempted = true,
                    timestamp = System.currentTimeMillis()
                )
                integrationEvents.value = (integrationEvents.value + errorEvent).takeLast(100)
            }
        }
    }

    override suspend fun getIntegrationStatistics(): IntegrationStatistics {
        val events = integrationEvents.value
        val coordinationEvents = systemCoordinationEvents.value

        val totalInteractions = events.count { it is IntegrationEvent.InteractionProcessed }
        val totalNavigations = events.count { it is IntegrationEvent.NavigationTriggered }
        val totalEnhancements = events.count { it is IntegrationEvent.EnhancementApplied }
        val totalErrors = events.count { it is IntegrationEvent.ErrorOccurred }

        val coordinationSuccess = coordinationEvents.count { it.success }
        val coordinationTotal = coordinationEvents.size

        return IntegrationStatistics(
            totalInteractionsProcessed = interactionCount,
            totalNavigationActions = totalNavigations.toLong(),
            totalUXEnhancements = totalEnhancements.toLong(),
            averageIntegrationLatencyMs = if (interactionCount > 0) {
                totalIntegrationTime.toDouble() / interactionCount
            } else 0.0,
            systemCoordinationEfficiency = if (coordinationTotal > 0) {
                coordinationSuccess.toDouble() / coordinationTotal
            } else 0.0,
            crossSystemCommunicationSuccess = if (totalErrors > 0) {
                1.0 - (totalErrors.toDouble() / events.size)
            } else 1.0,
            userExperienceConsistency = calculateUserExperienceConsistency(events),
            errorRecoveryRate = calculateErrorRecoveryRate(events)
        )
    }

    override suspend fun reset() {
        interactionCount = 0
        coordinationCount = 0
        totalIntegrationTime = 0
        integrationEvents.value = emptyList()
        systemCoordinationEvents.value = emptyList()
        updateStatus()
    }

    override suspend fun shutdown() {
        try {
            Timber.d("Shutting down Interaction Integration Layer")
            reset()
            _status.value = IntegrationStatus()
        } catch (e: Exception) {
            Timber.e(e, "Error during Interaction Integration Layer shutdown")
        }
    }

    override fun observeIntegrationEvents(): Flow<IntegrationEvent> = integrationEvents

    override fun observeSystemCoordination(): Flow<SystemCoordinationEvent> = systemCoordinationEvents

    /**
     * Update integration status
     */
    private fun updateStatus() {
        _status.value = _status.value.copy(
            totalInteractionsProcessed = interactionCount,
            totalCoordinations = coordinationCount,
            averageIntegrationTimeMs = if (interactionCount > 0) {
                totalIntegrationTime.toDouble() / interactionCount
            } else 0.0,
            lastActivityTime = System.currentTimeMillis()
        )
    }

    /**
     * Calculate user experience consistency across systems
     */
    private fun calculateUserExperienceConsistency(events: List<IntegrationEvent>): Double {
        val interactionEvents = events.filterIsInstance<IntegrationEvent.InteractionProcessed>()
        if (interactionEvents.isEmpty()) return 0.0

        // Measure consistency based on processing time variance
        val processingTimes = interactionEvents.map { it.processingTimeMs }
        val averageTime = processingTimes.average()

        val variance = if (processingTimes.isNotEmpty()) {
            processingTimes.sumOf { (it - averageTime) * (it - averageTime) } / processingTimes.size
        } else 0.0

        val standardDeviation = kotlin.math.sqrt(variance)

        // Lower standard deviation means more consistent experience
        return (1.0 - (standardDeviation / averageTime).coerceAtMost(1.0)).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate error recovery rate
     */
    private fun calculateErrorRecoveryRate(events: List<IntegrationEvent>): Double {
        val errorEvents = events.filterIsInstance<IntegrationEvent.ErrorOccurred>()
        if (errorEvents.isEmpty()) return 1.0

        val recoveredErrors = errorEvents.count { it.recoveryAttempted }
        return recoveredErrors.toDouble() / errorEvents.size
    }
}

/**
 * Factory for creating Interaction Integration instances
 */
object InteractionIntegrationFactory {

    fun create(): InteractionIntegration {
        return InteractionIntegrationImpl()
    }
}