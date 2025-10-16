package com.roshni.games.core.navigation.controller

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.roshni.games.core.navigation.NavigationDestinations
import com.roshni.games.core.navigation.model.NavigationContext
import com.roshni.games.core.navigation.model.NavigationEvent
import com.roshni.games.core.navigation.model.NavigationEventBus
import com.roshni.games.core.navigation.model.NavigationResult
import com.roshni.games.core.navigation.rules.ConditionalRule
import com.roshni.games.core.navigation.rules.FeatureGateRule
import com.roshni.games.core.navigation.rules.PermissionRule
import com.roshni.games.core.navigation.rules.SimpleFeatureGate
import com.roshni.games.core.navigation.rules.SimplePermissionRequirement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class NavigationFlowControllerTest {

    @Mock
    private lateinit var mockNavController: NavController

    private lateinit var navigationFlowController: NavigationFlowController
    private lateinit var eventBus: NavigationEventBus
    private lateinit var testDispatcher: CoroutineDispatcher

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = StandardTestDispatcher()
        eventBus = NavigationEventBus()
        navigationFlowController = NavigationFlowControllerImpl(testDispatcher, eventBus)
    }

    @Test
    fun `initialize should set up controller correctly`() = runTest {
        // Given
        val initialContext = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.HOME
        )

        // When
        val result = navigationFlowController.initialize(mockNavController, initialContext)

        // Then
        assertTrue(result)
        val state = navigationFlowController.navigationState.value
        assertTrue(state.isInitialized)
        assertEquals(NavigationDestinations.HOME, state.currentDestination)
    }

    @Test
    fun `navigate should return success for valid destination`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)
        whenever(mockNavController.navigate(any<String>(), any<NavOptions>())).then { }

        // When
        val result = navigationFlowController.navigate(NavigationDestinations.GAME_LIBRARY)

        // Then
        assertTrue(result.isSuccess())
        val successResult = result.getSuccessOrNull()
        assertNotNull(successResult)
        assertEquals(NavigationDestinations.GAME_LIBRARY, successResult.destination)
    }

    @Test
    fun `navigate should return failure for blocked destination`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        // Register a rule that blocks navigation
        val blockingRule = PermissionRule(
            name = "Block All Navigation",
            description = "Blocks all navigation for testing"
        ).apply {
            addGlobalPermissionRequirement(
                SimplePermissionRequirement(
                    description = "Require admin permission",
                    requiredPermissions = setOf("admin"),
                    requireAll = true
                )
            )
        }

        navigationFlowController.registerRule(blockingRule)

        // When
        val result = navigationFlowController.navigate(NavigationDestinations.GAME_LIBRARY)

        // Then
        assertTrue(result.isFailure())
        val failureResult = result.getFailureOrNull()
        assertNotNull(failureResult)
        assertEquals(NavigationResult.NavigationFailureReason.RULE_VIOLATION, failureResult.reason)
    }

    @Test
    fun `registerRule should add rule to controller`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)
        val rule = ConditionalRule(name = "Test Rule", description = "Test rule")

        // When
        val result = navigationFlowController.registerRule(rule)

        // Then
        assertTrue(result)
        val registeredRule = navigationFlowController.getRule(rule.id)
        assertNotNull(registeredRule)
        assertEquals(rule.id, registeredRule.id)
    }

    @Test
    fun `enableRule should enable registered rule`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)
        val rule = ConditionalRule(name = "Test Rule", description = "Test rule")
        navigationFlowController.registerRule(rule)

        // When
        val result = navigationFlowController.enableRule(rule.id)

        // Then
        assertTrue(result)
        val state = navigationFlowController.navigationState.value
        assertEquals(1, state.enabledRuleCount)
    }

    @Test
    fun `calculateOptimalRoute should return route information`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)
        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY
        )

        // When
        val route = navigationFlowController.calculateOptimalRoute(
            NavigationDestinations.GAME_LIBRARY,
            context
        )

        // Then
        assertEquals(NavigationDestinations.GAME_LIBRARY, route.destination)
        assertTrue(route.route.isNotEmpty())
        assertTrue(route.confidence in 0.0..1.0)
        assertTrue(route.estimatedTimeMs >= 0)
    }

    @Test
    fun `canNavigateTo should return true for allowed destination`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)
        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY,
            permissions = setOf("basic_access")
        )

        // When
        val canNavigate = navigationFlowController.canNavigateTo(NavigationDestinations.GAME_LIBRARY, context)

        // Then
        assertTrue(canNavigate)
    }

    @Test
    fun `getAlternativeDestinations should return alternatives for blocked navigation`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        // Register a rule that blocks navigation and provides alternatives
        val ruleWithAlternatives = PermissionRule(
            name = "Rule with Alternatives",
            description = "Blocks navigation but provides alternatives"
        ).apply {
            addPermissionRequirement(NavigationDestinations.GAME_LIBRARY,
                SimplePermissionRequirement(
                    description = "Require premium permission",
                    requiredPermissions = setOf("premium"),
                    alternativeDestination = NavigationDestinations.HOME
                )
            )
        }

        navigationFlowController.registerRule(ruleWithAlternatives)
        val context = NavigationContext(
            currentDestination = NavigationDestinations.HOME,
            targetDestination = NavigationDestinations.GAME_LIBRARY
        )

        // When
        val alternatives = navigationFlowController.getAlternativeDestinations(
            NavigationDestinations.GAME_LIBRARY,
            context
        )

        // Then
        assertTrue(alternatives.isNotEmpty())
    }

    @Test
    fun `validateRules should return validation result`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        val validRule = ConditionalRule(name = "Valid Rule", description = "Valid rule")
        val invalidRule = ConditionalRule(name = "", description = "") // Invalid - empty name

        navigationFlowController.registerRule(validRule)
        navigationFlowController.registerRule(invalidRule)

        // When
        val validation = navigationFlowController.validateRules()

        // Then
        assertFalse(validation.isValid)
        assertTrue(validation.errors.isNotEmpty())
    }

    @Test
    fun `getStatistics should return navigation statistics`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        // Perform some navigation
        navigationFlowController.navigate(NavigationDestinations.GAME_LIBRARY)
        navigationFlowController.navigate(NavigationDestinations.PROFILE)

        // When
        val statistics = navigationFlowController.getStatistics()

        // Then
        assertTrue(statistics.totalNavigations >= 0)
        assertTrue(statistics.ruleStatistics.isNotEmpty())
    }

    @Test
    fun `observeEvents should emit navigation events`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        var eventReceived: NavigationEvent? = null

        // Collect events
        val job = kotlinx.coroutines.launch {
            navigationFlowController.observeEvents().collect { event ->
                eventReceived = event
            }
        }

        // When
        navigationFlowController.navigate(NavigationDestinations.GAME_LIBRARY)

        // Small delay to allow event processing
        kotlinx.coroutines.delay(100)

        // Then
        assertNotNull(eventReceived)

        job.cancel()
    }

    @Test
    fun `shutdown should cleanup resources`() = runTest {
        // Given
        navigationFlowController.initialize(mockNavController)

        // When
        navigationFlowController.shutdown()

        // Then
        val state = navigationFlowController.navigationState.value
        assertTrue(state.isShuttingDown)
    }
}