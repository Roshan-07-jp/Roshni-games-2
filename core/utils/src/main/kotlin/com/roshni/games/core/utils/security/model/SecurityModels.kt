package com.roshni.games.core.utils.security.model

import java.time.LocalDateTime

// ==================== SECURITY CONTEXT ====================

/**
 * Comprehensive security context containing user session and permission information
 */
data class SecurityContext(
    val userId: String,
    val sessionId: String,
    val authenticatedAt: LocalDateTime,
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),
    val permissions: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val deviceId: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val expiresAt: LocalDateTime = authenticatedAt.plusHours(24),
    val isActive: Boolean = true
) {
    /**
     * Check if the security context is currently valid
     */
    fun isValid(): Boolean {
        return isActive &&
               LocalDateTime.now().isBefore(expiresAt) &&
               !userId.isBlank() &&
               !sessionId.isBlank()
    }

    /**
     * Check if the context has expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }

    /**
     * Check if the context has a specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if the context has a specific role
     */
    fun hasRole(role: String): Boolean {
        return roles.contains(role)
    }

    /**
     * Create a copy with updated last activity time
     */
    fun withUpdatedActivity(): SecurityContext {
        return copy(lastActivityAt = LocalDateTime.now())
    }

    /**
     * Create a copy with additional permissions
     */
    fun withPermissions(vararg additionalPermissions: String): SecurityContext {
        return copy(permissions = permissions + additionalPermissions)
    }

    /**
     * Create a copy with additional roles
     */
    fun withRoles(vararg additionalRoles: String): SecurityContext {
        return copy(roles = roles + additionalRoles)
    }

    /**
     * Create a copy with updated metadata
     */
    fun withMetadata(additionalMetadata: Map<String, Any>): SecurityContext {
        return copy(metadata = metadata + additionalMetadata)
    }
}

// ==================== AUTHENTICATION RESULT ====================

/**
 * Result of an authentication attempt
 */
