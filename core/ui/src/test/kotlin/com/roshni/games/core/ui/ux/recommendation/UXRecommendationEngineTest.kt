package com.roshni.games.core.ui.ux.recommendation

import com.roshni.games.core.ui.ux.engine.EnhancementFeedback
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UXRecommendationEngineTest {

    private lateinit var recommendationEngine: UXRecommendationEngineImpl
    private lateinit var testContext: UXContext
    private lateinit var testInteraction: UserInteraction

    @Before
    fun setup() {
        // Create test enhancements
        val testEnhancements = listOf(
            UXEnhancement.VisualFeedback(
                id = "visual_scale",
                animationType = UXEnhancement.VisualFeedback.AnimationType.SCALE_UP
            ),
            UXEnhancement.AudioFeedback(
                id = "audio_click",
                soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
            ),
            UXEnhancement.HapticFeedback(
                id = "haptic_tick",
                pattern = UXEnhancement.HapticFeedback.HapticPattern.LIGHT_TICK
            ),
            UXEnhancement.ContextualHelp(
                id = "contextual_help",
                helpType = UXEnhancement.ContextualHelp.HelpType.TOOLTIP,
                title = "Test Help",
                message = "This is a test help message",
                triggerCondition = UXEnhancement.ContextualHelp.TriggerCondition("TEST_EVENT")
            )
        )

        recommendationEngine = UXRecommendationEngineImpl(testEnhancements)

        testContext = UXContext(
            userId = "test_user",
            sessionId = "test_session",
            screenName = "test_screen",
            userPreferences = UXContext.UserPreferences(
                soundEnabled = true,
                hapticFeedbackEnabled = true,
                animationSpeed = UXContext.AnimationSpeed.NORMAL,
                theme = UXContext.ThemePreference.LIGHT
            ),
            deviceCapabilities = UXContext.DeviceCapabilities(
                hasVibrator = true,
                hasSpeaker = true,
                screenRefreshRate = 60
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
    fun `getRecommendations should return relevant enhancements for context`() = runTest {
        // When
        val recommendations = recommendationEngine.getRecommendations(testContext, testInteraction, 5)

        // Then
        assertNotNull(recommendations)
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.size <= 5)

        // Should include enhancements that match user preferences
        val hasAudioRecommendation = recommendations.any { it.id == "audio_click" }
        assertTrue(hasAudioRecommendation)

        val hasHapticRecommendation = recommendations.any { it.id == "haptic_tick" }
        assertTrue(hasHapticRecommendation)
    }

    @Test
    fun `getRecommendations should respect user preferences`() = runTest {
        // Given - Context with sound disabled
        val contextNoSound = testContext.copy(
            userPreferences = testContext.userPreferences.copy(soundEnabled = false)
        )

        // When
        val recommendations = recommendationEngine.getRecommendations(contextNoSound, testInteraction, 5)

        // Then
        assertNotNull(recommendations)

        // Should not recommend audio enhancements when sound is disabled
        val hasAudioRecommendation = recommendations.any { it.id == "audio_click" }
        assertFalse(hasAudioRecommendation)
    }

    @Test
    fun `getRecommendations should respect device capabilities`() = runTest {
        // Given - Context with no vibrator
        val contextNoVibrator = testContext.copy(
            deviceCapabilities = testContext.deviceCapabilities.copy(hasVibrator = false)
        )

        // When
        val recommendations = recommendationEngine.getRecommendations(contextNoVibrator, testInteraction, 5)

        // Then
        assertNotNull(recommendations)

        // Should not recommend haptic enhancements when vibrator is not available
        val hasHapticRecommendation = recommendations.any { it.id == "haptic_tick" }
        assertFalse(hasHapticRecommendation)
    }

    @Test
    fun `updateUserContext should update user profile correctly`() = runTest {
        // When
        recommendationEngine.updateUserContext(testContext)

        // Then (no exception should be thrown and state should be updated)
        val stats = recommendationEngine.getRecommendationStatistics()
        assertEquals(1, stats["totalUsers"])
    }

    @Test
    fun `recordFeedback should update enhancement metrics`() = runTest {
        // Given
        val feedback = EnhancementFeedback(
            rating = 5,
            helpful = true,
            comments = "Great enhancement!"
        )

        // When
        recommendationEngine.recordFeedback("visual_scale", feedback)

        // Then (no exception should be thrown)
        val stats = recommendationEngine.getRecommendationStatistics()
        assertEquals(1, stats["totalFeedbackCount"])
    }

    @Test
    fun `getRecommendationStatistics should return correct statistics`() = runTest {
        // Given
        recommendationEngine.updateUserContext(testContext)
        recommendationEngine.recordFeedback("visual_scale",
            EnhancementFeedback(rating = 4, helpful = true)
        )

        // When
        val stats = recommendationEngine.getRecommendationStatistics()

        // Then
        assertNotNull(stats)
        assertEquals(1, stats["totalUsers"])
        assertEquals(1, stats["totalFeedbackCount"])
        assertNotNull(stats["modelParameters"])
    }

    @Test
    fun `trainModel should update learning parameters`() = runTest {
        // Given
        recommendationEngine.recordFeedback("visual_scale",
            EnhancementFeedback(rating = 5, helpful = true)
        )

        // When
        recommendationEngine.trainModel()

        // Then (no exception should be thrown)
        val stats = recommendationEngine.getRecommendationStatistics()
        assertNotNull(stats["modelParameters"])
    }

    @Test
    fun `clearUserData should reset all user data`() = runTest {
        // Given
        recommendationEngine.updateUserContext(testContext)
        recommendationEngine.recordFeedback("visual_scale",
            EnhancementFeedback(rating = 4, helpful = true)
        )

        // Verify data exists
        var stats = recommendationEngine.getRecommendationStatistics()
        assertEquals(1, stats["totalUsers"])

        // When
        recommendationEngine.clearUserData()

        // Then
        stats = recommendationEngine.getRecommendationStatistics()
        assertEquals(0, stats["totalUsers"])
        assertEquals(0, stats["totalFeedbackCount"])
    }

    @Test
    fun `getRecommendations should limit results correctly`() = runTest {
        // When
        val recommendations = recommendationEngine.getRecommendations(testContext, testInteraction, 2)

        // Then
        assertNotNull(recommendations)
        assertTrue(recommendations.size <= 2)
    }

    @Test
    fun `getRecommendations should handle null interaction gracefully`() = runTest {
        // When
        val recommendations = recommendationEngine.getRecommendations(testContext, null, 5)

        // Then
        assertNotNull(recommendations)
        // Should still return recommendations based on context alone
    }

    @Test
    fun `recordFeedback should handle multiple feedback entries`() = runTest {
        // Given
        val feedbacks = listOf(
            EnhancementFeedback(rating = 5, helpful = true),
            EnhancementFeedback(rating = 3, helpful = false),
            EnhancementFeedback(rating = 4, helpful = true)
        )

        // When
        feedbacks.forEach { feedback ->
            recommendationEngine.recordFeedback("visual_scale", feedback)
        }

        // Then
        val stats = recommendationEngine.getRecommendationStatistics()
        assertEquals(3, stats["totalFeedbackCount"])
    }
}