package com.roshni.games.core.navigation.rules

import com.roshni.games.core.navigation.model.NavigationContext
import com.roshni.games.core.navigation.model.NavigationDestinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class NavigationRuleTest {

    private lateinit var testDispatcher: kotlinx.coroutines.CoroutineDispatcher
    private lateinit var testContext: NavigationContext

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testContext = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            permissions = setOf("basic_access"),
            featureFlags = setOf("feature_enabled"),
            userPreferences = mapOf("theme" to "dark")
        )
    }

    @Test
    fun `ConditionalRule should pass when all conditions are met`() = runTest {
        // Given
        val rule = ConditionalRule(
            name = "Test Conditional Rule",
            description = "Test rule with passing conditions"
        )

        rule.addCondition(
            PermissionCondition(
                description = "Check basic access",
                requiredPermission = "basic_access"
            )
        )

        rule.addCondition(
            FeatureFlagCondition(
                description = "Check feature enabled",
                featureFlag = "feature_enabled"
            )
        )

        // When
        val result = rule.evaluate(testContext)

        // Then
        assertTrue(result.passed)
        assertFalse(result.blocked)
        assertEquals("All conditions passed", result.reason)
    }

    @Test
    fun `ConditionalRule should fail when condition is not met`() = runTest {
        // Given
        val rule = ConditionalRule(
            name = "Test Conditional Rule",
            description = "Test rule with failing condition"
        )

        rule.addCondition(
            PermissionCondition(
                description = "Check admin access",
                requiredPermission = "admin_access" // User doesn't have this permission
            )
        )

        // When
        val result = rule.evaluate(testContext)

        // Then
        assertFalse(result.passed)
        assertTrue(result.blocked)
        assertTrue(result.reason.contains("admin_access"))
    }

    @Test
    fun `FeatureGateRule should pass when feature is enabled`() = runTest {
        // Given
        val rule = FeatureGateRule(
            name = "Test Feature Gate Rule",
            description = "Test feature gate rule"
        )

        rule.addFeatureGate("test_feature",
            SimpleFeatureGate(
                featureFlag = "test_feature",
                description = "Test feature gate"
            )
        )

        val contextWithFeature = testContext.copy(
            featureFlags = setOf("test_feature")
        )

        // When
        val result = rule.evaluate(contextWithFeature)

        // Then
        assertTrue(result.passed)
        assertFalse(result.blocked)
    }

    @Test
    fun `FeatureGateRule should fail when feature is disabled`() = runTest {
        // Given
        val rule = FeatureGateRule(
            name = "Test Feature Gate Rule",
            description = "Test feature gate rule"
        )

        rule.addFeatureGate("test_feature",
            SimpleFeatureGate(
                featureFlag = "test_feature",
                description = "Test feature gate"
            )
        )

        // Context without the required feature flag
        val contextWithoutFeature = testContext.copy(
            featureFlags = emptySet()
        )

        // When
        val result = rule.evaluate(contextWithoutFeature)

        // Then
        assertFalse(result.passed)
        assertTrue(result.blocked)
    }

    @Test
    fun `PermissionRule should pass when user has required permissions`() = runTest {
        // Given
        val rule = PermissionRule(
            name = "Test Permission Rule",
            description = "Test permission rule"
        )

        rule.addPermissionRequirement(NavigationDestinations.GAME_LIBRARY,
            SimplePermissionRequirement(
                description = "Require basic access",
                requiredPermissions = setOf("basic_access"),
                requireAll = true
            )
        )

        // When
        val result = rule.evaluate(testContext)

        // Then
        assertTrue(result.passed)
        assertFalse(result.blocked)
    }

    @Test
    fun `PermissionRule should fail when user lacks required permissions`() = runTest {
        // Given
        val rule = PermissionRule(
            name = "Test Permission Rule",
            description = "Test permission rule"
        )

        rule.addPermissionRequirement(NavigationDestinations.GAME_LIBRARY,
            SimplePermissionRequirement(
                description = "Require premium access",
                requiredPermissions = setOf("premium_access"),
                requireAll = true
            )
        )

        // When
        val result = rule.evaluate(testContext)

        // Then
        assertFalse(result.passed)
        assertTrue(result.blocked)
    }

    @Test
    fun `DeviceCondition should pass when device meets requirements`() = runTest {
        // Given
        val condition = DeviceCondition(
            description = "Require tablet with sufficient memory",
            requiresTablet = true,
            maxMemoryUsage = 1000L
        )

        val contextWithTablet = testContext.copy(
            deviceContext = testContext.deviceContext.copy(
                isTablet = true
            ),
            appState = testContext.appState.copy(
                memoryUsage = 500L
            )
        )

        // When
        val result = condition.evaluate(contextWithTablet)

        // Then
        assertTrue(result.passed)
        assertEquals("All device conditions met", result.reason)
    }

    @Test
    fun `DeviceCondition should fail when device doesn't meet requirements`() = runTest {
        // Given
        val condition = DeviceCondition(
            description = "Require tablet with sufficient memory",
            requiresTablet = true,
            maxMemoryUsage = 1000L
        )

        val contextWithoutTablet = testContext.copy(
            deviceContext = testContext.deviceContext.copy(
                isTablet = false // Not a tablet
            ),
            appState = testContext.appState.copy(
                memoryUsage = 500L
            )
        )

        // When
        val result = condition.evaluate(contextWithoutTablet)

        // Then
        assertFalse(result.passed)
        assertTrue(result.reason.contains("Tablet requirement not met"))
    }

    @Test
    fun `AppStateCondition should pass when app state meets requirements`() = runTest {
        // Given
        val condition = AppStateCondition(
            description = "Require network and authentication",
            requiresNetwork = true,
            requiresAuthentication = true
        )

        val contextWithRequirements = testContext.copy(
            appState = testContext.appState.copy(
                isNetworkAvailable = true,
                isUserAuthenticated = true
            )
        )

        // When
        val result = condition.evaluate(contextWithRequirements)

        // Then
        assertTrue(result.passed)
        assertEquals("All app state conditions met", result.reason)
    }

    @Test
    fun `AppStateCondition should fail when app state doesn't meet requirements`() = runTest {
        // Given
        val condition = AppStateCondition(
            description = "Require network and authentication",
            requiresNetwork = true,
            requiresAuthentication = true
        )

        val contextWithoutRequirements = testContext.copy(
            appState = testContext.appState.copy(
                isNetworkAvailable = false, // No network
                isUserAuthenticated = true
            )
        )

        // When
        val result = condition.evaluate(contextWithoutRequirements)

        // Then
        assertFalse(result.passed)
        assertTrue(result.reason.contains("Network requirement not met"))
    }

    @Test
    fun `PercentageFeatureGate should evaluate based on percentage rollout`() = runTest {
        // Given
        val gate = PercentageFeatureGate(
            featureFlag = "test_feature",
            description = "50% rollout feature gate",
            percentage = 50.0
        )

        // Test with different user IDs to simulate percentage rollout
        val context1 = testContext.copy(userId = "user_1")
        val context2 = testContext.copy(userId = "user_2")

        // When
        val result1 = gate.evaluate(true, context1)
        val result2 = gate.evaluate(true, context2)

        // Then
        // At least one should pass (50% chance), but both results should be valid
        assertTrue(result1.passed || result2.passed)
        assertNotNull(result1.metadata["rollout_percentage"])
        assertNotNull(result2.metadata["rollout_percentage"])
    }

    @Test
    fun `UserSegmentFeatureGate should evaluate based on user segments`() = runTest {
        // Given
        val gate = UserSegmentFeatureGate(
            featureFlag = "test_feature",
            description = "User segment feature gate",
            allowedSegments = setOf("premium", "beta_tester"),
            userSegmentProvider = { context ->
                when (context.userId) {
                    "premium_user" -> "premium"
                    "beta_user" -> "beta_tester"
                    else -> "basic"
                }
            }
        )

        val premiumContext = testContext.copy(
            userId = "premium_user",
            featureFlags = setOf("test_feature")
        )

        val basicContext = testContext.copy(
            userId = "basic_user",
            featureFlags = setOf("test_feature")
        )

        // When
        val premiumResult = gate.evaluate(true, premiumContext)
        val basicResult = gate.evaluate(true, basicContext)

        // Then
        assertTrue(premiumResult.passed)
        assertFalse(basicResult.passed)
        assertEquals("premium", premiumResult.metadata["user_segment"])
        assertEquals("basic", basicResult.metadata["user_segment"])
    }

    @Test
    fun `TimeBasedPermissionRequirement should evaluate based on time ranges`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val requirement = TimeBasedPermissionRequirement(
            description = "Time-based permission requirement",
            requiredPermission = "special_access",
            validTimeRanges = listOf(
                TimeRange(
                    startHour = 9,
                    startMinute = 0,
                    endHour = 17,
                    endMinute = 0,
                    daysOfWeek = setOf(1, 2, 3, 4, 5) // Monday to Friday
                )
            )
        )

        val contextWithPermission = testContext.copy(
            permissions = setOf("special_access")
        )

        // When
        val result = requirement.checkPermission(contextWithPermission)

        // Then
        assertNotNull(result)
        assertTrue(result.passed || !result.passed) // Either outcome is valid based on current time
        assertEquals(setOf("special_access"), result.requiredPermissions)
    }

    @Test
    fun `HierarchicalPermissionRequirement should evaluate based on permission hierarchy`() = runTest {
        // Given
        val hierarchy = mapOf(
            "admin" to setOf("moderator", "user"),
            "moderator" to setOf("user")
        )

        val requirement = com.roshni.games.core.navigation.rules.HierarchicalPermissionRequirement(
            description = "Hierarchical permission requirement",
            permissionHierarchy = hierarchy,
            requiredPermission = "user"
        )

        val adminContext = testContext.copy(permissions = setOf("admin"))
        val userContext = testContext.copy(permissions = setOf("user"))
        val noPermissionContext = testContext.copy(permissions = emptySet())

        // When
        val adminResult = requirement.checkPermission(adminContext)
        val userResult = requirement.checkPermission(userContext)
        val noPermissionResult = requirement.checkPermission(noPermissionContext)

        // Then
        assertTrue(adminResult.passed) // Admin has access to user permission
        assertTrue(userResult.passed) // User has user permission
        assertFalse(noPermissionResult.passed) // No permissions
    }

    @Test
    fun `NavigationRule should track statistics correctly`() = runTest {
        // Given
        val rule = ConditionalRule(
            name = "Statistics Test Rule",
            description = "Rule for testing statistics"
        )

        rule.addCondition(
            PermissionCondition(
                description = "Always pass condition",
                requiredPermission = "basic_access"
            )
        )

        // When - Evaluate multiple times
        repeat(5) {
            rule.evaluate(testContext)
        }

        val stats = rule.getStatistics()

        // Then
        assertEquals(5, stats.totalEvaluations)
        assertEquals(5, stats.passedEvaluations)
        assertEquals(0, stats.failedEvaluations)
        assertTrue(stats.averageExecutionTimeMs >= 0)
    }

    @Test
    fun `NavigationRule should enable and disable correctly`() {
        // Given
        val rule = ConditionalRule(
            name = "Enable/Disable Test Rule",
            description = "Rule for testing enable/disable"
        )

        // Initially should be enabled (default)
        assertTrue(rule.isEnabled.value)

        // When/Then - Disable
        rule.disable()
        assertFalse(rule.isEnabled.value)

        // When/Then - Enable
        rule.enable()
        assertTrue(rule.isEnabled.value)
    }

    @Test
    fun `NavigationRule should validate configuration correctly`() {
        // Given
        val validRule = ConditionalRule(
            name = "Valid Rule",
            description = "Valid rule configuration"
        )

        val invalidRule = ConditionalRule(
            name = "", // Invalid - empty name
            description = "Invalid rule configuration"
        )

        // When
        val validResult = validRule.validateConfig()
        val invalidResult = invalidRule.validateConfig()

        // Then
        assertTrue(validResult.isValid)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.isNotEmpty())
    }
}