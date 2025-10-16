package com.roshni.games.core.navigation.model

import com.roshni.games.core.navigation.NavigationDestinations

/**
 * Context information for navigation operations
 */
data class NavigationContext(
    /**
     * The current destination in the navigation graph
     */
    val currentDestination: String,

    /**
     * The target destination for navigation
     */
    val targetDestination: String,

    /**
     * User ID if available
     */
    val userId: String? = null,

    /**
     * Session ID for tracking navigation flow
     */
    val sessionId: String? = null,

    /**
     * Navigation arguments/parameters
     */
    val arguments: Map<String, Any> = emptyMap(),

    /**
     * User permissions for access control
     */
    val permissions: Set<String> = emptySet(),

    /**
     * Active feature flags that may affect navigation
     */
    val featureFlags: Set<String> = emptySet(),

    /**
     * Current user preferences that may influence routing
     */
    val userPreferences: Map<String, Any> = emptyMap(),

    /**
     * Device context information
     */
    val deviceContext: DeviceContext = DeviceContext(),

    /**
     * Application state information
     */
    val appState: AppState = AppState(),

    /**
     * Timestamp when navigation context was created
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Unique identifier for this navigation context
     */
    val contextId: String = "nav_ctx_${timestamp}_${kotlin.random.Random.nextInt(1000)}"
) {

    /**
     * Check if user has specific permission
     */
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)

    /**
     * Check if feature flag is enabled
     */
    fun isFeatureEnabled(featureFlag: String): Boolean = featureFlags.contains(featureFlag)

    /**
     * Get argument value by key
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgument(key: String): T? = arguments[key] as? T

    /**
     * Get user preference value by key
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getUserPreference(key: String): T? = userPreferences[key] as? T

    /**
     * Create a copy with updated arguments
     */
    fun withArguments(vararg pairs: Pair<String, Any>): NavigationContext {
        val newArguments = arguments.toMutableMap().apply { putAll(pairs) }
        return copy(arguments = newArguments)
    }

    /**
     * Create a copy with additional permissions
     */
    fun withPermissions(vararg permissions: String): NavigationContext {
        val newPermissions = this.permissions.toMutableSet().apply { addAll(permissions) }
        return copy(permissions = newPermissions)
    }

    /**
     * Create a copy with additional feature flags
     */
    fun withFeatureFlags(vararg flags: String): NavigationContext {
        val newFeatureFlags = this.featureFlags.toMutableSet().apply { addAll(flags) }
        return copy(featureFlags = newFeatureFlags)
    }
}

/**
 * Device-specific context information
 */
data class DeviceContext(
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val screenDensity: Float = 0f,
    val isTablet: Boolean = false,
    val isLandscape: Boolean = false,
    val hasNotch: Boolean = false,
    val navigationBarHeight: Int = 0,
    val statusBarHeight: Int = 0,
    val isDarkMode: Boolean = false,
    val locale: String = "en"
)

/**
 * Application state context information
 */
data class AppState(
    val isNetworkAvailable: Boolean = true,
    val isUserAuthenticated: Boolean = false,
    val isAppInForeground: Boolean = true,
    val currentGameSession: String? = null,
    val batteryLevel: Int = 100,
    val storageAvailable: Long = 0L,
    val memoryUsage: Long = 0L,
    val isLowEndDevice: Boolean = false
)

/**
 * Builder for creating NavigationContext instances
 */
class NavigationContextBuilder {
    private var currentDestination: String = NavigationDestinations.HOME
    private var targetDestination: String = NavigationDestinations.HOME
    private var userId: String? = null
    private var sessionId: String? = null
    private var arguments: MutableMap<String, Any> = mutableMapOf()
    private var permissions: MutableSet<String> = mutableSetOf()
    private var featureFlags: MutableSet<String> = mutableSetOf()
    private var userPreferences: MutableMap<String, Any> = mutableMapOf()
    private var deviceContext: DeviceContext = DeviceContext()
    private var appState: AppState = AppState()

    fun currentDestination(destination: String) = apply { currentDestination = destination }
    fun targetDestination(destination: String) = apply { targetDestination = destination }
    fun userId(id: String?) = apply { userId = id }
    fun sessionId(id: String?) = apply { sessionId = id }
    fun argument(key: String, value: Any) = apply { arguments[key] = value }
    fun arguments(vararg pairs: Pair<String, Any>) = apply { arguments.putAll(pairs) }
    fun permission(permission: String) = apply { permissions.add(permission) }
    fun permissions(vararg permissions: String) = apply { this.permissions.addAll(permissions) }
    fun featureFlag(flag: String) = apply { featureFlags.add(flag) }
    fun featureFlags(vararg flags: String) = apply { this.featureFlags.addAll(flags) }
    fun userPreference(key: String, value: Any) = apply { userPreferences[key] = value }
    fun userPreferences(vararg pairs: Pair<String, Any>) = apply { userPreferences.putAll(pairs) }
    fun deviceContext(context: DeviceContext) = apply { deviceContext = context }
    fun appState(state: AppState) = apply { appState = state }

    fun build(): NavigationContext {
        return NavigationContext(
            currentDestination = currentDestination,
            targetDestination = targetDestination,
            userId = userId,
            sessionId = sessionId,
            arguments = arguments,
            permissions = permissions,
            featureFlags = featureFlags,
            userPreferences = userPreferences,
            deviceContext = deviceContext,
            appState = appState
        )
    }
}

/**
 * Convenience function to create NavigationContext using builder pattern
 */
fun navigationContext(block: NavigationContextBuilder.() -> Unit): NavigationContext {
    return NavigationContextBuilder().apply(block).build()
}