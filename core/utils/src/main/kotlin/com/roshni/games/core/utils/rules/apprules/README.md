# App Rules Engine (F)

The App Rules Engine is a comprehensive business rule validation and enforcement system designed for the Roshni Games platform. It provides high-level application rules including gameplay, monetization, and social features, building upon the existing RuleEngine for complex condition evaluation.

## Overview

The App Rules Engine (F) extends the existing rules system to handle business-level operations with:

- **Business Rule Types**: Gameplay, Monetization, and Social rules
- **Priority-based Execution**: Rules execute in order based on priority and dependencies
- **Validation & Enforcement**: Comprehensive validation and enforcement mechanisms
- **Integration**: Seamless integration with existing RuleEngine for complex conditions
- **Real-time Processing**: Support for continuous validation and rule evaluation

## Architecture

### Core Components

1. **AppRulesEngine**: Main interface for business rule management
2. **BusinessRule**: Base interface for all business rule types
3. **ValidationContext**: Context information for rule validation
4. **EnforcementContext**: Context information for rule enforcement
5. **RulePriorityExecutor**: Handles priority-based rule execution
6. **BusinessLogicValidator**: Core validation logic
7. **BusinessLogicEnforcer**: Core enforcement logic

### Rule Categories

- **GAMEPLAY**: Rules related to game mechanics and player progression
- **MONETIZATION**: Rules related to in-app purchases and revenue
- **SOCIAL**: Rules related to social features and interactions
- **SECURITY**: Rules related to security and access control
- **COMPLIANCE**: Rules related to legal and regulatory compliance
- **PERFORMANCE**: Rules related to app performance and optimization
- **ACCESSIBILITY**: Rules related to accessibility features
- **CUSTOM**: Custom business rules

## Usage Examples

### Basic Usage

```kotlin
// Inject the App Rules Engine
@Inject
@AppRulesEngineInstance
lateinit var appRulesEngine: AppRulesEngine

// Validate a purchase operation
suspend fun validatePurchase(userId: String, productId: String, price: Double) {
    val operation = BusinessOperation(
        operationType = BusinessOperationType.PURCHASE_INITIATE,
        operationId = UUID.randomUUID().toString(),
        userId = userId,
        data = mapOf(
            "productId" to productId,
            "price" to price,
            "currency" to "USD"
        )
    )

    val context = ValidationContext(
        operation = operation,
        userContext = UserContext(
            userId = userId,
            userProfile = UserProfile()
        ),
        applicationContext = ApplicationContext(
            appId = "com.roshni.games",
            appVersion = "1.0.0",
            gameState = GameState()
        ),
        environmentContext = EnvironmentContext(
            deviceInfo = DeviceInfo()
        )
    )

    val result = appRulesEngine.validateOperation(operation, context)

    if (result.isValid) {
        // Proceed with purchase
        proceedWithPurchase(productId, price)
    } else {
        // Show error message
        showError(result.getPrimaryFailureReason())
    }
}
```

### Using Extension Functions

```kotlin
// Simple validation using extension functions
suspend fun checkGameAccess(userId: String, gameId: String): Boolean {
    return appRulesEngine.isAllowed(
        operationType = BusinessOperationType.GAME_START,
        userId = userId,
        data = mapOf("gameId" to gameId)
    )
}

// Simple enforcement
suspend fun processGameCompletion(userId: String, score: Long) {
    val result = appRulesEngine.enforce(
        operationType = BusinessOperationType.GAME_END,
        userId = userId,
        data = mapOf("score" to score)
    )

    if (result.isSuccessful) {
        // Update UI with results
        updateGameCompleteUI(result)
    }
}
```

### Registering Custom Business Rules

```kotlin
// Create a custom gameplay rule
val customGameplayRule = GameplayBusinessRule(
    id = "level_progression_rule",
    name = "Level Progression Rule",
    description = "Controls level progression based on score and achievements",
    priority = 1,
    gameplayConditions = listOf(
        GameplayCondition.ScoreCondition(
            id = "min_score_condition",
            minScore = 1000L
        ),
        GameplayCondition.LevelCondition(
            id = "max_level_condition",
            maxLevel = 50
        )
    ),
    gameplayActions = listOf(
        GameplayAction.ModifyGameplay(
            id = "unlock_next_level",
            modifications = mapOf("unlockLevel" to "next")
        ),
        GameplayAction.ShowMessage(
            id = "level_complete_message",
            message = "Congratulations! Level completed!",
            messageType = GameplayAction.ShowMessage.MessageType.SUCCESS
        )
    )
)

// Register the rule
appRulesEngine.registerBusinessRule(customGameplayRule)
```

