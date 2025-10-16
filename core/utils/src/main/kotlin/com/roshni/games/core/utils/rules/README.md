# Conditional Logic System (CLS)

The Conditional Logic System provides a comprehensive rule engine for implementing dynamic gameplay mechanics, feature gates, permission controls, and content restrictions in Roshni Games.

## Overview

The CLS consists of several key components:

- **RuleContext**: Contains all contextual information for rule evaluation
- **RuleResult**: Represents the outcome of rule evaluation with actions to execute
- **GameplayCondition**: Sealed class defining various condition types
- **GameplayAction**: Sealed class defining various action types
- **Rule Types**: Interfaces for different rule categories (GameplayRule, PermissionRule, etc.)
- **RuleEngine**: Core engine for rule registration, evaluation, and execution

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   RuleContext   │───▶│   RuleEngine     │───▶│  RuleResult     │
│                 │    │                  │    │                 │
│ - User Profile  │    │ - Registration   │    │ - Is Allowed    │
│ - Device Info   │    │ - Evaluation     │    │ - Actions       │
│ - Game State    │    │ - Execution      │    │ - Confidence    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼───────┐ ┌─────▼──────┐ ┌─────▼──────┐
        │GameplayRule   │ │Permission  │ │FeatureGate │
        │               │ │Rule        │ │Rule        │
        │- Conditions   │ │- Permissions│ │- Rollout   │
        │- Actions      │ │- Access    │ │- Strategy  │
        └───────────────┘ └────────────┘ └────────────┘
```

## Quick Start

### Basic Usage

```kotlin
// Get the rule engine instance
val ruleEngine = RuleEngineImpl.getInstance()

// Create a rule context
val context = RuleContext(
    userId = "user123",
    gameId = "puzzle-game",
    userProfile = UserProfile(
        age = 25,
        isPremium = true,
        playTime = PlayTimeInfo(totalPlayTime = 3600000) // 1 hour
    ),
    gameState = GameState(
        currentLevel = 5,
        score = 1500,
        difficulty = "medium"
    )
)

// Register a simple gameplay rule
val levelUpRule = object : GameplayRule {
    override val id = "level-up-bonus"
    override val name = "Level Up Bonus Rule"
    override val description = "Grants bonus points when leveling up"
    override val category = "progression"
    override val enabled = true
    override val tags = listOf("level", "bonus", "progression")
    override val createdAt = System.currentTimeMillis()
    override val modifiedAt = System.currentTimeMillis()
    override val version = 1

    override val conditions = listOf(
        GameplayCondition.LevelCondition(
            id = "min-level-reached",
            minLevel = 5,
            description = "Player must reach level 5"
        )
    )

    override val actions = listOf(
        GameplayAction.ShowMessage(
            id = "level-bonus-message",
            message = "Congratulations! You've reached level 5!",
            messageType = ShowMessage.MessageType.SUCCESS
        ),
        GameplayAction.ModifyGameplay(
            id = "add-bonus-points",
            modifications = mapOf("bonusPoints" to 1000)
        )
    )

    override val continuousEvaluation = false
    override val evaluationInterval = 0L

    override suspend fun shouldTrigger(context: RuleContext): Boolean {
        return context.gameState.currentLevel == 5
    }

    override fun getPriority(): Int = 1

    override suspend fun isActive(context: RuleContext): Boolean = true

    override suspend fun evaluate(context: RuleContext): RuleResult {
        val allConditionsMet = conditions.all { it.evaluate(context) }

        return if (allConditionsMet && shouldTrigger(context)) {
            RuleResult.success(
                ruleId = id,
                ruleType = RuleResult.RuleType.GAMEPLAY,
                reason = "Player reached level 5",
                actions = actions.map { it as RuleResult.RuleAction }
            )
        } else {
            RuleResult.failure(
                ruleId = id,
                ruleType = RuleResult.RuleType.GAMEPLAY,
                reason = "Conditions not met or rule should not trigger"
            )
        }
    }

    override suspend fun validate(): Boolean = true

    override suspend fun getMetadata(): Map<String, Any> = mapOf(
        "triggerLevel" to 5,
        "bonusPoints" to 1000
    )
}

// Register and evaluate the rule
ruleEngine.registerRule(levelUpRule)
val result = ruleEngine.evaluateRule("level-up-bonus", context)

// Execute actions if rule passed
if (result.isAllowed) {
    ruleEngine.executeActions(result, context)
}
```

### Dependency Injection Setup

Add the rule engine module to your application:

```kotlin
@HiltAndroidApp
class RoshniGamesApplication : Application() {

    @Inject
    lateinit var ruleEngine: RuleEngine

