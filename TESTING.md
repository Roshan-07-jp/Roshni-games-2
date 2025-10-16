# Roshni Games Testing Framework

## Overview

This document describes the comprehensive testing framework implemented for Roshni Games, including unit tests, integration tests, UI tests, and performance tests.

## Testing Structure

### 1. Unit Tests
- **Location**: `src/test/kotlin/`
- **Framework**: JUnit 5
- **Mocking**: MockK
- **Coroutines**: Turbine for testing flows

### 2. Integration Tests
- **Location**: `src/androidTest/kotlin/`
- **Framework**: AndroidJUnit4
- **Coverage**: Service integration, database operations, network calls

### 3. UI Tests
- **Location**: `src/androidTest/kotlin/`
- **Framework**: Compose Testing
- **Coverage**: UI components, user interactions, navigation

## Test Categories

### Service Layer Tests
```kotlin
// Example: Game Loader Service Test
@Test
fun `loadModule should successfully load a module`() = runTest {
    // Given
    val moduleId = "test-module"

    // When
    val result = gameLoaderService.loadModule(moduleId).first()

    // Then
    assertTrue(result is GameModuleLoadState.Success)
}
```

### UI Component Tests
```kotlin
// Example: Compose Component Test
@Test
fun gamingButton_shouldDisplayText() {
    // Given
    val buttonText = "Test Button"

    // When
    composeTestRule.setContent {
        RoshniGamesTheme {
            GamingButton(text = buttonText, onClick = {})
        }
    }

    // Then
    composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
}
```

### Database Tests
```kotlin
// Example: Room Database Test
@Test
fun `insert and retrieve game module`() = runTest {
    // Given
    val gameModule = createTestGameModule()

    // When
    gameModuleDao.insertGame(gameModule.toEntity())
    val retrieved = gameModuleDao.getGame(gameModule.id).first()

    // Then
    assertEquals(gameModule.id, retrieved?.id)
}
```

## Testing Utilities

### TestCoroutineRule (JUnit 4)
```kotlin
@get:Rule
val coroutineRule = TestCoroutineRule()

@Test
fun testWithCoroutines() = runTest {
    // Test coroutine-based code
}
```

### TestDispatcherProvider (JUnit 5)
```kotlin
@Before
fun setup() {
    TestDispatcherProvider.setup()
}

@After
fun teardown() {
    TestDispatcherProvider.teardown()
}
```

### MockDataProvider
```kotlin
// Create mock data for testing
val mockModule = MockDataProvider.createMockGameModule()
val mockSyncOperation = MockDataProvider.createMockSyncOperation()
val mockAchievement = MockDataProvider.createMockAchievement()
```

## Running Tests

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Integration Tests
```bash
./gradlew connectedDebugAndroidTest
```

### UI Tests
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.roshni.games.core.designsystem.components.GamingButtonTest
```

### All Tests
```bash
./gradlew check connectedCheck
```

## Test Configuration

### Gradle Configuration
The testing framework is configured in `gradle/testing.gradle.kts` with:
- JUnit 5 platform
- MockK for mocking
- Turbine for coroutine testing
- Robolectric for Android unit tests
- Compose Testing for UI tests

### Test Dependencies
All testing dependencies are centralized in `gradle/libs.versions.toml`:
- `junit` - JUnit 5 framework
- `mockk` - Kotlin mocking library
- `turbine` - Coroutine flow testing
- `robolectric` - Android framework testing
- `androidx-compose-ui-test` - Compose UI testing

## Best Practices

### 1. Test Naming
Use descriptive test names that explain what is being tested:
```kotlin
@Test
fun `loadModule should return success when module exists`()
```

### 2. Arrange-Act-Assert Pattern
```kotlin
@Test
fun testGameModuleLoading() {
    // Arrange - Set up test data and mocks
    val moduleId = "test-module"

    // Act - Execute the code under test
    val result = gameLoaderService.loadModule(moduleId).first()

    // Assert - Verify the results
    assertTrue(result is GameModuleLoadState.Success)
}
```

### 3. Coroutine Testing
Always use `runTest` for testing suspend functions and flows:
```kotlin
@Test
fun testAsyncOperation() = runTest {
    // Test code that uses coroutines
}
```

### 4. Mocking Guidelines
- Mock external dependencies (network, database, file system)
- Use real implementations for business logic
- Verify interactions with mocks using `coVerify`

### 5. UI Testing
- Use `createComposeRule()` for Compose components
- Test user interactions and state changes
- Verify UI state after actions

## Coverage Goals

- **Unit Tests**: > 80% coverage for business logic
- **Integration Tests**: Cover critical user flows
- **UI Tests**: Cover main user interactions
- **Performance Tests**: Monitor memory usage and execution time

## Continuous Integration

Tests are automatically run on:
- Pull requests
- Main branch pushes
- Release builds

## Debugging Tests

### Running Specific Tests
```bash
# Run specific test class
./gradlew :service:game-loader:testDebugUnitTest --tests "*.GameLoaderServiceTest"

# Run specific test method
./gradlew :service:game-loader:testDebugUnitTest --tests "*.GameLoaderServiceTest.testModuleLoading"
```

### Test Logging
Tests include detailed logging for:
- Test execution
- Assertion failures
- Coroutine debugging
- Memory usage

## Performance Testing

### Memory Leak Detection
- Uses LeakCanary for memory leak detection
- Monitors object allocation in tests

### Execution Time Monitoring
- Tests track execution time
- Performance regression detection
- Database query optimization

## Troubleshooting

### Common Issues

1. **Coroutine Test Failures**
   - Ensure proper use of `runTest`
   - Check dispatcher configuration

2. **Compose Test Failures**
   - Verify Compose rule setup
   - Check for proper theme wrapping

3. **MockK Issues**
   - Use `coEvery` for coroutines
   - Verify mock setup before test execution

### Debug Mode
Enable debug logging in tests:
```kotlin
// In test setup
Timber.plant(Timber.DebugTree())
```

## Contributing

When adding new features:
1. Write unit tests for all business logic
2. Add integration tests for critical flows
3. Include UI tests for new screens/components
4. Update this documentation

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [MockK Documentation](https://mockk.io/)
- [Turbine Documentation](https://github.com/cashapp/turbine)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)