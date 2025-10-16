package com.roshni.games.core.ui.interaction

import com.roshni.games.core.navigation.controller.NavigationFlowController
import com.roshni.games.core.ui.ux.engine.UXEnhancementEngine
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Core interface for the Interaction Response System
 * Handles user interactions and generates appropriate reactions
 */
interface InteractionResponseSystem {

    /**
     * Current status of the system
     */
    val status: StateFlow<SystemStatus>

    /**
     * Whether the system is initialized and ready
     */
    val isReady: Boolean

    /**
     * Initialize the interaction response system
     */
    suspend fun initialize(
        uxEngine: UXEnhancementEngine,
        navigationController: NavigationFlowController,
        context: UXContext
    ): Boolean

    /**
     * Process a user interaction and generate appropriate reactions
     */
    suspend fun processInteraction(
        interaction: UserInteraction,
        context: UXContext
    ): InteractionResponse

    /**
     * Get personalized reactions for a user based on their behavior patterns
     */
    suspend fun getPersonalizedReactions(
        context: UXContext,
        limit: Int = 10
    ): List<PersonalizedReaction>

    /**
     * Learn from user interaction patterns
     */
    suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    )

    /**
     * Get interaction statistics
     */
    suspend fun getInteractionStatistics(): InteractionStatistics

    /**
     * Register a custom reaction pattern
     */
    suspend fun registerReactionPattern(pattern: InteractionPattern): Boolean

    /**
     * Unregister a reaction pattern
     */
    suspend fun unregisterReactionPattern(patternId: String): Boolean

    /**
     * Update user behavior model
     */
    suspend fun updateUserBehaviorModel(context: UXContext)

    /**
     * Clear all learned patterns and reset the system
     */
    suspend fun reset()

    /**
     * Shutdown the system and cleanup resources
     */
    suspend fun shutdown()

    /**
     * Observe system status changes
     */
    fun observeStatus(): Flow<SystemStatus>

    /**
     * Observe interaction processing results
     */
    fun observeInteractionResults(): Flow<InteractionProcessingResult>
}

/**
 * Status of the interaction response system
 */
data class SystemStatus(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val lastActivityTime: Long? = null,
    val totalInteractionsProcessed: Long = 0,
    val totalReactionsGenerated: Long = 0,
    val averageProcessingTimeMs: Double = 0.0,
    val errorCount: Long = 0,
    val patternCount: Int = 0,
    val personalizationScore: Double = 0.0
)

/**
 * Result of interaction processing
 */
data class InteractionProcessingResult(
    val interactionId: String,
    val reactions: List<ImmediateReaction>,
    val personalizedReactions: List<PersonalizedReaction>,
    val uxEnhancements: List<UXEnhancement>,
    val navigationActions: List<NavigationAction>,
    val processingTimeMs: Long,
    val success: Boolean,
    val timestamp: Long,
    val error: String? = null
)

/**
 * Statistics about interaction processing
 */
data class InteractionStatistics(
    val totalInteractionsProcessed: Long = 0,
    val totalReactionsGenerated: Long = 0,
    val averageReactionTimeMs: Double = 0.0,
    val mostCommonInteractionTypes: List<String> = emptyList(),
    val mostEffectiveReactionTypes: List<String> = emptyList(),
    val userEngagementScore: Double = 0.0,
    val personalizationAccuracy: Double = 0.0,
    val patternDiscoveryRate: Double = 0.0,
    val reactionSuccessRate: Double = 0.0
)

/**
 * Default implementation of the Interaction Response System
 */
