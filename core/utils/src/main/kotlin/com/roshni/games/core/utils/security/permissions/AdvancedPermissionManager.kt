package com.roshni.games.core.utils.security.permissions

import com.roshni.games.core.utils.security.model.SecurityContext
import com.roshni.games.core.utils.security.model.UserPermissions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Advanced permission management system with hierarchical and time-based permissions
 */
class AdvancedPermissionManager {

    private val mutex = Mutex()
    private val permissionCache = mutableMapOf<String, PermissionCacheEntry>()

    /**
     * Cache entry for permission checks
     */
    private data class PermissionCacheEntry(
        val userId: String,
        val permission: String,
        val resource: String?,
        val context: Map<String, Any>,
        val result: Boolean,
        val cachedAt: LocalDateTime,
        val expiresAt: LocalDateTime
    ) {
        fun isValid(): Boolean {
            return LocalDateTime.now().isBefore(expiresAt)
        }
    }

    /**
     * Time-based permission rule
     */
    data class TimeBasedRule(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val daysOfWeek: Set<Int> = (1..7).toSet(), // 1 = Monday, 7 = Sunday
        val timeZone: String = "UTC"
    ) {
        fun isActive(): Boolean {
            val now = LocalDateTime.now()
            val currentDayOfWeek = now.dayOfWeek.value

            if (!daysOfWeek.contains(currentDayOfWeek)) {
                return false
            }

            return now.isAfter(startTime) && now.isBefore(endTime)
        }
    }

    /**
     * Location-based permission rule
     */
    data class LocationBasedRule(
        val allowedLocations: Set<String>,
        val blockedLocations: Set<String> = emptySet(),
        val requireExactMatch: Boolean = false
    ) {
        fun isAllowed(location: String?): Boolean {
            if (location == null) return allowedLocations.isEmpty()

            if (blockedLocations.contains(location)) {
                return false
            }

            return allowedLocations.isEmpty() || allowedLocations.contains(location)
        }
    }

    /**
     * Context-based permission rule
     */
    data class ContextBasedRule(
        val requiredContext: Map<String, Any>,
        val forbiddenContext: Map<String, Any> = emptyMap(),
        val matchAll: Boolean = true // true = AND logic, false = OR logic
    ) {
        fun isAllowed(context: Map<String, Any>): Boolean {
            // Check forbidden context first
            for ((key, value) in forbiddenContext) {
                if (context[key] == value) {
                    return false
                }
            }

            // Check required context
            if (requiredContext.isEmpty()) {
                return true
            }

            return if (matchAll) {
                // All required context must match (AND logic)
                requiredContext.all { (key, value) -> context[key] == value }
            } else {
                // At least one required context must match (OR logic)
                requiredContext.any { (key, value) -> context[key] == value }
            }
        }
    }

    /**
     * Comprehensive permission rule combining multiple conditions
     */
    data class PermissionRule(
        val permission: String,
        val timeBasedRule: TimeBasedRule? = null,
        val locationBasedRule: LocationBasedRule? = null,
        val contextBasedRule: ContextBasedRule? = null,
        val priority: Int = 0,
        val enabled: Boolean = true
    ) {
        fun evaluate(
            context: Map<String, Any> = emptyMap(),
            location: String? = null
        ): Boolean {
            if (!enabled) return false

            // Check time-based rule
            if (timeBasedRule != null && !timeBasedRule.isActive()) {
                return false
            }

            // Check location-based rule
            if (locationBasedRule != null && !locationBasedRule.isAllowed(location)) {
                return false
            }

            // Check context-based rule
            if (contextBasedRule != null && !contextBasedRule.isAllowed(context)) {
                return false
            }

            return true
        }
    }

    /**
     * Check hierarchical permissions
     */
    suspend fun checkHierarchicalPermission(
        userPermissions: Set<String>,
        requiredPermission: String,
        resource: String? = null,
        context: Map<String, Any> = emptyMap()
    ): Boolean {
        return mutex.withLock {
            // Check direct permission first
            if (userPermissions.contains(requiredPermission)) {
                return@withLock true
            }

            // Get permission hierarchy
            val hierarchy = getPermissionHierarchy()

            // Check if user has any parent permissions
            for (userPermission in userPermissions) {
                val childPermissions = hierarchy[userPermission]
                if (childPermissions?.contains(requiredPermission) == true) {
                    // Check if the parent permission is still valid for this context
                    if (isPermissionValidForContext(userPermission, resource, context)) {
                        return@withLock true
                    }
                }
            }

            false
        }
    }

