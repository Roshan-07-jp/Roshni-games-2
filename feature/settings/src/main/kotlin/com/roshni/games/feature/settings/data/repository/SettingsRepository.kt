package com.roshni.games.feature.settings.data.repository

import com.roshni.games.core.utils.Result
import com.roshni.games.feature.settings.data.datasource.SettingsDataSource
import com.roshni.games.feature.settings.data.model.AccessibilitySettings
import com.roshni.games.feature.settings.data.model.AccountSettings
import com.roshni.games.feature.settings.data.model.AppSettings
import com.roshni.games.feature.settings.data.model.GameplaySettings
import com.roshni.games.feature.settings.data.model.LanguageSettings
import com.roshni.games.feature.settings.data.model.NotificationSettings
import com.roshni.games.feature.settings.data.model.ParentalControlsSettings
import com.roshni.games.feature.settings.data.model.PrivacySettings
import com.roshni.games.feature.settings.data.model.SettingsSection
import com.roshni.games.feature.settings.data.model.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SettingsRepository {
    fun getAppSettings(): Flow<Result<AppSettings>>
    suspend fun updateThemeSettings(themeSettings: ThemeSettings): Result<Boolean>
    suspend fun updateLanguageSettings(languageSettings: LanguageSettings): Result<Boolean>
    suspend fun updateAccessibilitySettings(accessibilitySettings: AccessibilitySettings): Result<Boolean>
    suspend fun updateParentalControlsSettings(parentalControlsSettings: ParentalControlsSettings): Result<Boolean>
    suspend fun updateAccountSettings(accountSettings: AccountSettings): Result<Boolean>
    suspend fun updateNotificationSettings(notificationSettings: NotificationSettings): Result<Boolean>
    suspend fun updateGameplaySettings(gameplaySettings: GameplaySettings): Result<Boolean>
    suspend fun updatePrivacySettings(privacySettings: PrivacySettings): Result<Boolean>
    suspend fun getSettingsSections(): Result<List<SettingsSection>>
    suspend fun exportUserData(): Result<String>
    suspend fun importUserData(filePath: String): Result<Boolean>
    suspend fun deleteAccount(): Result<Boolean>
    suspend fun resetSettings(): Result<Boolean>
}

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsDataSource
) : SettingsRepository {

    override fun getAppSettings(): Flow<Result<AppSettings>> {
        return settingsDataSource.getAppSettings()
            .map { settings -> Result.Success(settings) }
            .catch { error -> Result.Error(error) }
    }

    override suspend fun updateThemeSettings(themeSettings: ThemeSettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateThemeSettings(themeSettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateLanguageSettings(languageSettings: LanguageSettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateLanguageSettings(languageSettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAccessibilitySettings(accessibilitySettings: AccessibilitySettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateAccessibilitySettings(accessibilitySettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateParentalControlsSettings(parentalControlsSettings: ParentalControlsSettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateParentalControlsSettings(parentalControlsSettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAccountSettings(accountSettings: AccountSettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateAccountSettings(accountSettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateNotificationSettings(notificationSettings: NotificationSettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateNotificationSettings(notificationSettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGameplaySettings(gameplaySettings: GameplaySettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updateGameplaySettings(gameplaySettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePrivacySettings(privacySettings: PrivacySettings): Result<Boolean> {
        return try {
            val success = settingsDataSource.updatePrivacySettings(privacySettings)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getSettingsSections(): Result<List<SettingsSection>> {
        return try {
            val sections = settingsDataSource.getSettingsSections()
            Result.Success(sections)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun exportUserData(): Result<String> {
        return try {
            val filePath = settingsDataSource.exportUserData()
            Result.Success(filePath)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun importUserData(filePath: String): Result<Boolean> {
        return try {
            val success = settingsDataSource.importUserData(filePath)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val success = settingsDataSource.deleteAccount()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun resetSettings(): Result<Boolean> {
        return try {
            val success = settingsDataSource.resetSettings()
            Result.Success(success)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}