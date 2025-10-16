package com.roshni.games.feature.settings.data.model

data class AppSettings(
    val theme: ThemeSettings = ThemeSettings(),
    val language: LanguageSettings = LanguageSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings(),
    val parentalControls: ParentalControlsSettings = ParentalControlsSettings(),
    val account: AccountSettings = AccountSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val gameplay: GameplaySettings = GameplaySettings(),
    val privacy: PrivacySettings = PrivacySettings()
)

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,
    val highContrastMode: Boolean = false
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class LanguageSettings(
    val currentLanguage: String = "en",
    val availableLanguages: List<String> = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh"),
    val languageNames: Map<String, String> = mapOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "ru" to "Русский",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文"
    )
)

data class AccessibilitySettings(
    val talkBackEnabled: Boolean = false,
    val highContrastText: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val screenReaderOptimizations: Boolean = true,
    val hapticFeedback: Boolean = true
)

data class ParentalControlsSettings(
    val isEnabled: Boolean = false,
    val pinCode: String? = null,
    val allowedAgeRating: AgeRating = AgeRating.EVERYONE,
    val dailyPlayTimeLimit: Int? = null, // in minutes
    val allowedPlayTime: PlayTimeRestriction = PlayTimeRestriction.NONE,
    val blockInAppPurchases: Boolean = true,
    val requireApprovalForNewGames: Boolean = false
)

enum class AgeRating {
    EVERYONE, EVERYONE_10_PLUS, TEEN, MATURE, ADULTS_ONLY
}

enum class PlayTimeRestriction {
    NONE, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_HOURS
}

data class AccountSettings(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val syncEnabled: Boolean = true,
    val autoBackup: Boolean = true,
    val lastBackupDate: String? = null
)

data class NotificationSettings(
    val pushNotifications: Boolean = true,
    val achievementNotifications: Boolean = true,
    val gameUpdateNotifications: Boolean = true,
    val friendActivityNotifications: Boolean = true,
    val marketingNotifications: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

data class GameplaySettings(
    val autoSave: Boolean = true,
    val autoSaveInterval: Int = 5, // in minutes
    val showHints: Boolean = true,
    val difficultyAdjustment: Boolean = true,
    val adaptiveDifficulty: Boolean = false,
    val soundEffects: Boolean = true,
    val backgroundMusic: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.8f
)

data class PrivacySettings(
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val personalizedAds: Boolean = false,
    val shareUsageData: Boolean = false,
    val allowDataCollection: Boolean = true
)

data class SettingsSection(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val items: List<SettingsItem> = emptyList()
)

data class SettingsItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: SettingsItemType,
    val value: Any? = null,
    val options: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

enum class SettingsItemType {
    TOGGLE, DROPDOWN, SLIDER, INPUT, BUTTON, NAVIGATION
}