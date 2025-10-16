package com.roshni.games.core.ui.interaction.pattern

import com.roshni.games.core.ui.interaction.InteractionResponse
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.math.abs

/**
 * Interface for learning and managing user interaction patterns
 */
interface InteractionPatternSystem {

    /**
     * Current status of the pattern system
     */
    val status: StateFlow<PatternSystemStatus>

    /**
     * Whether the system is initialized and ready
     */
    val isReady: Boolean

    /**
     * Initialize the pattern system
     */
    suspend fun initialize(context: UXContext): Boolean

    /**
     * Learn from a user interaction and response
     */
    suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    )

    /**
     * Register a new interaction pattern
     */
    suspend fun registerPattern(pattern: InteractionPattern): Boolean

    /**
     * Unregister an interaction pattern
     */
    suspend fun unregisterPattern(patternId: String): Boolean

    /**
     * Get all registered patterns
     */
    suspend fun getAllPatterns(): List<InteractionPattern>

    /**
     * Get patterns by category
     */
    suspend fun getPatternsByCategory(category: PatternCategory): List<InteractionPattern>

    /**
     * Get patterns by interaction type
     */
    suspend fun getPatternsByInteractionType(interactionType: UserInteraction.InteractionType): List<InteractionPattern>

    /**
     * Find matching patterns for an interaction
     */
    suspend fun findMatchingPatterns(
        interaction: UserInteraction,
        context: UXContext
    ): List<PatternMatch>

    /**
     * Update user behavior model based on recent interactions
     */
    suspend fun updateUserBehaviorModel(context: UXContext)

    /**
     * Get pattern statistics
     */
    suspend fun getPatternStatistics(): PatternStatistics

    /**
     * Get pattern count
     */
    suspend fun getPatternCount(): Int

    /**
     * Clear all learned patterns
     */
    suspend fun reset()

    /**
     * Observe pattern discoveries
     */
    fun observePatternDiscoveries(): Flow<PatternDiscovery>

    /**
     * Observe pattern matches
     */
    fun observePatternMatches(): Flow<PatternMatch>
}

/**
 * Status of the pattern system
 */
data class PatternSystemStatus(
    val isInitialized: Boolean = false,
    val totalPatternsLearned: Int = 0,
    val totalInteractionsAnalyzed: Long = 0,
    val patternDiscoveryRate: Double = 0.0,
    val averageConfidence: Double = 0.0,
    val lastLearningTime: Long? = null,
    val memoryUsageKb: Long = 0
)

/**
 * Statistics about pattern learning and matching
 */
data class PatternStatistics(
    val totalPatternsLearned: Int = 0,
    val totalInteractionsAnalyzed: Long = 0,
    val averagePatternConfidence: Double = 0.0,
    val mostCommonPatterns: List<String> = emptyList(),
    val patternDiscoveryRate: Double = 0.0,
    val averagePatternsPerInteraction: Double = 0.0,
    val learningAccuracy: Double = 0.0,
    val memoryEfficiency: Double = 0.0
)

/**
 * Result of pattern matching
 */
