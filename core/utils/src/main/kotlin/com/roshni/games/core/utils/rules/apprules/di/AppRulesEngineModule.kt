package com.roshni.games.core.utils.rules.apprules.di

import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.rules.RuleEngineImpl
import com.roshni.games.core.utils.rules.apprules.AppRulesEngine
import com.roshni.games.core.utils.rules.apprules.AppRulesEngineFactory
import com.roshni.games.core.utils.rules.apprules.BusinessLogicEnforcer
import com.roshni.games.core.utils.rules.apprules.BusinessLogicValidator
import com.roshni.games.core.utils.rules.apprules.RulePriorityExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing App Rules Engine dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppRulesEngineModule {

    /**
     * Provides the singleton instance of the App Rules Engine
     */
    @Provides
    @Singleton
    @AppRulesEngineInstance
    fun provideAppRulesEngine(
        @RuleEngineInstance ruleEngine: RuleEngine,
        businessLogicValidator: BusinessLogicValidator,
        businessLogicEnforcer: BusinessLogicEnforcer
    ): AppRulesEngine {
        return AppRulesEngineFactory.create(
            ruleEngine = ruleEngine,
            businessLogicValidator = businessLogicValidator,
            businessLogicEnforcer = businessLogicEnforcer
        )
    }

    /**
     * Provides the business logic validator
     */
    @Provides
    @Singleton
    fun provideBusinessLogicValidator(
        rulePriorityExecutor: RulePriorityExecutor
    ): BusinessLogicValidator {
        return BusinessLogicValidator(rulePriorityExecutor)
    }

    /**
     * Provides the business logic enforcer
     */
    @Provides
    @Singleton
    fun provideBusinessLogicEnforcer(
        rulePriorityExecutor: RulePriorityExecutor
    ): BusinessLogicEnforcer {
        return BusinessLogicEnforcer(rulePriorityExecutor)
    }

    /**
     * Provides the rule priority executor
     */
    @Provides
    @Singleton
    fun provideRulePriorityExecutor(): RulePriorityExecutor {
        return RulePriorityExecutor()
    }

    /**
     * Provides an App Rules Engine instance scoped to the current context
     * This can be useful for testing or when multiple engines are needed
     */
    @Provides
    @ContextScopedAppRulesEngine
    fun provideContextScopedAppRulesEngine(
        @RuleEngineInstance ruleEngine: RuleEngine
    ): AppRulesEngine {
        return AppRulesEngineFactory.createTestInstance()
    }
}

/**
 * Qualifier for the main singleton App Rules Engine instance
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppRulesEngineInstance

/**
 * Qualifier for context-scoped App Rules Engine instances
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ContextScopedAppRulesEngine

/**
 * Extension functions for easy injection and usage
 */
object AppRulesEngineExtensions {

    /**
     * Validate a business operation using the App Rules Engine
     */
    suspend fun AppRulesEngine.validate(
        operationType: com.roshni.games.core.utils.rules.apprules.BusinessOperationType,
        userId: String,
        data: Map<String, Any> = emptyMap()
    ): com.roshni.games.core.utils.rules.apprules.ValidationResult {
        val operation = com.roshni.games.core.utils.rules.apprules.BusinessOperation(
            operationType = operationType,
            operationId = java.util.UUID.randomUUID().toString(),
            userId = userId,
            data = data
        )

        // Create basic context - in real usage, this would be more comprehensive
        val context = com.roshni.games.core.utils.rules.apprules.ValidationContext(
            operation = operation,
            userContext = com.roshni.games.core.utils.rules.apprules.UserContext(
                userId = userId,
                userProfile = com.roshni.games.core.utils.rules.RuleContext.UserProfile()
            ),
            applicationContext = com.roshni.games.core.utils.rules.apprules.ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "1.0.0",
                gameState = com.roshni.games.core.utils.rules.RuleContext.GameState()
            ),
            environmentContext = com.roshni.games.core.utils.rules.apprules.EnvironmentContext(
                deviceInfo = com.roshni.games.core.utils.rules.RuleContext.DeviceInfo()
            )
        )

        return validateOperation(operation, context)
    }

    /**
     * Check if an operation is allowed
     */
    suspend fun AppRulesEngine.isAllowed(
        operationType: com.roshni.games.core.utils.rules.apprules.BusinessOperationType,
        userId: String,
        data: Map<String, Any> = emptyMap()
    ): Boolean {
        val result = validate(operationType, userId, data)
        return result.isValid
    }

    /**
     * Enforce rules for a business operation
     */
    suspend fun AppRulesEngine.enforce(
        operationType: com.roshni.games.core.utils.rules.apprules.BusinessOperationType,
        userId: String,
        data: Map<String, Any> = emptyMap()
    ): com.roshni.games.core.utils.rules.apprules.EnforcementResult {
        val operation = com.roshni.games.core.utils.rules.apprules.BusinessOperation(
            operationType = operationType,
            operationId = java.util.UUID.randomUUID().toString(),
            userId = userId,
            data = data
        )

        val validationContext = com.roshni.games.core.utils.rules.apprules.ValidationContext(
            operation = operation,
            userContext = com.roshni.games.core.utils.rules.apprules.UserContext(
                userId = userId,
                userProfile = com.roshni.games.core.utils.rules.RuleContext.UserProfile()
            ),
            applicationContext = com.roshni.games.core.utils.rules.apprules.ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "1.0.0",
                gameState = com.roshni.games.core.utils.rules.RuleContext.GameState()
            ),
            environmentContext = com.roshni.games.core.utils.rules.apprules.EnvironmentContext(
                deviceInfo = com.roshni.games.core.utils.rules.RuleContext.DeviceInfo()
            )
        )

        val enforcementContext = com.roshni.games.core.utils.rules.apprules.EnforcementContext(
            operation = operation,
            validationResult = com.roshni.games.core.utils.rules.apprules.ValidationResult(
                isValid = true,
                operation = operation,
                context = validationContext
            ),
            userContext = validationContext.userContext,
            applicationContext = validationContext.applicationContext,
            environmentContext = validationContext.environmentContext
        )

        return enforceRules(operation, enforcementContext)
    }
}