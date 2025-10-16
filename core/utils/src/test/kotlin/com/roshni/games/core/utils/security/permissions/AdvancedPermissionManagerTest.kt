package com.roshni.games.core.utils.security.permissions

import com.roshni.games.core.utils.security.model.SecurityContext
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancedPermissionManagerTest {

    private lateinit var permissionManager: AdvancedPermissionManager

    @Before
    fun setup() {
        permissionManager = AdvancedPermissionManager()
    }

    @Test
    fun `test hierarchical permission checking`() = runTest {
        // Given
        val userPermissions = setOf(
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // When - Check if admin has basic access (hierarchical)
        val hasBasicAccess = permissionManager.checkHierarchicalPermission(
            userPermissions,
            com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
        )

        val hasUserAccess = permissionManager.checkHierarchicalPermission(
            userPermissions,
            com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name
        )

        val hasModeratorAccess = permissionManager.checkHierarchicalPermission(
            userPermissions,
            com.roshni.games.core.utils.security.permissions.SystemPermission.MODERATOR.name
        )

        // Then
        assertTrue(hasBasicAccess)
        assertTrue(hasUserAccess)
        assertTrue(hasModeratorAccess)
    }

    @Test
    fun `test direct permission checking`() = runTest {
        // Given
        val userPermissions = setOf(
            com.roshni.games.core.utils.security.permissions.GameplayPermission.PLAY_GAMES.name
        )

        // When - Check direct permission
        val hasPlayPermission = permissionManager.checkHierarchicalPermission(
            userPermissions,
            com.roshni.games.core.utils.security.permissions.GameplayPermission.PLAY_GAMES.name
        )

        val hasAdminPermission = permissionManager.checkHierarchicalPermission(
            userPermissions,
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // Then
        assertTrue(hasPlayPermission)
        assertFalse(hasAdminPermission)
    }

    @Test
    fun `test effective permissions calculation`() = runTest {
        // Given
        val userPermissions = setOf(
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // When
        val effectivePermissions = permissionManager.getEffectivePermissions(userPermissions)

        // Then
        assertTrue(effectivePermissions.contains(com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name))
        assertTrue(effectivePermissions.contains(com.roshni.games.core.utils.security.permissions.SystemPermission.MODERATOR.name))
        assertTrue(effectivePermissions.contains(com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name))
        assertTrue(effectivePermissions.contains(com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name))
    }

    @Test
    fun `test time-based permission rules`() = runTest {
        // Given
        val permission = "NIGHT_ACCESS"
        val rule = permissionManager.createTimeBasedRule(
            permission = permission,
            startHour = 22, // 10 PM
            startMinute = 0,
            endHour = 6,   // 6 AM
            endMinute = 0,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7) // All days
        )

        // When - Test during allowed hours (11 PM)
        val duringAllowedHours = rule.evaluate(
            context = mapOf("currentHour" to 23, "currentMinute" to 0)
        )

        // Then
        assertTrue(duringAllowedHours)
    }

    @Test
    fun `test location-based permission rules`() = runTest {
        // Given
        val permission = "LOCATION_ACCESS"
        val rule = permissionManager.createLocationBasedRule(
            permission = permission,
            allowedLocations = setOf("HOME", "WORK", "SCHOOL"),
            blockedLocations = setOf("RESTRICTED_AREA")
        )

        // When - Test allowed location
        val allowedLocation = rule.evaluate(
            context = emptyMap(),
            location = "HOME"
        )

        // Test blocked location
        val blockedLocation = rule.evaluate(
            context = emptyMap(),
            location = "RESTRICTED_AREA"
        )

        // Test unknown location (should be denied if not in allowed list)
        val unknownLocation = rule.evaluate(
            context = emptyMap(),
            location = "UNKNOWN"
        )

        // Then
        assertTrue(allowedLocation)
        assertFalse(blockedLocation)
        assertFalse(unknownLocation)
    }

    @Test
    fun `test context-based permission rules with AND logic`() = runTest {
        // Given
        val permission = "CONTEXT_ACCESS"
        val rule = permissionManager.createContextBasedRule(
            permission = permission,
            requiredContext = mapOf(
                "userType" to "PREMIUM",
                "deviceType" to "MOBILE",
                "appVersion" to "1.0.0"
            ),
            matchAll = true // AND logic
        )

        // When - Test with all required context
        val allContextMatch = rule.evaluate(
            context = mapOf(
                "userType" to "PREMIUM",
                "deviceType" to "MOBILE",
                "appVersion" to "1.0.0"
            )
        )

        // Test with partial context match
        val partialContextMatch = rule.evaluate(
            context = mapOf(
                "userType" to "PREMIUM",
                "deviceType" to "MOBILE"
                // Missing appVersion
            )
        )

        // Then
        assertTrue(allContextMatch)
        assertFalse(partialContextMatch)
    }

    @Test
    fun `test context-based permission rules with OR logic`() = runTest {
        // Given
        val permission = "CONTEXT_ACCESS"
        val rule = permissionManager.createContextBasedRule(
            permission = permission,
            requiredContext = mapOf(
                "userType" to "PREMIUM",
                "deviceType" to "MOBILE",
                "appVersion" to "1.0.0"
            ),
            matchAll = false // OR logic
        )

        // When - Test with partial context match
        val partialContextMatch = rule.evaluate(
            context = mapOf(
                "userType" to "PREMIUM"
                // Missing other fields but should match OR logic
            )
        )

        // Test with no context match
        val noContextMatch = rule.evaluate(
            context = emptyMap()
        )

        // Then
        assertTrue(partialContextMatch)
        assertFalse(noContextMatch)
    }

    @Test
    fun `test forbidden context rules`() = runTest {
        // Given
        val permission = "CONTEXT_ACCESS"
        val rule = permissionManager.createContextBasedRule(
            permission = permission,
            requiredContext = mapOf("userType" to "PREMIUM"),
            forbiddenContext = mapOf("status" to "SUSPENDED")
        )

        // When - Test with allowed context
        val allowedContext = rule.evaluate(
            context = mapOf(
                "userType" to "PREMIUM",
                "status" to "ACTIVE"
            )
        )

        // Test with forbidden context
        val forbiddenContext = rule.evaluate(
            context = mapOf(
                "userType" to "PREMIUM",
                "status" to "SUSPENDED"
            )
        )

        // Then
        assertTrue(allowedContext)
        assertFalse(forbiddenContext)
    }

    @Test
    fun `test complex permission checking with multiple rules`() = runTest {
        // Given
        val userPermissions = setOf(
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // When - Test permission with context
        val result = permissionManager.checkPermissionWithRules(
            userPermissions = userPermissions,
            requiredPermission = com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name,
            context = mapOf("environment" to "PRODUCTION"),
            location = "HOME"
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `test permission cache functionality`() = runTest {
        // Given
        val userPermissions = setOf(
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // When - First check (should cache)
        val result1 = permissionManager.checkPermissionWithRules(
            userPermissions = userPermissions,
            requiredPermission = com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name
        )

        // Second check (should use cache)
        val result2 = permissionManager.checkPermissionWithRules(
            userPermissions = userPermissions,
            requiredPermission = com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name
        )

        // Get cache statistics
        val cacheStats = permissionManager.getCacheStatistics()

        // Then
        assertTrue(result1)
        assertTrue(result2)
        assertTrue(cacheStats.containsKey("totalEntries"))
        assertTrue(cacheStats.containsKey("validEntries"))

        // Clear cache
        permissionManager.clearPermissionCache()
        val cacheStatsAfterClear = permissionManager.getCacheStatistics()
        assertTrue(cacheStatsAfterClear["totalEntries"] as Int == 0)
    }

    @Test
    fun `test security context extension functions`() = runTest {
        // Given
        val context = SecurityContext(
            userId = "testUser",
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
            ),
            deviceId = "device123",
            metadata = mapOf("location" to "HOME")
        )

        // When - Test hasPermission extension
        val hasBasicPermission = permissionManager.hasPermission(
            context = context,
            permission = com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
        )

        val hasAdminPermission = permissionManager.hasPermission(
            context = context,
            permission = com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name
        )

        // Test hasAnyPermission extension
        val hasAnyPermission = permissionManager.hasAnyPermission(
            context = context,
            permissions = setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name,
                "INVALID_PERMISSION"
            )
        )

        // Test hasAllPermissions extension
        val hasAllPermissions = permissionManager.hasAllPermissions(
            context = context,
            permissions = setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name
            )
        )

        // Then
        assertTrue(hasBasicPermission)
        assertTrue(hasAdminPermission)
        assertTrue(hasAnyPermission)
        assertTrue(hasAllPermissions)
    }

    @Test
    fun `test permission rule priority and enabling`() = runTest {
        // Given
        val rule = com.roshni.games.core.utils.security.permissions.AdvancedPermissionManager.PermissionRule(
            permission = "TEST_PERMISSION",
            priority = 10,
            enabled = false
        )

        // When
        val result = rule.evaluate()

        // Then
        assertFalse(result) // Should be false because rule is disabled
    }

    @Test
    fun `test overnight time-based rules`() = runTest {
        // Given - Create a rule that spans overnight (22:00 to 06:00)
        val rule = permissionManager.createTimeBasedRule(
            permission = "NIGHT_ACCESS",
            startHour = 22, // 10 PM
            startMinute = 0,
            endHour = 6,   // 6 AM
            endMinute = 0
        )

        // When - Test at 11 PM (during allowed period)
        val at23Hours = rule.evaluate(
            context = mapOf("currentHour" to 23, "currentMinute" to 0)
        )

        // Test at 1 AM (during allowed period)
        val at1Hour = rule.evaluate(
            context = mapOf("currentHour" to 1, "currentMinute" to 0)
        )

        // Test at 12 PM (during disallowed period)
        val at12Hours = rule.evaluate(
            context = mapOf("currentHour" to 12, "currentMinute" to 0)
        )

        // Then
        assertTrue(at23Hours)
        assertTrue(at1Hour)
        assertFalse(at12Hours)
    }

    @Test
    fun `test weekend-only time rules`() = runTest {
        // Given - Weekend only rule (Saturday and Sunday)
        val rule = permissionManager.createTimeBasedRule(
            permission = "WEEKEND_ACCESS",
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = setOf(6, 7) // Saturday and Sunday
        )

        // When - Test on Saturday
        val saturdayResult = rule.evaluate(
            context = mapOf("currentDayOfWeek" to 6)
        )

        // Test on Sunday
        val sundayResult = rule.evaluate(
            context = mapOf("currentDayOfWeek" to 7)
        )

        // Test on Monday (weekday)
        val mondayResult = rule.evaluate(
            context = mapOf("currentDayOfWeek" to 1)
        )

        // Then
        assertTrue(saturdayResult)
        assertTrue(sundayResult)
        assertFalse(mondayResult)
    }

    @Test
    fun `test empty location rules allow all locations`() = runTest {
        // Given - Rule with no location restrictions
        val rule = permissionManager.createLocationBasedRule(
            permission = "UNIVERSAL_ACCESS",
            allowedLocations = emptySet()
        )

        // When - Test with any location
        val result = rule.evaluate(
            context = emptyMap(),
            location = "ANYWHERE"
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `test empty context rules allow all contexts`() = runTest {
        // Given - Rule with no context restrictions
        val rule = permissionManager.createContextBasedRule(
            permission = "UNIVERSAL_ACCESS",
            requiredContext = emptyMap()
        )

        // When - Test with any context
        val result = rule.evaluate(
            context = mapOf("anyKey" to "anyValue")
        )

        // Then
        assertTrue(result)
    }
}