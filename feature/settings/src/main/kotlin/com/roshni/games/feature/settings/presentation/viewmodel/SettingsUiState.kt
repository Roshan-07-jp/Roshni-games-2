package com.roshni.games.feature.settings.presentation.viewmodel

import com.roshni.games.feature.settings.domain.model.SettingsState

data class SettingsUiState(
    val settingsState: SettingsState = SettingsState(
        appSettings = com.roshni.games.feature.settings.domain.model.AppSettings(
            theme = com.roshni.games.feature.settings.domain.model.ThemeSettings(
                themeMode = com.roshni.games.feature.settings.domain.model.ThemeMode.SYSTEM,
                useDynamicColors = true,
                highContrastMode = false
            ),
            language = com.roshni.games.feature.settings.domain.model.LanguageSettings(
                currentLanguage = "en",
                availableLanguages = emptyList(),
                languageNames = emptyMap()
            ),
            accessibility = com.roshni.games.feature.settings.domain.model.AccessibilitySettings(
                talkBackEnabled = false,
                highContrastText = false,
                largeText = false,
                reduceMotion = false,
                screenReaderOptimizations = true,
                hapticFeedback = true
            ),
            parentalControls = com.roshni.games.feature.settings.domain.model.ParentalControlsSettings(
                isEnabled = false,
                pinCode = null,
                allowedAgeRating = com.roshni.games.feature.settings.domain.model.AgeRating.EVERYONE,
                dailyPlayTimeLimit = null,
                allowedPlayTime = com.roshni.games.feature.settings.domain.model.PlayTimeRestriction.NONE,
                blockInAppPurchases = true,
                requireApprovalForNewGames = false
            ),
            account = com.roshni.games.feature.settings.domain.model.AccountSettings(
                userId = null,
                email = null,
                displayName = null,
                avatarUrl = null,
                isLoggedIn = false,
                syncEnabled = true,
                autoBackup = true,
                lastBackupDate = null
            ),
            notifications = com.roshni.games.feature.settings.domain.model.NotificationSettings(
                pushNotifications = true,
                achievementNotifications = true,
                gameUpdateNotifications = true,
                friendActivityNotifications = true,
                marketingNotifications = false,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            gameplay = com.roshni.games.feature.settings.domain.model.GameplaySettings(
                autoSave = true,
                autoSaveInterval = 5,
                showHints = true,
                difficultyAdjustment = true,
                adaptiveDifficulty = false,
                soundEffects = true,
                backgroundMusic = true,
                musicVolume = 0.7f,
                sfxVolume = 0.8f
            ),
            privacy = com.roshni.games.feature.settings.domain.model.PrivacySettings(
                analyticsEnabled = true,
                crashReportingEnabled = true,
                personalizedAds = false,
                shareUsageData = false,
                allowDataCollection = true
            )
        ),
        settingsSections = emptyList()
    ),
    val expandedSections: Set<String> = emptySet(),
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showExportSuccessDialog: Boolean = false,
    val showImportSuccessDialog: Boolean = false
)

sealed class SettingsAction {
    data class ToggleSection(val sectionId: String) : SettingsAction()
    data class UpdateThemeMode(val themeMode: com.roshni.games.feature.settings.domain.model.ThemeMode) : SettingsAction()
    data class UpdateLanguage(val languageCode: String) : SettingsAction()
    data class UpdateToggleSetting(val sectionId: String, val itemId: String, val value: Boolean) : SettingsAction()
    data class UpdateDropdownSetting(val sectionId: String, val itemId: String, val value: String) : SettingsAction()
    object ExportData : SettingsAction()
    object ImportData : SettingsAction()
    object ResetSettings : SettingsAction()
    object DeleteAccount : SettingsAction()
    object DismissDialogs : SettingsAction()
    data class NavigateToSection(val sectionId: String) : SettingsAction()
}