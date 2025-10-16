package com.roshni.games.core.ui.ux.engine

import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import com.roshni.games.core.ui.ux.recommendation.UXRecommendationEngine
import com.roshni.games.core.ui.ux.rules.UXEnhancementRuleEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class UXEnhancementEngineTest {

    @Mock
    private lateinit var mockRuleEngine: UXEnhancementRuleEngine

    @Mock
    private lateinit var mockRecommendationEngine: UXRecommendationEngine

    private lateinit var uxEnhancementEngine: UXEnhancementEngineImpl
    private lateinit var testContext: UXContext
    private lateinit var testInteraction: UserInteraction

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        uxEnhancementEngine = UXEnhancementEngineImpl(mockRuleEngine, mockRecommendationEngine)

        testContext = UXContext(
            userId = "test_user",
            sessionId = "test_session",
            screenName = "test_screen",
            userPreferences = UXContext.UserPreferences(
                soundEnabled = true,
                hapticFeedbackEnabled = true,
                animationSpeed = UXContext.AnimationSpeed.NORMAL
            ),
            deviceCapabilities = UXContext.DeviceCapabilities(
                hasVibrator = true,
                hasSpeaker = true
            )
        )

        testInteraction = UserInteraction(
            id = "test_interaction",
            type = UserInteraction.InteractionType.BUTTON_CLICK,
            timestamp = System.currentTimeMillis(),
            context = UserInteraction.InteractionContext(
                screenName = "test_screen",
                componentId = "test_button",
                userId = "test_user",
                sessionId = "test_session",
                deviceInfo = UserInteraction.DeviceInfo()
            )
        )
    }

    @Test
    fun `initialize should set engine status correctly`() = runTest {
        // Given
        whenever(mockRuleEngine.rules).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))

        // When
        val result = uxEnhancementEngine.initialize(testContext)

        // Then
        assertTrue(result)
        assertTrue(uxEnhancementEngine.isReady)
        assertEquals(0, uxEnhancementEngine.status.value.totalInteractionsProcessed)
        assertEquals(0, uxEnhancementEngine.status.value.totalEnhancementsApplied)
    }

    @Test
    fun `processInteraction should return enhanced interaction with enhancements`() = runTest {
        // Given
        val visualEnhancement = UXEnhancement.VisualFeedback(
            id = "test_visual",
            animationType = UXEnhancement.VisualFeedback.AnimationType.SCALE_UP
        )

        val hapticEnhancement = UXEnhancement.HapticFeedback(
            id = "test_haptic",
            pattern = UXEnhancement.HapticFeedback.HapticPattern.LIGHT_TICK
        )

        whenever(mockRuleEngine.evaluateRules(testContext, testInteraction))
            .thenReturn(listOf(visualEnhancement, hapticEnhancement))
        whenever(mockRecommendationEngine.getRecommendations(testContext, testInteraction, 5))
            .thenReturn(emptyList())

        uxEnhancementEngine.initialize(testContext)

        // When
        val result = uxEnhancementEngine.processInteraction(testInteraction, testContext)

        // Then
        assertNotNull(result)
        assertEquals(testInteraction.id, result.originalInteraction.id)
        assertEquals(2, result.enhancements.size)
        assertTrue(result.enhancements.any { it.id == "test_visual" })
        assertTrue(result.enhancements.any { it.id == "test_haptic" })
        assertTrue(result.confidence > 0.0)
        assertTrue(result.personalizationScore >= 0.0)
    }

    @Test
    fun `processInteraction should handle empty enhancements gracefully`() = runTest {
        // Given
        whenever(mockRuleEngine.evaluateRules(testContext, testInteraction))
            .thenReturn(emptyList())
        whenever(mockRecommendationEngine.getRecommendations(testContext, testInteraction, 5))
            .thenReturn(emptyList())

        uxEnhancementEngine.initialize(testContext)

        // When
        val result = uxEnhancementEngine.processInteraction(testInteraction, testContext)

        // Then
        assertNotNull(result)
        assertEquals(testInteraction.id, result.originalInteraction.id)
        assertTrue(result.enhancements.isEmpty())
        assertEquals(0.0, result.confidence, 0.01)
        assertEquals(0.0, result.personalizationScore, 0.01)
    }

    @Test
    fun `getEnhancementsForContext should return rule-based enhancements`() = runTest {
        // Given
        val enhancement = UXEnhancement.AudioFeedback(
            id = "test_audio",
            soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
        )

        whenever(mockRuleEngine.evaluateRules(testContext, null))
            .thenReturn(listOf(enhancement))

        uxEnhancementEngine.initialize(testContext)

        // When
        val result = uxEnhancementEngine.getEnhancementsForContext(testContext)

        // Then
        assertEquals(1, result.size)
        assertEquals("test_audio", result.first().id)
    }

    @Test
    fun `getPersonalizedRecommendations should return recommendations from engine`() = runTest {
        // Given
        val enhancement = UXEnhancement.ContextualHelp(
            id = "test_help",
            helpType = UXEnhancement.ContextualHelp.HelpType.TOOLTIP,
            title = "Test Help",
            message = "This is a test help message",
            triggerCondition = UXEnhancement.ContextualHelp.TriggerCondition("TEST_EVENT")
        )

        whenever(mockRecommendationEngine.getRecommendations(testContext, null, 10))
            .thenReturn(listOf(enhancement))

        uxEnhancementEngine.initialize(testContext)

        // When
        val result = uxEnhancementEngine.getPersonalizedRecommendations(testContext, 10)

        // Then
        assertEquals(1, result.size)
        assertEquals("test_help", result.first().id)
    }

    @Test
    fun `recordEnhancementFeedback should update recommendation engine`() = runTest {
        // Given
        val feedback = EnhancementFeedback(
            rating = 5,
            helpful = true,
            comments = "Great enhancement!"
        )

        uxEnhancementEngine.initialize(testContext)

        // When/Then (no exception should be thrown)
        uxEnhancementEngine.recordEnhancementFeedback("test_enhancement", "test_interaction", feedback)
    }

    @Test
    fun `getEnhancementStatistics should return correct statistics`() = runTest {
        // Given
        val enhancement = UXEnhancement.VisualFeedback(
            id = "test_visual",
            animationType = UXEnhancement.VisualFeedback.AnimationType.BOUNCE
        )

        whenever(mockRuleEngine.evaluateRules(testContext, testInteraction))
            .thenReturn(listOf(enhancement))
        whenever(mockRecommendationEngine.getRecommendations(testContext, testInteraction, 5))
            .thenReturn(emptyList())

        uxEnhancementEngine.initialize(testContext)

        // Process some interactions
        repeat(3) {
            uxEnhancementEngine.processInteraction(testInteraction, testContext)
        }

        // When
        val stats = uxEnhancementEngine.getEnhancementStatistics()

        // Then
        assertEquals(3, stats.totalEnhancementsApplied)
        assertEquals(UXEnhancement.Type.VISUAL_FEEDBACK, stats.enhancementsByType.keys.first())
        assertEquals(3, stats.enhancementsByType[UXEnhancement.Type.VISUAL_FEEDBACK])
    }

    @Test
    fun `reset should clear all data`() = runTest {
        // Given
        val enhancement = UXEnhancement.VisualFeedback(
            id = "test_visual",
            animationType = UXEnhancement.VisualFeedback.AnimationType.BOUNCE
        )

        whenever(mockRuleEngine.evaluateRules(testContext, testInteraction))
            .thenReturn(listOf(enhancement))
        whenever(mockRecommendationEngine.getRecommendations(testContext, testInteraction, 5))
            .thenReturn(emptyList())

        uxEnhancementEngine.initialize(testContext)
        uxEnhancementEngine.processInteraction(testInteraction, testContext)

        // Verify we have data
        assertEquals(1, uxEnhancementEngine.status.value.totalInteractionsProcessed)

        // When
        uxEnhancementEngine.reset()

        // Then
        assertEquals(0, uxEnhancementEngine.status.value.totalInteractionsProcessed)
        assertEquals(0, uxEnhancementEngine.status.value.totalEnhancementsApplied)
    }

    @Test
    fun `shutdown should cleanup resources gracefully`() = runTest {
        // Given
        uxEnhancementEngine.initialize(testContext)

        // When/Then (no exception should be thrown)
        uxEnhancementEngine.shutdown()
    }
}