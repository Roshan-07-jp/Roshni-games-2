package com.roshni.games.feature.settings.domain.repository

import com.roshni.games.feature.settings.domain.model.AccessibilitySettings
import com.roshni.games.feature.settings.domain.model.AccountSettings
import com.roshni.games.feature.settings.domain.model.AppSettings
import com.roshni.games.feature.settings.domain.model.GameplaySettings
import com.roshni.games.feature.settings.domain.model.LanguageSettings
import com.roshni.games.feature.settings.domain.model.NotificationSettings
import com.roshni.games.feature.settings.domain.model.ParentalControlsSettings
import com.roshni.games.feature.settings.domain.model.PrivacySettings
import com.roshni.games.feature.settings.domain.model.SettingsSection
import com.roshni.games.feature.settings.domain.model.SettingsState
import com.roshni.games.feature.settings.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface SettingsDomainRepository {
    fun getSettingsState(): Flow<SettingsState>
    suspend fun updateThemeSettings(themeSettings: ThemeSettings): Boolean
    suspend fun updateLanguageSettings(languageSettings: LanguageSettings): Boolean
    suspend fun updateAccessibilitySettings(accessibilitySettings: AccessibilitySettings): Boolean
    suspend fun updateParentalControlsSettings(parentalControlsSettings: ParentalControlsSettings): Boolean
    suspend fun updateAccountSettings(accountSettings: AccountSettings): Boolean
    suspend fun updateNotificationSettings(notificationSettings: NotificationSettings): Boolean
    suspend fun updateGameplaySettings(gameplaySettings: GameplaySettings): Boolean
    suspend fun updatePrivacySettings(privacySettings: PrivacySettings): Boolean
    suspend fun exportUserData(): String?
    suspend fun importUserData(filePath: String): Boolean
    suspend fun deleteAccount(): Boolean
    suspend fun resetSettings(): Boolean
}