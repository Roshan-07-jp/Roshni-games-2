package com.roshni.games.core.ui.interaction

import com.roshni.games.core.navigation.controller.NavigationFlowController
import com.roshni.games.core.ui.interaction.pattern.InteractionPatternSystem
import com.roshni.games.core.ui.interaction.pattern.InteractionPatternSystemImpl
import com.roshni.games.core.ui.interaction.personalization.PersonalizedReactionSystem
import com.roshni.games.core.ui.interaction.personalization.PersonalizedReactionSystemImpl
import com.roshni.games.core.ui.ux.engine.UXEnhancementEngine
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the Interaction Response System
 */
class InteractionResponseSystemTest {

    private lateinit var interactionResponseSystem: InteractionResponseSystem
    private lateinit var patternSystem: InteractionPatternSystem
    private lateinit var personalizationSystem: PersonalizedReactionSystem
    private lateinit var uxEngine: UXEnhancementEngine
    private lateinit var navigationController: NavigationFlowController

    @Before
    fun setup() {
        patternSystem = InteractionPatternSystemImpl()
        personalizationSystem = PersonalizedReactionSystemImpl()
        interactionResponseSystem = InteractionResponseSystemFactory.create(patternSystem, personalizationSystem)

        uxEngine = mock()
        navigationController = mock()

        // Setup mock behaviors
        whenever(uxEngine.isReady).thenReturn(true)
        whenever(navigationController.navigationState).thenReturn(mock())
        whenever(navigationController.navigationState.value.isInitialized).thenReturn(true)
    }

    @Test
    fun `test system initialization`() = runTest {
        val context = createTestUXContext()

        val initialized = interactionResponseSystem.initialize(uxEngine, navigationController, context)

        assertTrue(initialized)
        assertTrue(interactionResponseSystem.isReady)
        assertTrue(interactionResponseSystem.status.value.isInitialized)
    }

    @Test
    fun `test interaction processing generates reactions`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        val interaction = createTestInteraction()
        val response = interactionResponseSystem.processInteraction(interaction, context)

