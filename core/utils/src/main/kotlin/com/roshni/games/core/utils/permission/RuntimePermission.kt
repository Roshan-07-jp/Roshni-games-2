package com.roshni.games.core.utils.permission

import android.Manifest
import androidx.annotation.StringRes

/**
 * Sealed class hierarchy for Android runtime permissions used in the Roshni Games platform.
 * Each permission includes Android manifest permission mapping and user-friendly descriptions.
 */
sealed class RuntimePermission(
    val androidPermission: String,
    val displayName: String,
    @StringRes val descriptionResId: Int? = null,
    val isCritical: Boolean = false,
    val category: PermissionCategory = PermissionCategory.OTHER
) {

    // ==================== CAMERA PERMISSIONS ====================

    /**
     * Camera permission for taking photos and recording videos
     */
    object CameraPermission : RuntimePermission(
        androidPermission = Manifest.permission.CAMERA,
        displayName = "Camera",
        descriptionResId = R.string.permission_camera_description,
        isCritical = false,
        category = PermissionCategory.MEDIA
    )

    // ==================== MICROPHONE PERMISSIONS ====================

    /**
     * Microphone permission for audio recording and voice features
     */
    object MicrophonePermission : RuntimePermission(
        androidPermission = Manifest.permission.RECORD_AUDIO,
        displayName = "Microphone",
        descriptionResId = R.string.permission_microphone_description,
        isCritical = false,
        category = PermissionCategory.MEDIA
    )

    // ==================== LOCATION PERMISSIONS ====================

    /**
     * Coarse location permission for approximate location access
     */
    object LocationCoarsePermission : RuntimePermission(
        androidPermission = Manifest.permission.ACCESS_COARSE_LOCATION,
        displayName = "Approximate Location",
        descriptionResId = R.string.permission_location_coarse_description,
        isCritical = false,
        category = PermissionCategory.LOCATION
    )

    /**
     * Fine location permission for precise location access
     */
    object LocationFinePermission : RuntimePermission(
        androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
        displayName = "Precise Location",
        descriptionResId = R.string.permission_location_fine_description,
        isCritical = false,
        category = PermissionCategory.LOCATION
    )

    /**
     * Background location permission for continuous location tracking
     */
    object LocationBackgroundPermission : RuntimePermission(
        androidPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        displayName = "Background Location",
        descriptionResId = R.string.permission_location_background_description,
        isCritical = false,
        category = PermissionCategory.LOCATION
    )

    // ==================== STORAGE PERMISSIONS ====================

    /**
     * Read external storage permission for accessing media files
     */
    object StorageReadPermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_EXTERNAL_STORAGE,
        displayName = "Read Storage",
        descriptionResId = R.string.permission_storage_read_description,
        isCritical = false,
        category = PermissionCategory.STORAGE
    )

    /**
     * Write external storage permission for saving files
     */
    object StorageWritePermission : RuntimePermission(
        androidPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        displayName = "Write Storage",
        descriptionResId = R.string.permission_storage_write_description,
        isCritical = false,
        category = PermissionCategory.STORAGE
    )

    /**
     * Read media images permission (Android 13+)
     */
    object MediaImagesPermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_MEDIA_IMAGES,
        displayName = "Read Images",
        descriptionResId = R.string.permission_media_images_description,
        isCritical = false,
        category = PermissionCategory.STORAGE
    )

    /**
     * Read media videos permission (Android 13+)
     */
    object MediaVideosPermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_MEDIA_VIDEO,
        displayName = "Read Videos",
        descriptionResId = R.string.permission_media_videos_description,
        isCritical = false,
        category = PermissionCategory.STORAGE
    )

    /**
     * Read media audio permission (Android 13+)
     */
    object MediaAudioPermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_MEDIA_AUDIO,
        displayName = "Read Audio",
        descriptionResId = R.string.permission_media_audio_description,
        isCritical = false,
        category = PermissionCategory.STORAGE
    )

    // ==================== NOTIFICATION PERMISSIONS ====================

    /**
     * Notification permission for showing notifications (Android 13+)
     */
    object NotificationPermission : RuntimePermission(
        androidPermission = Manifest.permission.POST_NOTIFICATIONS,
        displayName = "Notifications",
        descriptionResId = R.string.permission_notification_description,
        isCritical = false,
        category = PermissionCategory.NOTIFICATION
    )

    // ==================== BACKGROUND PROCESSING PERMISSIONS ====================

    /**
     * Background processing permission for running tasks in background
     */
    object BackgroundProcessingPermission : RuntimePermission(
        androidPermission = Manifest.permission.WAKE_LOCK,
        displayName = "Background Processing",
        descriptionResId = R.string.permission_background_processing_description,
        isCritical = false,
        category = PermissionCategory.SYSTEM
    )

    // ==================== PHONE PERMISSIONS ====================

    /**
     * Phone state permission for accessing phone information
     */
    object PhoneStatePermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_PHONE_STATE,
        displayName = "Phone State",
        descriptionResId = R.string.permission_phone_state_description,
        isCritical = false,
        category = PermissionCategory.PHONE
    )

    /**
     * Call phone permission for making phone calls
     */
    object CallPhonePermission : RuntimePermission(
        androidPermission = Manifest.permission.CALL_PHONE,
        displayName = "Make Phone Calls",
        descriptionResId = R.string.permission_call_phone_description,
        isCritical = false,
        category = PermissionCategory.PHONE
    )

    // ==================== CONTACTS PERMISSIONS ====================

    /**
     * Read contacts permission for accessing contact information
     */
    object ContactsReadPermission : RuntimePermission(
        androidPermission = Manifest.permission.READ_CONTACTS,
        displayName = "Read Contacts",
        descriptionResId = R.string.permission_contacts_read_description,
        isCritical = false,
        category = PermissionCategory.CONTACTS
    )

    /**
     * Write contacts permission for modifying contact information
     */
    object ContactsWritePermission : RuntimePermission(
        androidPermission = Manifest.permission.WRITE_CONTACTS,
        displayName = "Write Contacts",
        descriptionResId = R.string.permission_contacts_write_description,
        isCritical = false,
        category = PermissionCategory.CONTACTS
    )

    // ==================== SENSORS PERMISSIONS ====================

    /**
     * Body sensors permission for accessing health and fitness sensors
     */
    object BodySensorsPermission : RuntimePermission(
        androidPermission = Manifest.permission.BODY_SENSORS,
        displayName = "Body Sensors",
        descriptionResId = R.string.permission_body_sensors_description,
        isCritical = false,
        category = PermissionCategory.SENSORS
    )

    /**
     * Activity recognition permission for recognizing physical activities
     */
    object ActivityRecognitionPermission : RuntimePermission(
        androidPermission = Manifest.permission.ACTIVITY_RECOGNITION,
        displayName = "Activity Recognition",
        descriptionResId = R.string.permission_activity_recognition_description,
        isCritical = false,
        category = PermissionCategory.SENSORS
    )

    // ==================== CUSTOM PERMISSIONS ====================

    /**
     * Custom permission for application-specific permissions not covered by Android
     */
    data class CustomPermission(
        val permissionId: String,
        val androidPermission: String,
        val displayName: String,
        @StringRes val descriptionResId: Int? = null,
        val isCritical: Boolean = false,
        val category: PermissionCategory = PermissionCategory.OTHER
    ) : RuntimePermission(
        androidPermission = androidPermission,
        displayName = displayName,
        descriptionResId = descriptionResId,
        isCritical = isCritical,
        category = category
    )

    // ==================== COMPANION OBJECTS ====================

    companion object {
        /**
         * Get all standard runtime permissions
         */
        val ALL_STANDARD_PERMISSIONS: Set<RuntimePermission> = setOf(
            CameraPermission,
            MicrophonePermission,
            LocationCoarsePermission,
            LocationFinePermission,
            LocationBackgroundPermission,
            StorageReadPermission,
            StorageWritePermission,
            MediaImagesPermission,
            MediaVideosPermission,
            MediaAudioPermission,
            NotificationPermission,
            BackgroundProcessingPermission,
            PhoneStatePermission,
            CallPhonePermission,
            ContactsReadPermission,
            ContactsWritePermission,
            BodySensorsPermission,
            ActivityRecognitionPermission
        )

        /**
         * Get permissions by category
         */
        fun getPermissionsByCategory(category: PermissionCategory): Set<RuntimePermission> {
            return ALL_STANDARD_PERMISSIONS.filter { it.category == category }.toSet()
        }

        /**
         * Get critical permissions
         */
        fun getCriticalPermissions(): Set<RuntimePermission> {
            return ALL_STANDARD_PERMISSIONS.filter { it.isCritical }.toSet()
        }

        /**
         * Find a permission by Android permission string
         */
        fun fromAndroidPermission(androidPermission: String): RuntimePermission? {
            return ALL_STANDARD_PERMISSIONS.find { it.androidPermission == androidPermission }
        }

        /**
         * Check if an Android permission string is a runtime permission
         */
        fun isRuntimePermission(androidPermission: String): Boolean {
            return fromAndroidPermission(androidPermission) != null
        }
    }
}

