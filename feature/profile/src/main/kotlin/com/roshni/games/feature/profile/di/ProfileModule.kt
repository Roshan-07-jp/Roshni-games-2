package com.roshni.games.feature.profile.di

import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.feature.profile.data.datasource.ProfileDataSource
import com.roshni.games.feature.profile.data.datasource.ProfileDataSourceImpl
import com.roshni.games.feature.profile.data.repository.ProfileRepository
import com.roshni.games.feature.profile.data.repository.ProfileRepositoryImpl
import com.roshni.games.feature.profile.domain.repository.ProfileDomainRepository
import com.roshni.games.feature.profile.domain.repository.ProfileDomainRepositoryImpl
import com.roshni.games.feature.profile.domain.usecase.GetProfileStateUseCase
import com.roshni.games.feature.profile.domain.usecase.UpdateUserProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ProfileModule {

    @Provides
    @ViewModelScoped
    fun provideProfileDataSource(
        gameDao: GameDao,
        playerDao: PlayerDao,
        scoreDao: ScoreDao
    ): ProfileDataSource {
        return ProfileDataSourceImpl(gameDao, playerDao, scoreDao)
    }

    @Provides
    @ViewModelScoped
    fun provideProfileRepository(
        profileDataSource: ProfileDataSource
    ): ProfileRepository {
        return ProfileRepositoryImpl(profileDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideProfileDomainRepository(
        profileRepository: ProfileRepository
    ): ProfileDomainRepository {
        return ProfileDomainRepositoryImpl(profileRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetProfileStateUseCase(
        profileDomainRepository: ProfileDomainRepository
    ): GetProfileStateUseCase {
        return GetProfileStateUseCase(profileDomainRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateUserProfileUseCase(
        profileDomainRepository: ProfileDomainRepository
    ): UpdateUserProfileUseCase {
        return UpdateUserProfileUseCase(profileDomainRepository)
    }
}