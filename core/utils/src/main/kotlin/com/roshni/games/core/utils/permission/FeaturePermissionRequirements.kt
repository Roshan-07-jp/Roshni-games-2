package com.roshni.games.core.utils.permission

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Defines permission requirements for a specific feature
 */
data class FeaturePermissionRequirements(
    val featureId: String,
    val featureName: String,
    val requiredPermissions: Set<RuntimePermission>,
    val optionalPermissions: Set<RuntimePermission> = emptySet(),
    val rationale: FeatureRationale? = null,
    val fallbackStrategy: PermissionFallback? = null,
    val priority: FeaturePriority = FeaturePriority.NORMAL,
    val isCritical: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Get all permissions (required + optional)
     */
    fun getAllPermissions(): Set<RuntimePermission> {
        return requiredPermissions + optionalPermissions
    }

    /**
     * Check if this feature can work with current permissions
     */
    fun canFeatureWork(grantedPermissions: Set<RuntimePermission>): Boolean {
        return requiredPermissions.all { grantedPermissions.contains(it) }
    }

    /**
     * Get missing required permissions
     */
    fun getMissingPermissions(grantedPermissions: Set<RuntimePermission>): Set<RuntimePermission> {
        return requiredPermissions.filter { !grantedPermissions.contains(it) }.toSet()
    }

    /**
     * Get missing optional permissions
     */
    fun getMissingOptionalPermissions(grantedPermissions: Set<RuntimePermission>): Set<RuntimePermission> {
        return optionalPermissions.filter { !grantedPermissions.contains(it) }.toSet()
    }

    /**
     * Create a copy with updated permissions
     */
    fun withPermissions(
        required: Set<RuntimePermission> = requiredPermissions,
        optional: Set<RuntimePermission> = optionalPermissions
    ): FeaturePermissionRequirements {
        return copy(requiredPermissions = required, optionalPermissions = optional)
    }

    /**
     * Create a copy with updated rationale
     */
    fun withRationale(rationale: FeatureRationale): FeaturePermissionRequirements {
        return copy(rationale = rationale)
    }

    /**
     * Create a copy with updated fallback strategy
     */
    fun withFallbackStrategy(fallbackStrategy: PermissionFallback): FeaturePermissionRequirements {
        return copy(fallbackStrategy = fallbackStrategy)
    }
}

/**
 * Rationale information for explaining why permissions are needed
 */
data class FeatureRationale(
    val title: String,
    val message: String,
    val benefits: List<String> = emptyList(),
    val consequences: List<String> = emptyList(),
    val learnMoreUrl: String? = null,
    val showIcon: Boolean = true,
    val allowSkip: Boolean = false
) {

    fun hasBenefits(): Boolean = benefits.isNotEmpty()
    fun hasConsequences(): Boolean = consequences.isNotEmpty()
    fun hasLearnMore(): Boolean = !learnMoreUrl.isNullOrBlank()
}

/**
 * Priority levels for features
 */