data class PatternMatch(
    val pattern: InteractionPattern,
    val confidence: Double,
    val matchedConditions: List<String>,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Notification of a newly discovered pattern
 */
data class PatternDiscovery(
    val pattern: InteractionPattern,
    val discoveryMethod: DiscoveryMethod,
    val confidence: Double,
    val sampleSize: Int,
    val timestamp: Long
)

/**
 * How a pattern was discovered
 */
enum class DiscoveryMethod {
    FREQUENCY_ANALYSIS, SEQUENTIAL_PATTERN, TEMPORAL_CLUSTERING,
    CONTEXTUAL_SIMILARITY, BEHAVIORAL_GROUPING, MANUAL_DEFINITION
}

/**
 * Categories of interaction patterns
 */
enum class PatternCategory {
    TEMPORAL, SPATIAL, BEHAVIORAL, CONTEXTUAL, SEQUENTIAL, FREQUENCY_BASED
}

/**
 * Default implementation of the Interaction Pattern System
 */
class InteractionPatternSystemImpl : InteractionPatternSystem {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(PatternSystemStatus())
    override val status: StateFlow<PatternSystemStatus> = _status.asStateFlow()

    private val registeredPatterns = MutableStateFlow<List<InteractionPattern>>(emptyList())
    private val patternMatches = MutableStateFlow<List<PatternMatch>>(emptyList())
    private val patternDiscoveries = MutableStateFlow<List<PatternDiscovery>>(emptyList())

    // Learning data storage
    private val interactionHistory = mutableListOf<InteractionData>()
    private val patternFrequency = mutableMapOf<String, Int>()
    private val userBehaviorModel = mutableMapOf<String, UserBehaviorProfile>()

    override val isReady: Boolean
        get() = _status.value.isInitialized

    override suspend fun initialize(context: UXContext): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing Interaction Pattern System")

            _status.value = _status.value.copy(isInitialized = false)

            // Initialize with default patterns
            initializeDefaultPatterns()

            // Build initial behavior model from context
            updateUserBehaviorModel(context)

            _status.value = _status.value.copy(
                isInitialized = true,
                lastLearningTime = System.currentTimeMillis(),
                totalPatternsLearned = registeredPatterns.value.size
            )

            Timber.d("Interaction Pattern System initialized with ${registeredPatterns.value.size} patterns")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Interaction Pattern System")
            false
        }
    }

    override suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    ) {
        mutex.withLock {
            try {
                // Store interaction data for pattern analysis
                val interactionData = InteractionData(
                    interaction = interaction,
                    response = response,
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
                interactionHistory.add(interactionData)

                // Keep only recent history (last 1000 interactions)
                if (interactionHistory.size > 1000) {
                    interactionHistory.removeAt(0)
                }

                // Update pattern frequencies
                updatePatternFrequencies(interaction, response)

                // Attempt to discover new patterns
                discoverNewPatterns(interaction, response, context)

                // Update user behavior model
                updateUserBehaviorModel(context)

                // Update statistics
                updatePatternStatistics()

                Timber.d("Learned from interaction ${interaction.id}")

            } catch (e: Exception) {
                Timber.e(e, "Failed to learn from interaction ${interaction.id}")
            }
        }
    }

    override suspend fun registerPattern(pattern: InteractionPattern): Boolean = mutex.withLock {
        try {
            // Check if pattern already exists
            if (registeredPatterns.value.any { it.id == pattern.id }) {
                return false
            }

            // Validate pattern
            if (!isValidPattern(pattern)) {
                return false
            }

            registeredPatterns.value = registeredPatterns.value + pattern
            updatePatternStatistics()

            Timber.d("Registered pattern ${pattern.id}")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to register pattern ${pattern.id}")
            false
        }
    }

    override suspend fun unregisterPattern(patternId: String): Boolean = mutex.withLock {
        try {
            val initialSize = registeredPatterns.value.size
            registeredPatterns.value = registeredPatterns.value.filter { it.id != patternId }

            val success = registeredPatterns.value.size < initialSize
            if (success) {
                updatePatternStatistics()
                Timber.d("Unregistered pattern $patternId")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister pattern $patternId")
            false
        }
    }

    override suspend fun getAllPatterns(): List<InteractionPattern> {
        return registeredPatterns.value
    }

    override suspend fun getPatternsByCategory(category: PatternCategory): List<InteractionPattern> {
        return registeredPatterns.value.filter { it.category == category }
    }

    override suspend fun getPatternsByInteractionType(interactionType: UserInteraction.InteractionType): List<InteractionPattern> {
        return registeredPatterns.value.filter { pattern ->
            pattern.triggers.any { it.interactionType == interactionType }
        }
    }

    override suspend fun findMatchingPatterns(
        interaction: UserInteraction,
        context: UXContext
    ): List<PatternMatch> = mutex.withLock {
        val matches = mutableListOf<PatternMatch>()

        for (pattern in registeredPatterns.value) {
            val confidence = calculatePatternMatchConfidence(pattern, interaction, context)
            if (confidence > 0.3) { // Minimum confidence threshold
                val matchedConditions = identifyMatchedConditions(pattern, interaction, context)

                val match = PatternMatch(
                    pattern = pattern,
                    confidence = confidence,
                    matchedConditions = matchedConditions,
                    timestamp = System.currentTimeMillis()
                )

                matches.add(match)
            }
        }

        // Sort by confidence
        val sortedMatches = matches.sortedByDescending { it.confidence }

        // Store matches for observation
        patternMatches.value = (patternMatches.value + sortedMatches).takeLast(100)

        sortedMatches
    }

    override suspend fun updateUserBehaviorModel(context: UXContext) {
        mutex.withLock {
            try {
                val userId = context.userId ?: "anonymous"

                val profile = userBehaviorModel.getOrPut(userId) {
                    UserBehaviorProfile(userId = userId)
                }

                // Update profile based on recent interactions
                val recentInteractions = interactionHistory.takeLast(50)
                profile.updateFromInteractions(recentInteractions)

                userBehaviorModel[userId] = profile

            } catch (e: Exception) {
                Timber.e(e, "Failed to update user behavior model")
            }
        }
    }

    override suspend fun getPatternStatistics(): PatternStatistics {
        val patterns = registeredPatterns.value
        val matches = patternMatches.value

        val mostCommonPatterns = patternFrequency.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        return PatternStatistics(
            totalPatternsLearned = patterns.size,
            totalInteractionsAnalyzed = interactionHistory.size.toLong(),
            averagePatternConfidence = if (matches.isNotEmpty()) {
                matches.map { it.confidence }.average()
            } else 0.0,
            mostCommonPatterns = mostCommonPatterns,
            patternDiscoveryRate = patterns.size.toDouble() / interactionHistory.size.coerceAtLeast(1),
            averagePatternsPerInteraction = if (interactionHistory.isNotEmpty()) {
                matches.size.toDouble() / interactionHistory.size
            } else 0.0,
            learningAccuracy = calculateLearningAccuracy(),
            memoryEfficiency = calculateMemoryEfficiency()
        )
    }

    override suspend fun getPatternCount(): Int {
        return registeredPatterns.value.size
    }

    override suspend fun reset() {
        interactionHistory.clear()
        patternFrequency.clear()
        userBehaviorModel.clear()
        registeredPatterns.value = emptyList()
        patternMatches.value = emptyList()
        patternDiscoveries.value = emptyList()
        updatePatternStatistics()
    }

    override fun observePatternDiscoveries(): Flow<PatternDiscovery> = patternDiscoveries

    override fun observePatternMatches(): Flow<PatternMatch> = patternMatches

    /**
     * Initialize with default interaction patterns
     */
    private suspend fun initializeDefaultPatterns() {
        val defaultPatterns = listOf(
            // Temporal patterns
            InteractionPattern(
                id = "rapid_tapping",
                name = "Rapid Tapping Pattern",
                description = "User taps rapidly on buttons",
                category = PatternCategory.TEMPORAL,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = UserInteraction.InteractionType.TAP,
                        conditions = listOf(
                            PatternCondition.TimeBasedCondition(
                                maxIntervalMs = 200,
                                minOccurrences = 3
                            )
                        )
                    )
                ),
                confidence = 0.7,
                priority = 2
            ),

            // Spatial patterns
            InteractionPattern(
                id = "corner_preference",
                name = "Corner Preference Pattern",
                description = "User prefers interactions in screen corners",
                category = PatternCategory.SPATIAL,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = UserInteraction.InteractionType.TAP,
                        conditions = listOf(
                            PatternCondition.SpatialCondition(
                                preferredRegions = listOf(
                                    ScreenRegion.TOP_LEFT, ScreenRegion.TOP_RIGHT,
                                    ScreenRegion.BOTTOM_LEFT, ScreenRegion.BOTTOM_RIGHT
                                ),
                                minOccurrences = 5
                            )
                        )
                    )
                ),
                confidence = 0.6,
                priority = 1
            ),

            // Behavioral patterns
            InteractionPattern(
                id = "completionist_behavior",
                name = "Completionist Behavior",
                description = "User thoroughly explores all options before proceeding",
                category = PatternCategory.BEHAVIORAL,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = UserInteraction.InteractionType.BUTTON_CLICK,
                        conditions = listOf(
                            PatternCondition.SequentialCondition(
                                requiredSequence = listOf("explore", "check_all", "proceed"),
                                toleranceMs = 30000
                            )
                        )
                    )
                ),
                confidence = 0.8,
                priority = 3
            ),

            // Contextual patterns
            InteractionPattern(
                id = "gaming_session",
                name = "Gaming Session Pattern",
                description = "User behavior during gaming sessions",
                category = PatternCategory.CONTEXTUAL,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = UserInteraction.InteractionType.GAME_ACTION,
                        conditions = listOf(
                            PatternCondition.ContextualCondition(
                                requiredContext = mapOf(
                                    "gameState" to "active",
                                    "sessionDuration" to "long"
                                )
                            )
                        )
                    )
                ),
                confidence = 0.7,
                priority = 2
            )
        )

        defaultPatterns.forEach { registerPattern(it) }
    }

    /**
     * Update pattern frequency tracking
     */
    private fun updatePatternFrequencies(interaction: UserInteraction, response: InteractionResponse) {
        // Track interaction type frequency
        val interactionKey = interaction.type.name
        patternFrequency[interactionKey] = patternFrequency.getOrDefault(interactionKey, 0) + 1

        // Track screen frequency
        val screenKey = "screen_${interaction.context.screenName}"
        patternFrequency[screenKey] = patternFrequency.getOrDefault(screenKey, 0) + 1

        // Track component frequency
        interaction.context.componentId?.let { componentId ->
            val componentKey = "component_$componentId"
            patternFrequency[componentKey] = patternFrequency.getOrDefault(componentKey, 0) + 1
        }
    }

    /**
     * Attempt to discover new patterns from interaction data
     */
    private suspend fun discoverNewPatterns(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    ) {
        // Frequency-based pattern discovery
        discoverFrequencyPatterns(interaction, context)

        // Sequential pattern discovery
        discoverSequentialPatterns(interaction, context)

        // Temporal pattern discovery
        discoverTemporalPatterns(interaction, context)
    }

    /**
     * Discover patterns based on interaction frequency
     */
    private suspend fun discoverFrequencyPatterns(interaction: UserInteraction, context: UXContext) {
        val recentInteractions = interactionHistory.takeLast(20)
        val interactionType = interaction.type

        // Check if this interaction type occurs frequently
        val typeCount = recentInteractions.count { it.interaction.type == interactionType }
        if (typeCount >= 5) {
            val patternId = "freq_${interactionType.name.lowercase()}_${System.currentTimeMillis()}"

            val pattern = InteractionPattern(
                id = patternId,
                name = "Frequent ${interactionType.name} Pattern",
                description = "High frequency of ${interactionType.name} interactions",
                category = PatternCategory.FREQUENCY_BASED,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = interactionType,
                        conditions = listOf(
                            PatternCondition.FrequencyCondition(
                                minOccurrences = 5,
                                timeWindowMs = 60000 // 1 minute
                            )
                        )
                    )
                ),
                confidence = 0.6,
                priority = 1,
                metadata = mapOf(
                    "discoveredBy" to "frequency_analysis",
                    "sampleSize" to typeCount
                )
            )

            if (registerPattern(pattern)) {
                val discovery = PatternDiscovery(
                    pattern = pattern,
                    discoveryMethod = DiscoveryMethod.FREQUENCY_ANALYSIS,
                    confidence = 0.6,
                    sampleSize = typeCount,
                    timestamp = System.currentTimeMillis()
                )

                patternDiscoveries.value = (patternDiscoveries.value + discovery).takeLast(50)
            }
        }
    }

    /**
     * Discover sequential interaction patterns
     */
    private suspend fun discoverSequentialPatterns(interaction: UserInteraction, context: UXContext) {
        val recentInteractions = interactionHistory.takeLast(10)

        if (recentInteractions.size >= 3) {
            // Look for repeating sequences
            val sequence = recentInteractions.map { it.interaction.type }
            val uniqueTypes = sequence.distinct()

            if (uniqueTypes.size <= 3) { // Simple repeating pattern
                val patternId = "seq_${uniqueTypes.joinToString("_")}_${System.currentTimeMillis()}"

                val pattern = InteractionPattern(
                    id = patternId,
                    name = "Sequential Pattern: ${uniqueTypes.joinToString(" → ")}",
                    description = "Repeating sequence of interaction types",
                    category = PatternCategory.SEQUENTIAL,
                    triggers = listOf(
                        InteractionTrigger(
                            interactionType = interaction.type,
                            conditions = listOf(
                                PatternCondition.SequentialCondition(
                                    requiredSequence = uniqueTypes.map { it.name },
                                    toleranceMs = 5000
                                )
                            )
                        )
                    ),
                    confidence = 0.7,
                    priority = 2,
                    metadata = mapOf(
                        "discoveredBy" to "sequential_analysis",
                        "sequence" to uniqueTypes
                    )
                )

                if (registerPattern(pattern)) {
                    val discovery = PatternDiscovery(
                        pattern = pattern,
                        discoveryMethod = DiscoveryMethod.SEQUENTIAL_PATTERN,
                        confidence = 0.7,
                        sampleSize = recentInteractions.size,
                        timestamp = System.currentTimeMillis()
                    )

                    patternDiscoveries.value = (patternDiscoveries.value + discovery).takeLast(50)
                }
            }
        }
    }

    /**
     * Discover temporal clustering patterns
     */
    private suspend fun discoverTemporalPatterns(interaction: UserInteraction, context: UXContext) {
        val recentInteractions = interactionHistory.takeLast(30)
        val now = System.currentTimeMillis()

        // Group interactions by time clusters
        val timeClusters = recentInteractions
            .groupBy { interactionData ->
                val timeDiff = now - interactionData.timestamp
                when {
                    timeDiff < 30000 -> "recent" // 30 seconds
                    timeDiff < 300000 -> "medium" // 5 minutes
                    else -> "old"
                }
            }

        val recentCluster = timeClusters["recent"] ?: emptyList()
        if (recentCluster.size >= 8) { // High activity cluster
            val patternId = "temporal_cluster_${System.currentTimeMillis()}"

            val pattern = InteractionPattern(
                id = patternId,
                name = "High Activity Temporal Cluster",
                description = "Period of high interaction frequency",
                category = PatternCategory.TEMPORAL,
                triggers = listOf(
                    InteractionTrigger(
                        interactionType = interaction.type,
                        conditions = listOf(
                            PatternCondition.TemporalCondition(
                                timeWindowMs = 30000,
                                minInteractions = 8
                            )
                        )
                    )
                ),
                confidence = 0.65,
                priority = 2,
                metadata = mapOf(
                    "discoveredBy" to "temporal_clustering",
                    "clusterSize" to recentCluster.size
                )
            )

            if (registerPattern(pattern)) {
                val discovery = PatternDiscovery(
                    pattern = pattern,
                    discoveryMethod = DiscoveryMethod.TEMPORAL_CLUSTERING,
                    confidence = 0.65,
                    sampleSize = recentCluster.size,
                    timestamp = System.currentTimeMillis()
                )

                patternDiscoveries.value = (patternDiscoveries.value + discovery).takeLast(50)
            }
        }
    }

    /**
     * Calculate how well a pattern matches an interaction
     */
    private fun calculatePatternMatchConfidence(
        pattern: InteractionPattern,
        interaction: UserInteraction,
        context: UXContext
    ): Double {
        var totalConfidence = 0.0
        var conditionCount = 0

        for (trigger in pattern.triggers) {
            if (trigger.interactionType == interaction.type) {
                for (condition in trigger.conditions) {
                    val conditionConfidence = evaluateCondition(condition, interaction, context)
                    totalConfidence += conditionConfidence
                    conditionCount++
                }
            }
        }

        return if (conditionCount > 0) {
            totalConfidence / conditionCount
        } else 0.0
    }

    /**
     * Identify which conditions of a pattern were matched
     */
    private fun identifyMatchedConditions(
        pattern: InteractionPattern,
        interaction: UserInteraction,
        context: UXContext
    ): List<String> {
        val matchedConditions = mutableListOf<String>()

        for (trigger in pattern.triggers) {
            if (trigger.interactionType == interaction.type) {
                for (condition in trigger.conditions) {
                    if (evaluateCondition(condition, interaction, context) > 0.5) {
                        matchedConditions.add(condition.toString())
                    }
                }
            }
        }

        return matchedConditions
    }

    /**
     * Evaluate a pattern condition against an interaction
     */
    private fun evaluateCondition(
        condition: PatternCondition,
        interaction: UserInteraction,
        context: UXContext
    ): Double {
        return when (condition) {
            is PatternCondition.TimeBasedCondition -> {
                evaluateTimeBasedCondition(condition, interaction)
            }
            is PatternCondition.SpatialCondition -> {
                evaluateSpatialCondition(condition, interaction)
            }
            is PatternCondition.FrequencyCondition -> {
                evaluateFrequencyCondition(condition, interaction)
            }
            is PatternCondition.SequentialCondition -> {
                evaluateSequentialCondition(condition, interaction)
            }
            is PatternCondition.ContextualCondition -> {
                evaluateContextualCondition(condition, context)
            }
            is PatternCondition.TemporalCondition -> {
                evaluateTemporalCondition(condition, interaction)
            }
        }
    }

    private fun evaluateTimeBasedCondition(condition: PatternCondition.TimeBasedCondition, interaction: UserInteraction): Double {
        val recentInteractions = interactionHistory.takeLast(10)
        val sameTypeInteractions = recentInteractions.filter { it.interaction.type == interaction.type }

        val timeDiffs = sameTypeInteractions.zipWithNext { a, b ->
            abs(a.timestamp - b.timestamp)
        }

        val avgTimeDiff = if (timeDiffs.isNotEmpty()) timeDiffs.average() else Double.MAX_VALUE

        return if (avgTimeDiff <= condition.maxIntervalMs) {
            (condition.maxIntervalMs / avgTimeDiff).coerceAtMost(1.0)
        } else 0.0
    }

    private fun evaluateSpatialCondition(condition: PatternCondition.SpatialCondition, interaction: UserInteraction): Double {
        val position = interaction.context.position ?: return 0.0

        val region = determineScreenRegion(
            position.x, position.y,
            interaction.context.deviceInfo.screenWidth,
            interaction.context.deviceInfo.screenHeight
        )

        return if (region in condition.preferredRegions) 1.0 else 0.0
    }

    private fun evaluateFrequencyCondition(condition: PatternCondition.FrequencyCondition, interaction: UserInteraction): Double {
        val timeWindow = System.currentTimeMillis() - condition.timeWindowMs
        val recentInteractions = interactionHistory.filter { it.timestamp > timeWindow }
        val sameTypeCount = recentInteractions.count { it.interaction.type == interaction.type }

        return (sameTypeCount.toDouble() / condition.minOccurrences).coerceAtMost(1.0)
    }

    private fun evaluateSequentialCondition(condition: PatternCondition.SequentialCondition, interaction: UserInteraction): Double {
        val recentInteractions = interactionHistory.takeLast(10)
        val recentTypes = recentInteractions.map { it.interaction.type.name }

        return if (recentTypes.takeLast(condition.requiredSequence.size) == condition.requiredSequence) {
            1.0
        } else 0.0
    }

    private fun evaluateContextualCondition(condition: PatternCondition.ContextualCondition, context: UXContext): Double {
        var matchCount = 0
        var totalConditions = condition.requiredContext.size

        for ((key, value) in condition.requiredContext) {
            val contextValue = when (key) {
                "screenName" -> context.screenName
                "gameState" -> context.currentGameState?.let { "active" } ?: "inactive"
                "sessionDuration" -> {
                    context.sessionId?.let { "active" } ?: "none"
                }
                else -> null
            }

            if (contextValue == value) {
                matchCount++
            }
        }

        return matchCount.toDouble() / totalConditions
    }

    private fun evaluateTemporalCondition(condition: PatternCondition.TemporalCondition, interaction: UserInteraction): Double {
        val timeWindow = System.currentTimeMillis() - condition.timeWindowMs
        val recentInteractions = interactionHistory.filter { it.timestamp > timeWindow }

        return (recentInteractions.size.toDouble() / condition.minInteractions).coerceAtMost(1.0)
    }

    /**
     * Determine which region of the screen a position falls into
     */
    private fun determineScreenRegion(x: Float, y: Float, screenWidth: Int, screenHeight: Int): ScreenRegion {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2

        return when {
            x < centerX / 2 && y < centerY / 2 -> ScreenRegion.TOP_LEFT
            x > centerX + (centerX / 2) && y < centerY / 2 -> ScreenRegion.TOP_RIGHT
            x < centerX / 2 && y > centerY + (centerY / 2) -> ScreenRegion.BOTTOM_LEFT
            x > centerX + (centerX / 2) && y > centerY + (centerY / 2) -> ScreenRegion.BOTTOM_RIGHT
            y < centerY / 2 -> ScreenRegion.TOP
            y > centerY + (centerY / 2) -> ScreenRegion.BOTTOM
            x < centerX / 2 -> ScreenRegion.LEFT
            x > centerX + (centerX / 2) -> ScreenRegion.RIGHT
            else -> ScreenRegion.CENTER
        }
    }

    /**
     * Calculate learning accuracy based on pattern performance
     */
    private fun calculateLearningAccuracy(): Double {
        // This would be calculated based on how well patterns predict user behavior
        // For now, return a placeholder based on pattern confidence
        return patternMatches.value.map { it.confidence }.average().coerceIn(0.0, 1.0)
    }

    /**
     * Calculate memory efficiency of pattern storage
     */
    private fun calculateMemoryEfficiency(): Double {
        val totalPatterns = registeredPatterns.value.size
        val totalMatches = patternMatches.value.size

        // Simple efficiency metric based on pattern reuse
        return if (totalPatterns > 0) {
            (totalMatches.toDouble() / totalPatterns).coerceAtMost(1.0)
        } else 0.0
    }

    /**
     * Update pattern system statistics
     */
    private fun updatePatternStatistics() {
        _status.value = _status.value.copy(
            totalPatternsLearned = registeredPatterns.value.size,
            totalInteractionsAnalyzed = interactionHistory.size.toLong(),
            patternDiscoveryRate = registeredPatterns.value.size.toDouble() / interactionHistory.size.coerceAtLeast(1),
            averageConfidence = patternMatches.value.map { it.confidence }.average().coerceAtMost(1.0),
            lastLearningTime = System.currentTimeMillis()
        )
    }

    /**
     * Validate that a pattern is properly configured
     */
    private fun isValidPattern(pattern: InteractionPattern): Boolean {
        return pattern.id.isNotBlank() &&
                pattern.name.isNotBlank() &&
                pattern.triggers.isNotEmpty()
    }
}

