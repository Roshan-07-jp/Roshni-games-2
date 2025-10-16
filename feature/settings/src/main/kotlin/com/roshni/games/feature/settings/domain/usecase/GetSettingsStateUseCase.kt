package com.roshni.games.feature.settings.domain.usecase

import com.roshni.games.feature.settings.domain.model.SettingsState
import com.roshni.games.feature.settings.domain.repository.SettingsDomainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsStateUseCase @Inject constructor(
    private val settingsDomainRepository: SettingsDomainRepository
) {
    operator fun invoke(): Flow<SettingsState> {
        return settingsDomainRepository.getSettingsState()
    }
}