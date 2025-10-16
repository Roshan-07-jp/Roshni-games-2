package com.roshni.games.core.utils.feature

import com.roshni.games.core.utils.feature.features.AccessibilityFeature
import com.roshni.games.core.utils.feature.features.GameLibraryFeature
import com.roshni.games.core.utils.feature.features.ParentalControlsFeature
import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.rules.RuleEngineImpl
import com.roshni.games.core.utils.workflow.WorkflowContext
import com.roshni.games.core.utils.workflow.WorkflowEngine
import com.roshni.games.core.utils.workflow.WorkflowEngineImpl
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the Feature Logic Framework
 */
class FeatureManagerTest {

    private lateinit var featureManager: FeatureManager
    private lateinit var ruleEngine: RuleEngine
    private lateinit var workflowEngine: WorkflowEngine

    private lateinit var gameLibraryFeature: GameLibraryFeature
    private lateinit var parentalControlsFeature: ParentalControlsFeature
    private lateinit var accessibilityFeature: AccessibilityFeature

    @Before
    fun setup() {
        // Initialize engines
        ruleEngine = RuleEngineImpl.getInstance()
        workflowEngine = WorkflowEngineImpl.getInstance()

        // Initialize feature manager
        featureManager = FeatureManagerImpl(ruleEngine, workflowEngine)

        // Initialize features
        gameLibraryFeature = GameLibraryFeature()
        parentalControlsFeature = ParentalControlsFeature()
        accessibilityFeature = AccessibilityFeature()
    }

    @Test
    fun `test feature registration and retrieval`() = runTest {
        // Register features
        val gameLibraryRegistered = featureManager.registerFeature(gameLibraryFeature)
        val parentalControlsRegistered = featureManager.registerFeature(parentalControlsFeature)
        val accessibilityRegistered = featureManager.registerFeature(accessibilityFeature)

        assertTrue(gameLibraryRegistered, "GameLibraryFeature should be registered successfully")
        assertTrue(parentalControlsRegistered, "ParentalControlsFeature should be registered successfully")
        assertTrue(accessibilityRegistered, "AccessibilityFeature should be registered successfully")

        // Verify features are registered
        val registeredFeatures = featureManager.getAllFeatures()
        assertEquals(3, registeredFeatures.size, "Should have 3 registered features")

        // Test feature retrieval
        val retrievedGameLibrary = featureManager.getFeature("game_library")
        val retrievedParentalControls = featureManager.getFeature("parental_controls")
        val retrievedAccessibility = featureManager.getFeature("accessibility")

        assertNotNull(retrievedGameLibrary, "GameLibraryFeature should be retrievable")
        assertNotNull(retrievedParentalControls, "ParentalControlsFeature should be retrievable")
        assertNotNull(retrievedAccessibility, "AccessibilityFeature should be retrievable")

        assertEquals("game_library", retrievedGameLibrary.id)
        assertEquals("parental_controls", retrievedParentalControls.id)
        assertEquals("accessibility", retrievedAccessibility.id)
    }

    @Test
    fun `test feature categorization and filtering`() = runTest {
        // Register all features
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)
        featureManager.registerFeature(accessibilityFeature)

        // Test category filtering
        val gameplayFeatures = featureManager.getFeaturesByCategory(FeatureCategory.GAMEPLAY)
        val parentalFeatures = featureManager.getFeaturesByCategory(FeatureCategory.PARENTAL_CONTROLS)
        val accessibilityFeatures = featureManager.getFeaturesByCategory(FeatureCategory.ACCESSIBILITY)

        assertEquals(1, gameplayFeatures.size, "Should have 1 gameplay feature")
        assertEquals(1, parentalFeatures.size, "Should have 1 parental controls feature")
        assertEquals(1, accessibilityFeatures.size, "Should have 1 accessibility feature")

        assertEquals("game_library", gameplayFeatures.first().id)
        assertEquals("parental_controls", parentalFeatures.first().id)
        assertEquals("accessibility", accessibilityFeatures.first().id)

        // Test tag filtering
        val securityFeatures = featureManager.getFeaturesByTags(listOf("security"))
        val accessibilityTags = featureManager.getFeaturesByTags(listOf("accessibility", "a11y"))