enum class FeaturePriority(val level: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * Result of requesting permissions for a feature
 */
data class FeaturePermissionResult(
    val featureRequirements: FeaturePermissionRequirements,
    val results: Map<RuntimePermission, PermissionResult>,
    val success: Boolean,
    val canFeatureWork: Boolean,
    val fallbackResult: PermissionFallbackResult? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Get all granted permissions
     */
    fun getGrantedPermissions(): Set<RuntimePermission> {
        return results.filterValues { it is PermissionResult.Granted }.keys
    }

    /**
     * Get all denied permissions
     */
    fun getDeniedPermissions(): Set<RuntimePermission> {
        return results.filterValues { it is PermissionResult.Denied }.keys
    }

    /**
     * Get permissions that resulted in errors
     */
    fun getErrorPermissions(): Set<RuntimePermission> {
        return results.filterValues { it is PermissionResult.Error }.keys
    }

    /**
     * Check if all required permissions were granted
     */
    fun allRequiredGranted(): Boolean {
        return featureRequirements.requiredPermissions.all { permission ->
            results[permission] is PermissionResult.Granted
        }
    }

    /**
     * Check if any critical permissions were denied
     */
    fun hasCriticalDenials(): Boolean {
        return featureRequirements.requiredPermissions.any { permission ->
            val result = results[permission]
            result is PermissionResult.Denied && permission.isCritical
        }
    }
}

/**
 * Manager for handling feature-based permission requirements
 */
class FeaturePermissionManager {

    private val _featureRequirements = MutableStateFlow<Map<String, FeaturePermissionRequirements>>(emptyMap())
    val featureRequirements: StateFlow<Map<String, FeaturePermissionRequirements>> = _featureRequirements.asStateFlow()

    private val _permissionStatus = MutableStateFlow<Map<RuntimePermission, PermissionStatus>>(emptyMap())
    val permissionStatus: StateFlow<Map<RuntimePermission, PermissionStatus>> = _permissionStatus.asStateFlow()

    /**
     * Register feature permission requirements
     */
    fun registerFeatureRequirements(requirements: FeaturePermissionRequirements) {
        val current = _featureRequirements.value.toMutableMap()
        current[requirements.featureId] = requirements
        _featureRequirements.value = current
    }

    /**
     * Register multiple feature permission requirements
     */
    fun registerFeatureRequirements(requirements: List<FeaturePermissionRequirements>) {
        val current = _featureRequirements.value.toMutableMap()
        requirements.forEach { current[it.featureId] = it }
        _featureRequirements.value = current
    }

    /**
     * Unregister feature permission requirements
     */
    fun unregisterFeatureRequirements(featureId: String) {
        val current = _featureRequirements.value.toMutableMap()
        current.remove(featureId)
        _featureRequirements.value = current
    }

    /**
     * Get requirements for a specific feature
     */
    fun getFeatureRequirements(featureId: String): FeaturePermissionRequirements? {
        return _featureRequirements.value[featureId]
    }

    /**
     * Update permission status
     */
    fun updatePermissionStatus(permission: RuntimePermission, status: PermissionStatus) {
        val current = _permissionStatus.value.toMutableMap()
        current[permission] = status
        _permissionStatus.value = current
    }

    /**
     * Update multiple permission statuses
     */
    fun updatePermissionStatuses(statuses: Map<RuntimePermission, PermissionStatus>) {
        val current = _permissionStatus.value.toMutableMap()
        current.putAll(statuses)
        _permissionStatus.value = current
    }

    /**
     * Get all features that can work with current permissions
     */
    fun getWorkingFeatures(): Set<String> {
        val currentPermissions = _permissionStatus.value.filterValues { it.isGranted() }.keys
        return _featureRequirements.value.filterValues { requirement ->
            requirement.canFeatureWork(currentPermissions)
        }.keys
    }

    /**
     * Get all features that cannot work due to missing permissions
     */
    fun getNonWorkingFeatures(): Set<String> {
        val currentPermissions = _permissionStatus.value.filterValues { it.isGranted() }.keys
        return _featureRequirements.value.filterValues { requirement ->
            !requirement.canFeatureWork(currentPermissions)
        }.keys
    }

    /**
     * Get features by priority
     */
    fun getFeaturesByPriority(): Map<FeaturePriority, Set<String>> {
        return _featureRequirements.value.values.groupBy { it.priority }.mapValues { (_, features) ->
            features.map { it.featureId }.toSet()
        }
    }

    /**
     * Get critical features that are not working
     */
    fun getCriticalNonWorkingFeatures(): Set<String> {
        val currentPermissions = _permissionStatus.value.filterValues { it.isGranted() }.keys
        return _featureRequirements.value.filterValues { requirement ->
            requirement.isCritical && !requirement.canFeatureWork(currentPermissions)
        }.keys
    }

    /**
     * Clear all registered requirements
     */
    fun clearAllRequirements() {
        _featureRequirements.value = emptyMap()
    }

    /**
     * Clear all permission statuses
     */
    fun clearAllPermissionStatuses() {
        _permissionStatus.value = emptyMap()
    }

    /**
     * Reset the manager to initial state
     */
    fun reset() {
        clearAllRequirements()
        clearAllPermissionStatuses()
    }
}

/**
 * Builder for creating feature permission requirements
 */
class FeaturePermissionRequirementsBuilder(private val featureId: String) {

    private var featureName: String = ""
    private var requiredPermissions: Set<RuntimePermission> = emptySet()
    private var optionalPermissions: Set<RuntimePermission> = emptySet()
    private var rationale: FeatureRationale? = null
    private var fallbackStrategy: PermissionFallback? = null
    private var priority: FeaturePriority = FeaturePriority.NORMAL
    private var isCritical: Boolean = false
    private var metadata: Map<String, Any> = emptyMap()

    fun setFeatureName(name: String) {
        featureName = name
    }

    fun setRequiredPermissions(permissions: Set<RuntimePermission>) {
        requiredPermissions = permissions
    }

    fun addRequiredPermission(permission: RuntimePermission) {
        requiredPermissions = requiredPermissions + permission
    }

    fun setOptionalPermissions(permissions: Set<RuntimePermission>) {
        optionalPermissions = permissions
    }

    fun addOptionalPermission(permission: RuntimePermission) {
        optionalPermissions = optionalPermissions + permission
    }

    fun setRationale(rationale: FeatureRationale) {
        this.rationale = rationale
    }

    fun setFallbackStrategy(fallback: PermissionFallback) {
        this.fallbackStrategy = fallback
    }

    fun setPriority(priority: FeaturePriority) {
        this.priority = priority
    }

    fun setCritical(isCritical: Boolean) {
        this.isCritical = isCritical
    }

    fun setMetadata(metadata: Map<String, Any>) {
        this.metadata = metadata
    }

    fun build(): FeaturePermissionRequirements {
        require(featureName.isNotBlank()) { "Feature name cannot be blank" }
        require(requiredPermissions.isNotEmpty() || optionalPermissions.isNotEmpty()) {
            "Feature must have at least one required or optional permission"
        }

        return FeaturePermissionRequirements(
            featureId = featureId,
            featureName = featureName,
            requiredPermissions = requiredPermissions,
            optionalPermissions = optionalPermissions,
            rationale = rationale,
            fallbackStrategy = fallbackStrategy,
            priority = priority,
            isCritical = isCritical,
            metadata = metadata
        )
    }
}

/**
 * DSL function for building feature permission requirements
 */
fun featurePermissionRequirements(featureId: String, block: FeaturePermissionRequirementsBuilder.() -> Unit): FeaturePermissionRequirements {
    return FeaturePermissionRequirementsBuilder(featureId).apply(block).build()
}