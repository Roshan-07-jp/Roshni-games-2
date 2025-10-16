# Navigation Flow Controller

## Overview

The Navigation Flow Controller is an intelligent navigation system that provides advanced routing capabilities, rule-based navigation control, and optimal route calculation for the Roshni Games application. It enhances the existing Android Navigation Component with sophisticated decision-making capabilities.

## Architecture

### Core Components

#### 1. Navigation Models
- **NavigationContext**: Encapsulates all context information for navigation decisions
- **NavigationResult**: Represents the outcome of navigation operations (Success, Failure, Cancelled)
- **NavigationEvent**: Events emitted during navigation lifecycle

#### 2. Navigation Rules System
- **NavigationRule**: Base interface for all navigation rules
- **ConditionalRule**: Evaluates navigation based on conditional logic
- **FeatureGateRule**: Controls navigation based on feature flags
- **PermissionRule**: Manages access control through permissions

#### 3. Navigation Controller
- **NavigationFlowController**: Main interface for navigation management
- **NavigationFlowControllerImpl**: Default implementation with A* route optimization

#### 4. Integration Layer
- **NavigationIntegration**: Bridges new system with existing NavigationActions
- **EnhancedNavigationActions**: Drop-in replacement for existing navigation actions

## Key Features

### Intelligent Route Optimization
- **A* Algorithm**: Finds optimal paths through navigation graph
- **Context-Aware Routing**: Considers user permissions, feature flags, and device capabilities
- **Performance Optimization**: Caches frequently used routes and rule evaluations

### Rule-Based Navigation Control
- **Conditional Rules**: Evaluate complex business logic conditions
- **Feature Gates**: Control access based on feature flag states
- **Permission Rules**: Enforce access control policies
- **Device Compatibility**: Check device capabilities before navigation

### Advanced Context Management
- **User Context**: Permissions, preferences, and session information
- **Device Context**: Screen size, orientation, performance characteristics
- **Application State**: Network status, authentication, battery level

### Event-Driven Architecture
- **Navigation Events**: Real-time event emission and observation
- **State Management**: Reactive state updates and notifications
- **Performance Monitoring**: Detailed statistics and analytics

## Usage Examples

### Basic Navigation

```kotlin
// Initialize the navigation controller
val navController = findNavController()
val navigationFlowController = NavigationFlowControllerImpl(dispatcher, eventBus)
navigationFlowController.initialize(navController)

// Navigate with intelligent routing
val result = navigationFlowController.navigate("game_library")
when (result) {
    is NavigationResult.Success -> {
        // Navigation successful
        println("Navigated to: ${result.destination}")
    }
    is NavigationResult.Failure -> {
        // Navigation blocked or failed
        println("Failed: ${result.errorMessage}")
        // Show alternatives if available
        if (result.hasAlternatives) {
            showAlternativeDestinations(result.suggestedAlternatives)
        }
    }
    is NavigationResult.Cancelled -> {
        // Navigation was cancelled
        println("Navigation cancelled: ${result.reason}")
    }
}
```

### Using Enhanced Navigation Actions

```kotlin
// Create enhanced navigation actions that integrate with the flow controller
val enhancedActions = NavigationIntegration(
    navigationFlowController,
    dispatcher
).createEnhancedNavigationActions(navController)

// Use like regular NavigationActions but with intelligent routing
enhancedActions.navigateToGameLibrary()
enhancedActions.navigateToGame("game_id_123")
enhancedActions.navigateToProfile()
```

### Registering Navigation Rules

```kotlin
// Create and register a permission rule
val permissionRule = PermissionRule(
    name = "Premium Content Access",
    description = "Controls access to premium game content"
).apply {
    addGlobalPermissionRequirement(
        SimplePermissionRequirement(
            description = "Require premium subscription",
            requiredPermissions = setOf("premium_access"),
            alternativeDestination = NavigationDestinations.GAME_LIBRARY
        )
    )
}

navigationFlowController.registerRule(permissionRule)

// Create and register a feature gate rule
val featureGateRule = FeatureGateRule(
    name = "Beta Features",
    description = "Controls access to beta features"
).apply {
    addFeatureGate("new_game_modes",
        PercentageFeatureGate(
            featureFlag = "new_game_modes",
            percentage = 25.0, // 25% rollout
            description = "New game modes feature gate"
        )
    )
}

navigationFlowController.registerRule(featureGateRule)
```

### Advanced Context Usage

