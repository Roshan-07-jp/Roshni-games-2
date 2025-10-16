package com.roshni.games.core.utils.terms.di

import com.roshni.games.core.database.dao.TermsDao
import com.roshni.games.core.utils.terms.TermsAndConditionsFeature
import com.roshni.games.core.utils.terms.TermsAndConditionsManager
import com.roshni.games.core.utils.terms.TermsAndConditionsManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TermsAndConditionsModule {

    @Provides
    @Singleton
    fun provideTermsAndConditionsManager(
        termsDao: TermsDao
    ): TermsAndConditionsManager {
        return TermsAndConditionsManagerImpl(termsDao)
    }

    @Provides
    @Singleton
    fun provideTermsAndConditionsFeature(
        termsManager: TermsAndConditionsManager
    ): TermsAndConditionsFeature {
        return TermsAndConditionsFeature(termsManager)
    }
}