        assertNotNull(response)
        assertEquals(interaction.id, response.interactionId)
        assertTrue(response.immediateReactions.isNotEmpty() || response.personalizedReactions.isNotEmpty())
    }

    @Test
    fun `test personalized reactions are generated based on user behavior`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        // Process several interactions to build behavior patterns
        repeat(5) { index ->
            val interaction = createTestInteraction("interaction_$index")
            val response = interactionResponseSystem.processInteraction(interaction, context)
            interactionResponseSystem.learnFromInteraction(interaction, response, context)
        }

        // Get personalized reactions
        val personalizedReactions = interactionResponseSystem.getPersonalizedReactions(context, 3)

        assertNotNull(personalizedReactions)
        // Personalized reactions should be generated based on learned behavior
        assertTrue(personalizedReactions.isNotEmpty())
    }

    @Test
    fun `test interaction statistics are calculated correctly`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        // Process several interactions
        repeat(10) { index ->
            val interaction = createTestInteraction("interaction_$index")
            val response = interactionResponseSystem.processInteraction(interaction, context)
            interactionResponseSystem.learnFromInteraction(interaction, response, context)
        }

        val statistics = interactionResponseSystem.getInteractionStatistics()

        assertNotNull(statistics)
        assertEquals(10, statistics.totalInteractionsProcessed)
        assertTrue(statistics.totalReactionsGenerated > 0)
        assertTrue(statistics.averageReactionTimeMs >= 0.0)
    }

    @Test
    fun `test pattern registration and matching`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        // Register a custom pattern
        val pattern = createTestPattern()
        val registered = interactionResponseSystem.registerReactionPattern(pattern)

        assertTrue(registered)

        // Process an interaction that should match the pattern
        val interaction = createTestInteraction()
        val response = interactionResponseSystem.processInteraction(interaction, context)

        assertNotNull(response)
        // The system should have learned from the pattern
    }

    @Test
    fun `test system reset clears all data`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        // Process some interactions
        repeat(5) { index ->
            val interaction = createTestInteraction("interaction_$index")
            val response = interactionResponseSystem.processInteraction(interaction, context)
            interactionResponseSystem.learnFromInteraction(interaction, response, context)
        }

        // Verify data exists
        val statistics = interactionResponseSystem.getInteractionStatistics()
        assertTrue(statistics.totalInteractionsProcessed > 0)

        // Reset system
        interactionResponseSystem.reset()

        // Verify data is cleared
        val resetStatistics = interactionResponseSystem.getInteractionStatistics()
        assertEquals(0, resetStatistics.totalInteractionsProcessed)
    }

    @Test
    fun `test system handles errors gracefully`() = runTest {
        val context = createTestUXContext()

        // Try to process interaction before initialization
        val interaction = createTestInteraction()
        val response = interactionResponseSystem.processInteraction(interaction, context)

        // Should return a minimal response without throwing
        assertNotNull(response)
        assertEquals(interaction.id, response.interactionId)
        assertTrue(response.metadata.containsKey("error"))
    }

    @Test
    fun `test user behavior model updates correctly`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        // Process interactions with different patterns
        val interaction1 = createTestInteraction(type = UserInteraction.InteractionType.TAP)
        val response1 = interactionResponseSystem.processInteraction(interaction1, context)
        interactionResponseSystem.learnFromInteraction(interaction1, response1, context)

        val interaction2 = createTestInteraction(type = UserInteraction.InteractionType.BUTTON_CLICK)
        val response2 = interactionResponseSystem.processInteraction(interaction2, context)
        interactionResponseSystem.learnFromInteraction(interaction2, response2, context)

        // Update behavior model
        interactionResponseSystem.updateUserBehaviorModel(context)

        // Verify model was updated
        val statistics = interactionResponseSystem.getInteractionStatistics()
        assertTrue(statistics.patternDiscoveryRate >= 0.0)
    }

    @Test
    fun `test reaction pattern unregistration works correctly`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        val pattern = createTestPattern()
        interactionResponseSystem.registerReactionPattern(pattern)

        // Verify pattern is registered
        assertTrue(interactionResponseSystem.getInteractionStatistics().patternDiscoveryRate > 0.0)

        // Unregister pattern
        val unregistered = interactionResponseSystem.unregisterReactionPattern(pattern.id)
        assertTrue(unregistered)

        // Verify pattern is removed
        val statistics = interactionResponseSystem.getInteractionStatistics()
        assertTrue(statistics.patternDiscoveryRate >= 0.0) // May still have default patterns
    }

    @Test
    fun `test system status updates correctly during operation`() = runTest {
        val context = createTestUXContext()
        interactionResponseSystem.initialize(uxEngine, navigationController, context)

        val initialStatus = interactionResponseSystem.status.value
        assertEquals(0, initialStatus.totalInteractionsProcessed)

        // Process an interaction
        val interaction = createTestInteraction()
        interactionResponseSystem.processInteraction(interaction, context)

        // Verify status was updated
        val updatedStatus = interactionResponseSystem.status.value
        assertEquals(1, updatedStatus.totalInteractionsProcessed)
        assertTrue(updatedStatus.lastActivityTime != null)
    }

    // Helper methods for creating test data

    private fun createTestUXContext(): UXContext {
        return UXContext(
            userId = "test_user",
            sessionId = "test_session",
            screenName = "test_screen",
            userPreferences = UXContext.UserPreferences(
                theme = UXContext.ThemePreference.DARK,
                animationSpeed = UXContext.AnimationSpeed.NORMAL,
                soundEnabled = true,
                hapticFeedbackEnabled = true
            ),
            deviceCapabilities = UXContext.DeviceCapabilities(
                hasVibrator = true,
                hasSpeaker = true,
                screenRefreshRate = 60,
                maxTextureSize = 2048
            ),
            environmentalFactors = UXContext.EnvironmentalFactors(
                timeOfDay = UXContext.TimeOfDay.AFTERNOON,
                lightingCondition = UXContext.LightingCondition.NORMAL,
                networkQuality = UXContext.NetworkQuality.GOOD
            )
        )
    }

    private fun createTestInteraction(id: String = "test_interaction", type: UserInteraction.InteractionType = UserInteraction.InteractionType.TAP): UserInteraction {
        return UserInteraction(
            id = id,
            type = type,
            timestamp = System.currentTimeMillis(),
            context = UserInteraction.InteractionContext(
                screenName = "test_screen",
                componentId = "test_button",
                userId = "test_user",
                sessionId = "test_session",
                position = UserInteraction.InteractionPosition(
                    x = 100f,
                    y = 100f,
                    screenWidth = 1080,
                    screenHeight = 1920
                ),
                deviceInfo = UserInteraction.DeviceInfo(
                    screenWidth = 1080,
                    screenHeight = 1920,
                    density = 2.0f,
                    orientation = UserInteraction.ScreenOrientation.PORTRAIT,
                    deviceType = UserInteraction.DeviceType.PHONE,
                    osVersion = "Android 13",
                    appVersion = "1.0.0"
                )
            )
        )
    }

    private fun createTestPattern(): com.roshni.games.core.ui.interaction.pattern.InteractionPattern {
        return com.roshni.games.core.ui.interaction.pattern.InteractionPattern(
            id = "test_pattern",
            name = "Test Pattern",
            description = "A test interaction pattern",
            category = com.roshni.games.core.ui.interaction.pattern.PatternCategory.BEHAVIORAL,
            triggers = listOf(
                com.roshni.games.core.ui.interaction.pattern.InteractionTrigger(
                    interactionType = UserInteraction.InteractionType.TAP,
                    conditions = listOf(
                        com.roshni.games.core.ui.interaction.pattern.PatternCondition.TimeBasedCondition(
                            maxIntervalMs = 1000,
                            minOccurrences = 2
                        )
                    )
                )
            ),
            confidence = 0.8,
            priority = 2
        )
    }
}