    override fun onCreate() {
        super.onCreate()

        // Initialize rule engine
        lifecycleScope.launch {
            // Register default rules
            registerDefaultRules()
        }
    }

    private suspend fun registerDefaultRules() {
        // Register built-in rules
        val parentalControlRule = ParentalControlRuleImpl()
        val featureGateRule = FeatureGateRuleImpl()

        ruleEngine.registerRule(parentalControlRule)
        ruleEngine.registerRule(featureGateRule)
    }
}
```

## Rule Types

### GameplayRule

Controls gameplay mechanics and player experience:

```kotlin
class AchievementUnlockRule : GameplayRule {
    override val conditions = listOf(
        GameplayCondition.ScoreCondition(
            id = "high-score-achievement",
            minScore = 10000L,
            description = "Player must achieve high score"
        ),
        GameplayCondition.AchievementCondition(
            id = "no-previous-achievement",
            requiredAchievements = listOf("first-win"),
            description = "Player must have basic achievement"
        )
    )

    override val actions = listOf(
        GameplayAction.UnlockContent(
            id = "unlock-achievement",
            contentIds = listOf("high-scorer-badge", "exclusive-level")
        ),
        GameplayAction.ShowMessage(
            id = "achievement-notification",
            message = "New achievement unlocked!",
            messageType = ShowMessage.MessageType.ACHIEVEMENT
        )
    )

    // ... implementation details
}
```

### PermissionRule

Controls access based on user permissions:

```kotlin
class PremiumFeatureRule : PermissionRule {
    override val requiredPermission = "premium_access"
    override val alternativePermissions = listOf("admin_access", "beta_tester")
    override val showPermissionRequest = true
    override val denialMessage = "This feature requires a premium subscription"

    override suspend fun hasPermission(context: RuleContext): Boolean {
        return context.userProfile.isPremium ||
               context.userProfile.subscriptionTier in listOf("premium", "vip")
    }

    override suspend fun getPermissionRationale(context: RuleContext): String {
        return "Premium features include exclusive content and advanced gameplay options"
    }

    // ... implementation details
}
```

### FeatureGateRule

Controls feature availability with rollout strategies:

```kotlin
class NewGameModeRule : FeatureGateRule {
    override val featureId = "experimental-mode"
    override val gatingStrategy = GatingStrategy.ROLLOUT
    override val rolloutPercentage = 0.25f // 25% rollout
    override val enabled = true

    override val featureConditions = listOf(
        GameplayCondition.UserCondition(
            id = "premium-users-only",
            requirePremium = true,
            description = "Only premium users get early access"
        )
    )

    override suspend fun isFeatureEnabled(context: RuleContext): Boolean {
        // Check rollout percentage
        val userHash = context.userId.hashCode() % 100
        val rolloutThreshold = (rolloutPercentage * 100).toInt()

        if (userHash >= rolloutThreshold) return false

        // Check feature conditions
        return featureConditions.all { it.evaluate(context) }
    }

    // ... implementation details
}
```

### ContentRestrictionRule

Controls content access based on age and parental controls:

```kotlin
class MatureContentRule : ContentRestrictionRule {
    override val contentId = "violent-game-mode"
    override val ageRating = AgeRating.MATURE_17_PLUS
    override val contentCategories = listOf("action", "combat")
    override val requiresParentalConsent = true

    override suspend fun isContentAppropriate(context: RuleContext): Boolean {
        val userAge = context.userProfile.age ?: return false

        // Check age requirement
        if (userAge < 17) return false

        // Check parental controls
        if (context.userProfile.parentalControlsEnabled) {
            return context.userProfile.age?.let { it >= 18 } == true
        }

        return true
    }

    // ... implementation details
}
```

### ParentalControlRule

Enforces parental restrictions and time limits:

```kotlin
class PlayTimeLimitRule : ParentalControlRule {
    override val controlType = ControlType.PLAY_TIME_LIMIT
    override val severity = Severity.HIGH
    override val allowOverride = true

    override val timeRestrictions = TimeRestrictions(
        dailyLimitMinutes = 120, // 2 hours per day
        weeklyLimitMinutes = 600, // 10 hours per week
        allowedHours = listOf(8..12, 14..20), // 8-12 AM, 2-8 PM
        bedtimeEnforcement = true,
        bedtimeHour = 22 // 10 PM
    )

    override suspend fun isRestrictionActive(context: RuleContext): Boolean {
        val currentHour = getCurrentHour()
        val playTime = context.userProfile.playTime

        // Check bedtime
        if (timeRestrictions.bedtimeEnforcement &&
            currentHour >= timeRestrictions.bedtimeHour!!) {
            return true
        }

        // Check daily limit
        if (timeRestrictions.dailyLimitMinutes != null &&
            playTime.dailyPlayTime >= timeRestrictions.dailyLimitMinutes * 60 * 1000) {
            return true
        }

        // Check allowed hours
        if (timeRestrictions.allowedHours != null &&
            currentHour !in timeRestrictions.allowedHours.flatten()) {
            return true
        }

        return false
    }