sealed class AuthenticationResult {
    /**
     * Successful authentication
     */
    data class Success(
        val userId: String,
        val sessionId: String,
        val context: SecurityContext,
        val expiresAt: LocalDateTime,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthenticationResult() {
        fun isValid(): Boolean {
            return context.isValid() && LocalDateTime.now().isBefore(expiresAt)
        }
    }

    /**
     * Failed authentication
     */
    data class Failure(
        val reason: String,
        val canRetry: Boolean = true,
        val retryAfter: LocalDateTime? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthenticationResult() {
        fun canRetryNow(): Boolean {
            return canRetry && (retryAfter == null || LocalDateTime.now().isAfter(retryAfter))
        }
    }

    /**
     * Authentication error (system error)
     */
    data class Error(
        val message: String,
        val cause: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthenticationResult()

    /**
     * Multi-factor authentication required
     */
    data class MfaRequired(
        val userId: String,
        val mfaType: MfaType,
        val challengeId: String,
        val expiresAt: LocalDateTime,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthenticationResult() {
        fun isChallengeValid(): Boolean {
            return LocalDateTime.now().isBefore(expiresAt)
        }
    }

    /**
     * Account locked due to security policy
     */
    data class AccountLocked(
        val userId: String,
        val reason: String,
        val unlockAt: LocalDateTime? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthenticationResult() {
        fun canUnlockNow(): Boolean {
            return unlockAt == null || LocalDateTime.now().isAfter(unlockAt)
        }
    }
}

/**
 * Multi-factor authentication types
 */
enum class MfaType(val displayName: String) {
    SMS("SMS Code"),
    EMAIL("Email Code"),
    TOTP("Authenticator App"),
    BIOMETRIC("Biometric"),
    HARDWARE_KEY("Hardware Key")
}

// ==================== AUTHORIZATION RESULT ====================

/**
 * Result of an authorization check
 */
sealed class AuthorizationResult {
    /**
     * Access granted
     */
    data class Granted(
        val userId: String,
        val permission: String,
        val resource: String? = null,
        val context: Map<String, Any> = emptyMap(),
        val grantedAt: LocalDateTime = LocalDateTime.now(),
        val expiresAt: LocalDateTime? = null,
        val conditions: Map<String, Any> = emptyMap(),
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthorizationResult() {
        fun isValid(): Boolean {
            return expiresAt == null || LocalDateTime.now().isBefore(expiresAt)
        }

        fun hasCondition(condition: String): Boolean {
            return conditions.containsKey(condition)
        }

        fun getCondition(condition: String): Any? {
            return conditions[condition]
        }
    }

    /**
     * Access denied
     */
    data class Denied(
        val reason: String,
        val userId: String? = null,
        val permission: String? = null,
        val resource: String? = null,
        val context: Map<String, Any> = emptyMap(),
        val deniedAt: LocalDateTime = LocalDateTime.now(),
        val alternativePermissions: Set<String> = emptySet(),
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthorizationResult() {
        fun hasAlternativePermissions(): Boolean {
            return alternativePermissions.isNotEmpty()
        }

        fun getRequiredPermissions(): Set<String> {
            return setOfNotNull(permission) + alternativePermissions
        }
    }

    /**
     * Authorization error (system error)
     */
    data class Error(
        val message: String,
        val cause: String? = null,
        val userId: String? = null,
        val permission: String? = null,
        val resource: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthorizationResult()

    /**
     * Conditional access granted with restrictions
     */
    data class Conditional(
        val userId: String,
        val permission: String,
        val resource: String? = null,
        val conditions: Map<String, Any>,
        val restrictions: Map<String, Any>,
        val grantedAt: LocalDateTime = LocalDateTime.now(),
        val expiresAt: LocalDateTime? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AuthorizationResult() {
        fun isValid(): Boolean {
            return expiresAt == null || LocalDateTime.now().isBefore(expiresAt)
        }

        fun hasRestriction(restriction: String): Boolean {
            return restrictions.containsKey(restriction)
        }

        fun getRestriction(restriction: String): Any? {
            return restrictions[restriction]
        }
    }
}

// ==================== SESSION INFORMATION ====================

/**
 * Information about an active security session
 */
data class SessionInfo(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val createdAt: LocalDateTime,
    val lastActivityAt: LocalDateTime,
    val metadata: Map<String, Any> = emptyMap(),
    val isActive: Boolean = true
) {
    fun isExpired(maxSessionTimeHours: Long = 24): Boolean {
        return LocalDateTime.now().isAfter(createdAt.plusHours(maxSessionTimeHours))
    }

    fun isInactive(maxInactiveTimeMinutes: Long = 30): Boolean {
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(maxInactiveTimeMinutes))
    }

    fun withUpdatedActivity(): SessionInfo {
        return copy(lastActivityAt = LocalDateTime.now())
    }
}

// ==================== USER PERMISSIONS ====================

/**
 * Comprehensive user permissions information
 */
data class UserPermissions(
    val userId: String,
    val permissions: Set<Any>,
    val grantedPermissions: Set<Any> = emptySet(),
    val revokedPermissions: Set<Any> = emptySet(),
    val grantedBy: String? = null,
    val grantedAt: LocalDateTime? = null,
    val revokedBy: String? = null,
    val revokedAt: LocalDateTime? = null,
    val revocationReason: String? = null,
    val validUntil: LocalDateTime? = null,
    val conditions: Map<String, Any> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap()
) {
    fun isValid(): Boolean {
        return validUntil == null || LocalDateTime.now().isBefore(validUntil)
    }

    fun hasPermission(permission: Any): Boolean {
        return permissions.contains(permission) && isValid()
    }

    fun getActivePermissions(): Set<Any> {
        return if (isValid()) permissions else emptySet()
    }

    fun getRevokedPermissions(): Set<Any> {
        return revokedPermissions
    }

    fun withPermission(permission: Any): UserPermissions {
        return copy(permissions = permissions + permission)
    }

    fun withoutPermission(permission: Any): UserPermissions {
        return copy(permissions = permissions - permission)
    }
}

// ==================== SECURITY PRINCIPAL ====================

/**
 * Represents a security principal (user or system entity)
 */
data class SecurityPrincipal(
    val id: String,
    val type: PrincipalType,
    val name: String,
    val permissions: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val attributes: Map<String, Any> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastLoginAt: LocalDateTime? = null,
    val isActive: Boolean = true
) {
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    fun hasRole(role: String): Boolean {
        return roles.contains(role)
    }

    fun isValid(): Boolean {
        return isActive && id.isNotBlank()
    }
}

/**
 * Types of security principals
 */
enum class PrincipalType(val displayName: String) {
    USER("User"),
    SERVICE_ACCOUNT("Service Account"),
    SYSTEM("System"),
    GUEST("Guest"),
    ADMIN("Administrator")
}