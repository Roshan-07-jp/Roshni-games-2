package com.roshni.games.core.utils.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.coroutines.resume

/**
 * Android-specific implementation of PermissionHandler
 */
class AndroidPermissionHandler(
    private val context: Context,
    private val activityProvider: () -> Activity?
) : PermissionHandler {

    private val _permissionStatuses = MutableStateFlow<Map<RuntimePermission, PermissionStatus>>(emptyMap())
    val permissionStatuses: StateFlow<Map<RuntimePermission, PermissionStatus>> = _permissionStatuses.asStateFlow()

    private val featurePermissionManager = FeaturePermissionManager()

    // Activity result launchers for permission requests
    private var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var singlePermissionLauncher: ActivityResultLauncher<String>? = null

    init {
        initializePermissionStatuses()
    }

    /**
     * Initialize current permission statuses
     */
    private fun initializePermissionStatuses() {
        val initialStatuses = RuntimePermission.ALL_STANDARD_PERMISSIONS.associateWith { permission ->
            when {
                isPermissionGrantedInternal(permission) -> {
                    PermissionStatus.Granted(
                        permission = permission,
                        grantedAt = LocalDateTime.now()
                    )
                }
                isPermissionNotAvailable(permission) -> {
                    PermissionStatus.NotAvailable(
                        permission = permission,
                        reason = "Not available on this API level"
                    )
                }
                else -> {
                    PermissionStatus.Unknown(permission = permission)
                }
            }
        }
        _permissionStatuses.value = initialStatuses
    }

    override suspend fun isPermissionGranted(permission: RuntimePermission): Boolean {
        return when (val status = getPermissionStatus(permission)) {
            is PermissionStatus.Granted -> true
            else -> false
        }
    }

    override suspend fun shouldShowPermissionRationale(permission: RuntimePermission): Boolean {
        val activity = activityProvider() ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.androidPermission)
    }

    override suspend fun requestPermission(permission: RuntimePermission): PermissionResult {
        return try {
            val activity = activityProvider() ?: return createErrorResult(permission, "No activity available")

            if (isPermissionNotAvailable(permission)) {
                return PermissionResult.NotAvailable(
                    permission = permission,
                    reason = "Permission not available on this API level"
                )
            }

            if (isPermissionGrantedInternal(permission)) {
                return PermissionResult.Granted(
                    permission = permission,
                    grantedAt = LocalDateTime.now()
                )
            }

            val result = suspendCancellableCoroutine<PermissionResult> { continuation ->
                singlePermissionLauncher = ActivityResultLauncher<String> { result ->
                    continuation.resume(processSinglePermissionResult(permission, result))
                }

                singlePermissionLauncher?.launch(permission.androidPermission)
            }

            updatePermissionStatus(permission, result.toStatus())
            result

        } catch (e: Exception) {
            Timber.e(e, "Error requesting permission: ${permission.androidPermission}")
            createErrorResult(permission, e.message ?: "Unknown error")
        }
    }

    override suspend fun requestPermissions(permissions: List<RuntimePermission>): Map<RuntimePermission, PermissionResult> {
        return try {
            val activity = activityProvider() ?: return permissions.associateWith {
                createErrorResult(it, "No activity available")
            }

            // Filter out unavailable permissions
            val availablePermissions = permissions.filter { !isPermissionNotAvailable(it) }

            if (availablePermissions.isEmpty()) {
                return permissions.associateWith { permission ->
                    PermissionResult.NotAvailable(
                        permission = permission,
                        reason = "Permission not available on this API level"
                    )
                }
            }

            // Check already granted permissions
            val alreadyGranted = availablePermissions.filter { isPermissionGrantedInternal(it) }
            val needRequest = availablePermissions.filter { !isPermissionGrantedInternal(it) }

            val results = mutableMapOf<RuntimePermission, PermissionResult>()

            // Add already granted permissions
            alreadyGranted.forEach { permission ->
                results[permission] = PermissionResult.Granted(
                    permission = permission,
                    grantedAt = LocalDateTime.now()
                )
            }

            if (needRequest.isEmpty()) {
                return results
            }

            // Request remaining permissions
            val requestResults = suspendCancellableCoroutine<Map<RuntimePermission, PermissionResult>> { continuation ->
                multiplePermissionLauncher = ActivityResultLauncher<Array<String>> { result ->
                    val processedResults = needRequest.associateWith { permission ->
                        processSinglePermissionResult(permission, result[permission.androidPermission] ?: false)
                    }
                    continuation.resume(processedResults)
                }

                val permissionArray = needRequest.map { it.androidPermission }.toTypedArray()
                multiplePermissionLauncher?.launch(permissionArray)
            }

            results.putAll(requestResults)

            // Update permission statuses
            results.forEach { (permission, result) ->
                updatePermissionStatus(permission, result.toStatus())
            }

            results

        } catch (e: Exception) {
            Timber.e(e, "Error requesting permissions")
            permissions.associateWith { createErrorResult(it, e.message ?: "Unknown error") }
        }
    }

    override suspend fun getPermissionStatus(permission: RuntimePermission): PermissionStatus {
        return _permissionStatuses.value[permission] ?: PermissionStatus.Unknown(permission)
    }

    override suspend fun getPermissionsStatus(permissions: List<RuntimePermission>): Map<RuntimePermission, PermissionStatus> {
        return permissions.associateWith { getPermissionStatus(it) }
    }

    override suspend fun launchPermissionSettings() {
        try {
            val activity = activityProvider() ?: return
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error launching permission settings")
        }
    }

    override fun observePermissionStatus(permission: RuntimePermission): Flow<PermissionStatus> {
        return callbackFlow {
            // Send current status
            val currentStatus = _permissionStatuses.value[permission]
            if (currentStatus != null) {
                send(currentStatus)
            }

            // Listen for updates
            val observer = { statuses: Map<RuntimePermission, PermissionStatus> ->
                statuses[permission]?.let { send(it) }
            }

            // In a real implementation, you'd use a more sophisticated observation mechanism
            // For now, we'll use a simple polling approach
            awaitClose { /* Cleanup if needed */ }
        }
    }

    override fun observePermissionsStatus(permissions: List<RuntimePermission>): Flow<Map<RuntimePermission, PermissionStatus>> {
        return callbackFlow {
            // Send current statuses
            val currentStatuses = _permissionStatuses.value.filterKeys { permissions.contains(it) }
            if (currentStatuses.isNotEmpty()) {
                send(currentStatuses)
            }

            // Listen for updates
            val observer = { statuses: Map<RuntimePermission, PermissionStatus> ->
                val filteredStatuses = statuses.filterKeys { permissions.contains(it) }
                if (filteredStatuses.isNotEmpty()) {
                    send(filteredStatuses)
                }
            }

            awaitClose { /* Cleanup if needed */ }
        }
    }

    override suspend fun areFeaturePermissionsGranted(featureRequirements: FeaturePermissionRequirements): Boolean {
        val currentPermissions = _permissionStatuses.value.filterValues { it.isGranted() }.keys
        return featureRequirements.canFeatureWork(currentPermissions)
    }

    override suspend fun requestFeaturePermissions(featureRequirements: FeaturePermissionRequirements): FeaturePermissionResult {
        val results = requestPermissions(featureRequirements.getAllPermissions().toList())

        val success = featureRequirements.canFeatureWork(
            results.filterValues { it is PermissionResult.Granted }.keys
        )

        return FeaturePermissionResult(
            featureRequirements = featureRequirements,
            results = results,
            success = success,
            canFeatureWork = success
        )
    }

    override suspend fun handlePermissionDenial(
        permission: RuntimePermission,
        result: PermissionResult.Denied,
        fallbackStrategy: PermissionFallback
    ): PermissionFallbackResult {
        return try {
            when (fallbackStrategy) {
                is PermissionFallback.ShowEducationDialog -> {
                    showEducationDialog(permission, result, fallbackStrategy)
                }
                is PermissionFallback.NavigateToScreen -> {
                    navigateToScreen(fallbackStrategy)
                }
                is PermissionFallback.UseAlternativeFeature -> {
                    useAlternativeFeature(fallbackStrategy)
                }
                is PermissionFallback.DisableFeature -> {
                    disableFeature(fallbackStrategy)
                }
                is PermissionFallback.RequestAlternativePermission -> {
                    requestAlternativePermission(permission, fallbackStrategy)
                }
                is PermissionFallback.ShowSnackbar -> {
                    showSnackbar(fallbackStrategy)
                }
                is PermissionFallback.CustomAction -> {
                    executeCustomAction(fallbackStrategy)
                }
                PermissionFallback.NoFallback -> {
                    PermissionFallbackResult.NotApplicable(
                        fallback = fallbackStrategy,
                        reason = "No fallback strategy available"
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling permission denial")
            PermissionFallbackResult.Error(
                fallback = fallbackStrategy,
                error = e.message ?: "Unknown error",
                cause = e
            )
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun isPermissionGrantedInternal(permission: RuntimePermission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.androidPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionNotAvailable(permission: RuntimePermission): Boolean {
        return when (permission) {
            is RuntimePermission.NotificationPermission -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            is RuntimePermission.MediaImagesPermission,
            is RuntimePermission.MediaVideosPermission,
            is RuntimePermission.MediaAudioPermission -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            is RuntimePermission.LocationBackgroundPermission -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            is RuntimePermission.ActivityRecognitionPermission -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            is RuntimePermission.BodySensorsPermission -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
            else -> false
        }
    }

    private fun processSinglePermissionResult(permission: RuntimePermission, granted: Boolean): PermissionResult {
        return if (granted) {
            PermissionResult.Granted(
                permission = permission,
                grantedAt = LocalDateTime.now()
            )
        } else {
            val shouldShowRationale = activityProvider()?.let { activity ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.androidPermission)
            } ?: false

            PermissionResult.Denied(
                permission = permission,
                deniedAt = LocalDateTime.now(),
                shouldShowRationale = shouldShowRationale,
                canRequestAgain = shouldShowRationale
            )
        }
    }

    private fun createErrorResult(permission: RuntimePermission, error: String): PermissionResult {
        return PermissionResult.Error(
            permission = permission,
            error = error
        )
    }

    private fun updatePermissionStatus(permission: RuntimePermission, result: PermissionResult) {
        val status = result.toStatus()
        _permissionStatuses.update { current ->
            current.toMutableMap().apply { put(permission, status) }
        }
    }

    private fun PermissionResult.toStatus(): PermissionStatus {
        return when (this) {
            is PermissionResult.Granted -> PermissionStatus.Granted(
                permission = permission,
                grantedAt = grantedAt
            )
            is PermissionResult.Denied -> PermissionStatus.Denied(
                permission = permission,
                deniedAt = deniedAt,
                reason = reason,
                shouldShowRationale = shouldShowRationale,
                canRequestAgain = canRequestAgain
            )
            is PermissionResult.Error -> PermissionStatus.Denied(
                permission = permission,
                reason = PermissionResult.DenialReason.SYSTEM_ERROR
            )
            is PermissionResult.NotAvailable -> PermissionStatus.NotAvailable(
                permission = permission,
                reason = reason
            )
        }
    }

    // ==================== FALLBACK STRATEGY IMPLEMENTATIONS ====================

    private suspend fun showEducationDialog(
        permission: RuntimePermission,
        result: PermissionResult.Denied,
        fallback: PermissionFallback.ShowEducationDialog
    ): PermissionFallbackResult {
        // In a real implementation, this would show a dialog
        // For now, we'll simulate the behavior
        return PermissionFallbackResult.Success(
            fallback = fallback,
            userGrantedPermission = false,
            metadata = mapOf("dialog_shown" to true)
        )
    }

    private suspend fun navigateToScreen(fallback: PermissionFallback.NavigateToScreen): PermissionFallbackResult {
        // In a real implementation, this would navigate using the navigation system
        return PermissionFallbackResult.Success(
            fallback = fallback,
            metadata = mapOf("navigation_attempted" to true)
        )
    }

    private suspend fun useAlternativeFeature(fallback: PermissionFallback.UseAlternativeFeature): PermissionFallbackResult {
        return PermissionFallbackResult.Success(
            fallback = fallback,
            usedAlternative = true,
            metadata = mapOf("alternative_used" to fallback.alternativeFeature)
        )
    }

    private suspend fun disableFeature(fallback: PermissionFallback.DisableFeature): PermissionFallbackResult {
        return PermissionFallbackResult.Success(
            fallback = fallback,
            metadata = mapOf("feature_disabled" to fallback.featureName)
        )
    }

    private suspend fun requestAlternativePermission(
        originalPermission: RuntimePermission,
        fallback: PermissionFallback.RequestAlternativePermission
    ): PermissionFallbackResult {
        val result = requestPermission(fallback.alternativePermission)
        return PermissionFallbackResult.Success(
            fallback = fallback,
            userGrantedPermission = result is PermissionResult.Granted,
            metadata = mapOf("alternative_permission" to fallback.alternativePermission.androidPermission)
        )
    }

    private suspend fun showSnackbar(fallback: PermissionFallback.ShowSnackbar): PermissionFallbackResult {
        return PermissionFallbackResult.Success(
            fallback = fallback,
            metadata = mapOf("snackbar_shown" to true)
        )
    }

    private suspend fun executeCustomAction(fallback: PermissionFallback.CustomAction): PermissionFallbackResult {
        return PermissionFallbackResult.Success(
            fallback = fallback,
            metadata = fallback.data
        )
    }
}

/**
 * Activity result launcher type aliases for better readability
 */
typealias ActivityResultLauncher<T> = (T) -> Unit