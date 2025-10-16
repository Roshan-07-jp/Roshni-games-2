package com.roshni.games.feature.settings.domain.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.settings.data.model.AccessibilitySettings
import com.roshni.games.feature.settings.data.model.AccountSettings
import com.roshni.games.feature.settings.data.model.AppSettings
import com.roshni.games.feature.settings.data.model.GameplaySettings
import com.roshni.games.feature.settings.data.model.LanguageSettings
import com.roshni.games.feature.settings.data.model.NotificationSettings
import com.roshni.games.feature.settings.data.model.ParentalControlsSettings
import com.roshni.games.feature.settings.data.model.PrivacySettings
import com.roshni.games.feature.settings.data.model.SettingsSection
import com.roshni.games.feature.settings.data.model.ThemeMode
import com.roshni.games.feature.settings.data.model.ThemeSettings
import com.roshni.games.feature.settings.data.repository.SettingsRepository
import com.roshni.games.feature.settings.domain.model.AccessibilitySettings as DomainAccessibilitySettings
import com.roshni.games.feature.settings.domain.model.AccountSettings as DomainAccountSettings
import com.roshni.games.feature.settings.domain.model.AgeRating
import com.roshni.games.feature.settings.domain.model.AppSettings as DomainAppSettings
import com.roshni.games.feature.settings.domain.model.GameplaySettings as DomainGameplaySettings
import com.roshni.games.feature.settings.domain.model.LanguageSettings as DomainLanguageSettings
import com.roshni.games.feature.settings.domain.model.NotificationSettings as DomainNotificationSettings
import com.roshni.games.feature.settings.domain.model.ParentalControlsSettings as DomainParentalControlsSettings
import com.roshni.games.feature.settings.domain.model.PlayTimeRestriction
import com.roshni.games.feature.settings.domain.model.PrivacySettings as DomainPrivacySettings
import com.roshni.games.feature.settings.domain.model.SettingsItem
import com.roshni.games.feature.settings.domain.model.SettingsItemType
import com.roshni.games.feature.settings.domain.model.SettingsSection as DomainSettingsSection
import com.roshni.games.feature.settings.domain.model.SettingsState
import com.roshni.games.feature.settings.domain.model.ThemeMode as DomainThemeMode
import com.roshni.games.feature.settings.domain.model.ThemeSettings as DomainThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsDomainRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : SettingsDomainRepository {

    override fun getSettingsState(): Flow<SettingsState> {
        return settingsRepository.getAppSettings().map { result ->
            when (result) {
                is Result.Success -> {
                    val appSettings = result.data
                    SettingsState(
                        appSettings = appSettings.toDomainAppSettings(),
                        settingsSections = emptyList(), // Will be loaded separately
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    SettingsState(
                        appSettings = DomainAppSettings(
                            theme = DomainThemeSettings(DomainThemeMode.SYSTEM, true, false),
                            language = DomainLanguageSettings("en", emptyList(), emptyMap()),
                            accessibility = DomainAccessibilitySettings(false, false, false, false, true, true),
                            parentalControls = DomainParentalControlsSettings(false, null, AgeRating.EVERYONE, null, PlayTimeRestriction.NONE, true, false),
                            account = DomainAccountSettings(null, null, null, null, false, true, true, null),
                            notifications = DomainNotificationSettings(true, true, true, true, false, true, true),
                            gameplay = DomainGameplaySettings(true, 5, true, true, false, true, true, 0.7f, 0.8f),
                            privacy = DomainPrivacySettings(true, true, false, false, true)
                        ),
                        settingsSections = emptyList(),
                        isLoading = false,
                        error = result.exception.message
                    )
                }
            }
        }
    }

    override suspend fun updateThemeSettings(themeSettings: DomainThemeSettings): Boolean {
        return when (val result = settingsRepository.updateThemeSettings(themeSettings.toDataThemeSettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateLanguageSettings(languageSettings: DomainLanguageSettings): Boolean {
        return when (val result = settingsRepository.updateLanguageSettings(languageSettings.toDataLanguageSettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateAccessibilitySettings(accessibilitySettings: DomainAccessibilitySettings): Boolean {
        return when (val result = settingsRepository.updateAccessibilitySettings(accessibilitySettings.toDataAccessibilitySettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateParentalControlsSettings(parentalControlsSettings: DomainParentalControlsSettings): Boolean {
        return when (val result = settingsRepository.updateParentalControlsSettings(parentalControlsSettings.toDataParentalControlsSettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateAccountSettings(accountSettings: DomainAccountSettings): Boolean {
        return when (val result = settingsRepository.updateAccountSettings(accountSettings.toDataAccountSettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateNotificationSettings(notificationSettings: DomainNotificationSettings): Boolean {
        return when (val result = settingsRepository.updateNotificationSettings(notificationSettings.toDataNotificationSettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updateGameplaySettings(gameplaySettings: DomainGameplaySettings): Boolean {
        return when (val result = settingsRepository.updateGameplaySettings(gameplaySettings.toDataGameplaySettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun updatePrivacySettings(privacySettings: DomainPrivacySettings): Boolean {
        return when (val result = settingsRepository.updatePrivacySettings(privacySettings.toDataPrivacySettings())) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun exportUserData(): String? {
        return when (val result = settingsRepository.exportUserData()) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }

    override suspend fun importUserData(filePath: String): Boolean {
        return when (val result = settingsRepository.importUserData(filePath)) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun deleteAccount(): Boolean {
        return when (val result = settingsRepository.deleteAccount()) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    override suspend fun resetSettings(): Boolean {
        return when (val result = settingsRepository.resetSettings()) {
            is Result.Success -> result.data
            is Result.Error -> false
        }
    }

    // Conversion functions
    private fun AppSettings.toDomainAppSettings(): DomainAppSettings {
        return DomainAppSettings(
            theme = theme.toDomainThemeSettings(),
            language = language.toDomainLanguageSettings(),
            accessibility = accessibility.toDomainAccessibilitySettings(),
            parentalControls = parentalControls.toDomainParentalControlsSettings(),
            account = account.toDomainAccountSettings(),
            notifications = notifications.toDomainNotificationSettings(),
            gameplay = gameplay.toDomainGameplaySettings(),
            privacy = privacy.toDomainPrivacySettings()
        )
    }

    private fun ThemeSettings.toDomainThemeSettings(): DomainThemeSettings {
        return DomainThemeSettings(
            themeMode = when (themeMode) {
                ThemeMode.LIGHT -> DomainThemeMode.LIGHT
                ThemeMode.DARK -> DomainThemeMode.DARK
                ThemeMode.SYSTEM -> DomainThemeMode.SYSTEM
            },
            useDynamicColors = useDynamicColors,
            highContrastMode = highContrastMode
        )
    }

    private fun LanguageSettings.toDomainLanguageSettings(): DomainLanguageSettings {
        return DomainLanguageSettings(
            currentLanguage = currentLanguage,
            availableLanguages = availableLanguages,
            languageNames = languageNames
        )
    }

    private fun AccessibilitySettings.toDomainAccessibilitySettings(): DomainAccessibilitySettings {
        return DomainAccessibilitySettings(
            talkBackEnabled = talkBackEnabled,
            highContrastText = highContrastText,
            largeText = largeText,
            reduceMotion = reduceMotion,
            screenReaderOptimizations = screenReaderOptimizations,
            hapticFeedback = hapticFeedback
        )
    }

    private fun ParentalControlsSettings.toDomainParentalControlsSettings(): DomainParentalControlsSettings {
        return DomainParentalControlsSettings(
            isEnabled = isEnabled,
            pinCode = pinCode,
            allowedAgeRating = when (allowedAgeRating) {
                com.roshni.games.feature.settings.data.model.AgeRating.EVERYONE -> AgeRating.EVERYONE
                com.roshni.games.feature.settings.data.model.AgeRating.EVERYONE_10_PLUS -> AgeRating.EVERYONE_10_PLUS
                com.roshni.games.feature.settings.data.model.AgeRating.TEEN -> AgeRating.TEEN
                com.roshni.games.feature.settings.data.model.AgeRating.MATURE -> AgeRating.MATURE
                com.roshni.games.feature.settings.data.model.AgeRating.ADULTS_ONLY -> AgeRating.ADULTS_ONLY
            },
            dailyPlayTimeLimit = dailyPlayTimeLimit,
            allowedPlayTime = when (allowedPlayTime) {
                com.roshni.games.feature.settings.data.model.PlayTimeRestriction.NONE -> PlayTimeRestriction.NONE
                com.roshni.games.feature.settings.data.model.PlayTimeRestriction.WEEKDAYS_ONLY -> PlayTimeRestriction.WEEKDAYS_ONLY
                com.roshni.games.feature.settings.data.model.PlayTimeRestriction.WEEKENDS_ONLY -> PlayTimeRestriction.WEEKENDS_ONLY
                com.roshni.games.feature.settings.data.model.PlayTimeRestriction.CUSTOM_HOURS -> PlayTimeRestriction.CUSTOM_HOURS
            },
            blockInAppPurchases = blockInAppPurchases,
            requireApprovalForNewGames = requireApprovalForNewGames
        )
    }

    private fun AccountSettings.toDomainAccountSettings(): DomainAccountSettings {
        return DomainAccountSettings(
            userId = userId,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            isLoggedIn = isLoggedIn,
            syncEnabled = syncEnabled,
            autoBackup = autoBackup,
            lastBackupDate = lastBackupDate
        )
    }

    private fun NotificationSettings.toDomainNotificationSettings(): DomainNotificationSettings {
        return DomainNotificationSettings(
            pushNotifications = pushNotifications,
            achievementNotifications = achievementNotifications,
            gameUpdateNotifications = gameUpdateNotifications,
            friendActivityNotifications = friendActivityNotifications,
            marketingNotifications = marketingNotifications,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )
    }

    private fun GameplaySettings.toDomainGameplaySettings(): DomainGameplaySettings {
        return DomainGameplaySettings(
            autoSave = autoSave,
            autoSaveInterval = autoSaveInterval,
            showHints = showHints,
            difficultyAdjustment = difficultyAdjustment,
            adaptiveDifficulty = adaptiveDifficulty,
            soundEffects = soundEffects,
            backgroundMusic = backgroundMusic,
            musicVolume = musicVolume,
            sfxVolume = sfxVolume
        )
    }

    private fun PrivacySettings.toDomainPrivacySettings(): DomainPrivacySettings {
        return DomainPrivacySettings(
            analyticsEnabled = analyticsEnabled,
            crashReportingEnabled = crashReportingEnabled,
            personalizedAds = personalizedAds,
            shareUsageData = shareUsageData,
            allowDataCollection = allowDataCollection
        )
    }

    // Reverse conversions (Domain to Data)
    private fun DomainThemeSettings.toDataThemeSettings(): ThemeSettings {
        return ThemeSettings(
            themeMode = when (themeMode) {
                DomainThemeMode.LIGHT -> ThemeMode.LIGHT
                DomainThemeMode.DARK -> ThemeMode.DARK
                DomainThemeMode.SYSTEM -> ThemeMode.SYSTEM
            },
            useDynamicColors = useDynamicColors,
            highContrastMode = highContrastMode
        )
    }

    private fun DomainLanguageSettings.toDataLanguageSettings(): LanguageSettings {
        return LanguageSettings(
            currentLanguage = currentLanguage,
            availableLanguages = availableLanguages,
            languageNames = languageNames
        )
    }

    private fun DomainAccessibilitySettings.toDataAccessibilitySettings(): AccessibilitySettings {
        return AccessibilitySettings(
            talkBackEnabled = talkBackEnabled,
            highContrastText = highContrastText,
            largeText = largeText,
            reduceMotion = reduceMotion,
            screenReaderOptimizations = screenReaderOptimizations,
            hapticFeedback = hapticFeedback
        )
    }

    private fun DomainParentalControlsSettings.toDataParentalControlsSettings(): ParentalControlsSettings {
        return ParentalControlsSettings(
            isEnabled = isEnabled,
            pinCode = pinCode,
            allowedAgeRating = when (allowedAgeRating) {
                AgeRating.EVERYONE -> com.roshni.games.feature.settings.data.model.AgeRating.EVERYONE
                AgeRating.EVERYONE_10_PLUS -> com.roshni.games.feature.settings.data.model.AgeRating.EVERYONE_10_PLUS
                AgeRating.TEEN -> com.roshni.games.feature.settings.data.model.AgeRating.TEEN
                AgeRating.MATURE -> com.roshni.games.feature.settings.data.model.AgeRating.MATURE
                AgeRating.ADULTS_ONLY -> com.roshni.games.feature.settings.data.model.AgeRating.ADULTS_ONLY
            },
            dailyPlayTimeLimit = dailyPlayTimeLimit,
            allowedPlayTime = when (allowedPlayTime) {
                PlayTimeRestriction.NONE -> com.roshni.games.feature.settings.data.model.PlayTimeRestriction.NONE
                PlayTimeRestriction.WEEKDAYS_ONLY -> com.roshni.games.feature.settings.data.model.PlayTimeRestriction.WEEKDAYS_ONLY
                PlayTimeRestriction.WEEKENDS_ONLY -> com.roshni.games.feature.settings.data.model.PlayTimeRestriction.WEEKENDS_ONLY
                PlayTimeRestriction.CUSTOM_HOURS -> com.roshni.games.feature.settings.data.model.PlayTimeRestriction.CUSTOM_HOURS
            },
            blockInAppPurchases = blockInAppPurchases,
            requireApprovalForNewGames = requireApprovalForNewGames
        )
    }

    private fun DomainAccountSettings.toDataAccountSettings(): AccountSettings {
        return AccountSettings(
            userId = userId,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            isLoggedIn = isLoggedIn,
            syncEnabled = syncEnabled,
            autoBackup = autoBackup,
            lastBackupDate = lastBackupDate
        )
    }

    private fun DomainNotificationSettings.toDataNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            pushNotifications = pushNotifications,
            achievementNotifications = achievementNotifications,
            gameUpdateNotifications = gameUpdateNotifications,
            friendActivityNotifications = friendActivityNotifications,
            marketingNotifications = marketingNotifications,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )
    }

    private fun DomainGameplaySettings.toDataGameplaySettings(): GameplaySettings {
        return GameplaySettings(
            autoSave = autoSave,
            autoSaveInterval = autoSaveInterval,
            showHints = showHints,
            difficultyAdjustment = difficultyAdjustment,
            adaptiveDifficulty = adaptiveDifficulty,
            soundEffects = soundEffects,
            backgroundMusic = backgroundMusic,
            musicVolume = musicVolume,
            sfxVolume = sfxVolume
        )
    }

    private fun DomainPrivacySettings.toDataPrivacySettings(): PrivacySettings {
        return PrivacySettings(
            analyticsEnabled = analyticsEnabled,
            crashReportingEnabled = crashReportingEnabled,
            personalizedAds = personalizedAds,
            shareUsageData = shareUsageData,
            allowDataCollection = allowDataCollection
        )
    }
}