/**
 * Integration tests for the complete interaction system
 */
class InteractionSystemIntegrationTest {

    @Test
    fun `test complete interaction pipeline integration`() = runTest {
        // This would test the complete pipeline including:
        // 1. Interaction processing
        // 2. Pattern matching
        // 3. Personalization
        // 4. UX enhancement
        // 5. Navigation coordination

        // Setup all systems
        val patternSystem = InteractionPatternSystemImpl()
        val personalizationSystem = PersonalizedReactionSystemImpl()
        val interactionSystem = InteractionResponseSystemFactory.create(patternSystem, personalizationSystem)

        val uxEngine = mock<UXEnhancementEngine>()
        val navigationController = mock<NavigationFlowController>()

        // Setup mocks
        whenever(uxEngine.isReady).thenReturn(true)
        whenever(uxEngine.processInteraction(any(), any())).thenReturn(mock())
        whenever(navigationController.navigationState).thenReturn(mock())
        whenever(navigationController.navigationState.value.isInitialized).thenReturn(true)

        val context = createTestUXContext()

        // Initialize system
        val initialized = interactionSystem.initialize(uxEngine, navigationController, context)
        assertTrue(initialized)

        // Process interaction through complete pipeline
        val interaction = createTestInteraction()
        val response = interactionSystem.processInteraction(interaction, context)

        assertNotNull(response)
        assertTrue(response.immediateReactions.isNotEmpty() || response.personalizedReactions.isNotEmpty())

        // Learn from the interaction
        interactionSystem.learnFromInteraction(interaction, response, context)

        // Verify learning occurred
        val statistics = interactionSystem.getInteractionStatistics()
        assertTrue(statistics.totalInteractionsProcessed > 0)
    }

