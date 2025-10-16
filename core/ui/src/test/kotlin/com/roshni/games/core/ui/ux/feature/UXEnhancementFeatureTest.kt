package com.roshni.games.core.ui.ux.feature

import com.roshni.games.core.ui.ux.engine.UXEnhancementEngine
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import com.roshni.games.core.utils.feature.FeatureContext
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

class UXEnhancementFeatureTest {

    @Mock
    private lateinit var mockUXEnhancementEngine: UXEnhancementEngine

    private lateinit var uxEnhancementFeature: UXEnhancementFeature
    private lateinit var testFeatureContext: FeatureContext
    private lateinit var testUXContext: UXContext
    private lateinit var testUserInteraction: UserInteraction

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        uxEnhancementFeature = UXEnhancementFeature(mockUXEnhancementEngine)

        testFeatureContext = FeatureContext(
            featureId = "ux_enhancement",
            executionId = "test_execution",
            userId = "test_user",
            sessionId = "test_session"
        )

        testUXContext = UXContext(
            userId = "test_user",
            sessionId = "test_session",
            screenName = "test_screen"
        )

        testUserInteraction = UserInteraction(
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
    fun `feature should have correct metadata`() {
        // Then
        assertEquals("ux_enhancement", uxEnhancementFeature.id)
        assertEquals("UX Enhancement System", uxEnhancementFeature.name)
        assertEquals(com.roshni.games.core.utils.feature.FeatureCategory.UI, uxEnhancementFeature.category)
        assertEquals(1, uxEnhancementFeature.version)
        assertTrue(uxEnhancementFeature.enabled)
    }

    @Test
    fun `initialize should setup engine correctly`() = runTest {
        // Given
        whenever(mockUXEnhancementEngine.initialize(testUXContext)).thenReturn(true)

        // When
        val result = uxEnhancementFeature.performInitialization(testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `initialize should handle engine initialization failure`() = runTest {
        // Given
        whenever(mockUXEnhancementEngine.initialize(testUXContext)).thenReturn(false)

        // When
        val result = uxEnhancementFeature.performInitialization(testFeatureContext)

        // Then
        assertFalse(result)
    }

    @Test
    fun `performEnable should enable feature correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.performEnable(testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `performDisable should disable feature correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.performDisable(testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `performExecute should return correct feature result`() = runTest {
        // Given
        val enhancements = listOf(
            UXEnhancement.VisualFeedback(
                id = "test_visual",
                animationType = UXEnhancement.VisualFeedback.AnimationType.SCALE_UP
            )
        )

        whenever(mockUXEnhancementEngine.getEnhancementsForContext(testUXContext))
            .thenReturn(enhancements)

        // When
        val result = uxEnhancementFeature.performExecute(testFeatureContext)

        // Then
        assertTrue(result.success)
        assertNotNull(result.data)
        assertEquals(1, result.data["enhancementCount"])
    }

    @Test
    fun `performCleanup should cleanup resources correctly`() = runTest {
        // When/Then (no exception should be thrown)
        uxEnhancementFeature.performCleanup()
    }

    @Test
    fun `performReset should reset feature state correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.performReset(testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateDependencies should return valid result for satisfied dependencies`() {
        // When
        val result = uxEnhancementFeature.validateDependencies()

        // Then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validateConfiguration should return valid result for correct configuration`() {
        // When
        val result = uxEnhancementFeature.validateConfiguration()

        // Then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `getEstimatedExecutionTimeMs should return reasonable estimate`() {
        // When
        val estimate = uxEnhancementFeature.getEstimatedExecutionTimeMs()

        // Then
        assertNotNull(estimate)
        assertTrue(estimate!! > 0)
        assertTrue(estimate < 1000) // Should be quick
    }

    @Test
    fun `handleUserAction should handle PROCESS_INTERACTION correctly`() = runTest {
        // Given
        val data = mapOf(
            "interaction" to "test_interaction_data",
            "uxContext" to "test_context_data"
        )

        // When
        val result = uxEnhancementFeature.handleUserAction("PROCESS_INTERACTION", data, testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `handleUserAction should handle RECORD_FEEDBACK correctly`() = runTest {
        // Given
        val data = mapOf(
            "enhancementId" to "test_enhancement",
            "interactionId" to "test_interaction",
            "rating" to 5,
            "helpful" to true
        )

        // When
        val result = uxEnhancementFeature.handleUserAction("RECORD_FEEDBACK", data, testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `handleUserAction should handle UPDATE_CONTEXT correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.handleUserAction("UPDATE_CONTEXT", emptyMap(), testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `handleSystemEvent should handle SCREEN_CHANGED correctly`() = runTest {
        // Given
        val data = mapOf("screenName" to "new_screen")

        // When
        val result = uxEnhancementFeature.handleSystemEvent("SCREEN_CHANGED", data, testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `handleSystemEvent should handle USER_PREFERENCES_CHANGED correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.handleSystemEvent("USER_PREFERENCES_CHANGED", emptyMap(), testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `handleSystemEvent should handle DEVICE_CAPABILITIES_CHANGED correctly`() = runTest {
        // When
        val result = uxEnhancementFeature.handleSystemEvent("DEVICE_CAPABILITIES_CHANGED", emptyMap(), testFeatureContext)

        // Then
        assertTrue(result)
    }

    @Test
    fun `processUserInteraction should return enhanced interaction`() = runTest {
        // Given
        val enhancedInteraction = com.roshni.games.core.ui.ux.model.EnhancedInteraction(
            originalInteraction = testUserInteraction,
            enhancements = listOf(
                UXEnhancement.VisualFeedback(
                    id = "test_visual",
                    animationType = UXEnhancement.VisualFeedback.AnimationType.SCALE_UP
                )
            ),
            appliedRules = emptyList(),
            personalizationScore = 0.8,
            confidence = 0.9
        )

        whenever(mockUXEnhancementEngine.processInteraction(testUserInteraction, testUXContext))
            .thenReturn(enhancedInteraction)

        // When
        val result = uxEnhancementFeature.processUserInteraction(testUserInteraction, testUXContext)

        // Then
        assertNotNull(result)
        assertEquals(testUserInteraction.id, result.originalInteraction.id)
        assertEquals(1, result.enhancements.size)
        assertEquals(0.8, result.personalizationScore, 0.01)
        assertEquals(0.9, result.confidence, 0.01)
    }

    @Test
    fun `getPersonalizedRecommendations should return recommendations from engine`() = runTest {
        // Given
        val recommendations = listOf(
            UXEnhancement.AudioFeedback(
                id = "test_audio",
                soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
            )
        )

        whenever(mockUXEnhancementEngine.getPersonalizedRecommendations(testUXContext, 10))
            .thenReturn(recommendations)

        // When
        val result = uxEnhancementFeature.getPersonalizedRecommendations(testUXContext, 10)

        // Then
        assertEquals(1, result.size)
        assertEquals("test_audio", result.first().id)
    }

    @Test
    fun `recordEnhancementFeedback should record feedback correctly`() = runTest {
        // When/Then (no exception should be thrown)
        uxEnhancementFeature.recordEnhancementFeedback(
            enhancementId = "test_enhancement",
            interactionId = "test_interaction",
            rating = 5,
            helpful = true,
            comments = "Great enhancement!"
        )
    }

    @Test
    fun `getFeatureStatistics should return comprehensive statistics`() = runTest {
        // Given
        val engineStats = mapOf(
            "totalEnhancementsApplied" to 10L,
            "averageUserRating" to 4.5,
            "userSatisfactionScore" to 0.85
        )

        whenever(mockUXEnhancementEngine.getEnhancementStatistics())
            .thenReturn(engineStats)

        // When
        val stats = uxEnhancementFeature.getFeatureStatistics()

        // Then
        assertNotNull(stats)
        assertEquals(0L, stats["interactionCount"]) // Initially 0
        assertEquals(0L, stats["enhancementCount"]) // Initially 0
        assertNotNull(stats["engineStatistics"])
    }

    @Test
    fun `registerEnhancementRule should register rule with engine`() = runTest {
        // Given
        val rule = com.roshni.games.core.ui.ux.rules.UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = com.roshni.games.core.ui.ux.rules.UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        whenever(mockUXEnhancementEngine.registerEnhancementRule(rule))
            .thenReturn(true)

        // When
        val result = uxEnhancementFeature.registerEnhancementRule(rule)

        // Then
        assertTrue(result)
    }

    @Test
    fun `feature should handle invalid actions gracefully`() = runTest {
        // When
        val result = uxEnhancementFeature.handleUserAction("INVALID_ACTION", emptyMap(), testFeatureContext)

        // Then
        assertFalse(result)
    }

    @Test
    fun `feature should handle invalid events gracefully`() = runTest {
        // When
        val result = uxEnhancementFeature.handleSystemEvent("INVALID_EVENT", emptyMap(), testFeatureContext)

        // Then
        assertFalse(result)
    }
}