```kotlin
// Create navigation context with full information
val context = navigationContext {
    currentDestination = NavigationDestinations.HOME
    targetDestination = NavigationDestinations.GAME_PLAYER
    userId = "user_123"
    argument("gameId", "game_456")
    permission("premium_access")
    featureFlag("advanced_gameplay")
    userPreference("difficulty", "hard")
}

// Navigate with context
val result = navigationFlowController.navigateWithContext(context)

// Check if navigation is allowed
val canNavigate = navigationFlowController.canNavigateTo(
    NavigationDestinations.PREMIUM_CONTENT,
    context
)

// Get alternative destinations if blocked
val alternatives = navigationFlowController.getAlternativeDestinations(
    NavigationDestinations.PREMIUM_CONTENT,
    context
)
```

## Integration Guide

### Dependency Injection Setup

The NavigationModule provides all necessary dependencies:

```kotlin
// In your Application class or main module
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNavigationEventBus(): NavigationEventBus = NavigationEventBus()

    @Provides
    @Singleton
    fun provideNavigationFlowController(
        @NavigationDispatcher dispatcher: CoroutineDispatcher,
        eventBus: NavigationEventBus
    ): NavigationFlowController = NavigationFlowControllerImpl(dispatcher, eventBus)
}
```

### Activity Integration

```kotlin
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationFlowController: NavigationFlowController

    @Inject
    lateinit var navigationIntegration: NavigationIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize navigation controller
        lifecycleScope.launch {
            val navController = findNavController(R.id.nav_host_fragment)
            navigationFlowController.initialize(navController)

            // Preload common navigation data
            navigationIntegration.preloadNavigationData()
        }
    }
}
```

### ViewModel Integration

```kotlin
class MainViewModel @Inject constructor(
    private val navigationFlowController: NavigationFlowController
) : ViewModel() {

    fun navigateToGameLibrary() {
        viewModelScope.launch {
            val result = navigationFlowController.navigate(NavigationDestinations.GAME_LIBRARY)
            // Handle result
        }
    }

    fun checkNavigationAvailability(destination: String): Boolean {
        return runBlocking {
            navigationFlowController.canNavigateTo(destination)
        }
    }
}
```

## Rule Configuration

### Conditional Rules

```kotlin
val gameAccessRule = ConditionalRule(
    name = "Game Access Control",
    description = "Controls access to different game categories"
).apply {
    // Only allow access during certain hours
    addCondition(AppStateCondition(
        description = "Check if it's gaming hours",
        minBatteryLevel = 20,
        requiresNetwork = true
    ))

    // Check device compatibility
    addCondition(DeviceCondition(
        description = "Check device capabilities",
        minScreenWidth = 800,
        maxMemoryUsage = 1024 * 1024 * 1024 // 1GB
    ))
}
```

### Feature Gate Rules

```kotlin
val experimentalFeatureRule = FeatureGateRule(
    name = "Experimental Features",
    description = "Controls experimental feature access"
).apply {
    // 10% rollout for new users
    addFeatureGate("experimental_ui",
        PercentageFeatureGate(
            featureFlag = "experimental_ui",
            percentage = 10.0,
            userIdBased = true
        )
    )

    // Segment-based rollout
    addFeatureGate("premium_features",
        UserSegmentFeatureGate(
            featureFlag = "premium_features",
            allowedSegments = setOf("premium_users", "beta_testers"),
            userSegmentProvider = { context ->
                // Custom logic to determine user segment
                determineUserSegment(context.userId)
            }
        )
    )
}
```

### Permission Rules

```kotlin
val securityRule = PermissionRule(
    name = "Security Access Control",
    description = "Enforces security permissions"
).apply {
    // Hierarchical permissions
    addPermissionRequirement(NavigationDestinations.ADMIN_PANEL,
        HierarchicalPermissionRequirement(
            description = "Require admin or higher",
            permissionHierarchy = mapOf(
                "super_admin" to setOf("admin"),
                "admin" to setOf("moderator"),
                "moderator" to setOf("user")
            ),
            requiredPermission = "admin"
        )
    )

    // Time-based permissions
    addPermissionRequirement(NavigationDestinations.LIVE_EVENTS,
        TimeBasedPermissionRequirement(
            description = "Access during event hours only",
            requiredPermission = "event_access",
            validTimeRanges = listOf(
                TimeRange(18, 0, 22, 0), // 6 PM to 10 PM
                TimeRange(10, 0, 14, 0)  // 10 AM to 2 PM
            )
        )
    )
}
```

## Performance Considerations

### Caching Strategy
- Rule evaluation results are cached for 30 seconds
- Route calculations are cached for optimal performance
- Cache invalidation based on context changes

### Memory Management
- Automatic cleanup of old cache entries
- Configurable cache sizes and durations
- Memory usage monitoring and optimization

### Performance Monitoring
- Detailed execution time tracking
- Rule evaluation performance metrics
- Navigation success rate monitoring

## Testing