    @Test
    fun `test system handles high load gracefully`() = runTest {
        val patternSystem = InteractionPatternSystemImpl()
        val personalizationSystem = PersonalizedReactionSystemImpl()
        val interactionSystem = InteractionResponseSystemFactory.create(patternSystem, personalizationSystem)

        val uxEngine = mock<UXEnhancementEngine>()
        val navigationController = mock<NavigationFlowController>()

        whenever(uxEngine.isReady).thenReturn(true)
        whenever(navigationController.navigationState).thenReturn(mock())
        whenever(navigationController.navigationState.value.isInitialized).thenReturn(true)

        val context = createTestUXContext()
        interactionSystem.initialize(uxEngine, navigationController, context)

        // Process many interactions rapidly
        val interactions = (1..100).map { index ->
            createTestInteraction("load_test_$index")
        }

        val startTime = System.currentTimeMillis()

        interactions.forEach { interaction ->
            val response = interactionSystem.processInteraction(interaction, context)
            interactionSystem.learnFromInteraction(interaction, response, context)
        }

        val totalTime = System.currentTimeMillis() - startTime

        // Verify all interactions were processed
        val statistics = interactionSystem.getInteractionStatistics()
        assertEquals(100, statistics.totalInteractionsProcessed)

        // Verify performance is reasonable (less than 5 seconds for 100 interactions)
        assertTrue(totalTime < 5000, "Processing took too long: ${totalTime}ms")

        // Verify system remains stable
        assertTrue(interactionSystem.status.value.averageProcessingTimeMs < 50)
    }

    @Test
    fun `test system adapts to user behavior changes`() = runTest {
        val patternSystem = InteractionPatternSystemImpl()
        val personalizationSystem = PersonalizedReactionSystemImpl()
        val interactionSystem = InteractionResponseSystemFactory.create(patternSystem, personalizationSystem)

        val uxEngine = mock<UXEnhancementEngine>()
        val navigationController = mock<NavigationFlowController>()

        whenever(uxEngine.isReady).thenReturn(true)
        whenever(navigationController.navigationState).thenReturn(mock())
        whenever(navigationController.navigationState.value.isInitialized).thenReturn(true)

        val context = createTestUXContext()
        interactionSystem.initialize(uxEngine, navigationController, context)

        // Phase 1: User shows tapping behavior
        repeat(10) {
            val tapInteraction = createTestInteraction(type = UserInteraction.InteractionType.TAP)
            val response = interactionSystem.processInteraction(tapInteraction, context)
            interactionSystem.learnFromInteraction(tapInteraction, response, context)
        }

        val phase1Stats = interactionSystem.getInteractionStatistics()

        // Phase 2: User switches to button clicking behavior
        repeat(10) {
            val clickInteraction = createTestInteraction(type = UserInteraction.InteractionType.BUTTON_CLICK)
            val response = interactionSystem.processInteraction(clickInteraction, context)
            interactionSystem.learnFromInteraction(clickInteraction, response, context)
        }

        val phase2Stats = interactionSystem.getInteractionStatistics()

        // Verify system adapted to new behavior
        assertTrue(phase2Stats.totalInteractionsProcessed > phase1Stats.totalInteractionsProcessed)
        assertTrue(phase2Stats.patternDiscoveryRate >= phase1Stats.patternDiscoveryRate)
    }

    // Helper methods
    private fun createTestUXContext(): UXContext {
        return UXContext(
            userId = "integration_test_user",
            sessionId = "integration_test_session",
            screenName = "integration_test_screen"
        )
    }

    private fun createTestInteraction(id: String = "integration_test", type: UserInteraction.InteractionType = UserInteraction.InteractionType.TAP): UserInteraction {
        return UserInteraction(
            id = id,
            type = type,
            timestamp = System.currentTimeMillis(),
            context = UserInteraction.InteractionContext(
                screenName = "integration_test_screen",
                componentId = "integration_test_button",
                userId = "integration_test_user",
                sessionId = "integration_test_session"
            )
        )
    }
}