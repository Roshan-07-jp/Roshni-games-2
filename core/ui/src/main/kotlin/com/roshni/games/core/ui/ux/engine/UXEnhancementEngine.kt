package com.roshni.games.core.ui.ux.engine

import com.roshni.games.core.ui.ux.model.EnhancedInteraction
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import com.roshni.games.core.ui.ux.recommendation.UXRecommendationEngine
import com.roshni.games.core.ui.ux.rules.UXEnhancementRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Core interface for the UX Enhancement Engine
 */
interface UXEnhancementEngine {

    /**
     * Current status of the engine
     */
    val status: StateFlow<EngineStatus>

    /**
     * Whether the engine is initialized and ready
     */
    val isReady: Boolean

    /**
     * Initialize the UX enhancement engine
     */
    suspend fun initialize(context: UXContext): Boolean

    /**
     * Process a user interaction and return enhanced version
     */
    suspend fun processInteraction(
        interaction: UserInteraction,
        context: UXContext
    ): EnhancedInteraction

    /**
     * Get applicable enhancements for a context without processing an interaction
     */
    suspend fun getEnhancementsForContext(context: UXContext): List<UXEnhancement>

    /**
     * Register a custom enhancement rule
     */
    suspend fun registerEnhancementRule(rule: com.roshni.games.core.ui.ux.rules.UXEnhancementRule): Boolean

    /**
     * Unregister an enhancement rule
     */
    suspend fun unregisterEnhancementRule(ruleId: String): Boolean

    /**
     * Get all registered enhancement rules
     */
    suspend fun getEnhancementRules(): List<com.roshni.games.core.ui.ux.rules.UXEnhancementRule>

    /**
     * Update user context for personalization
     */
    suspend fun updateUserContext(context: UXContext)

    /**
     * Get personalized recommendations for a user
     */
    suspend fun getPersonalizedRecommendations(
        context: UXContext,
        limit: Int = 10
    ): List<UXEnhancement>

    /**
     * Record user feedback for an enhancement
     */
    suspend fun recordEnhancementFeedback(
        enhancementId: String,
        interactionId: String,
        feedback: EnhancementFeedback
    )

    /**
     * Get enhancement statistics
     */
    suspend fun getEnhancementStatistics(): EnhancementStatistics

    /**
     * Clear all cached data and reset the engine
     */
    suspend fun reset()

    /**
     * Shutdown the engine and cleanup resources
     */
    suspend fun shutdown()

    /**
     * Observe engine status changes
     */
    fun observeStatus(): Flow<EngineStatus>

    /**
     * Observe enhancement applications
     */
    fun observeEnhancementApplications(): Flow<EnhancementApplication>
}

/**
 * Status of the UX enhancement engine
 */
data class EngineStatus(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val lastActivityTime: Long? = null,
    val totalInteractionsProcessed: Long = 0,
    val totalEnhancementsApplied: Long = 0,
    val averageProcessingTimeMs: Double = 0.0,
    val errorCount: Long = 0,
    val ruleCount: Int = 0,
    val recommendationCount: Int = 0
)

/**
 * Feedback for an enhancement
 */
data class EnhancementFeedback(
    val rating: Int, // 1-5 stars
    val helpful: Boolean,
    val comments: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val context: Map<String, Any> = emptyMap()
)

/**
 * Statistics about enhancement usage
 */
data class EnhancementStatistics(
    val totalEnhancementsApplied: Long = 0,
    val enhancementsByType: Map<UXEnhancement.Type, Long> = emptyMap(),
    val averageUserRating: Double = 0.0,
    val mostUsedEnhancements: List<String> = emptyList(),
    val leastUsedEnhancements: List<String> = emptyList(),
    val userSatisfactionScore: Double = 0.0,
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics()
) {

    data class PerformanceMetrics(
        val averageProcessingTimeMs: Double = 0.0,
        val memoryUsageKb: Long = 0,
        val cacheHitRate: Double = 0.0,
        val ruleEvaluationTimeMs: Double = 0.0,
        val recommendationTimeMs: Double = 0.0
    )
}