/**
 * Data class to store interaction data for pattern analysis
 */
private data class InteractionData(
    val interaction: UserInteraction,
    val response: InteractionResponse,
    val context: UXContext,
    val timestamp: Long
)

/**
 * User behavior profile for personalization
 */
private data class UserBehaviorProfile(
    val userId: String,
    val interactionPreferences: MutableMap<String, Double> = mutableMapOf(),
    val temporalPatterns: MutableMap<String, Double> = mutableMapOf(),
    val spatialPreferences: MutableMap<String, Double> = mutableMapOf(),
    val contextualBehaviors: MutableMap<String, Double> = mutableMapOf()
) {
    fun updateFromInteractions(interactions: List<InteractionData>) {
        // Update interaction type preferences
        val typeCounts = interactions.groupBy { it.interaction.type.name }
        typeCounts.forEach { (type, typeInteractions) ->
            interactionPreferences[type] = typeInteractions.size.toDouble() / interactions.size
        }

        // Update temporal patterns
        val hourOfDay = interactions.groupBy { it.timestamp / 3600000 % 24 }
        hourOfDay.forEach { (hour, hourInteractions) ->
            temporalPatterns[hour.toString()] = hourInteractions.size.toDouble() / interactions.size
        }
    }
}

/**
 * Screen regions for spatial pattern analysis
 */
private enum class ScreenRegion {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}