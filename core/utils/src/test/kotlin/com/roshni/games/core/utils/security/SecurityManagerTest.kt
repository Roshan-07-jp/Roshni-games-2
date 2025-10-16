package com.roshni.games.core.utils.security

import com.roshni.games.core.utils.security.model.AuthenticationResult
import com.roshni.games.core.utils.security.model.AuthorizationResult
import com.roshni.games.core.utils.security.model.SecurityContext
import com.roshni.games.core.utils.security.model.SecurityEvent
import com.roshni.games.core.utils.security.permissions.GameplayPermission
import com.roshni.games.core.utils.security.permissions.SystemPermission
import com.roshni.games.feature.parentalcontrols.domain.SecurityService
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityManagerTest {

    @Mock
    private lateinit var mockSecurityService: SecurityService

    private lateinit var securityManager: SecurityManagerImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        securityManager = SecurityManagerImpl(mockSecurityService)
    }

    @Test
    fun `test successful authentication`() = runTest {
        // Given
        val credentials = mapOf(
            "userId" to "testUser",
            "password" to "validPassword",
            "deviceId" to "device123"
        )

        whenever(mockSecurityService.authenticateWithPin("validPassword"))
            .thenReturn(Result.success(true))

        // When
        val result = securityManager.authenticate(credentials)

        // Then
        assertIs<AuthenticationResult.Success>(result)
        val successResult = result as AuthenticationResult.Success
        assertEquals("testUser", successResult.userId)
        assertNotNull(successResult.sessionId)
        assertTrue(successResult.context.isValid())
    }

    @Test
    fun `test failed authentication`() = runTest {
        // Given
        val credentials = mapOf(
            "userId" to "testUser",
            "password" to "invalidPassword",
            "deviceId" to "device123"
        )

        whenever(mockSecurityService.authenticateWithPin("invalidPassword"))
            .thenReturn(Result.success(false))

        // When
        val result = securityManager.authenticate(credentials)

        // Then
        assertIs<AuthenticationResult.Failure>(result)
        val failureResult = result as AuthenticationResult.Failure
        assertEquals("Authentication failed", failureResult.reason)
        assertTrue(failureResult.canRetry)
    }

    @Test
    fun `test successful authorization`() = runTest {
        // Given
        val userId = "testUser"
        val permission = GameplayPermission.PLAY_GAMES.name

        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(permission),
            deviceId = "device123"
        )

        // Initialize security manager first
        securityManager.initialize(context)

        // When
        val result = securityManager.authorize(userId, permission)

        // Then
        assertIs<AuthorizationResult.Granted>(result)
        val grantedResult = result as AuthorizationResult.Granted
        assertEquals(userId, grantedResult.userId)
        assertEquals(permission, grantedResult.permission)
    }

    @Test
    fun `test denied authorization`() = runTest {
        // Given
        val userId = "testUser"
        val permission = "ADMIN_ACCESS"

        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name), // No admin access
            deviceId = "device123"
        )

        // Initialize security manager first
        securityManager.initialize(context)

        // When
        val result = securityManager.authorize(userId, permission)

        // Then
        assertIs<AuthorizationResult.Denied>(result)
        val deniedResult = result as AuthorizationResult.Denied
        assertEquals("Insufficient permissions", deniedResult.reason)
    }

    @Test
    fun `test session validation`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        // Initialize and create session
        securityManager.initialize(context)
        val sessionId = securityManager.createSession(userId, "device123")

        // When
        val result = securityManager.validateSession(userId)

        // Then
        assertTrue(result.isSuccess)
        val updatedContext = result.getOrNull()
        assertNotNull(updatedContext)
        assertEquals(userId, updatedContext.userId)
    }

    @Test
    fun `test permission granting and revocation`() = runTest {
        // Given
        val userId = "testUser"
        val permissions = setOf("PREMIUM_ACCESS", "ADMIN_ACCESS")

        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = emptySet(),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // When - Grant permissions
        val grantResult = securityManager.grantPermissions(
            userId = userId,
            permissions = permissions,
            grantedBy = "admin"
        )

        // Then
        assertTrue(grantResult.isSuccess)

        val userPermissions = securityManager.getUserPermissions(userId)
        assertTrue(userPermissions.containsAll(permissions))

        // When - Revoke permissions
        val revokeResult = securityManager.revokePermissions(
            userId = userId,
            permissions = permissions,
            revokedBy = "admin",
            reason = "Test revocation"
        )

        // Then
        assertTrue(revokeResult.isSuccess)

        val updatedPermissions = securityManager.getUserPermissions(userId)
        assertFalse(updatedPermissions.containsAll(permissions))
    }

    @Test
    fun `test security event recording and retrieval`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        val event = SecurityEvent.AuthenticationEvent(
            userId = userId,
            success = true,
            method = "PIN",
            timestamp = LocalDateTime.now()
        )

        // When
        val recordResult = securityManager.recordSecurityEvent(event)

        // Then
        assertTrue(recordResult.isSuccess)

        val events = securityManager.getSecurityEvents(userId = userId)
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it.userId == userId })
    }

    @Test
    fun `test parental controls integration`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // When
        val gameplayAllowed = securityManager.isGameplayAllowed(userId, "game123")
        val contentAllowed = securityManager.isContentAllowed(userId, "GAME", "EVERYONE")
        val socialAllowed = securityManager.isSocialAllowed(userId, "MULTIPLAYER")

        // Then
        // Results depend on SecurityService mock, but should not throw exceptions
        assertIs<Boolean>(gameplayAllowed)
        assertIs<Boolean>(contentAllowed)
        assertIs<Boolean>(socialAllowed)
    }

    @Test
    fun `test session management`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // When - Create session
        val createResult = securityManager.createSession(
            userId = userId,
            deviceId = "device123",
            metadata = mapOf("appVersion" to "1.0.0")
        )

        // Then
        assertTrue(createResult.isSuccess)
        val sessionId = createResult.getOrNull()
        assertNotNull(sessionId)

        // Verify session exists
        val sessionInfo = securityManager.getSessionInfo(sessionId!!)
        assertNotNull(sessionInfo)
        assertEquals(userId, sessionInfo["userId"])

        // Test session validity
        val isValid = securityManager.isSessionValid(sessionId)
        assertTrue(isValid)

        // When - Refresh session
        val refreshResult = securityManager.refreshSession(sessionId)

        // Then
        assertTrue(refreshResult.isSuccess)

        // When - Invalidate session
        val invalidateResult = securityManager.invalidateSession(userId)

        // Then
        assertTrue(invalidateResult.isSuccess)

        // Verify session is no longer valid
        val isStillValid = securityManager.isSessionValid(sessionId)
        assertFalse(isStillValid)
    }

    @Test
    fun `test security metrics and monitoring`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // Record some events
        val event = SecurityEvent.AuthenticationEvent(
            userId = userId,
            success = true,
            method = "PIN",
            timestamp = LocalDateTime.now()
        )
        securityManager.recordSecurityEvent(event)

        // When
        val metrics = securityManager.getSecurityMetrics()
        val violations = securityManager.checkSecurityViolations(userId)
        val recommendations = securityManager.getSecurityRecommendations(userId)

        // Then
        assertIs<Map<String, Any>>(metrics)
        assertTrue(metrics.containsKey("totalEvents"))
        assertTrue(metrics.containsKey("authenticationEvents"))

        assertIs<List<String>>(violations)
        assertIs<List<String>>(recommendations)
    }

    @Test
    fun `test encryption and hashing utilities`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        val testData = "sensitive information"
        val testPassword = "testPassword123"

        // When - Test encryption
        val encryptedResult = securityManager.encryptData(testData)
        val decryptedResult = encryptedResult.getOrNull()?.let { securityManager.decryptData(it) }

        // Then
        assertTrue(encryptedResult.isSuccess)
        assertTrue(decryptedResult.isSuccess)

        val decryptedData = decryptedResult.getOrNull()
        assertNotNull(decryptedData)
        assertEquals(testData, decryptedData)

        // When - Test hashing
        val hash = securityManager.hashData(testPassword)
        val isValidHash = securityManager.verifyHash(testPassword, hash)

        // Then
        assertFalse(hash.isBlank())
        assertTrue(isValidHash)

        // Test invalid hash verification
        val isInvalidHash = securityManager.verifyHash("wrongPassword", hash)
        assertFalse(isInvalidHash)
    }

    @Test
    fun `test secure token generation`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(GameplayPermission.PLAY_GAMES.name),
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // When
        val token1 = securityManager.generateSecureToken(32)
        val token2 = securityManager.generateSecureToken(32)
        val shortToken = securityManager.generateSecureToken(16)

        // Then
        assertNotNull(token1)
        assertNotNull(token2)
        assertNotNull(shortToken)

        assertEquals(32, token1.length)
        assertEquals(32, token2.length)
        assertEquals(16, shortToken.length)

        // Tokens should be unique
        assertTrue(token1 != token2)

        // Tokens should not be empty or predictable
        assertFalse(token1.isBlank())
        assertFalse(token2.isBlank())
        assertFalse(shortToken.isBlank())
    }

    @Test
    fun `test hierarchical permissions`() = runTest {
        // Given
        val userId = "testUser"
        val context = SecurityContext(
            userId = userId,
            sessionId = "session123",
            authenticatedAt = LocalDateTime.now(),
            permissions = setOf(SystemPermission.ADMIN.name), // Admin should have all permissions
            deviceId = "device123"
        )

        securityManager.initialize(context)

        // When - Check if admin has basic access (hierarchical)
        val hasBasicAccess = securityManager.hasPermission(userId, SystemPermission.BASIC_ACCESS.name)
        val hasUserAccess = securityManager.hasPermission(userId, SystemPermission.USER.name)
        val hasModeratorAccess = securityManager.hasPermission(userId, SystemPermission.MODERATOR.name)

        // Then
        assertTrue(hasBasicAccess)
        assertTrue(hasUserAccess)
        assertTrue(hasModeratorAccess)

        // When - Get effective permissions
        val effectivePermissions = securityManager.getEffectivePermissions(userId)

        // Then
        assertTrue(effectivePermissions.contains(SystemPermission.BASIC_ACCESS.name))
        assertTrue(effectivePermissions.contains(SystemPermission.USER.name))
        assertTrue(effectivePermissions.contains(SystemPermission.MODERATOR.name))
        assertTrue(effectivePermissions.contains(SystemPermission.ADMIN.name))
    }

    @Test
    fun `test error handling in authentication`() = runTest {
        // Given
        val credentials = mapOf(
            "userId" to "testUser",
            "password" to "password"
        )

        // Mock SecurityService to throw exception
        whenever(mockSecurityService.authenticateWithPin("password"))
            .thenThrow(RuntimeException("Service unavailable"))

        // When
        val result = securityManager.authenticate(credentials)

        // Then
        assertIs<AuthenticationResult.Error>(result)
        val errorResult = result as AuthenticationResult.Error
        assertEquals("Unknown error", errorResult.message)
    }

    @Test
    fun `test error handling in authorization`() = runTest {
        // Given - No security context initialized

        // When
        val result = securityManager.authorize("testUser", "SOME_PERMISSION")

        // Then
        assertIs<AuthorizationResult.Denied>(result)
        val deniedResult = result as AuthorizationResult.Denied
        assertEquals("No security context found", deniedResult.reason)
    }
}