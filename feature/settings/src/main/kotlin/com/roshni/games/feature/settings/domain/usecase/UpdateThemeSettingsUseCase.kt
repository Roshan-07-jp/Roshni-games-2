package com.roshni.games.feature.settings.domain.usecase

import com.roshni.games.feature.settings.domain.model.ThemeSettings
import com.roshni.games.feature.settings.domain.repository.SettingsDomainRepository
import javax.inject.Inject

class UpdateThemeSettingsUseCase @Inject constructor(
    private val settingsDomainRepository: SettingsDomainRepository
) {
    suspend operator fun invoke(themeSettings: ThemeSettings): Boolean {
        return settingsDomainRepository.updateThemeSettings(themeSettings)
    }
}