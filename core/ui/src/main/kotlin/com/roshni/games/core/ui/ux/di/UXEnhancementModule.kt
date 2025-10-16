package com.roshni.games.core.ui.ux.di

import com.roshni.games.core.ui.ux.engine.UXEnhancementEngine
import com.roshni.games.core.ui.ux.engine.UXEnhancementEngineFactory
import com.roshni.games.core.ui.ux.feature.UXEnhancementFeature
import com.roshni.games.core.ui.ux.feature.UXEnhancementFeatureFactory
import com.roshni.games.core.ui.ux.recommendation.UXRecommendationEngine
import com.roshni.games.core.ui.ux.recommendation.UXRecommendationEngineFactory
import com.roshni.games.core.ui.ux.rules.UXEnhancementRuleEngine
import com.roshni.games.core.ui.ux.rules.UXEnhancementRuleEngineImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dependency injection module for UX Enhancement system
 */
@Module
@InstallIn(SingletonComponent::class)
object UXEnhancementModule {

    @Provides
    @Singleton
    @UXEnhancementRuleEngine
    fun provideUXEnhancementRuleEngine(): UXEnhancementRuleEngine {
        Timber.d("Creating UX Enhancement Rule Engine")
        return UXEnhancementRuleEngineImpl()
    }

    @Provides
    @Singleton
    @UXRecommendationEngine
    fun provideUXRecommendationEngine(): UXRecommendationEngine {
        Timber.d("Creating UX Recommendation Engine")
        return UXRecommendationEngineFactory.createWithDefaultCatalog()
    }

    @Provides
    @Singleton
    @UXEnhancementEngine
    fun provideUXEnhancementEngine(
        @UXEnhancementRuleEngine ruleEngine: UXEnhancementRuleEngine,
        @UXRecommendationEngine recommendationEngine: UXRecommendationEngine
    ): UXEnhancementEngine {
        Timber.d("Creating UX Enhancement Engine")
        return UXEnhancementEngineFactory.create(ruleEngine, recommendationEngine)
    }

    @Provides
    @Singleton
    fun provideUXEnhancementFeature(
        @UXEnhancementEngine uxEnhancementEngine: UXEnhancementEngine
    ): UXEnhancementFeature {
        Timber.d("Creating UX Enhancement Feature")
        return UXEnhancementFeatureFactory.createWithCustomEngines(uxEnhancementEngine)
    }
}

/**
 * ViewModel-scoped module for UX Enhancement components
 */
@Module
@InstallIn(ViewModelComponent::class)
object UXEnhancementViewModelModule {

    @Provides
    @ViewModelScoped
    @UXEnhancementDispatcher
    fun provideUXEnhancementDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @ViewModelScoped
    fun provideUXEnhancementConfig(): UXEnhancementConfig {
        return UXEnhancementConfig(
            enableAnalytics = true,
            enablePersonalization = true,
            maxEnhancementsPerInteraction = 5,
            enhancementTimeoutMs = 5000L,
            cacheSize = 100,
            enableHapticFeedback = true,
            enableAudioFeedback = true,
            enableVisualFeedback = true,
            enableContextualHelp = true
        )
    }
}

/**
 * Configuration for UX Enhancement system
 */
data class UXEnhancementConfig(
    val enableAnalytics: Boolean = true,
    val enablePersonalization: Boolean = true,
    val maxEnhancementsPerInteraction: Int = 5,
    val enhancementTimeoutMs: Long = 5000L,
    val cacheSize: Int = 100,
    val enableHapticFeedback: Boolean = true,
    val enableAudioFeedback: Boolean = true,
    val enableVisualFeedback: Boolean = true,
    val enableContextualHelp: Boolean = true
)

/**
 * Qualifiers for dependency injection
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UXEnhancementEngine

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UXEnhancementRuleEngine

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UXRecommendationEngine

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UXEnhancementDispatcher

/**
 * Interface for UX Enhancement service that can be injected into ViewModels and other components
 */
interface UXEnhancementService {

    /**
     * Process a user interaction with UX enhancements
     */
    suspend fun processInteraction(
        interaction: com.roshni.games.core.ui.ux.model.UserInteraction,
        context: com.roshni.games.core.ui.ux.model.UXContext
    ): com.roshni.games.core.ui.ux.model.EnhancedInteraction

    /**
     * Get personalized UX recommendations
     */
    suspend fun getPersonalizedRecommendations(
        context: com.roshni.games.core.ui.ux.model.UXContext,
        limit: Int = 10
    ): List<com.roshni.games.core.ui.ux.model.UXEnhancement>

    /**
     * Record user feedback for an enhancement
     */
    suspend fun recordFeedback(
        enhancementId: String,
        interactionId: String,
        feedback: com.roshni.games.core.ui.ux.engine.EnhancementFeedback
    )

    /**
     * Update user context for personalization
     */
    suspend fun updateUserContext(context: com.roshni.games.core.ui.ux.model.UXContext)

    /**
     * Get enhancement statistics
     */
    suspend fun getStatistics(): Map<String, Any>
}

/**
 * Implementation of UX Enhancement service
 */
