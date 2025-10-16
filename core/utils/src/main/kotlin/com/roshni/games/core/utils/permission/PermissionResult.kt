package com.roshni.games.core.utils.permission

import java.time.LocalDateTime

/**
 * Result of a permission request operation
 */
sealed class PermissionResult {
    /**
     * Permission was granted successfully
     */
    data class Granted(
        val permission: RuntimePermission,
        val grantedAt: LocalDateTime = LocalDateTime.now(),
        val isUserInitiated: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionResult() {
        fun isStillValid(): Boolean {
            // Permissions are typically valid until revoked by user or system
            return true
        }
    }

    /**
     * Permission was denied by the user
     */
    data class Denied(
        val permission: RuntimePermission,
        val deniedAt: LocalDateTime = LocalDateTime.now(),
        val reason: DenialReason = DenialReason.USER_DENIED,
        val shouldShowRationale: Boolean = false,
        val canRequestAgain: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionResult() {

        fun shouldEducateUser(): Boolean {
            return shouldShowRationale && reason == DenialReason.USER_DENIED
        }

        fun requiresSettingsApp(): Boolean {
            return !canRequestAgain && reason == DenialReason.DENIED_PERMANENTLY
        }
    }

    /**
     * Permission request failed due to system error
     */
    data class Error(
        val permission: RuntimePermission,
        val error: String,
        val cause: Throwable? = null,
        val canRetry: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionResult() {

        fun isRetryable(): Boolean {
            return canRetry && cause !is SecurityException
        }
    }

    /**
     * Permission is not available on this device/API level
     */
    data class NotAvailable(
        val permission: RuntimePermission,
        val reason: String,
        val alternativePermissions: Set<RuntimePermission> = emptySet(),
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionResult() {

        fun hasAlternatives(): Boolean {
            return alternativePermissions.isNotEmpty()
        }
    }

    /**
     * Reasons why a permission might be denied
     */
    enum class DenialReason(val displayName: String) {
        USER_DENIED("User denied permission"),
        DENIED_PERMANENTLY("Permission permanently denied"),
        RESTRICTED_BY_ADMIN("Restricted by device administrator"),
        RESTRICTED_BY_POLICY("Restricted by security policy"),
        NOT_AVAILABLE_ON_DEVICE("Not available on this device"),
        API_LEVEL_TOO_LOW("Requires higher API level"),
        SYSTEM_ERROR("System error occurred")
    }
}

/**
 * Current status of a permission
 */
sealed class PermissionStatus {
    /**
     * Permission is currently granted
     */
    data class Granted(
        val permission: RuntimePermission,
        val grantedAt: LocalDateTime? = null,
        val isUserInitiated: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionStatus()

    /**
     * Permission is currently denied
     */
    data class Denied(
        val permission: RuntimePermission,
        val deniedAt: LocalDateTime? = null,
        val reason: PermissionResult.DenialReason = PermissionResult.DenialReason.USER_DENIED,
        val shouldShowRationale: Boolean = false,
        val canRequestAgain: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionStatus()

    /**
     * Permission status is unknown or not determined yet
     */
    data class Unknown(
        val permission: RuntimePermission,
        val reason: String = "Status not yet determined",
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionStatus()

    /**
     * Permission is not available on this device
     */
    data class NotAvailable(
        val permission: RuntimePermission,
        val reason: String,
        val alternativePermissions: Set<RuntimePermission> = emptySet(),
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionStatus()

    /**
     * Check if the permission is currently granted
     */
    fun isGranted(): Boolean = this is Granted

    /**
     * Check if the permission is currently denied
     */
    fun isDenied(): Boolean = this is Denied

    /**
     * Check if the permission status is unknown
     */
    fun isUnknown(): Boolean = this is Unknown

    /**
     * Check if the permission is not available
     */
    fun isNotAvailable(): Boolean = this is NotAvailable

    /**
     * Get the permission from any status
     */
    fun getPermission(): RuntimePermission {
        return when (this) {
            is Granted -> permission
            is Denied -> permission
            is Unknown -> permission
            is NotAvailable -> permission
        }
    }
}

/**
 * Fallback strategy when permission is denied
 */
sealed class PermissionFallback {
    /**
     * Show a custom dialog to educate the user about the permission
     */
    data class ShowEducationDialog(
        val title: String,
        val message: String,
        val positiveButtonText: String = "Grant Permission",
        val negativeButtonText: String = "Not Now",
        val showSettingsButton: Boolean = true,
        val customActions: List<FallbackAction> = emptyList()
    ) : PermissionFallback()

    /**
     * Navigate to a specific screen to handle the missing permission
     */
    data class NavigateToScreen(
        val screenRoute: String,
        val arguments: Map<String, Any> = emptyMap(),
        val showBackButton: Boolean = true
    ) : PermissionFallback()

    /**
     * Use an alternative feature that doesn't require the permission
     */
    data class UseAlternativeFeature(
        val alternativeFeature: String,
        val description: String,
        val userMessage: String
    ) : PermissionFallback()

    /**
     * Disable the feature that requires the permission
     */
    data class DisableFeature(
        val featureName: String,
        val userMessage: String,
        val showEnableOption: Boolean = true
    ) : PermissionFallback()

    /**
     * Request a different but related permission
     */
    data class RequestAlternativePermission(
        val alternativePermission: RuntimePermission,
        val reason: String
    ) : PermissionFallback()

    /**
     * Show a snackbar with an action to grant permission
     */
    data class ShowSnackbar(
        val message: String,
        val actionText: String = "Grant",
        val duration: SnackbarDuration = SnackbarDuration.LONG
    ) : PermissionFallback()

    /**
     * Custom fallback action
     */
    data class CustomAction(
        val actionId: String,
        val data: Map<String, Any> = emptyMap()
    ) : PermissionFallback()

    /**
     * No fallback - feature cannot work without permission
     */
    object NoFallback : PermissionFallback()

    /**
     * Snackbar duration options
     */
    enum class SnackbarDuration {
        SHORT, LONG, INDEFINITE
    }
}

/**
 * Custom action for fallback strategies
 */
data class FallbackAction(
    val id: String,
    val title: String,
    val action: () -> Unit
)

/**
 * Result of executing a permission fallback strategy
 */
sealed class PermissionFallbackResult {
    /**
     * Fallback was successful - user granted permission or alternative worked
     */
    data class Success(
        val fallback: PermissionFallback,
        val userGrantedPermission: Boolean = false,
        val usedAlternative: Boolean = false,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionFallbackResult()

    /**
     * Fallback was shown but user declined
     */
    data class UserDeclined(
        val fallback: PermissionFallback,
        val canRetry: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionFallbackResult()

    /**
     * Fallback failed due to system error
     */
    data class Error(
        val fallback: PermissionFallback,
        val error: String,
        val cause: Throwable? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionFallbackResult()

    /**
     * Fallback is not applicable for this situation
     */
    data class NotApplicable(
        val fallback: PermissionFallback,
        val reason: String,
        val metadata: Map<String, Any> = emptyMap()
    ) : PermissionFallbackResult()
}