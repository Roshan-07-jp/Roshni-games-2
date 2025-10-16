package com.roshni.games.core.utils.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class PermissionHandlerTest {

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var permissionHandler: AndroidPermissionHandler
    private lateinit var mockActivityProvider: () -> Activity?

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        activity = Robolectric.setupActivity(Activity::class.java)

        mockActivityProvider = { activity }

        permissionHandler = AndroidPermissionHandler(context, mockActivityProvider)

        // Mock permission checks
        mockkStatic(ContextCompat::class)
        mockkStatic(ActivityCompat::class)
    }

    @Test
    fun `test camera permission granted returns true`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionHandler.isPermissionGranted(RuntimePermission.CameraPermission)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test camera permission denied returns false`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_DENIED

        // When
        val result = permissionHandler.isPermissionGranted(RuntimePermission.CameraPermission)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test should show rationale when permission denied and rationale needed`() = runTest {
        // Given
        every { ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) } returns true

        // When
        val result = permissionHandler.shouldShowPermissionRationale(RuntimePermission.CameraPermission)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test should not show rationale when permission granted`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED
        every { ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) } returns false

        // When
        val result = permissionHandler.shouldShowPermissionRationale(RuntimePermission.CameraPermission)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test get permission status returns correct status for granted permission`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED

        // When
        val status = permissionHandler.getPermissionStatus(RuntimePermission.CameraPermission)

        // Then
        assertTrue(status.isGranted())
        assertEquals(RuntimePermission.CameraPermission, status.getPermission())
    }

    @Test
    fun `test get permission status returns correct status for denied permission`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_DENIED
        every { ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) } returns true

        // When
        val status = permissionHandler.getPermissionStatus(RuntimePermission.CameraPermission)

        // Then
        assertTrue(status.isDenied())
        assertEquals(RuntimePermission.CameraPermission, status.getPermission())
    }

    @Test
    fun `test observe permission status emits status updates`() = runTest {
        // Given
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED

        // When
        val flow = permissionHandler.observePermissionStatus(RuntimePermission.CameraPermission)
        val status = flow.first()

        // Then
        assertTrue(status.isGranted())
    }

    @Test
    fun `test feature permission requirements can work with granted permissions`() = runTest {
        // Given
        val requirements = FeaturePermissionRequirements(
            featureId = "camera_feature",
            featureName = "Camera Feature",
            requiredPermissions = setOf(RuntimePermission.CameraPermission),
            optionalPermissions = setOf(RuntimePermission.StorageWritePermission)
        )

        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_DENIED

        // When
        val canWork = permissionHandler.areFeaturePermissionsGranted(requirements)

        // Then
        assertTrue(canWork) // Should work because required permission is granted
    }

    @Test
    fun `test feature permission requirements cannot work with missing required permissions`() = runTest {
        // Given
        val requirements = FeaturePermissionRequirements(
            featureId = "camera_feature",
            featureName = "Camera Feature",
            requiredPermissions = setOf(RuntimePermission.CameraPermission),
            optionalPermissions = setOf(RuntimePermission.StorageWritePermission)
        )

        every { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_GRANTED

        // When
        val canWork = permissionHandler.areFeaturePermissionsGranted(requirements)

        // Then
        assertFalse(canWork) // Should not work because required permission is missing
    }

    @Test
    fun `test handle permission denial with education dialog fallback`() = runTest {
        // Given
        val permission = RuntimePermission.CameraPermission
        val deniedResult = PermissionResult.Denied(
            permission = permission,
            deniedAt = LocalDateTime.now(),
            shouldShowRationale = true
        )
        val fallback = PermissionFallback.ShowEducationDialog(
            title = "Camera Access",
            message = "We need camera access for this feature"
        )

        // When
        val result = permissionHandler.handlePermissionDenial(permission, deniedResult, fallback)

        // Then
        assertTrue(result is PermissionFallbackResult.Success)
    }

    @Test
    fun `test handle permission denial with alternative feature fallback`() = runTest {
        // Given
        val permission = RuntimePermission.CameraPermission
        val deniedResult = PermissionResult.Denied(
            permission = permission,
            deniedAt = LocalDateTime.now()
        )
        val fallback = PermissionFallback.UseAlternativeFeature(
            alternativeFeature = "text_input",
            description = "Use text input instead",
            userMessage = "You can use text input instead of camera"
        )

        // When
        val result = permissionHandler.handlePermissionDenial(permission, deniedResult, fallback)

        // Then
        assertTrue(result is PermissionFallbackResult.Success)
        assertTrue((result as PermissionFallbackResult.Success).usedAlternative)
    }

    @Test
    fun `test runtime permission sealed classes have correct properties`() {
        // Test CameraPermission
        assertEquals(Manifest.permission.CAMERA, RuntimePermission.CameraPermission.androidPermission)
        assertEquals("Camera", RuntimePermission.CameraPermission.displayName)
        assertFalse(RuntimePermission.CameraPermission.isCritical)
        assertEquals(RuntimePermission.PermissionCategory.MEDIA, RuntimePermission.CameraPermission.category)

        // Test MicrophonePermission
        assertEquals(Manifest.permission.RECORD_AUDIO, RuntimePermission.MicrophonePermission.androidPermission)
        assertEquals("Microphone", RuntimePermission.MicrophonePermission.displayName)
        assertFalse(RuntimePermission.MicrophonePermission.isCritical)
        assertEquals(RuntimePermission.PermissionCategory.MEDIA, RuntimePermission.MicrophonePermission.category)

        // Test LocationFinePermission
        assertEquals(Manifest.permission.ACCESS_FINE_LOCATION, RuntimePermission.LocationFinePermission.androidPermission)
        assertEquals("Precise Location", RuntimePermission.LocationFinePermission.displayName)
        assertFalse(RuntimePermission.LocationFinePermission.isCritical)
        assertEquals(RuntimePermission.PermissionCategory.LOCATION, RuntimePermission.LocationFinePermission.category)
    }

    @Test
    fun `test runtime permission companion object functions`() {
        // Test fromAndroidPermission
        val cameraPermission = RuntimePermission.fromAndroidPermission(Manifest.permission.CAMERA)
        assertNotNull(cameraPermission)
        assertEquals(RuntimePermission.CameraPermission, cameraPermission)

        // Test isRuntimePermission
        assertTrue(RuntimePermission.isRuntimePermission(Manifest.permission.CAMERA))
        assertFalse(RuntimePermission.isRuntimePermission("nonexistent.permission"))

        // Test getPermissionsByCategory
        val mediaPermissions = RuntimePermission.getPermissionsByCategory(RuntimePermission.PermissionCategory.MEDIA)
        assertTrue(mediaPermissions.contains(RuntimePermission.CameraPermission))
        assertTrue(mediaPermissions.contains(RuntimePermission.MicrophonePermission))

        // Test getCriticalPermissions
        val criticalPermissions = RuntimePermission.getCriticalPermissions()
        assertTrue(criticalPermissions.isEmpty()) // No critical permissions by default
    }

    @Test
    fun `test custom permission creation`() {
        // Given
        val customPermission = RuntimePermission.CustomPermission(
            permissionId = "custom.test",
            androidPermission = "com.example.CUSTOM_PERMISSION",
            displayName = "Custom Test Permission",
            isCritical = true,
            category = RuntimePermission.PermissionCategory.OTHER
        )

        // Then
        assertEquals("custom.test", customPermission.permissionId)
        assertEquals("com.example.CUSTOM_PERMISSION", customPermission.androidPermission)
        assertEquals("Custom Test Permission", customPermission.displayName)
        assertTrue(customPermission.isCritical)
        assertEquals(RuntimePermission.PermissionCategory.OTHER, customPermission.category)
    }

    @Test
    fun `test feature permission requirements builder DSL`() {
        // Given
        val requirements = featurePermissionRequirements("test_feature") {
            setFeatureName("Test Feature")
            addRequiredPermission(RuntimePermission.CameraPermission)
            addOptionalPermission(RuntimePermission.StorageWritePermission)
            setPriority(FeaturePriority.HIGH)
            setCritical(true)
        }

        // Then
        assertEquals("test_feature", requirements.featureId)
        assertEquals("Test Feature", requirements.featureName)
        assertTrue(requirements.requiredPermissions.contains(RuntimePermission.CameraPermission))
        assertTrue(requirements.optionalPermissions.contains(RuntimePermission.StorageWritePermission))
        assertEquals(FeaturePriority.HIGH, requirements.priority)
        assertTrue(requirements.isCritical)
    }

    @Test
    fun `test permission result sealed classes have correct properties`() {
        // Test Granted result
        val grantedAt = LocalDateTime.now()
        val grantedResult = PermissionResult.Granted(
            permission = RuntimePermission.CameraPermission,
            grantedAt = grantedAt,
            isUserInitiated = true
        )

        assertEquals(RuntimePermission.CameraPermission, grantedResult.permission)
        assertEquals(grantedAt, grantedResult.grantedAt)
        assertTrue(grantedResult.isUserInitiated)

        // Test Denied result
        val deniedAt = LocalDateTime.now()
        val deniedResult = PermissionResult.Denied(
            permission = RuntimePermission.CameraPermission,
            deniedAt = deniedAt,
            reason = PermissionResult.DenialReason.USER_DENIED,
            shouldShowRationale = true,
            canRequestAgain = true
        )

        assertEquals(RuntimePermission.CameraPermission, deniedResult.permission)
        assertEquals(deniedAt, deniedResult.deniedAt)
        assertEquals(PermissionResult.DenialReason.USER_DENIED, deniedResult.reason)
        assertTrue(deniedResult.shouldShowRationale)
        assertTrue(deniedResult.canRequestAgain)
        assertTrue(deniedResult.shouldEducateUser())

        // Test Error result
        val errorResult = PermissionResult.Error(
            permission = RuntimePermission.CameraPermission,
            error = "Test error",
            canRetry = true
        )

        assertEquals(RuntimePermission.CameraPermission, errorResult.permission)
        assertEquals("Test error", errorResult.error)
        assertTrue(errorResult.canRetry)
        assertTrue(errorResult.isRetryable())
    }

    @Test
    fun `test permission status sealed classes have correct properties`() {
        // Test Granted status
        val grantedStatus = PermissionStatus.Granted(
            permission = RuntimePermission.CameraPermission,
            grantedAt = LocalDateTime.now()
        )

        assertTrue(grantedStatus.isGranted())
        assertFalse(grantedStatus.isDenied())
        assertEquals(RuntimePermission.CameraPermission, grantedStatus.getPermission())

        // Test Denied status
        val deniedStatus = PermissionStatus.Denied(
            permission = RuntimePermission.CameraPermission,
            reason = PermissionResult.DenialReason.USER_DENIED,
            shouldShowRationale = true
        )

        assertFalse(deniedStatus.isGranted())
        assertTrue(deniedStatus.isDenied())
        assertEquals(RuntimePermission.CameraPermission, deniedStatus.getPermission())

        // Test Unknown status
        val unknownStatus = PermissionStatus.Unknown(
            permission = RuntimePermission.CameraPermission,
            reason = "Not determined"
        )

        assertFalse(unknownStatus.isGranted())
        assertFalse(unknownStatus.isDenied())
        assertTrue(unknownStatus.isUnknown())
        assertEquals(RuntimePermission.CameraPermission, unknownStatus.getPermission())

        // Test NotAvailable status
        val notAvailableStatus = PermissionStatus.NotAvailable(
            permission = RuntimePermission.CameraPermission,
            reason = "Not available on this device"
        )

        assertFalse(notAvailableStatus.isGranted())
        assertFalse(notAvailableStatus.isDenied())
        assertTrue(notAvailableStatus.isNotAvailable())
        assertEquals(RuntimePermission.CameraPermission, notAvailableStatus.getPermission())
    }

    @Test
    fun `test permission fallback sealed classes have correct properties`() {
        // Test ShowEducationDialog fallback
        val educationDialog = PermissionFallback.ShowEducationDialog(
            title = "Permission Needed",
            message = "We need this permission",
            positiveButtonText = "Grant",
            negativeButtonText = "Deny"
        )

        assertEquals("Permission Needed", educationDialog.title)
        assertEquals("We need this permission", educationDialog.message)
        assertEquals("Grant", educationDialog.positiveButtonText)
        assertEquals("Deny", educationDialog.negativeButtonText)

        // Test UseAlternativeFeature fallback
        val alternativeFeature = PermissionFallback.UseAlternativeFeature(
            alternativeFeature = "text_input",
            description = "Use text instead",
            userMessage = "You can use text input instead"
        )

        assertEquals("text_input", alternativeFeature.alternativeFeature)
        assertEquals("Use text instead", alternativeFeature.description)
        assertEquals("You can use text input instead", alternativeFeature.userMessage)

        // Test DisableFeature fallback
        val disableFeature = PermissionFallback.DisableFeature(
            featureName = "camera_feature",
            userMessage = "Camera feature disabled",
            showEnableOption = true
        )

        assertEquals("camera_feature", disableFeature.featureName)
        assertEquals("Camera feature disabled", disableFeature.userMessage)
        assertTrue(disableFeature.showEnableOption)
    }

    @Test
    fun `test permission fallback result sealed classes have correct properties`() {
        // Test Success result
        val successResult = PermissionFallbackResult.Success(
            fallback = PermissionFallback.NoFallback,
            userGrantedPermission = true,
            usedAlternative = false
        )

        assertTrue(successResult.userGrantedPermission)
        assertFalse(successResult.usedAlternative)

        // Test UserDeclined result
        val declinedResult = PermissionFallbackResult.UserDeclined(
            fallback = PermissionFallback.NoFallback,
            canRetry = true
        )

        assertTrue(declinedResult.canRetry)

        // Test Error result
        val errorResult = PermissionFallbackResult.Error(
            fallback = PermissionFallback.NoFallback,
            error = "Test error"
        )

        assertEquals("Test error", errorResult.error)

        // Test NotApplicable result
        val notApplicableResult = PermissionFallbackResult.NotApplicable(
            fallback = PermissionFallback.NoFallback,
            reason = "Not applicable"
        )

        assertEquals("Not applicable", notApplicableResult.reason)
    }
}