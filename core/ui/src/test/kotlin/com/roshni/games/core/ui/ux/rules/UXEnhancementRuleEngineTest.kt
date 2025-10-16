package com.roshni.games.core.ui.ux.rules

import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UXEnhancementRuleEngineTest {

    private lateinit var ruleEngine: UXEnhancementRuleEngineImpl
    private lateinit var testContext: UXContext
    private lateinit var testInteraction: UserInteraction

    @Before
    fun setup() {
        ruleEngine = UXEnhancementRuleEngineImpl()

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
            ),
            environmentalFactors = UXContext.EnvironmentalFactors(
                timeOfDay = UXContext.TimeOfDay.MORNING,
                lightingCondition = UXContext.LightingCondition.BRIGHT,
                batteryLevel = 85
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
    fun `addRule should successfully add valid rule`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(
                UXEnhancementRule.RuleCondition.UserPreferenceCondition(
                    preference = "soundEnabled",
                    operator = UXEnhancementRule.ComparisonOperator.EQUALS,
                    value = true
                )
            ),
            actions = listOf(
                UXEnhancementRule.RuleAction.ApplyEnhancement(
                    UXEnhancement.AudioFeedback(
                        id = "test_audio",
                        soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
                    )
                )
            )
        )

        // When
        val result = ruleEngine.addRule(rule)

        // Then
        assertTrue(result)
        assertEquals(1, ruleEngine.rules.value.size)
        assertEquals("test_rule", ruleEngine.rules.value.first().id)
    }

    @Test
    fun `addRule should reject duplicate rule IDs`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        // When
        ruleEngine.addRule(rule)
        val result = ruleEngine.addRule(rule)

        // Then
        assertFalse(result)
        assertEquals(1, ruleEngine.rules.value.size)
    }

    @Test
    fun `evaluateRules should return enhancements for matching conditions`() = runTest {
        // Given
        val audioEnhancement = UXEnhancement.AudioFeedback(
            id = "test_audio",
            soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
        )

        val rule = UXEnhancementRule(
            id = "audio_rule",
            name = "Audio Rule",
            description = "Provides audio feedback when sound is enabled",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(
                UXEnhancementRule.RuleCondition.UserPreferenceCondition(
                    preference = "soundEnabled",
                    operator = UXEnhancementRule.ComparisonOperator.EQUALS,
                    value = true
                )
            ),
            actions = listOf(
                UXEnhancementRule.RuleAction.ApplyEnhancement(audioEnhancement)
            )
        )

        ruleEngine.addRule(rule)

        // When
        val result = ruleEngine.evaluateRules(testContext, testInteraction)

        // Then
        assertEquals(1, result.size)
        assertEquals("test_audio", result.first().id)
    }

    @Test
    fun `evaluateRules should return empty list for non-matching conditions`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "disabled_sound_rule",
            name = "Disabled Sound Rule",
            description = "Should not match when sound is disabled",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(
                UXEnhancementRule.RuleCondition.UserPreferenceCondition(
                    preference = "soundEnabled",
                    operator = UXEnhancementRule.ComparisonOperator.EQUALS,
                    value = false
                )
            ),
            actions = listOf(
                UXEnhancementRule.RuleAction.ApplyEnhancement(
                    UXEnhancement.AudioFeedback(
                        id = "test_audio",
                        soundType = UXEnhancement.AudioFeedback.SoundType.CLICK
                    )
                )
            )
        )

        ruleEngine.addRule(rule)

        // When
        val result = ruleEngine.evaluateRules(testContext, testInteraction)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRulesByCategory should filter rules correctly`() = runTest {
        // Given
        val personalizationRule = UXEnhancementRule(
            id = "personalization_rule",
            name = "Personalization Rule",
            description = "Test personalization rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        val accessibilityRule = UXEnhancementRule(
            id = "accessibility_rule",
            name = "Accessibility Rule",
            description = "Test accessibility rule",
            category = UXEnhancementRule.RuleCategory.ACCESSIBILITY,
            conditions = listOf(),
            actions = listOf()
        )

        ruleEngine.addRule(personalizationRule)
        ruleEngine.addRule(accessibilityRule)

        // When
        val personalizationRules = ruleEngine.getRulesByCategory(UXEnhancementRule.RuleCategory.PERSONALIZATION)
        val accessibilityRules = ruleEngine.getRulesByCategory(UXEnhancementRule.RuleCategory.ACCESSIBILITY)

        // Then
        assertEquals(1, personalizationRules.size)
        assertEquals("personalization_rule", personalizationRules.first().id)
        assertEquals(1, accessibilityRules.size)
        assertEquals("accessibility_rule", accessibilityRules.first().id)
    }

    @Test
    fun `setRuleEnabled should enable and disable rules correctly`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        ruleEngine.addRule(rule)

        // Initially enabled
        assertTrue(ruleEngine.getRule("test_rule")?.isEnabled == true)

        // When/Then - Disable rule
        val disabled = ruleEngine.setRuleEnabled("test_rule", false)
        assertTrue(disabled)
        assertFalse(ruleEngine.getRule("test_rule")?.isEnabled == true)

        // When/Then - Re-enable rule
        val enabled = ruleEngine.setRuleEnabled("test_rule", true)
        assertTrue(enabled)
        assertTrue(ruleEngine.getRule("test_rule")?.isEnabled == true)
    }

    @Test
    fun `getRuleStatistics should return correct statistics for rules`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        ruleEngine.addRule(rule)

        // Execute rule multiple times
        repeat(5) {
            ruleEngine.evaluateRules(testContext, testInteraction)
        }

        // When
        val stats = ruleEngine.getRuleStatistics("test_rule")

        // Then
        assertNotNull(stats)
        assertEquals(5, stats?.executionCount)
        assertTrue(stats?.successRate == 1.0) // All executions should succeed in test
    }

    @Test
    fun `resetRuleStatistics should clear execution statistics`() = runTest {
        // Given
        val rule = UXEnhancementRule(
            id = "test_rule",
            name = "Test Rule",
            description = "A test rule",
            category = UXEnhancementRule.RuleCategory.PERSONALIZATION,
            conditions = listOf(),
            actions = listOf()
        )

        ruleEngine.addRule(rule)

        // Execute rule
        ruleEngine.evaluateRules(testContext, testInteraction)

        // Verify statistics exist
        val statsBefore = ruleEngine.getRuleStatistics("test_rule")
        assertEquals(1, statsBefore?.executionCount)

        // When
        ruleEngine.resetRuleStatistics("test_rule")

        // Then
        val statsAfter = ruleEngine.getRuleStatistics("test_rule")
        assertEquals(0, statsAfter?.executionCount)
    }
}