package com.roshni.games.feature.settings.domain.model

data class SettingsState(
    val appSettings: AppSettings,
    val settingsSections: List<SettingsSection>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showResetConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val showExportSuccess: Boolean = false,
    val showImportSuccess: Boolean = false,
    val exportFilePath: String? = null
)

data class AppSettings(
    val theme: ThemeSettings,
    val language: LanguageSettings,
    val accessibility: AccessibilitySettings,
    val parentalControls: ParentalControlsSettings,
    val account: AccountSettings,
    val notifications: NotificationSettings,
    val gameplay: GameplaySettings,
    val privacy: PrivacySettings
)

data class ThemeSettings(
    val themeMode: ThemeMode,
    val useDynamicColors: Boolean,
    val highContrastMode: Boolean
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class LanguageSettings(
    val currentLanguage: String,
    val availableLanguages: List<String>,
    val languageNames: Map<String, String>
)

data class AccessibilitySettings(
    val talkBackEnabled: Boolean,
    val highContrastText: Boolean,
    val largeText: Boolean,
    val reduceMotion: Boolean,
    val screenReaderOptimizations: Boolean,
    val hapticFeedback: Boolean
)

data class ParentalControlsSettings(
    val isEnabled: Boolean,
    val pinCode: String?,
    val allowedAgeRating: AgeRating,
    val dailyPlayTimeLimit: Int?,
    val allowedPlayTime: PlayTimeRestriction,
    val blockInAppPurchases: Boolean,
    val requireApprovalForNewGames: Boolean
)

enum class AgeRating {
    EVERYONE, EVERYONE_10_PLUS, TEEN, MATURE, ADULTS_ONLY
}

enum class PlayTimeRestriction {
    NONE, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_HOURS
}

data class AccountSettings(
    val userId: String?,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val isLoggedIn: Boolean,
    val syncEnabled: Boolean,
    val autoBackup: Boolean,
    val lastBackupDate: String?
)

data class NotificationSettings(
    val pushNotifications: Boolean,
    val achievementNotifications: Boolean,
    val gameUpdateNotifications: Boolean,
    val friendActivityNotifications: Boolean,
    val marketingNotifications: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean
)

data class GameplaySettings(
    val autoSave: Boolean,
    val autoSaveInterval: Int,
    val showHints: Boolean,
    val difficultyAdjustment: Boolean,
    val adaptiveDifficulty: Boolean,
    val soundEffects: Boolean,
    val backgroundMusic: Boolean,
    val musicVolume: Float,
    val sfxVolume: Float
)

data class PrivacySettings(
    val analyticsEnabled: Boolean,
    val crashReportingEnabled: Boolean,
    val personalizedAds: Boolean,
    val shareUsageData: Boolean,
    val allowDataCollection: Boolean
)

data class SettingsSection(
    val id: String,
    val title: String,
    val description: String?,
    val icon: String?,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val id: String,
    val title: String,
    val description: String?,
    val type: SettingsItemType,
    val value: Any?,
    val options: List<String>,
    val isEnabled: Boolean
)

enum class SettingsItemType {
    TOGGLE, DROPDOWN, SLIDER, INPUT, BUTTON, NAVIGATION
}

sealed class SettingsNavigationEvent {
    object NavigateToProfile : SettingsNavigationEvent()
    object NavigateToParentalControlsSetup : SettingsNavigationEvent()
    object NavigateToLanguageSelection : SettingsNavigationEvent()
    data class ShowDialog(val dialogType: SettingsDialogType) : SettingsNavigationEvent()
}

enum class SettingsDialogType {
    RESET_SETTINGS,
    DELETE_ACCOUNT,
    EXPORT_DATA,
    IMPORT_DATA,
    PIN_SETUP,
    AGE_RATING_SELECTION
}