### Unit Tests
```kotlin
@Test
fun `navigation should succeed for authorized user`() = runTest {
    // Given
    val rule = PermissionRule().apply {
        addPermissionRequirement("destination",
            SimplePermissionRequirement(
                requiredPermissions = setOf("access"),
                requireAll = true
            )
        )
    }

    val context = NavigationContext(
        targetDestination = "destination",
        permissions = setOf("access")
    )

    // When
    val result = rule.evaluate(context)

    // Then
    assertTrue(result.passed)
}
```

### Integration Tests
```kotlin
@Test
fun `full navigation flow should work end-to-end`() = runTest {
    // Given
    navigationFlowController.initialize(mockNavController)
    val context = NavigationContext(
        targetDestination = NavigationDestinations.GAME_LIBRARY,
        permissions = setOf("basic_access"),
        featureFlags = setOf("game_library_enabled")
    )

    // When
    val result = navigationFlowController.navigateWithContext(context)

    // Then
    assertTrue(result.isSuccess())
    verify(mockNavController).navigate(eq(NavigationDestinations.GAME_LIBRARY), any())
}
```

## Best Practices

### Rule Design
1. **Keep rules simple and focused**: Each rule should have a single responsibility
2. **Use descriptive names**: Rule names should clearly indicate their purpose
3. **Provide alternatives**: Always suggest alternative destinations when blocking navigation
4. **Consider performance**: Expensive operations should be cached or optimized

### Context Management
1. **Keep context lightweight**: Only include necessary information
2. **Update context appropriately**: Use context updaters for incremental changes
3. **Handle context expiration**: Consider when context information becomes stale

### Error Handling
1. **Graceful degradation**: Fall back to basic navigation when intelligent routing fails
2. **User feedback**: Provide clear error messages and alternative options
3. **Logging**: Comprehensive logging for debugging and monitoring

## Migration Guide

### From Basic NavigationActions

```kotlin
// Before
val navigationActions = NavigationActions(navController)
navigationActions.navigateToGameLibrary()

// After
val enhancedActions = navigationIntegration.createEnhancedNavigationActions(navController)
enhancedActions.navigateToGameLibrary() // Same API, enhanced functionality
```

### Adding Intelligence to Existing Navigation

```kotlin
// Wrap existing navigation logic
class GameViewModel @Inject constructor(
    private val navigationFlowController: NavigationFlowController
) {

    fun launchGame(gameId: String) {
        viewModelScope.launch {
            val context = NavigationContext(
                currentDestination = "game_details/$gameId",
                targetDestination = NavigationDestinations.GAME_PLAYER,
                arguments = mapOf("gameId" to gameId),
                permissions = getUserPermissions(),
                featureFlags = getActiveFeatureFlags()
            )

            val result = navigationFlowController.navigateWithContext(context)

            if (result.isSuccess()) {
                // Proceed with game launch
                startGame(gameId)
            } else {
                // Handle navigation failure
                handleNavigationFailure(result)
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Navigation blocked unexpectedly**
   - Check rule evaluation logs
   - Verify user permissions and feature flags
   - Review rule priorities and conditions

2. **Performance issues**
   - Monitor cache hit rates
   - Check rule evaluation times
   - Consider optimizing complex conditions

3. **Memory usage**
   - Monitor cache sizes
   - Check for memory leaks in rule implementations
   - Consider reducing cache durations

### Debug Information

Enable debug logging to get detailed information:

```kotlin
// Enable detailed logging
Timber.plant(DebugTree())

// The navigation system will log:
// - Rule evaluation results
// - Route calculation details
// - Performance metrics
// - Cache operations
```

## API Reference

### NavigationFlowController

Main interface for navigation management:

- `initialize(navController, context)`: Initialize the controller
- `navigate(destination, arguments, options, context)`: Navigate to destination
- `registerRule(rule)`: Register a navigation rule
- `calculateOptimalRoute(destination, context)`: Calculate optimal route
- `canNavigateTo(destination, context)`: Check navigation eligibility

### NavigationContext

Context information for navigation:

- `currentDestination`: Current location in navigation graph
- `targetDestination`: Desired destination
- `permissions`: User permissions
- `featureFlags`: Active feature flags
- `deviceContext`: Device information
- `appState`: Application state

### NavigationRule

Base interface for navigation rules:

- `evaluate(context)`: Evaluate rule against context
- `shouldEvaluate(context)`: Check if rule applies to context
- `getAlternativeDestinations(context)`: Suggest alternatives when blocking

## Contributing

When adding new features:

1. **Follow existing patterns**: Use established interfaces and implementations
2. **Add comprehensive tests**: Cover success and failure scenarios
3. **Update documentation**: Keep README and code comments current
4. **Consider performance**: Profile new features for performance impact

## License

This navigation system is part of the Roshni Games platform and follows the same licensing terms.