class UXEnhancementServiceImpl(
    private val uxEnhancementEngine: UXEnhancementEngine,
    private val config: UXEnhancementConfig
) : UXEnhancementService {

    override suspend fun processInteraction(
        interaction: com.roshni.games.core.ui.ux.model.UserInteraction,
        context: com.roshni.games.core.ui.ux.model.UXContext
    ): com.roshni.games.core.ui.ux.model.EnhancedInteraction {
        return uxEnhancementEngine.processInteraction(interaction, context)
    }

    override suspend fun getPersonalizedRecommendations(
        context: com.roshni.games.core.ui.ux.model.UXContext,
        limit: Int
    ): List<com.roshni.games.core.ui.ux.model.UXEnhancement> {
        return uxEnhancementEngine.getPersonalizedRecommendations(context, limit)
    }

    override suspend fun recordFeedback(
        enhancementId: String,
        interactionId: String,
        feedback: com.roshni.games.core.ui.ux.engine.EnhancementFeedback
    ) {
        uxEnhancementEngine.recordEnhancementFeedback(enhancementId, interactionId, feedback)
    }

    override suspend fun updateUserContext(context: com.roshni.games.core.ui.ux.model.UXContext) {
        uxEnhancementEngine.updateUserContext(context)
    }

    override suspend fun getStatistics(): Map<String, Any> {
        return uxEnhancementEngine.getEnhancementStatistics().let { stats ->
            mapOf(
                "config" to config,
                "engineStats" to stats,
                "isEnabled" to true,
                "version" to "1.0.0"
            )
        }
    }
}

/**
 * Bindings for UX Enhancement service
 */
@Module
@InstallIn(ViewModelComponent::class)
interface UXEnhancementServiceModule {

    @Binds
    @ViewModelScoped
    fun bindUXEnhancementService(
        impl: UXEnhancementServiceImpl
    ): UXEnhancementService
}

/**
 * Extension functions for easier injection and usage
 */
object UXEnhancementExtensions {

    /**
     * Create a UXContext with common defaults
     */
    fun createUXContext(
        userId: String? = null,
        sessionId: String? = null,
        screenName: String,
        gameId: String? = null,
        componentId: String? = null,
        userPreferences: com.roshni.games.core.ui.ux.model.UXContext.UserPreferences = com.roshni.games.core.ui.ux.model.UXContext.UserPreferences(),
        deviceCapabilities: com.roshni.games.core.ui.ux.model.UXContext.DeviceCapabilities = com.roshni.games.core.ui.ux.model.UXContext.DeviceCapabilities(),
        currentGameState: com.roshni.games.core.ui.ux.model.UXContext.GameState? = null,
        interactionHistory: List<com.roshni.games.core.ui.ux.model.UserInteraction> = emptyList(),
        environmentalFactors: com.roshni.games.core.ui.ux.model.UXContext.EnvironmentalFactors = com.roshni.games.core.ui.ux.model.UXContext.EnvironmentalFactors()
    ): com.roshni.games.core.ui.ux.model.UXContext {
        return com.roshni.games.core.ui.ux.model.UXContext(
            userId = userId,
            sessionId = sessionId,
            screenName = screenName,
            gameId = gameId,
            componentId = componentId,
            userPreferences = userPreferences,
            deviceCapabilities = deviceCapabilities,
            currentGameState = currentGameState,
            interactionHistory = interactionHistory,
            environmentalFactors = environmentalFactors
        )
    }

    /**
     * Create a UserInteraction with common defaults
     */
    fun createUserInteraction(
        id: String,
        type: com.roshni.games.core.ui.ux.model.UserInteraction.InteractionType,
        screenName: String,
        componentId: String? = null,
        userId: String? = null,
        sessionId: String? = null,
        gameId: String? = null,
        position: com.roshni.games.core.ui.ux.model.UserInteraction.InteractionPosition? = null,
        metadata: Map<String, Any> = emptyMap()
    ): com.roshni.games.core.ui.ux.model.UserInteraction {
        val context = com.roshni.games.core.ui.ux.model.UserInteraction.InteractionContext(
            screenName = screenName,
            componentId = componentId,
            userId = userId,
            sessionId = sessionId,
            gameId = gameId,
            position = position,
            deviceInfo = com.roshni.games.core.ui.ux.model.UserInteraction.DeviceInfo(),
            accessibilityInfo = com.roshni.games.core.ui.ux.model.UserInteraction.AccessibilityInfo()
        )

        return com.roshni.games.core.ui.ux.model.UserInteraction(
            id = id,
            type = type,
            timestamp = System.currentTimeMillis(),
            context = context,
            metadata = metadata
        )
    }

    /**
     * Create enhancement feedback with common defaults
     */
    fun createEnhancementFeedback(
        rating: Int,
        helpful: Boolean,
        comments: String? = null,
        context: Map<String, Any> = emptyMap()
    ): com.roshni.games.core.ui.ux.engine.EnhancementFeedback {
        return com.roshni.games.core.ui.ux.engine.EnhancementFeedback(
            rating = rating,
            helpful = helpful,
            comments = comments,
            context = context
        )
    }
}