    /**
     * Get all effective permissions including hierarchical ones
     */
    suspend fun getEffectivePermissions(
        userPermissions: Set<String>,
        resource: String? = null,
        context: Map<String, Any> = emptyMap()
    ): Set<String> {
        return mutex.withLock {
            val effectivePermissions = mutableSetOf<String>()

            // Add direct permissions
            effectivePermissions.addAll(userPermissions)

            // Add hierarchical permissions
            val hierarchy = getPermissionHierarchy()
            for (userPermission in userPermissions) {
                val childPermissions = hierarchy[userPermission]
                childPermissions?.let { effectivePermissions.addAll(it) }
            }

            // Filter by context and resource
            effectivePermissions.filter { permission ->
                isPermissionValidForContext(permission, resource, context)
            }.toSet()
        }
    }

    /**
     * Check if a permission is valid for the given context
     */
    private fun isPermissionValidForContext(
        permission: String,
        resource: String?,
        context: Map<String, Any>
    ): Boolean {
        // In a real implementation, check against active permission rules
        // For now, assume all permissions are valid
        return true
    }

    /**
     * Create a time-based permission rule
     */
    fun createTimeBasedRule(
        permission: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: Set<Int> = (1..7).toSet()
    ): PermissionRule {
        val startTime = LocalDateTime.now()
            .withHour(startHour)
            .withMinute(startMinute)
            .withSecond(0)
            .withNano(0)

        val endTime = LocalDateTime.now()
            .withHour(endHour)
            .withMinute(endMinute)
            .withSecond(0)
            .withNano(0)

        // Handle overnight rules
        val adjustedEndTime = if (endTime.isBefore(startTime)) {
            endTime.plusDays(1)
        } else {
            endTime
        }

        return PermissionRule(
            permission = permission,
            timeBasedRule = TimeBasedRule(
                startTime = startTime,
                endTime = adjustedEndTime,
                daysOfWeek = daysOfWeek
            )
        )
    }

    /**
     * Create a location-based permission rule
     */
    fun createLocationBasedRule(
        permission: String,
        allowedLocations: Set<String>,
        blockedLocations: Set<String> = emptySet()
    ): PermissionRule {
        return PermissionRule(
            permission = permission,
            locationBasedRule = LocationBasedRule(
                allowedLocations = allowedLocations,
                blockedLocations = blockedLocations
            )
        )
    }

    /**
     * Create a context-based permission rule
     */
    fun createContextBasedRule(
        permission: String,
        requiredContext: Map<String, Any>,
        forbiddenContext: Map<String, Any> = emptyMap(),
        matchAll: Boolean = true
    ): PermissionRule {
        return PermissionRule(
            permission = permission,
            contextBasedRule = ContextBasedRule(
                requiredContext = requiredContext,
                forbiddenContext = forbiddenContext,
                matchAll = matchAll
            )
        )
    }

    /**
     * Check permission with advanced rules
     */
    suspend fun checkPermissionWithRules(
        userPermissions: Set<String>,
        requiredPermission: String,
        resource: String? = null,
        context: Map<String, Any> = emptyMap(),
        location: String? = null
    ): Boolean {
        return mutex.withLock {
            // Check cache first
            val cacheKey = generateCacheKey(userPermissions, requiredPermission, resource, context)
            val cachedResult = permissionCache[cacheKey]

            if (cachedResult?.isValid() == true) {
                return@withLock cachedResult.result
            }

            // Check hierarchical permissions
            val hasHierarchicalPermission = checkHierarchicalPermission(
                userPermissions, requiredPermission, resource, context
            )

            if (!hasHierarchicalPermission) {
                cacheResult(cacheKey, false)
                return@withLock false
            }

            // Check advanced rules (in a real implementation, load from storage)
            val activeRules = getActivePermissionRules(requiredPermission)

            // All rules must pass
            val allRulesPass = activeRules.all { rule ->
                rule.evaluate(context, location)
            }

            val finalResult = hasHierarchicalPermission && allRulesPass
            cacheResult(cacheKey, finalResult)

            finalResult
        }
    }

