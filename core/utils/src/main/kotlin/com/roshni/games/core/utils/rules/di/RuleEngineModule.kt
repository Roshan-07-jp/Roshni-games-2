package com.roshni.games.core.utils.rules.di

import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.rules.RuleEngineImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing rule engine dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RuleEngineModule {

    /**
     * Provides the singleton instance of the rule engine
     */
    @Provides
    @Singleton
    @RuleEngineInstance
    fun provideRuleEngine(): RuleEngine {
        return RuleEngineImpl.getInstance()
    }

    /**
     * Provides a rule engine instance scoped to the current context
     * This can be useful for testing or when multiple rule engines are needed
     */
    @Provides
    @ContextScopedRuleEngine
    fun provideContextScopedRuleEngine(): RuleEngine {
        return RuleEngineImpl()
    }
}

/**
 * Qualifier for the main singleton rule engine instance
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RuleEngineInstance

/**
 * Qualifier for context-scoped rule engine instances
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ContextScopedRuleEngine