/**
 * Result of enhancement application
 */
data class EnhancementApplication(
    val interactionId: String,
    val enhancements: List<UXEnhancement>,
    val appliedRules: List<String>,
    val processingTimeMs: Long,
    val success: Boolean,
    val timestamp: Long,
    val error: String? = null
)

/**
 * Default implementation of the UX Enhancement Engine
 */
class UXEnhancementEngineImpl(
    private val ruleEngine: UXEnhancementRuleEngine,
    private val recommendationEngine: UXRecommendationEngine
) : UXEnhancementEngine {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(EngineStatus())
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val enhancementApplications = MutableStateFlow<List<EnhancementApplication>>(emptyList())
    private val enhancementFeedback = MutableStateFlow<List<EnhancementFeedback>>(emptyList())

    private var interactionCount = 0L
    private var enhancementCount = 0L
    private var totalProcessingTime = 0L

    override val isReady: Boolean
        get() = _status.value.isInitialized

    override suspend fun initialize(context: UXContext): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing UX Enhancement Engine")

            _status.value = _status.value.copy(isInitialized = false)

            // Initialize rule engine
            // Note: Rule engine initialization would need proper context

            // Initialize recommendation engine
            // Note: Recommendation engine initialization would need proper context

            _status.value = _status.value.copy(
                isInitialized = true,
                lastActivityTime = System.currentTimeMillis(),
                ruleCount = ruleEngine.rules.value.size,
                recommendationCount = 0 // Would be set from recommendation engine
            )

            Timber.d("UX Enhancement Engine initialized successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize UX Enhancement Engine")
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
    ): EnhancedInteraction = mutex.withLock {
        val startTime = System.currentTimeMillis()

        try {
            _status.value = _status.value.copy(isProcessing = true)

            // Get applicable enhancements from rule engine
            val ruleEnhancements = ruleEngine.evaluateRules(context, interaction)

            // Get personalized recommendations
            val recommendedEnhancements = recommendationEngine.getRecommendations(
                context = context,
                interaction = interaction,
                limit = 5
            )

            // Combine and filter enhancements
            val allEnhancements = (ruleEnhancements + recommendedEnhancements)
                .distinctBy { it.id }
                .filter { it.meetsConditions(context) }
                .sortedByDescending { it.priority }

            // Calculate personalization score and confidence
            val personalizationScore = calculatePersonalizationScore(context, allEnhancements)
            val confidence = calculateConfidence(context, interaction, allEnhancements)

            // Create enhanced interaction
            val enhancedInteraction = EnhancedInteraction(
                originalInteraction = interaction,
                enhancements = allEnhancements,
                appliedRules = ruleEnhancements.mapNotNull { enhancement ->
                    // This would need to track which rule generated which enhancement
                    null
                },
                personalizationScore = personalizationScore,
                confidence = confidence,
                timestamp = System.currentTimeMillis()
            )

            // Update statistics
            val processingTime = System.currentTimeMillis() - startTime
            interactionCount++
            enhancementCount += allEnhancements.size
            totalProcessingTime += processingTime

            updateStatus()

            // Record application
            val application = EnhancementApplication(
                interactionId = interaction.id,
                enhancements = allEnhancements,
                appliedRules = emptyList(), // Would be populated from rule tracking
                processingTimeMs = processingTime,
                success = true,
                timestamp = System.currentTimeMillis()
            )

            enhancementApplications.value = (enhancementApplications.value + application).takeLast(1000)

            Timber.d("Processed interaction ${interaction.id} with ${allEnhancements.size} enhancements in ${processingTime}ms")

            enhancedInteraction

        } catch (e: Exception) {
            Timber.e(e, "Failed to process interaction ${interaction.id}")

            // Update error statistics
            _status.value = _status.value.copy(errorCount = _status.value.errorCount + 1)

            // Return interaction with no enhancements
            EnhancedInteraction(
                originalInteraction = interaction,
                enhancements = emptyList(),
                appliedRules = emptyList(),
                personalizationScore = 0.0,
                confidence = 0.0,
                timestamp = System.currentTimeMillis()
            )
        } finally {
            _status.value = _status.value.copy(isProcessing = false)
        }
    }

    override suspend fun getEnhancementsForContext(context: UXContext): List<UXEnhancement> {
        return ruleEngine.evaluateRules(context, null)
    }

    override suspend fun registerEnhancementRule(rule: com.roshni.games.core.ui.ux.rules.UXEnhancementRule): Boolean {
        val success = ruleEngine.addRule(rule)
        if (success) {
            updateStatus()
        }
        return success
    }

    override suspend fun unregisterEnhancementRule(ruleId: String): Boolean {
        val success = ruleEngine.removeRule(ruleId)
        if (success) {
            updateStatus()
        }
        return success
    }

    override suspend fun getEnhancementRules(): List<com.roshni.games.core.ui.ux.rules.UXEnhancementRule> {
        return ruleEngine.rules.value
    }

    override suspend fun updateUserContext(context: UXContext) {
        // Update recommendation engine with new context
        recommendationEngine.updateUserContext(context)
    }

    override suspend fun getPersonalizedRecommendations(
        context: UXContext,
        limit: Int
    ): List<UXEnhancement> {
        return recommendationEngine.getRecommendations(context, null, limit)
    }

    override suspend fun recordEnhancementFeedback(
        enhancementId: String,
        interactionId: String,
        feedback: EnhancementFeedback
    ) {
        enhancementFeedback.value = enhancementFeedback.value + feedback
        recommendationEngine.recordFeedback(enhancementId, feedback)
    }

    override suspend fun getEnhancementStatistics(): EnhancementStatistics {
        val feedback = enhancementFeedback.value
        val applications = enhancementApplications.value

        val enhancementsByType = applications
            .flatMap { it.enhancements }
            .groupBy { it.type }
            .mapValues { it.value.size.toLong() }

        val averageRating = if (feedback.isNotEmpty()) {
            feedback.map { it.rating }.average()
        } else 0.0

        val mostUsedEnhancements = applications
            .flatMap { app -> app.enhancements.map { it.id } }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }

        return EnhancementStatistics(
            totalEnhancementsApplied = applications.sumOf { it.enhancements.size.toLong() },
            enhancementsByType = enhancementsByType,
            averageUserRating = averageRating,
            mostUsedEnhancements = mostUsedEnhancements,
            userSatisfactionScore = calculateSatisfactionScore(feedback),
            performanceMetrics = EnhancementStatistics.PerformanceMetrics(
                averageProcessingTimeMs = if (interactionCount > 0) {
                    totalProcessingTime.toDouble() / interactionCount
                } else 0.0,
                cacheHitRate = 0.0, // Would be calculated from cache metrics
                ruleEvaluationTimeMs = 0.0, // Would be calculated from rule engine metrics
                recommendationTimeMs = 0.0 // Would be calculated from recommendation engine metrics
            )
        )
    }

    override suspend fun reset() {
        interactionCount = 0
        enhancementCount = 0
        totalProcessingTime = 0
        enhancementApplications.value = emptyList()
        enhancementFeedback.value = emptyList()
        updateStatus()
    }

    override suspend fun shutdown() {
        try {
            Timber.d("Shutting down UX Enhancement Engine")
            reset()
            _status.value = EngineStatus()
        } catch (e: Exception) {
            Timber.e(e, "Error during UX Enhancement Engine shutdown")
        }
    }

    override fun observeStatus(): Flow<EngineStatus> = status

    override fun observeEnhancementApplications(): Flow<EnhancementApplication> = enhancementApplications

    /**
     * Calculate personalization score based on context and enhancements
     */
    private fun calculatePersonalizationScore(
        context: UXContext,
        enhancements: List<UXEnhancement>
    ): Double {
        if (enhancements.isEmpty()) return 0.0

        var score = 0.0

        // Factor in user preferences match
        val preferenceMatches = enhancements.count { enhancement ->
            enhancement.conditions.any { condition ->
                when (condition.type) {
                    UXEnhancement.EnhancementCondition.ConditionType.USER_PREFERENCE -> true
                    else -> false
                }
            }
        }
        score += (preferenceMatches.toDouble() / enhancements.size) * 0.4

        // Factor in device capability utilization
        val capabilityMatches = enhancements.count { enhancement ->
            enhancement.conditions.any { condition ->
                when (condition.type) {
                    UXEnhancement.EnhancementCondition.ConditionType.DEVICE_CAPABILITY -> true
                    else -> false
                }
            }
        }
        score += (capabilityMatches.toDouble() / enhancements.size) * 0.3

        // Factor in contextual relevance
        val contextualMatches = enhancements.count { enhancement ->
            enhancement.conditions.any { condition ->
                when (condition.type) {
                    UXEnhancement.EnhancementCondition.ConditionType.ENVIRONMENTAL_FACTOR,
                    UXEnhancement.EnhancementCondition.ConditionType.GAME_STATE -> true
                    else -> false
                }
            }
        }
        score += (contextualMatches.toDouble() / enhancements.size) * 0.3

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate confidence score for enhancements
     */
    private fun calculateConfidence(
        context: UXContext,
        interaction: UserInteraction,
        enhancements: List<UXEnhancement>
    ): Double {
        if (enhancements.isEmpty()) return 0.0

        var confidence = 0.0

        // Base confidence from rule matches
        val ruleMatches = enhancements.count { it.meetsConditions(context) }
        confidence += (ruleMatches.toDouble() / enhancements.size) * 0.6

        // Boost confidence for high-priority enhancements
        val highPriorityCount = enhancements.count { it.priority == UXEnhancement.Priority.HIGH }
        confidence += (highPriorityCount.toDouble() / enhancements.size) * 0.2

        // Factor in interaction history relevance
        val history = context.interactionHistory
        if (history.isNotEmpty()) {
            val recentInteractions = history.takeLast(10)
            val similarInteractions = recentInteractions.filter {
                it.type == interaction.type && it.context.screenName == interaction.context.screenName
            }
            confidence += (similarInteractions.size.toDouble() / recentInteractions.size) * 0.2
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate user satisfaction score from feedback
     */
    private fun calculateSatisfactionScore(feedback: List<EnhancementFeedback>): Double {
        if (feedback.isEmpty()) return 0.0

        val averageRating = feedback.map { it.rating }.average()
        val helpfulRatio = feedback.count { it.helpful }.toDouble() / feedback.size

        return (averageRating / 5.0 * 0.7) + (helpfulRatio * 0.3)
    }

    /**
     * Update engine status
     */
    private fun updateStatus() {
        _status.value = _status.value.copy(
            lastActivityTime = System.currentTimeMillis(),
            totalInteractionsProcessed = interactionCount,
            totalEnhancementsApplied = enhancementCount,
            averageProcessingTimeMs = if (interactionCount > 0) {
                totalProcessingTime.toDouble() / interactionCount
            } else 0.0,
            ruleCount = ruleEngine.rules.value.size
        )
    }
}

/**
 * Factory for creating UX Enhancement Engine instances
 */
object UXEnhancementEngineFactory {

    fun create(
        ruleEngine: UXEnhancementRuleEngine,
        recommendationEngine: UXRecommendationEngine
    ): UXEnhancementEngine {
        return UXEnhancementEngineImpl(ruleEngine, recommendationEngine)
    }

    fun createWithDefaults(): UXEnhancementEngine {
        val ruleEngine = UXEnhancementRuleEngineImpl()
        // Recommendation engine would be created here
        // val recommendationEngine = UXRecommendationEngineImpl()
        // For now, create a placeholder
        val recommendationEngine = object : UXRecommendationEngine {
            override suspend fun getRecommendations(
                context: UXContext,
                interaction: UserInteraction?,
                limit: Int
            ): List<UXEnhancement> = emptyList()

            override suspend fun updateUserContext(context: UXContext) {}
            override suspend fun recordFeedback(enhancementId: String, feedback: EnhancementFeedback) {}
            override suspend fun getRecommendationStatistics(): Map<String, Any> = emptyMap()
        }

        return UXEnhancementEngineImpl(ruleEngine, recommendationEngine)
    }
}