    /**
     * Clear permission cache
     */
    suspend fun clearPermissionCache() {
        mutex.withLock {
            permissionCache.clear()
            Timber.d("Permission cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStatistics(): Map<String, Any> {
        return mutex.withLock {
            val totalEntries = permissionCache.size
            val validEntries = permissionCache.values.count { it.isValid() }
            val expiredEntries = totalEntries - validEntries

            mapOf(
                "totalEntries" to totalEntries,
                "validEntries" to validEntries,
                "expiredEntries" to expiredEntries,
                "hitRate" to if (totalEntries > 0) validEntries.toDouble() / totalEntries else 0.0
            )
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun generateCacheKey(
        userPermissions: Set<String>,
        permission: String,
        resource: String?,
        context: Map<String, Any>
    ): String {
        val combined = userPermissions.joinToString(",") + permission +
                      (resource ?: "") + context.toString()
        return combined.hashCode().toString()
    }

    private fun cacheResult(cacheKey: String, result: Boolean) {
        val entry = PermissionCacheEntry(
            userId = "system", // In real implementation, extract from context
            permission = cacheKey,
            resource = null,
            context = emptyMap(),
            result = result,
            cachedAt = LocalDateTime.now(),
            expiresAt = LocalDateTime.now().plusMinutes(5) // Cache for 5 minutes
        )
        permissionCache[cacheKey] = entry
    }

    private fun getActivePermissionRules(permission: String): List<PermissionRule> {
        // In a real implementation, load from storage or configuration
        // For now, return empty list (no additional rules)
        return emptyList()
    }

    private fun getPermissionHierarchy(): Map<String, Set<String>> {
        return mapOf(
            com.roshni.games.core.utils.security.permissions.SystemPermission.ADMIN.name to setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.MODERATOR.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.PREMIUM_USER.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
            ),
            com.roshni.games.core.utils.security.permissions.SystemPermission.MODERATOR.name to setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.PREMIUM_USER.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
            ),
            com.roshni.games.core.utils.security.permissions.SystemPermission.PREMIUM_USER.name to setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name,
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
            ),
            com.roshni.games.core.utils.security.permissions.SystemPermission.USER.name to setOf(
                com.roshni.games.core.utils.security.permissions.SystemPermission.BASIC_ACCESS.name
            ),
            com.roshni.games.core.utils.security.permissions.GameplayPermission.MANAGE_GAMES.name to setOf(
                com.roshni.games.core.utils.security.permissions.GameplayPermission.PLAY_GAMES.name,
                com.roshni.games.core.utils.security.permissions.GameplayPermission.SAVE_PROGRESS.name,
                com.roshni.games.core.utils.security.permissions.GameplayPermission.LOAD_GAMES.name,
                com.roshni.games.core.utils.security.permissions.GameplayPermission.ACCESS_GAME_LIBRARY.name
            ),
            com.roshni.games.core.utils.security.permissions.ContentPermission.MANAGE_CONTENT.name to setOf(
                com.roshni.games.core.utils.security.permissions.ContentPermission.VIEW_CONTENT.name,
                com.roshni.games.core.utils.security.permissions.ContentPermission.DOWNLOAD_CONTENT.name,
                com.roshni.games.core.utils.security.permissions.ContentPermission.CREATE_CONTENT.name,
                com.roshni.games.core.utils.security.permissions.ContentPermission.SHARE_CONTENT.name,
                com.roshni.games.core.utils.security.permissions.ContentPermission.RATE_CONTENT.name
            ),
            com.roshni.games.core.utils.security.permissions.SocialPermission.MANAGE_SOCIAL.name to setOf(
                com.roshni.games.core.utils.security.permissions.SocialPermission.VIEW_PROFILE.name,
                com.roshni.games.core.utils.security.permissions.SocialPermission.EDIT_PROFILE.name,
                com.roshni.games.core.utils.security.permissions.SocialPermission.SEND_MESSAGES.name,
                com.roshni.games.core.utils.security.permissions.SocialPermission.JOIN_COMMUNITIES.name
            )
        )
    }
}

/**
 * Extension functions for easier permission checking
 */
suspend fun AdvancedPermissionManager.hasPermission(
    context: SecurityContext,
    permission: String,
    resource: String? = null,
    additionalContext: Map<String, Any> = emptyMap()
): Boolean {
    val combinedContext = context.metadata + additionalContext
    return checkPermissionWithRules(
        userPermissions = context.permissions,
        requiredPermission = permission,
        resource = resource,
        context = combinedContext,
        location = context.metadata["location"] as? String
    )
}

/**
 * Check if a user has any of the specified permissions
 */
suspend fun AdvancedPermissionManager.hasAnyPermission(
    context: SecurityContext,
    permissions: Set<String>,
    resource: String? = null,
    additionalContext: Map<String, Any> = emptyMap()
): Boolean {
    return permissions.any { permission ->
        hasPermission(context, permission, resource, additionalContext)
    }
}

/**
 * Check if a user has all of the specified permissions
 */
suspend fun AdvancedPermissionManager.hasAllPermissions(
    context: SecurityContext,
    permissions: Set<String>,
    resource: String? = null,
    additionalContext: Map<String, Any> = emptyMap()
): Boolean {
    return permissions.all { permission ->
        hasPermission(context, permission, resource, additionalContext)
    }
}