class InteractionResponseSystemImpl(
    private val patternSystem: InteractionPatternSystem,
    private val personalizationSystem: PersonalizedReactionSystem
) : InteractionResponseSystem {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(SystemStatus())
    override val status: StateFlow<SystemStatus> = _status.asStateFlow()

    private val interactionResults = MutableStateFlow<List<InteractionProcessingResult>>(emptyList())

    private var interactionCount = 0L
    private var reactionCount = 0L
    private var totalProcessingTime = 0L

    private lateinit var uxEngine: UXEnhancementEngine
    private lateinit var navigationController: NavigationFlowController

    override val isReady: Boolean
        get() = _status.value.isInitialized && ::uxEngine.isInitialized && ::navigationController.isInitialized

    override suspend fun initialize(
        uxEngine: UXEnhancementEngine,
        navigationController: NavigationFlowController,
        context: UXContext
    ): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing Interaction Response System")

            _status.value = _status.value.copy(isInitialized = false)

            this.uxEngine = uxEngine
            this.navigationController = navigationController

            // Initialize pattern system
            patternSystem.initialize(context)

            // Initialize personalization system
            personalizationSystem.initialize(context)

            _status.value = _status.value.copy(
                isInitialized = true,
                lastActivityTime = System.currentTimeMillis(),
                patternCount = patternSystem.getPatternCount()
            )

            Timber.d("Interaction Response System initialized successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Interaction Response System")
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
    ): InteractionResponse = mutex.withLock {
        val startTime = System.currentTimeMillis()

        try {
            _status.value = _status.value.copy(isProcessing = true)

            // Get immediate reactions based on interaction type
            val immediateReactions = generateImmediateReactions(interaction, context)

            // Get personalized reactions based on user behavior
            val personalizedReactions = personalizationSystem.getPersonalizedReactions(
                interaction = interaction,
                context = context,
                limit = 5
            )

            // Get UX enhancements from the UX engine
            val uxEnhancements = uxEngine.processInteraction(interaction, context).enhancements

            // Determine navigation actions if needed
            val navigationActions = determineNavigationActions(interaction, context, immediateReactions)

            // Create interaction response
            val response = InteractionResponse(
                interactionId = interaction.id,
                immediateReactions = immediateReactions,
                personalizedReactions = personalizedReactions,
                uxEnhancements = uxEnhancements,
                navigationActions = navigationActions,
                context = context,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf(
                    "processingTimeMs" to (System.currentTimeMillis() - startTime),
                    "reactionCount" to immediateReactions.size,
                    "personalizationScore" to calculatePersonalizationScore(personalizedReactions)
                )
            )

            // Learn from this interaction
            learnFromInteraction(interaction, response, context)

            // Update statistics
            val processingTime = System.currentTimeMillis() - startTime
            interactionCount++
            reactionCount += immediateReactions.size + personalizedReactions.size
            totalProcessingTime += processingTime

            updateStatus()

            // Record processing result
            val result = InteractionProcessingResult(
                interactionId = interaction.id,
                reactions = immediateReactions,
                personalizedReactions = personalizedReactions,
                uxEnhancements = uxEnhancements,
                navigationActions = navigationActions,
                processingTimeMs = processingTime,
                success = true,
                timestamp = System.currentTimeMillis()
            )

            interactionResults.value = (interactionResults.value + result).takeLast(1000)

            Timber.d("Processed interaction ${interaction.id} with ${immediateReactions.size + personalizedReactions.size} reactions in ${processingTime}ms")

            response

        } catch (e: Exception) {
            Timber.e(e, "Failed to process interaction ${interaction.id}")

            // Update error statistics
            _status.value = _status.value.copy(errorCount = _status.value.errorCount + 1)

            // Return minimal response
            InteractionResponse(
                interactionId = interaction.id,
                immediateReactions = emptyList(),
                personalizedReactions = emptyList(),
                uxEnhancements = emptyList(),
                navigationActions = emptyList(),
                context = context,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            )
        } finally {
            _status.value = _status.value.copy(isProcessing = false)
        }
    }

    override suspend fun getPersonalizedReactions(
        context: UXContext,
        limit: Int
    ): List<PersonalizedReaction> {
        return personalizationSystem.getPersonalizedReactions(null, context, limit)
    }

    override suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    ) {
        try {
            // Learn interaction patterns
            patternSystem.learnFromInteraction(interaction, response, context)

            // Update personalization model
            personalizationSystem.learnFromInteraction(interaction, response, context)

            // Update UX enhancement engine context
            uxEngine.updateUserContext(context)

        } catch (e: Exception) {
            Timber.e(e, "Failed to learn from interaction ${interaction.id}")
        }
    }

    override suspend fun getInteractionStatistics(): InteractionStatistics {
        val results = interactionResults.value
        val patterns = patternSystem.getAllPatterns()

        val interactionTypes = results
            .flatMap { result -> result.reactions.map { it.type.name } + result.personalizedReactions.map { it.type.name } }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }

        val reactionTypes = results
            .flatMap { result -> result.reactions.map { it.type.name } }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }

        return InteractionStatistics(
            totalInteractionsProcessed = interactionCount,
            totalReactionsGenerated = reactionCount,
            averageReactionTimeMs = if (interactionCount > 0) {
                totalProcessingTime.toDouble() / interactionCount
            } else 0.0,
            mostCommonInteractionTypes = interactionTypes,
            mostEffectiveReactionTypes = reactionTypes,
            userEngagementScore = calculateEngagementScore(results),
            personalizationAccuracy = calculatePersonalizationAccuracy(results),
            patternDiscoveryRate = patterns.size.toDouble() / interactionCount.coerceAtLeast(1),
            reactionSuccessRate = results.count { it.success }.toDouble() / results.size.coerceAtLeast(1)
        )
    }

    override suspend fun registerReactionPattern(pattern: InteractionPattern): Boolean {
        val success = patternSystem.registerPattern(pattern)
        if (success) {
            updateStatus()
        }
        return success
    }

    override suspend fun unregisterReactionPattern(patternId: String): Boolean {
        val success = patternSystem.unregisterPattern(patternId)
        if (success) {
            updateStatus()
        }
        return success
    }

    override suspend fun updateUserBehaviorModel(context: UXContext) {
        patternSystem.updateUserBehaviorModel(context)
        personalizationSystem.updateUserBehaviorModel(context)
    }

    override suspend fun reset() {
        interactionCount = 0
        reactionCount = 0
        totalProcessingTime = 0
        interactionResults.value = emptyList()
        patternSystem.reset()
        personalizationSystem.reset()
        updateStatus()
    }

    override suspend fun shutdown() {
        try {
            Timber.d("Shutting down Interaction Response System")
            reset()
            _status.value = SystemStatus()
        } catch (e: Exception) {
            Timber.e(e, "Error during Interaction Response System shutdown")
        }
    }

    override fun observeStatus(): Flow<SystemStatus> = status

    override fun observeInteractionResults(): Flow<InteractionProcessingResult> = interactionResults

    /**
     * Generate immediate reactions based on interaction type and context
     */
    private suspend fun generateImmediateReactions(
        interaction: UserInteraction,
        context: UXContext
    ): List<ImmediateReaction> {
        val reactions = mutableListOf<ImmediateReaction>()

        // Generate visual feedback for most interactions
        if (interaction.type != UserInteraction.InteractionType.VOICE_COMMAND) {
            reactions.add(
                ImmediateReaction.VisualFeedback(
                    id = "visual_${interaction.id}",
                    animationType = determineAnimationType(interaction.type),
                    priority = ImmediateReaction.Priority.MEDIUM,
                    duration = 300L,
                    conditions = listOf(
                        UXEnhancement.EnhancementCondition(
                            UXEnhancement.EnhancementCondition.ConditionType.USER_PREFERENCE,
                            "animationSpeed",
                            UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS
                        )
                    )
                )
            )
        }

        // Generate haptic feedback for touch interactions
        if (interaction.type in listOf(
                UserInteraction.InteractionType.TAP,
                UserInteraction.InteractionType.BUTTON_CLICK,
                UserInteraction.InteractionType.LONG_PRESS
            ) && context.userPreferences.hapticFeedbackEnabled) {
            reactions.add(
                ImmediateReaction.HapticFeedback(
                    id = "haptic_${interaction.id}",
                    pattern = determineHapticPattern(interaction.type),
                    intensity = ImmediateReaction.HapticFeedback.Intensity.MEDIUM,
                    priority = ImmediateReaction.Priority.HIGH
                )
            )
        }

        // Generate audio feedback for important interactions
        if (interaction.type in listOf(
                UserInteraction.InteractionType.BUTTON_CLICK,
                UserInteraction.InteractionType.NAVIGATION,
                UserInteraction.InteractionType.PURCHASE
            ) && context.userPreferences.soundEnabled) {
            reactions.add(
                ImmediateReaction.AudioFeedback(
                    id = "audio_${interaction.id}",
                    soundType = determineSoundType(interaction.type),
                    volume = 0.7f,
                    priority = ImmediateReaction.Priority.MEDIUM
                )
            )
        }

        return reactions.filter { it.meetsConditions(context) }
    }

    /**
     * Determine navigation actions based on interaction and context
     */
    private suspend fun determineNavigationActions(
        interaction: UserInteraction,
        context: UXContext,
        reactions: List<ImmediateReaction>
    ): List<NavigationAction> {
        val actions = mutableListOf<NavigationAction>()

        // Check if interaction requires navigation
        when (interaction.type) {
            UserInteraction.InteractionType.NAVIGATION -> {
                val targetDestination = interaction.metadata["destination"] as? String
                if (targetDestination != null) {
                    actions.add(
                        NavigationAction.ImmediateNavigation(
                            id = "nav_${interaction.id}",
                            destination = targetDestination,
                            arguments = interaction.metadata.filterKeys { it.startsWith("arg_") }
                                .mapKeys { it.key.removePrefix("arg_") },
                            priority = NavigationAction.Priority.HIGH,
                            conditions = listOf(
                                UXEnhancement.EnhancementCondition(
                                    UXEnhancement.EnhancementCondition.ConditionType.ENVIRONMENTAL_FACTOR,
                                    "networkQuality",
                                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS
                                )
                            )
                        )
                    )
                }
            }
            UserInteraction.InteractionType.BUTTON_CLICK -> {
                // Check if button click should trigger contextual navigation
                val contextualDestination = determineContextualNavigation(interaction, context)
                if (contextualDestination != null) {
                    actions.add(
                        NavigationAction.ContextualNavigation(
                            id = "contextual_nav_${interaction.id}",
                            destination = contextualDestination,
                            triggerCondition = NavigationAction.ContextualNavigation.TriggerCondition(
                                event = "button_click",
                                delay = 100L
                            ),
                            priority = NavigationAction.Priority.MEDIUM
                        )
                    )
                }
            }
            else -> {
                // No navigation action needed
            }
        }

        return actions.filter { it.meetsConditions(context) }
    }

    /**
     * Calculate personalization score for reactions
     */
    private fun calculatePersonalizationScore(reactions: List<PersonalizedReaction>): Double {
        if (reactions.isEmpty()) return 0.0

        return reactions.map { it.confidence }.average().coerceIn(0.0, 1.0)
    }

    /**
     * Calculate user engagement score
     */
    private fun calculateEngagementScore(results: List<InteractionProcessingResult>): Double {
        if (results.isEmpty()) return 0.0

        val successRate = results.count { it.success }.toDouble() / results.size
        val averageReactions = results.map { it.reactions.size + it.personalizedReactions.size }.average()

        return (successRate * 0.7 + (averageReactions / 10.0).coerceAtMost(1.0) * 0.3).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate personalization accuracy
     */
    private fun calculatePersonalizationAccuracy(results: List<InteractionProcessingResult>): Double {
        if (results.isEmpty()) return 0.0

        // This would be calculated based on user feedback and reaction effectiveness
        // For now, return a placeholder based on personalized reaction count
        val personalizedReactionRate = results.map { it.personalizedReactions.size }.average() / results.map { it.reactions.size }.average()
        return personalizedReactionRate.coerceIn(0.0, 1.0)
    }

    /**
     * Update system status
     */
    private fun updateStatus() {
        _status.value = _status.value.copy(
            lastActivityTime = System.currentTimeMillis(),
            totalInteractionsProcessed = interactionCount,
            totalReactionsGenerated = reactionCount,
            averageProcessingTimeMs = if (interactionCount > 0) {
                totalProcessingTime.toDouble() / interactionCount
            } else 0.0,
            patternCount = patternSystem.getPatternCount(),
            personalizationScore = calculatePersonalizationScore(interactionResults.value.flatMap { it.personalizedReactions })
        )
    }

    /**
     * Determine animation type based on interaction type
     */
    private fun determineAnimationType(interactionType: UserInteraction.InteractionType): ImmediateReaction.VisualFeedback.AnimationType {
        return when (interactionType) {
            UserInteraction.InteractionType.TAP -> ImmediateReaction.VisualFeedback.AnimationType.SCALE_DOWN
            UserInteraction.InteractionType.BUTTON_CLICK -> ImmediateReaction.VisualFeedback.AnimationType.BOUNCE
            UserInteraction.InteractionType.SWIPE -> ImmediateReaction.VisualFeedback.AnimationType.SLIDE_IN
            UserInteraction.InteractionType.LONG_PRESS -> ImmediateReaction.VisualFeedback.AnimationType.PULSE
            UserInteraction.InteractionType.SCROLL -> ImmediateReaction.VisualFeedback.AnimationType.FADE_IN
            else -> ImmediateReaction.VisualFeedback.AnimationType.FADE_IN
        }
    }

    /**
     * Determine haptic pattern based on interaction type
     */
    private fun determineHapticPattern(interactionType: UserInteraction.InteractionType): ImmediateReaction.HapticFeedback.HapticPattern {
        return when (interactionType) {
            UserInteraction.InteractionType.TAP -> ImmediateReaction.HapticFeedback.HapticPattern.LIGHT_TICK
            UserInteraction.InteractionType.BUTTON_CLICK -> ImmediateReaction.HapticFeedback.HapticPattern.MEDIUM_TICK
            UserInteraction.InteractionType.LONG_PRESS -> ImmediateReaction.HapticFeedback.HapticPattern.CONTINUOUS_HUM
            else -> ImmediateReaction.HapticFeedback.HapticPattern.LIGHT_TICK
        }
    }

    /**
     * Determine sound type based on interaction type
     */
    private fun determineSoundType(interactionType: UserInteraction.InteractionType): ImmediateReaction.AudioFeedback.SoundType {
        return when (interactionType) {
            UserInteraction.InteractionType.BUTTON_CLICK -> ImmediateReaction.AudioFeedback.SoundType.CLICK
            UserInteraction.InteractionType.NAVIGATION -> ImmediateReaction.AudioFeedback.SoundType.SUCCESS
            UserInteraction.InteractionType.PURCHASE -> ImmediateReaction.AudioFeedback.SoundType.SUCCESS
            else -> ImmediateReaction.AudioFeedback.SoundType.CLICK
        }
    }

    /**
     * Determine contextual navigation based on interaction and context
     */
    private fun determineContextualNavigation(interaction: UserInteraction, context: UXContext): String? {
        // This would implement logic to determine if a button click should trigger navigation
        // based on the current context and user behavior patterns
        return when (interaction.context.componentId) {
            "game_start_button" -> "game_player"
            "profile_button" -> "profile"
            "settings_button" -> "settings"
            else -> null
        }
    }
}

/**
 * Factory for creating Interaction Response System instances
 */
object InteractionResponseSystemFactory {

    fun create(
        patternSystem: InteractionPatternSystem,
        personalizationSystem: PersonalizedReactionSystem
    ): InteractionResponseSystem {
        return InteractionResponseSystemImpl(patternSystem, personalizationSystem)
    }

    fun createWithDefaults(): InteractionResponseSystem {
        val patternSystem = InteractionPatternSystemImpl()
        val personalizationSystem = PersonalizedReactionSystemImpl()

        return InteractionResponseSystemImpl(patternSystem, personalizationSystem)
    }
}