/**
 * Categories for organizing permissions
 */
enum class PermissionCategory(val displayName: String) {
    MEDIA("Media"),
    LOCATION("Location"),
    STORAGE("Storage"),
    NOTIFICATION("Notification"),
    SYSTEM("System"),
    PHONE("Phone"),
    CONTACTS("Contacts"),
    SENSORS("Sensors"),
    OTHER("Other")
}

// ==================== STRING RESOURCES PLACEHOLDER ====================

/**
 * Placeholder string resources for permission descriptions
 * In a real implementation, these would be defined in strings.xml
 */
object R {
    object string {
        const val permission_camera_description = 0
        const val permission_microphone_description = 0
        const val permission_location_coarse_description = 0
        const val permission_location_fine_description = 0
        const val permission_location_background_description = 0
        const val permission_storage_read_description = 0
        const val permission_storage_write_description = 0
        const val permission_media_images_description = 0
        const val permission_media_videos_description = 0
        const val permission_media_audio_description = 0
        const val permission_notification_description = 0
        const val permission_background_processing_description = 0
        const val permission_phone_state_description = 0
        const val permission_call_phone_description = 0
        const val permission_contacts_read_description = 0
        const val permission_contacts_write_description = 0
        const val permission_body_sensors_description = 0
        const val permission_activity_recognition_description = 0
    }
}