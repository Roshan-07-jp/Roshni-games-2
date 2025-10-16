package com.roshni.games.core.utils.permission

import kotlinx.coroutines.flow.Flow

/**
 * Core interface for handling runtime permissions in the application.
 * Provides a unified API for requesting, checking, and managing permissions across different platforms.
 */
interface PermissionHandler {

    /**
     * Check if a specific permission is currently granted
     */
    suspend fun isPermissionGranted(permission: RuntimePermission): Boolean

    /**
     * Check if a specific permission should show rationale before requesting
     */
    suspend fun shouldShowPermissionRationale(permission: RuntimePermission): Boolean

    /**
     * Request a single permission from the user
     */
    suspend fun requestPermission(permission: RuntimePermission): PermissionResult

    /**
     * Request multiple permissions from the user
     */
    suspend fun requestPermissions(permissions: List<RuntimePermission>): Map<RuntimePermission, PermissionResult>

    /**
     * Get the current status of a permission
     */
    suspend fun getPermissionStatus(permission: RuntimePermission): PermissionStatus

    /**
     * Get the current status of multiple permissions
     */
    suspend fun getPermissionsStatus(permissions: List<RuntimePermission>): Map<RuntimePermission, PermissionStatus>

    /**
     * Launch the settings screen for the user to manually manage permissions
     */
    suspend fun launchPermissionSettings()

    /**
     * Register a callback for permission status changes
     */
    fun observePermissionStatus(permission: RuntimePermission): Flow<PermissionStatus>

    /**
     * Register a callback for multiple permission status changes
     */
    fun observePermissionsStatus(permissions: List<RuntimePermission>): Flow<Map<RuntimePermission, PermissionStatus>>

    /**
     * Check if all required permissions for a feature are granted
     */
    suspend fun areFeaturePermissionsGranted(featureRequirements: FeaturePermissionRequirements): Boolean

    /**
     * Request all permissions required for a feature
     */
    suspend fun requestFeaturePermissions(featureRequirements: FeaturePermissionRequirements): FeaturePermissionResult

    /**
     * Handle permission denial with appropriate fallback strategy
     */
    suspend fun handlePermissionDenial(
        permission: RuntimePermission,
        result: PermissionResult.Denied,
        fallbackStrategy: PermissionFallback
    ): PermissionFallbackResult
}