package com.roshni.games.core.utils.security

import com.roshni.games.core.utils.security.model.AuthenticationResult
import com.roshni.games.core.utils.security.model.AuthorizationResult
import com.roshni.games.core.utils.security.model.SecurityContext
import com.roshni.games.core.utils.security.model.SecurityEvent
import com.roshni.games.core.utils.security.permissions.GameplayPermission
import com.roshni.games.core.utils.security.permissions.ContentPermission
import com.roshni.games.core.utils.security.permissions.SocialPermission
import com.roshni.games.core.utils.security.permissions.SystemPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

/**
 * Core security manager interface providing comprehensive access control and security management
 * for the Roshni Games platform. Integrates with existing SecurityService and provides
 * advanced permission management with hierarchical and time-based permissions.
 */
interface SecurityManager {

    // ==================== CORE SECURITY OPERATIONS ====================

    /**
     * Initialize the security manager with the provided context
     */
    suspend fun initialize(securityContext: SecurityContext): Result<Unit>

    /**
     * Authenticate a user with the provided credentials
     */
    suspend fun authenticate(credentials: Map<String, Any>): AuthenticationResult

    /**
     * Authorize a user for a specific action or resource
     */
    suspend fun authorize(
        userId: String,
        permission: Any,
        resource: String? = null,
        context: Map<String, Any> = emptyMap()
    ): AuthorizationResult

    /**
     * Validate current session and return updated security context
     */
    suspend fun validateSession(userId: String): Result<SecurityContext>

    /**
     * Invalidate user session
     */
    suspend fun invalidateSession(userId: String): Result<Unit>

    // ==================== PERMISSION MANAGEMENT ====================

    /**
     * Grant permissions to a user
     */
    suspend fun grantPermissions(
        userId: String,
        permissions: Set<Any>,
        grantedBy: String? = null,
        validUntil: LocalDateTime? = null,
        conditions: Map<String, Any> = emptyMap()
    ): Result<Unit>

    /**
     * Revoke permissions from a user
     */
    suspend fun revokePermissions(
        userId: String,
        permissions: Set<Any>,
        revokedBy: String? = null,
        reason: String? = null
    ): Result<Unit>

    /**
     * Check if user has specific permission
     */
    suspend fun hasPermission(
        userId: String,
        permission: Any,
        resource: String? = null,
        context: Map<String, Any> = emptyMap()
    ): Boolean

    /**
     * Get all permissions for a user
     */
    suspend fun getUserPermissions(userId: String): Set<Any>

    /**
     * Get effective permissions for a user (including hierarchical permissions)
     */
    suspend fun getEffectivePermissions(
        userId: String,
        resource: String? = null,
        context: Map<String, Any> = emptyMap()
    ): Set<Any>

    // ==================== SECURITY CONTEXT ====================

    /**
     * Get current security context for a user
     */
    suspend fun getSecurityContext(userId: String): SecurityContext?

    /**
     * Update security context for a user
     */
    suspend fun updateSecurityContext(
        userId: String,
        updates: Map<String, Any>
    ): Result<SecurityContext>

    /**
     * Get security context as flow for reactive updates
     */
    fun getSecurityContextFlow(userId: String): StateFlow<SecurityContext?>

    // ==================== SECURITY EVENTS ====================

    /**
     * Record a security event
     */
    suspend fun recordSecurityEvent(event: SecurityEvent): Result<Unit>

    /**
     * Get security events for a user
     */
    suspend fun getSecurityEvents(
        userId: String? = null,
        eventType: SecurityEvent.Type? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null
    ): List<SecurityEvent>

    /**
     * Get security events as flow for reactive monitoring
     */
    fun getSecurityEventsFlow(
        userId: String? = null,
        eventType: SecurityEvent.Type? = null
    ): Flow<SecurityEvent>

    // ==================== PARENTAL CONTROLS INTEGRATION ====================

    /**
     * Check if gameplay is allowed for a user (parental controls)
     */
    suspend fun isGameplayAllowed(userId: String, gameId: String? = null): Boolean

    /**
     * Check if content is allowed for a user (parental controls)
     */
    suspend fun isContentAllowed(
        userId: String,
        contentType: String,
        contentRating: String? = null
    ): Boolean

    /**
     * Check if social features are allowed for a user (parental controls)
     */
    suspend fun isSocialAllowed(userId: String, socialFeature: String? = null): Boolean

    /**
     * Get parental control restrictions for a user
     */
    suspend fun getParentalControlRestrictions(userId: String): Map<String, Any>

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Create a new security session
     */
    suspend fun createSession(
        userId: String,
        deviceId: String,
        metadata: Map<String, Any> = emptyMap()
    ): Result<String>

    /**
     * Refresh an existing session
     */
    suspend fun refreshSession(sessionId: String): Result<Unit>

    /**
     * Get session information
     */
    suspend fun getSessionInfo(sessionId: String): Map<String, Any>?

    /**
     * Check if session is valid and active
     */
    suspend fun isSessionValid(sessionId: String): Boolean

    // ==================== SECURITY MONITORING ====================

    /**
     * Get security metrics and statistics
     */
    suspend fun getSecurityMetrics(
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null
    ): Map<String, Any>

    /**
     * Check for security violations
     */
    suspend fun checkSecurityViolations(userId: String? = null): List<String>

    /**
     * Get security recommendations for a user
     */
    suspend fun getSecurityRecommendations(userId: String): List<String>

    // ==================== UTILITY METHODS ====================

    /**
     * Encrypt sensitive data
     */
    suspend fun encryptData(data: String, keyAlias: String? = null): Result<String>

    /**
     * Decrypt sensitive data
     */
    suspend fun decryptData(encryptedData: String, keyAlias: String? = null): Result<String>

    /**
     * Generate secure token
     */
    suspend fun generateSecureToken(length: Int = 32): String

    /**
     * Hash sensitive data
     */
    suspend fun hashData(data: String, algorithm: String = "SHA-256"): String

    /**
     * Verify hashed data
     */
    suspend fun verifyHash(data: String, hash: String, algorithm: String = "SHA-256"): Boolean
}