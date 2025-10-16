package com.roshni.games.core.navigation.model

import com.roshni.games.core.navigation.NavigationDestinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import assertFalse
import assertNotNull
import assertTrue

@ExperimentalCoroutinesApi
class NavigationModelTest {

    private lateinit var testDispatcher: kotlinx.coroutines.CoroutineDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
    }

    @Test
    fun `NavigationContext should store context information correctly`() {
        // Given
        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            userId = "test_user_123",
            sessionId = "session_456",
            arguments = mapOf("gameId" to "game_789"),
            permissions = setOf("basic_access", "premium_features"),
            featureFlags = setOf("new_ui", "beta_features"),
            userPreferences = mapOf("theme" to "dark", "language" to "en")
        )

        // Then
        assertEquals(NavigationDestinations.HOME, context.currentDestination)
        assertEquals(NavigationDestinations.GAME_LIBRARY, context.targetDestination)
        assertEquals("test_user_123", context.userId)
        assertEquals("session_456", context.sessionId)
        assertEquals("game_789", context.getArgument("gameId"))
        assertTrue(context.hasPermission("basic_access"))
        assertTrue(context.hasPermission("premium_features"))
        assertFalse(context.hasPermission("admin_access"))
        assertTrue(context.isFeatureEnabled("new_ui"))
        assertTrue(context.isFeatureEnabled("beta_features"))
        assertFalse(context.isFeatureEnabled("old_feature"))
        assertEquals("dark", context.getUserPreference("theme"))
        assertEquals("en", context.getUserPreference("language"))
        assertNotNull(context.contextId)
        assertTrue(context.timestamp > 0)
    }

    @Test
    fun `NavigationContext builder should create context correctly`() {
        // Given
        val context = navigationContext {
            currentDestination = NavigationDestinations.HOME
            targetDestination = NavigationDestinations.PROFILE
            userId = "builder_user"
            argument("userId", "builder_user")
            permission("read_profile")
            permission("edit_profile")
            featureFlag("profile_v2")
            userPreference("notifications", true)
        }

        // Then
        assertEquals(NavigationDestinations.HOME, context.currentDestination)
        assertEquals(NavigationDestinations.PROFILE, context.targetDestination)
        assertEquals("builder_user", context.userId)
        assertEquals("builder_user", context.getArgument("userId"))
        assertTrue(context.hasPermission("read_profile"))
        assertTrue(context.hasPermission("edit_profile"))
        assertTrue(context.isFeatureEnabled("profile_v2"))
        assertEquals(true, context.getUserPreference("notifications"))
    }

    @Test
    fun `NavigationContext withArguments should create new context with additional arguments`() {
        // Given
        val originalContext = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            arguments = mapOf("existing" to "value")
        )

        // When
        val updatedContext = originalContext.withArguments("new" to "new_value", "another" to 123)

        // Then
        assertEquals("value", updatedContext.getArgument("existing"))
        assertEquals("new_value", updatedContext.getArgument("new"))
        assertEquals(123, updatedContext.getArgument("another"))
        assertEquals(NavigationDestinations.HOME, updatedContext.currentDestination) // Unchanged
    }

    @Test
    fun `NavigationContext withPermissions should create new context with additional permissions`() {
        // Given
        val originalContext = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            permissions = setOf("basic")
        )

        // When
        val updatedContext = originalContext.withPermissions("premium", "admin")

        // Then
        assertTrue(updatedContext.hasPermission("basic"))
        assertTrue(updatedContext.hasPermission("premium"))
        assertTrue(updatedContext.hasPermission("admin"))
        assertEquals(NavigationDestinations.HOME, updatedContext.currentDestination) // Unchanged
    }

    @Test
    fun `NavigationContext withFeatureFlags should create new context with additional feature flags`() {
        // Given
        val originalContext = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            featureFlags = setOf("feature1")
        )

        // When
        val updatedContext = originalContext.withFeatureFlags("feature2", "feature3")

        // Then
        assertTrue(updatedContext.isFeatureEnabled("feature1"))
        assertTrue(updatedContext.isFeatureEnabled("feature2"))
        assertTrue(updatedContext.isFeatureEnabled("feature3"))
        assertEquals(NavigationDestinations.HOME, updatedContext.currentDestination) // Unchanged
    }

    @Test
    fun `NavigationResult Success should store success information correctly`() {
        // Given
        val successResult = NavigationResult.Success(
            destination = NavigationDestinations.GAME_LIBRARY,
            arguments = mapOf("gameId" to "test_game"),
            navigationTimeMs = 150L,
            actualRoute = "home -> game_library",
            appliedRules = listOf("permission_rule", "feature_gate_rule"),
            timestamp = 1234567890L,
            resultId = "test_result_123"
        )

        // Then
        assertTrue(successResult.isSuccess())
        assertFalse(successResult.isFailure())
        assertFalse(successResult.isCancelled())
        assertEquals(NavigationDestinations.GAME_LIBRARY, successResult.destination)
        assertEquals(150L, successResult.navigationTimeMs)
        assertEquals("home -> game_library", successResult.actualRoute)
        assertEquals(2, successResult.appliedRules.size)
        assertEquals("test_result_123", successResult.resultId)
        assertFalse(successResult.isInstantaneous) // 150ms is not instantaneous
        assertTrue(successResult.hasAppliedRules)
        assertEquals(0.6, successResult.getEfficiencyScore(), 0.1) // Should be "Fair" efficiency
    }

    @Test
    fun `NavigationResult Failure should store failure information correctly`() {
        // Given
        val failureResult = NavigationResult.Failure(
            attemptedDestination = NavigationDestinations.GAME_LIBRARY,
            reason = NavigationResult.NavigationFailureReason.PERMISSION_DENIED,
            errorMessage = "User lacks required permissions",
            suggestedAlternatives = listOf(NavigationDestinations.HOME, NavigationDestinations.PROFILE),
            blockingRules = listOf("premium_permission_rule"),
            timeToFailureMs = 50L,
            timestamp = 1234567890L,
            resultId = "failure_result_123"
        )

        // Then
        assertTrue(failureResult.isFailure())
        assertFalse(failureResult.isSuccess())
        assertFalse(failureResult.isCancelled())
        assertEquals(NavigationDestinations.GAME_LIBRARY, failureResult.attemptedDestination)
        assertEquals(NavigationResult.NavigationFailureReason.PERMISSION_DENIED, failureResult.reason)
        assertEquals("User lacks required permissions", failureResult.errorMessage)
        assertEquals(2, failureResult.suggestedAlternatives.size)
        assertEquals(1, failureResult.blockingRules.size)
        assertTrue(failureResult.isBlockedByRules)
        assertTrue(failureResult.hasAlternatives)
        assertEquals(NavigationResult.FailureSeverity.HIGH, failureResult.getSeverityLevel())
    }

    @Test
    fun `NavigationResult Cancelled should store cancellation information correctly`() {
        // Given
        val cancelledResult = NavigationResult.Cancelled(
            destination = NavigationDestinations.GAME_LIBRARY,
            reason = NavigationResult.NavigationCancellationReason.USER_CANCELLED,
            timeSpentMs = 200L,
            timestamp = 1234567890L,
            resultId = "cancelled_result_123"
        )

        // Then
        assertTrue(cancelledResult.isCancelled())
        assertFalse(cancelledResult.isSuccess())
        assertFalse(cancelledResult.isFailure())
        assertEquals(NavigationDestinations.GAME_LIBRARY, cancelledResult.destination)
        assertEquals(NavigationResult.NavigationCancellationReason.USER_CANCELLED, cancelledResult.reason)
        assertEquals(200L, cancelledResult.timeSpentMs)
        assertEquals("cancelled_result_123", cancelledResult.resultId)
    }

    @Test
    fun `NavigationResultUtils should provide correct utility functions`() {
        // Given
        val successResult = NavigationResult.Success(
            destination = NavigationDestinations.HOME,
            navigationTimeMs = 100L
        )

        val failureResult = NavigationResult.Failure(
            attemptedDestination = NavigationDestinations.GAME_LIBRARY,
            reason = NavigationResult.NavigationFailureReason.NETWORK_ERROR,
            errorMessage = "Network error",
            timeToFailureMs = 50L
        )

        val cancelledResult = NavigationResult.Cancelled(
            destination = NavigationDestinations.PROFILE,
            reason = NavigationResult.NavigationCancellationReason.TIMEOUT,
            timeSpentMs = 300L
        )

        // Test utility functions
        assertTrue(NavigationResultUtils.isSuccess(successResult))
        assertFalse(NavigationResultUtils.isSuccess(failureResult))
        assertFalse(NavigationResultUtils.isSuccess(cancelledResult))

        assertTrue(NavigationResultUtils.isFailure(failureResult))
        assertFalse(NavigationResultUtils.isFailure(successResult))
        assertFalse(NavigationResultUtils.isFailure(cancelledResult))

        assertTrue(NavigationResultUtils.isCancelled(cancelledResult))
        assertFalse(NavigationResultUtils.isCancelled(successResult))
        assertFalse(NavigationResultUtils.isCancelled(failureResult))

        // Test getOrNull functions
        assertNotNull(NavigationResultUtils.getSuccessOrNull(successResult))
        assertNotNull(NavigationResultUtils.getFailureOrNull(failureResult))
        assertNotNull(NavigationResultUtils.getCancelledOrNull(cancelledResult))

        assertEquals(successResult, NavigationResultUtils.getSuccessOrNull(failureResult))
        assertEquals(failureResult, NavigationResultUtils.getFailureOrNull(successResult))
        assertEquals(cancelledResult, NavigationResultUtils.getCancelledOrNull(successResult))
    }

    @Test
    fun `NavigationEvent should store event information correctly`() {
        // Given
        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY
        )

        val startedEvent = NavigationEvent.NavigationStarted(
            context = context,
            timestamp = 1234567890L,
            eventId = "start_event_123"
        )

        val completedEvent = NavigationEvent.NavigationCompleted(
            result = NavigationResult.Success(
                destination = NavigationDestinations.GAME_LIBRARY,
                navigationTimeMs = 150L
            ),
            timestamp = 1234567900L,
            eventId = "complete_event_123"
        )

        // Then
        assertEquals("NAVIGATION_STARTED", NavigationEventUtils.getEventType(startedEvent))
        assertEquals("NAVIGATION_COMPLETED", NavigationEventUtils.getEventType(completedEvent))

        assertTrue(NavigationEventUtils.isSuccessEvent(completedEvent))
        assertFalse(NavigationEventUtils.isSuccessEvent(startedEvent))

        assertNotNull(NavigationEventUtils.getContext(startedEvent))
        assertEquals(context, NavigationEventUtils.getContext(startedEvent))

        assertEquals(1234567890L, NavigationEventUtils.getTimestamp(startedEvent))
        assertEquals(1234567900L, NavigationEventUtils.getTimestamp(completedEvent))
    }

    @Test
    fun `NavigationEventBus should emit and collect events correctly`() = runTest {
        // Given
        val eventBus = NavigationEventBus()
        var receivedEvent: NavigationEvent? = null

        // Collect events
        val job = kotlinx.coroutines.launch {
            eventBus.events.collect { event ->
                receivedEvent = event
            }
        }

        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY
        )

        // When
        eventBus.emitNavigationStarted(context)

        // Small delay to allow event processing
        kotlinx.coroutines.delay(100)

        // Then
        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is NavigationEvent.NavigationStarted)

        val startedEvent = receivedEvent as NavigationEvent.NavigationStarted
        assertEquals(context, startedEvent.context)
        assertEquals(NavigationDestinations.HOME, startedEvent.context.currentDestination)

        job.cancel()
    }

    @Test
    fun `DeviceContext should store device information correctly`() {
        // Given
        val deviceContext = DeviceContext(
            screenWidth = 1920,
            screenHeight = 1080,
            screenDensity = 2.5f,
            isTablet = true,
            isLandscape = true,
            hasNotch = false,
            navigationBarHeight = 100,
            statusBarHeight = 50,
            isDarkMode = true,
            locale = "en_US"
        )

        // Then
        assertEquals(1920, deviceContext.screenWidth)
        assertEquals(1080, deviceContext.screenHeight)
        assertEquals(2.5f, deviceContext.screenDensity)
        assertTrue(deviceContext.isTablet)
        assertTrue(deviceContext.isLandscape)
        assertFalse(deviceContext.hasNotch)
        assertEquals(100, deviceContext.navigationBarHeight)
        assertEquals(50, deviceContext.statusBarHeight)
        assertTrue(deviceContext.isDarkMode)
        assertEquals("en_US", deviceContext.locale)
    }

    @Test
    fun `AppState should store application state correctly`() {
        // Given
        val appState = AppState(
            isNetworkAvailable = true,
            isUserAuthenticated = true,
            isAppInForeground = true,
            currentGameSession = "game_session_123",
            batteryLevel = 85,
            storageAvailable = 1024000000L, // 1GB
            memoryUsage = 256000L, // 256MB
            isLowEndDevice = false
        )

        // Then
        assertTrue(appState.isNetworkAvailable)
        assertTrue(appState.isUserAuthenticated)
        assertTrue(appState.isAppInForeground)
        assertEquals("game_session_123", appState.currentGameSession)
        assertEquals(85, appState.batteryLevel)
        assertEquals(1024000000L, appState.storageAvailable)
        assertEquals(256000L, appState.memoryUsage)
        assertFalse(appState.isLowEndDevice)
    }

    @Test
    fun `NavigationFailureReason should have correct severity levels`() {
        // Test different failure reasons and their expected severity levels
        val testCases = mapOf(
            NavigationResult.NavigationFailureReason.PERMISSION_DENIED to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.FEATURE_NOT_ENABLED to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.INVALID_DESTINATION to NavigationResult.FailureSeverity.MEDIUM,
            NavigationResult.NavigationFailureReason.NETWORK_ERROR to NavigationResult.FailureSeverity.LOW,
            NavigationResult.NavigationFailureReason.DEVICE_INCOMPATIBLE to NavigationResult.FailureSeverity.LOW,
            NavigationResult.NavigationFailureReason.USER_NOT_AUTHENTICATED to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.PARENTAL_CONTROLS_BLOCKED to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.MAINTENANCE_MODE to NavigationResult.FailureSeverity.MEDIUM,
            NavigationResult.NavigationFailureReason.NAVIGATION_NOT_ALLOWED to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.DEPENDENCY_MISSING to NavigationResult.FailureSeverity.MEDIUM,
            NavigationResult.NavigationFailureReason.RULE_VIOLATION to NavigationResult.FailureSeverity.HIGH,
            NavigationResult.NavigationFailureReason.UNKNOWN_ERROR to NavigationResult.FailureSeverity.MEDIUM
        )

        testCases.forEach { (reason, expectedSeverity) ->
            val failure = NavigationResult.Failure(
                attemptedDestination = NavigationDestinations.HOME,
                reason = reason,
                errorMessage = "Test error",
                timeToFailureMs = 0L
            )

            assertEquals(expectedSeverity, failure.getSeverityLevel())
        }
    }
}