        assertEquals(1, securityFeatures.size, "Should have 1 security feature")
        assertEquals(1, accessibilityTags.size, "Should have 1 accessibility feature")
    }

    @Test
    fun `test feature manager initialization`() = runTest {
        // Register features first
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)
        featureManager.registerFeature(accessibilityFeature)

        // Initialize feature manager
        val context = FeatureManagerContext(
            userId = "test_user",
            sessionId = "test_session",
            ruleEngine = ruleEngine,
            workflowEngine = workflowEngine
        )

        val initialized = featureManager.initialize(context)
        assertTrue(initialized, "FeatureManager should initialize successfully")

        val status = featureManager.status.value
        assertTrue(status.isInitialized, "FeatureManager should be initialized")
        assertEquals(3, status.registeredFeatureCount, "Should have 3 registered features")
    }

    @Test
    fun `test feature lifecycle management`() = runTest {
        // Register and initialize
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)

        val context = FeatureContext(
            featureId = "test",
            executionId = "test_execution",
            userId = "test_user"
        )

        // Test feature enablement
        val gameLibraryEnabled = featureManager.enableFeature("game_library", context)
        val parentalControlsEnabled = featureManager.enableFeature("parental_controls", context)

        assertTrue(gameLibraryEnabled, "GameLibraryFeature should be enabled")
        assertTrue(parentalControlsEnabled, "ParentalControlsFeature should be enabled")

        // Check enabled features
        val enabledFeatures = featureManager.enabledFeatures.value
        assertEquals(2, enabledFeatures.size, "Should have 2 enabled features")

        // Test feature disablement
        val gameLibraryDisabled = featureManager.disableFeature("game_library", context)
        assertTrue(gameLibraryDisabled, "GameLibraryFeature should be disabled")

        val updatedEnabledFeatures = featureManager.enabledFeatures.value
        assertEquals(1, updatedEnabledFeatures.size, "Should have 1 enabled feature")
        assertEquals("parental_controls", updatedEnabledFeatures.first().id)
    }

    @Test
    fun `test feature execution`() = runTest {
        // Register and enable feature
        featureManager.registerFeature(gameLibraryFeature)

        val context = FeatureContext(
            featureId = "game_library",
            executionId = "test_execution",
            userId = "test_user",
            variables = mutableMapOf("action" to "list_games")
        )

        featureManager.enableFeature("game_library", context)

        // Execute feature
        val result = featureManager.executeFeature("game_library", context)

        assertTrue(result.success, "Feature execution should be successful")
        assertTrue(result.executionTimeMs >= 0, "Execution time should be non-negative")
        assertNotNull(result.data, "Result should contain data")
    }

    @Test
    fun `test feature dependency resolution`() = runTest {
        // Register features with dependencies
        featureManager.registerFeature(parentalControlsFeature) // Depends on security
        featureManager.registerFeature(accessibilityFeature) // No dependencies

        val context = FeatureContext(
            featureId = "test",
            executionId = "test_execution",
            userId = "test_user"
        )

        // Test dependency checking
        val canEnableParentalControls = featureManager.canEnableFeature("parental_controls", context)
        // This might be false due to missing security dependency, which is expected

        val canEnableAccessibility = featureManager.canEnableFeature("accessibility", context)
        assertTrue(canEnableAccessibility, "AccessibilityFeature should be enableable without dependencies")

        // Test dependency resolution
        val resolvedFeatures = featureManager.resolveAndEnableFeatures(
            listOf("accessibility", "parental_controls"),
            context
        )

        // Should at least enable accessibility feature
        val enabledAccessibility = resolvedFeatures.any { it.id == "accessibility" }
        assertTrue(enabledAccessibility, "AccessibilityFeature should be resolved and enabled")
    }

    @Test
    fun `test feature validation`() = runTest {
        // Register features
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)
        featureManager.registerFeature(accessibilityFeature)

        // Validate all features
        val validationResult = featureManager.validateAllFeatures()

        // All features should be valid (they have proper basic structure)
        assertTrue(validationResult.isValid, "All features should be valid")
        assertEquals(3, validationResult.featureValidationResults.size, "Should have validation results for 3 features")
    }

    @Test
    fun `test feature event handling`() = runTest {
        // Register and enable feature
        featureManager.registerFeature(accessibilityFeature)

        val context = FeatureContext(
            featureId = "accessibility",
            executionId = "test_execution",
            userId = "test_user"
        )

        featureManager.enableFeature("accessibility", context)

        // Test event sending
        val event = FeatureEvent.UserAction(
            action = "toggle_high_contrast",
            data = emptyMap()
        )

        val eventHandled = featureManager.sendEventToFeature("accessibility", event, context)
        assertTrue(eventHandled, "Event should be handled successfully")

        // Test event broadcasting
        val broadcastResults = featureManager.broadcastEvent(event, context)
        assertTrue(broadcastResults.isNotEmpty(), "Broadcast should reach at least one feature")
    }

    @Test
    fun `test feature statistics and monitoring`() = runTest {
        // Register and enable feature
        featureManager.registerFeature(gameLibraryFeature)

        val context = FeatureContext(
            featureId = "game_library",
            executionId = "test_execution",
            userId = "test_user",
            variables = mutableMapOf("action" to "list_games")
        )

        featureManager.enableFeature("game_library", context)

        // Execute feature multiple times
        repeat(5) {
            featureManager.executeFeature("game_library", context)
        }

        // Check statistics
        val statistics = featureManager.getFeatureStatistics("game_library")
        assertEquals(5, statistics.totalExecutions, "Should have 5 executions")
        assertEquals(5, statistics.successfulExecutions, "All executions should be successful")
        assertTrue(statistics.averageExecutionTimeMs >= 0, "Average execution time should be non-negative")

        // Check overall statistics
        val overallStatistics = featureManager.getFeatureStatistics()
        assertEquals(5, overallStatistics.totalExecutions, "Overall should have 5 executions")
        assertTrue(overallStatistics.featuresExecuted.isNotEmpty(), "Should have feature execution data")
    }

    @Test
    fun `test feature configuration export and import`() = runTest {
        // Register features
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)

        // Export configuration
        val exportedConfig = featureManager.exportFeatures()
        assertNotNull(exportedConfig, "Exported configuration should not be null")
        assertTrue(exportedConfig.containsKey("features"), "Should contain features data")
        assertTrue(exportedConfig.containsKey("statistics"), "Should contain statistics data")

        val featuresData = exportedConfig["features"] as? List<*>
        assertNotNull(featuresData, "Features data should not be null")
        assertEquals(2, featuresData.size, "Should have data for 2 features")

        // Test import (basic validation)
        val importSuccess = featureManager.importFeatures(exportedConfig)
        assertTrue(importSuccess, "Configuration import should be successful")
    }

    @Test
    fun `test feature unregistration and cleanup`() = runTest {
        // Register features
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)

        // Verify registration
        var registeredFeatures = featureManager.getAllFeatures()
        assertEquals(2, registeredFeatures.size, "Should have 2 registered features")

        // Unregister one feature
        val unregistrationSuccess = featureManager.unregisterFeature("game_library")
        assertTrue(unregistrationSuccess, "Feature unregistration should be successful")

        // Verify unregistration
        registeredFeatures = featureManager.getAllFeatures()
        assertEquals(1, registeredFeatures.size, "Should have 1 registered feature")
        assertEquals("parental_controls", registeredFeatures.first().id)

        // Test unregistering non-existent feature
        val invalidUnregistration = featureManager.unregisterFeature("non_existent")
        assertFalse(invalidUnregistration, "Unregistering non-existent feature should fail")
    }

    @Test
    fun `test feature state observation`() = runTest {
        // Register feature
        featureManager.registerFeature(accessibilityFeature)

        val context = FeatureContext(
            featureId = "accessibility",
            executionId = "test_execution",
            userId = "test_user"
        )

        // Test state observation (basic test since flows are harder to test in unit tests)
        val feature = featureManager.getFeature("accessibility")
        assertNotNull(feature, "Feature should be registered")

        // Feature should start in UNINITIALIZED state
        assertEquals(FeatureState.UNINITIALIZED, feature.state.value)
    }

    @Test
    fun `test feature error handling`() = runTest {
        // Test executing non-existent feature
        val context = FeatureContext(
            featureId = "non_existent",
            executionId = "test_execution",
            userId = "test_user"
        )

        val result = featureManager.executeFeature("non_existent", context)
        assertFalse(result.success, "Executing non-existent feature should fail")
        assertTrue(result.errors.isNotEmpty(), "Should have error messages")
    }

    @Test
    fun `test feature manager shutdown`() = runTest {
        // Register features
        featureManager.registerFeature(gameLibraryFeature)
        featureManager.registerFeature(parentalControlsFeature)

        // Initialize
        val context = FeatureManagerContext(userId = "test_user")
        featureManager.initialize(context)

        // Verify initialized state
        assertTrue(featureManager.status.value.isInitialized)

        // Shutdown
        featureManager.shutdown()

        // Verify shutdown state
        val status = featureManager.status.value
        assertFalse(status.isInitialized, "Manager should not be initialized after shutdown")
        assertEquals(0, status.registeredFeatureCount, "Should have no registered features after shutdown")
    }

    @Test
    fun `test multiple feature execution with dependencies`() = runTest {
        // Register features
        featureManager.registerFeature(parentalControlsFeature)
        featureManager.registerFeature(accessibilityFeature)

        val context = FeatureContext(
            featureId = "test",
            executionId = "test_execution",
            userId = "test_user"
        )

        // Execute multiple features
        val results = featureManager.executeFeatures(
            listOf("accessibility", "parental_controls"),
            context
        )

        assertEquals(2, results.size, "Should have results for 2 features")

        // Both features should execute successfully (even with missing dependencies)
        results.forEach { result ->
            assertTrue(result.success, "Feature execution should be successful")
        }
    }

    @Test
    fun `test feature dependency validation`() = runTest {
        // Test that features with dependencies are properly validated
        val validationResult = featureManager.validateAllFeatures()

        // Even without dependencies satisfied, validation should check structure
        assertNotNull(validationResult, "Validation result should not be null")

        // Test individual feature validation
        val gameLibraryValidation = gameLibraryFeature.validate()
        assertNotNull(gameLibraryValidation, "Feature validation should not be null")

        val parentalControlsValidation = parentalControlsFeature.validate()
        assertNotNull(parentalControlsValidation, "Feature validation should not be null")

        val accessibilityValidation = accessibilityFeature.validate()
        assertNotNull(accessibilityValidation, "Feature validation should not be null")
    }
}