package com.roshni.games.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.feature.settings.domain.model.SettingsDialogType
import com.roshni.games.feature.settings.domain.model.SettingsNavigationEvent
import com.roshni.games.feature.settings.domain.model.SettingsState
import com.roshni.games.feature.settings.domain.model.ThemeMode
import com.roshni.games.feature.settings.domain.repository.SettingsDomainRepository
import com.roshni.games.feature.settings.domain.usecase.GetSettingsStateUseCase
import com.roshni.games.feature.settings.domain.usecase.UpdateThemeSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDomainRepository: SettingsDomainRepository,
    private val getSettingsStateUseCase: GetSettingsStateUseCase,
    private val updateThemeSettingsUseCase: UpdateThemeSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SettingsNavigationEvent>()
    val navigationEvent: SharedFlow<SettingsNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadSettings()
        loadSettingsSections()
    }

    private fun loadSettings() {
        getSettingsStateUseCase().onEach { settingsState ->
            _uiState.update { currentState ->
                currentState.copy(settingsState = settingsState)
            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettingsSections() {
        viewModelScope.launch {
            try {
                // In real implementation, this would load from repository
                // For now, we'll use a simple implementation
                val sections = createDefaultSettingsSections()
                _uiState.update { currentState ->
                    currentState.copy(
                        settingsState = currentState.settingsState.copy(
                            settingsSections = sections
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ToggleSection -> toggleSection(action.sectionId)
            is SettingsAction.UpdateThemeMode -> updateThemeMode(action.themeMode)
            is SettingsAction.UpdateLanguage -> updateLanguage(action.languageCode)
            is SettingsAction.UpdateToggleSetting -> updateToggleSetting(action.sectionId, action.itemId, action.value)
            is SettingsAction.UpdateDropdownSetting -> updateDropdownSetting(action.sectionId, action.itemId, action.value)
            SettingsAction.ExportData -> exportData()
            SettingsAction.ImportData -> importData()
            SettingsAction.ResetSettings -> resetSettings()
            SettingsAction.DeleteAccount -> deleteAccount()
            SettingsAction.DismissDialogs -> dismissDialogs()
            is SettingsAction.NavigateToSection -> navigateToSection(action.sectionId)
        }
    }

    private fun toggleSection(sectionId: String) {
        _uiState.update { currentState ->
            val expandedSections = currentState.expandedSections.toMutableSet()
            if (expandedSections.contains(sectionId)) {
                expandedSections.remove(sectionId)
            } else {
                expandedSections.add(sectionId)
            }
            currentState.copy(expandedSections = expandedSections)
        }
    }

    private fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settingsState.appSettings
                val updatedThemeSettings = currentSettings.theme.copy(themeMode = themeMode)

                val success = updateThemeSettingsUseCase(updatedThemeSettings)
                if (success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                appSettings = currentState.settingsState.appSettings.copy(
                                    theme = updatedThemeSettings
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settingsState.appSettings
                val updatedLanguageSettings = currentSettings.language.copy(currentLanguage = languageCode)

                val success = settingsDomainRepository.updateLanguageSettings(updatedLanguageSettings)
                if (success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                appSettings = currentState.settingsState.appSettings.copy(
                                    language = updatedLanguageSettings
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateToggleSetting(sectionId: String, itemId: String, value: Boolean) {
        // Implementation for updating toggle settings
        viewModelScope.launch {
            try {
                // Update the specific setting based on section and item ID
                // This would interact with the appropriate use case
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateDropdownSetting(sectionId: String, itemId: String, value: String) {
        // Implementation for updating dropdown settings
        viewModelScope.launch {
            try {
                // Update the specific setting based on section and item ID
                // This would interact with the appropriate use case
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(settingsState = it.settingsState.copy(isLoading = true)) }

                val filePath = settingsDomainRepository.exportUserData()
                if (filePath != null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(isLoading = false),
                            showExportSuccessDialog = true,
                            exportFilePath = filePath
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                isLoading = false,
                                error = "Failed to export data"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        settingsState = currentState.settingsState.copy(
                            isLoading = false,
                            error = "Failed to export data: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun importData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(settingsState = it.settingsState.copy(isLoading = true)) }

                // In real implementation, this would show a file picker
                // For now, we'll simulate with a placeholder path
                val success = settingsDomainRepository.importUserData("/placeholder/path")
                if (success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(isLoading = false),
                            showImportSuccessDialog = true
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                isLoading = false,
                                error = "Failed to import data"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        settingsState = currentState.settingsState.copy(
                            isLoading = false,
                            error = "Failed to import data: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun resetSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(settingsState = it.settingsState.copy(isLoading = true)) }

                val success = settingsDomainRepository.resetSettings()
                if (success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(isLoading = false),
                            showResetDialog = false
                        )
                    }
                    // Reload settings to show defaults
                    loadSettings()
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                isLoading = false,
                                error = "Failed to reset settings"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        settingsState = currentState.settingsState.copy(
                            isLoading = false,
                            error = "Failed to reset settings: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(settingsState = it.settingsState.copy(isLoading = true)) }

                val success = settingsDomainRepository.deleteAccount()
                if (success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(isLoading = false),
                            showDeleteAccountDialog = false
                        )
                    }
                    // In real implementation, this would navigate to login screen
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            settingsState = currentState.settingsState.copy(
                                isLoading = false,
                                error = "Failed to delete account"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        settingsState = currentState.settingsState.copy(
                            isLoading = false,
                            error = "Failed to delete account: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun dismissDialogs() {
        _uiState.update { currentState ->
            currentState.copy(
                showThemeDialog = false,
                showLanguageDialog = false,
                showResetDialog = false,
                showDeleteAccountDialog = false,
                showExportSuccessDialog = false,
                showImportSuccessDialog = false
            )
        }
    }

    private fun navigateToSection(sectionId: String) {
        viewModelScope.launch {
            when (sectionId) {
                "profile" -> _navigationEvent.emit(SettingsNavigationEvent.NavigateToProfile)
                "parental_controls" -> _navigationEvent.emit(SettingsNavigationEvent.NavigateToParentalControlsSetup)
                "language" -> _navigationEvent.emit(SettingsNavigationEvent.NavigateToLanguageSelection)
                else -> {
                    // Handle other section navigations
                }
            }
        }
    }

    private fun createDefaultSettingsSections(): List<com.roshni.games.feature.settings.domain.model.SettingsSection> {
        return listOf(
            com.roshni.games.feature.settings.domain.model.SettingsSection(
                id = "appearance",
                title = "Appearance",
                description = "Customize how the app looks and feels",
                icon = "🎨",
                items = listOf(
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "theme_mode",
                        title = "Theme",
                        description = "Choose your preferred theme",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.DROPDOWN,
                        value = _uiState.value.settingsState.appSettings.theme.themeMode.name,
                        options = listOf("Light", "Dark", "System")
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "dynamic_colors",
                        title = "Dynamic Colors",
                        description = "Use colors from your wallpaper",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.theme.useDynamicColors
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "high_contrast",
                        title = "High Contrast",
                        description = "Increase contrast for better visibility",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.theme.highContrastMode
                    )
                )
            ),
            com.roshni.games.feature.settings.domain.model.SettingsSection(
                id = "gameplay",
                title = "Gameplay",
                description = "Configure your gaming experience",
                icon = "🎮",
                items = listOf(
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "auto_save",
                        title = "Auto Save",
                        description = "Automatically save your progress",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.gameplay.autoSave
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "show_hints",
                        title = "Show Hints",
                        description = "Display helpful hints during gameplay",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.gameplay.showHints
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "sound_effects",
                        title = "Sound Effects",
                        description = "Play sound effects during games",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.gameplay.soundEffects
                    )
                )
            ),
            com.roshni.games.feature.settings.domain.model.SettingsSection(
                id = "notifications",
                title = "Notifications",
                description = "Manage notification preferences",
                icon = "🔔",
                items = listOf(
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "push_notifications",
                        title = "Push Notifications",
                        description = "Receive push notifications",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.notifications.pushNotifications
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "achievement_notifications",
                        title = "Achievement Notifications",
                        description = "Get notified when you unlock achievements",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.notifications.achievementNotifications
                    )
                )
            ),
            com.roshni.games.feature.settings.domain.model.SettingsSection(
                id = "account",
                title = "Account",
                description = "Manage your account settings",
                icon = "👤",
                items = listOf(
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "profile",
                        title = "Profile",
                        description = "Manage your profile information",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.NAVIGATION
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "sync",
                        title = "Sync Data",
                        description = "Sync your progress across devices",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.TOGGLE,
                        value = _uiState.value.settingsState.appSettings.account.syncEnabled
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "export_data",
                        title = "Export Data",
                        description = "Export your game data and settings",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.BUTTON
                    ),
                    com.roshni.games.feature.settings.domain.model.SettingsItem(
                        id = "delete_account",
                        title = "Delete Account",
                        description = "Permanently delete your account",
                        type = com.roshni.games.feature.settings.domain.model.SettingsItemType.BUTTON
                    )
                )
            )
        )
    }
}