### Advanced Validation with Custom Strategy

```kotlin
// Use strict validation for critical operations
val strictValidation = ValidationStrategy.STRICT

val result = businessLogicValidator.validateBusinessOperation(
    operation = purchaseOperation,
    context = validationContext,
    validationStrategy = strictValidation
)

// Check detailed results
result.ruleResults.forEach { ruleResult ->
    println("Rule ${ruleResult.ruleId}: ${ruleResult.isAllowed}")
    if (ruleResult.hasWarnings()) {
        ruleResult.warnings.forEach { warning ->
            println("Warning: $warning")
        }
    }
}
```

### Continuous Validation

```kotlin
// Start continuous validation for real-time rule checking
val validationFlow = appRulesEngine.startContinuousValidation(
    contextProvider = { getCurrentContext() },
    validationInterval = 5000L // Check every 5 seconds
)

// Observe validation results
validationFlow.collect { result ->
    if (!result.isValid) {
        // Handle validation failure
        handleValidationFailure(result)
    }
}
```

## Business Rule Types

### GameplayBusinessRule

Handles game mechanics, progression, and player experience:

```kotlin
val gameplayRule = GameplayBusinessRule(
    id = "achievement_unlock_rule",
    name = "Achievement Unlock Rule",
    description = "Unlocks achievements based on player progress",
    priority = 2,
    gameplayConditions = listOf(
        GameplayCondition.AchievementCondition(
            id = "score_achievement_condition",
            requiredAchievements = listOf("first_win", "score_master")
        )
    ),
    gameplayActions = listOf(
        GameplayAction.UnlockContent(
            id = "unlock_achievement_reward",
            contentIds = listOf("special_skin", "bonus_level")
        )
    ),
    gameModes = listOf("adventure", "challenge"),
    difficultyLevels = listOf("normal", "hard")
)
```

### MonetizationBusinessRule

Handles in-app purchases, subscriptions, and revenue:

```kotlin
val monetizationRule = MonetizationBusinessRule(
    id = "purchase_validation_rule",
    name = "Purchase Validation Rule",
    description = "Validates purchase conditions and applies pricing rules",
    priority = 3,
    purchaseConditions = listOf(
        MonetizationCondition(
            id = "age_verification_condition",
            conditionType = "age_check"
        ) { context ->
            val userAge = context.userProfile.age ?: 0
            userAge >= 18
        }
    ),
    pricingRules = listOf(
        PricingRule(
            id = "premium_pricing",
            productId = "premium_subscription",
            basePrice = 9.99,
            currency = "USD"
        )
    ),
    supportedCurrencies = listOf("USD", "EUR", "GBP"),
    regionalPricing = mapOf("EU" to 1.2, "UK" to 1.1)
)
```

### SocialBusinessRule

Handles social features, interactions, and community guidelines:

```kotlin
val socialRule = SocialBusinessRule(
    id = "friend_request_rule",
    name = "Friend Request Rule",
    description = "Controls friend request permissions and restrictions",
    priority = 1,
    socialConditions = listOf(
        SocialCondition(
            id = "age_compatibility_condition",
            conditionType = "age_check"
        ) { context ->
            // Check age compatibility for social features
            true
        }
    ),
    interactionRules = listOf(
        InteractionRule(
            id = "daily_friend_limit",
            interactionType = "friend_request",
            allowed = true,
            reason = "Daily friend request limit not exceeded",
            restrictions = listOf("max_10_per_day")
        )
    ),
    ageRestrictions = AgeRestriction(
        minAge = 13,
        requiresVerification = true
    )
)
```

## Integration with Existing RuleEngine

The App Rules Engine integrates seamlessly with the existing RuleEngine:

```kotlin
// The AppRulesEngine uses the existing RuleEngine internally
@Inject
@RuleEngineInstance
lateinit var ruleEngine: RuleEngine

// Create AppRulesEngine with the existing RuleEngine
val appRulesEngine = AppRulesEngineFactory.create(ruleEngine = ruleEngine)

// Register gameplay conditions as individual rules in the underlying engine
val gameplayRule = GameplayBusinessRule(...)
appRulesEngine.registerBusinessRule(gameplayRule)
// This automatically registers underlying GameplayConditions as Rules
```

## Best Practices

### Rule Design

1. **Keep rules focused**: Each rule should have a single responsibility
2. **Use appropriate priorities**: Higher priority rules execute first
3. **Include comprehensive validation**: Always validate rule configuration
4. **Provide meaningful error messages**: Help users understand rule failures

### Performance

1. **Use appropriate validation strategies**: Choose based on operation criticality
2. **Leverage parallel execution**: For non-dependent rules
3. **Monitor execution times**: Use built-in statistics and monitoring
4. **Cache rule results**: For frequently evaluated rules

### Error Handling

1. **Graceful degradation**: Handle rule failures without breaking the app
2. **Comprehensive logging**: Log rule evaluations for debugging
3. **User-friendly messages**: Convert technical rule failures to user messages
4. **Fallback mechanisms**: Provide alternatives when rules fail

## Testing

### Unit Testing Business Rules

```kotlin
@Test
fun testGameplayRule() = runBlocking {
    val rule = GameplayBusinessRule(
        id = "test_rule",
        name = "Test Rule",
        description = "Test rule for unit testing",
        // ... other parameters
    )

    val context = RuleContext(
        userId = "test_user",
        gameState = GameState(currentLevel = 5)
    )

    val result = rule.evaluate(context)

    assertTrue(result.isAllowed)
    assertEquals("Gameplay rule evaluation passed", result.reason)
}
```

### Integration Testing

```kotlin
@Test
fun testAppRulesEngineIntegration() = runBlocking {
    val appRulesEngine = AppRulesEngineFactory.createTestInstance()

    // Register test rules
    val testRule = GameplayBusinessRule(/* ... */)
    appRulesEngine.registerBusinessRule(testRule)

    // Test validation
    val operation = BusinessOperation(/* ... */)
    val context = ValidationContext(/* ... */)

    val result = appRulesEngine.validateOperation(operation, context)

    assertTrue(result.isValid)
}
```

## Monitoring and Analytics

### Rule Statistics

```kotlin
// Get overall statistics
val stats = appRulesEngine.getValidationStatistics()

println("Total validations: ${stats.totalValidations}")
println("Success rate: ${stats.successfulValidations.toDouble() / stats.totalValidations}")
println("Average execution time: ${stats.averageValidationTimeMs}ms")

// Get rule-specific statistics
val ruleStats = appRulesEngine.getValidationStatistics("specific_rule_id")
```

### Performance Monitoring

```kotlin
// Monitor rule execution performance
val status = appRulesEngine.getEngineStatus()

println("Engine running: ${status.isRunning}")
println("Active rules: ${status.activeRuleCount}")
println("Uptime: ${status.uptimeMs}ms")
println("Success rate: ${status.validationSuccessRate}")
```

## Deployment and Configuration

### Configuration Options

```kotlin
// Create engine with custom configuration
val appRulesEngine = AppRulesEngineImpl(
    ruleEngine = customRuleEngine,
    businessLogicValidator = CustomBusinessLogicValidator(),
    businessLogicEnforcer = CustomBusinessLogicEnforcer()
)
```

### Environment-specific Configuration

```kotlin
// Different configurations for different environments
val config = when (BuildConfig.BUILD_TYPE) {
    "debug" -> ValidationStrategy.COMPREHENSIVE
    "staging" -> ValidationStrategy.STRICT
    "release" -> ValidationStrategy.PERFORMANCE
    else -> ValidationStrategy.NORMAL
}
```

This App Rules Engine provides a robust foundation for business rule management in the Roshni Games platform, enabling sophisticated validation, enforcement, and monitoring of application behavior across gameplay, monetization, and social features.