    // ... implementation details
}
```

## Advanced Usage

### Continuous Evaluation

For real-time rule evaluation:

```kotlin
// Start continuous evaluation
val evaluationFlow = ruleEngine.startContinuousEvaluation(
    contextProvider = { getCurrentContext() },
    evaluationInterval = 1000L // Evaluate every second
)

// Observe results
evaluationFlow.collect { results ->
    results.forEach { result ->
        if (result.isAllowed) {
            ruleEngine.executeActions(result, getCurrentContext())
        }
    }
}
```

### Rule Statistics and Monitoring

```kotlin
// Get rule statistics
val stats = ruleEngine.getRuleStatistics()
println("Total evaluations: ${stats.totalEvaluations}")
println("Success rate: ${stats.successfulEvaluations.toDouble() / stats.totalEvaluations}")
println("Average evaluation time: ${stats.averageEvaluationTimeMs}ms")

// Get engine status
val status = ruleEngine.getEngineStatus()
println("Engine running: ${status.isRunning}")
println("Active rules: ${status.activeRuleCount}")
println("Uptime: ${status.uptimeMs}ms")
```

### Rule Validation

```kotlin
// Validate all rules
val validation = ruleEngine.validateAllRules()
if (!validation.isValid) {
    validation.errors.forEach { error ->
        println("Validation error: $error")
    }
}

// Validate individual rule
val rule = ruleEngine.getRule("my-rule")
if (rule?.validate() == false) {
    println("Rule is not properly configured")
}
```

## Best Practices

### 1. Rule Design
- Keep rules simple and focused on single responsibilities
- Use descriptive IDs and names for rules
- Include comprehensive descriptions and metadata
- Set appropriate priority levels for rule execution order

### 2. Performance
- Use continuous evaluation only when necessary
- Cache expensive computations in rule metadata
- Avoid complex logic in frequently evaluated rules
- Use appropriate evaluation intervals for continuous rules

### 3. Error Handling
- Always handle exceptions in rule evaluation
- Provide meaningful error messages and fallbacks
- Log rule evaluation results for debugging
- Implement proper cleanup in rule actions

### 4. Testing
- Test rules with various contexts and edge cases
- Mock external dependencies in rule tests
- Validate rule logic independently of the engine
- Monitor rule performance in production

## Integration Examples

### With Game Engine

```kotlin
class GameEngineIntegration(
    private val ruleEngine: RuleEngine
) {
    suspend fun onLevelComplete(level: Int, score: Long) {
        val context = RuleContext(
            userId = currentUserId,
            gameState = GameState(
                currentLevel = level,
                score = score
            )
        )

        val results = ruleEngine.evaluateRulesByCategory("progression", context)
        results.forEach { result ->
            if (result.isAllowed) {
                ruleEngine.executeActions(result, context)
            }
        }
    }
}
```

### With UI System

```kotlin
class UIRuleIntegration(
    private val ruleEngine: RuleEngine
) {
    suspend fun checkFeatureAvailability(featureId: String, userId: String): Boolean {
        val context = RuleContext(userId = userId)
        val results = ruleEngine.evaluateRulesByCategory("feature-gate", context)

        return results.any { result ->
            result.isAllowed && result.ruleId.contains(featureId)
        }
    }
}
```

## Migration Guide

### From Simple Boolean Flags

```kotlin
// Before
val isFeatureEnabled = user.isPremium && user.level >= 10

// After
val featureRule = FeatureGateRuleImpl(
    featureId = "premium-feature",
    conditions = listOf(
        UserCondition(requirePremium = true),
        LevelCondition(minLevel = 10)
    )
)
val result = ruleEngine.evaluateRule(featureRule.id, context)
val isFeatureEnabled = result.isAllowed
```

### From Hardcoded Logic

```kotlin
// Before
fun canAccessContent(user: User, content: Content): Boolean {
    if (user.age < content.minAge) return false
    if (user.parentalControlsEnabled && !content.approved) return false
    return true
}

// After
val contentRule = ContentRestrictionRuleImpl(
    contentId = content.id,
    ageRating = content.ageRating,
    requiresParentalConsent = content.requiresApproval
)
val result = ruleEngine.evaluateRule(contentRule.id, context)
return result.isAllowed
```

This migration provides better maintainability, testability, and flexibility in managing complex game logic and access controls.