package com.roshni.games.core.navigation.rules

import com.roshni.games.core.navigation.model.NavigationContext

/**
 * Rule that controls navigation based on user permissions and access control
 */
class PermissionRule(
    override val id: String = "permission_rule_${System.currentTimeMillis()}",
    override val name: String = "Permission Navigation Rule",
    override val description: String = "Controls navigation based on user permissions",
    override val priority: Int = 90,
    override val categories: Set<RuleCategory> = setOf(RuleCategory.PERMISSION, RuleCategory.SECURITY),
    override val config: RuleConfig = RuleConfig()
) : BaseNavigationRule(id, name, description, priority, categories, config = config) {

    private val permissionRequirements = mutableMapOf<String, PermissionRequirement>()

    /**
     * Add a permission requirement for a specific destination
     */
    fun addPermissionRequirement(destination: String, requirement: PermissionRequirement) {
        permissionRequirements[destination] = requirement
    }

    /**
     * Remove permission requirement for a destination
     */
    fun removePermissionRequirement(destination: String) {
        permissionRequirements.remove(destination)
    }

    /**
     * Add a global permission requirement (applies to all destinations)
     */
    fun addGlobalPermissionRequirement(requirement: PermissionRequirement) {
        permissionRequirements[GLOBAL_PERMISSION_KEY] = requirement
    }

    /**
     * Get all permission requirements
     */
    fun getPermissionRequirements(): Map<String, PermissionRequirement> = permissionRequirements.toMap()

    /**
     * Check if destination has permission requirement
     */
    fun hasPermissionRequirement(destination: String): Boolean {
        return permissionRequirements.containsKey(destination) ||
               permissionRequirements.containsKey(GLOBAL_PERMISSION_KEY)
    }

    override suspend fun performEvaluation(context: NavigationContext): RuleEvaluationResult {
        val targetDestination = context.targetDestination

        // Find applicable permission requirements
        val applicableRequirements = mutableListOf<PermissionRequirement>()

        // Check for destination-specific requirement
        permissionRequirements[targetDestination]?.let { applicableRequirements.add(it) }

        // Check for global requirement
        permissionRequirements[GLOBAL_PERMISSION_KEY]?.let { applicableRequirements.add(it) }

        if (applicableRequirements.isEmpty()) {
            return RuleEvaluationResult(
                passed = true,
                blocked = false,
                reason = "No permission requirements for destination: $targetDestination",
                executionTimeMs = 0,
                metadata = mapOf("permission_requirements_count" to 0)
            )
        }

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<PermissionCheckResult>()
        var allRequirementsPassed = true
        var lastFailedRequirement: PermissionRequirement? = null

        for (requirement in applicableRequirements) {
            val checkResult = requirement.checkPermission(context)
            results.add(checkResult)

            if (!checkResult.passed) {
                allRequirementsPassed = false
                lastFailedRequirement = requirement
                break // Stop at first failed requirement
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        return RuleEvaluationResult(
            passed = allRequirementsPassed,
            blocked = !allRequirementsPassed,
            reason = if (allRequirementsPassed) {
                "All permission requirements satisfied"
            } else {
                "Permission requirement failed: ${lastFailedRequirement?.description ?: "Unknown requirement"}"
            },
            executionTimeMs = executionTime,
            metadata = mapOf(
                "permission_requirements_checked" to applicableRequirements.size,
                "permission_requirements_passed" to results.count { it.passed },
                "permission_requirements_failed" to results.count { !it.passed },
                "permission_check_results" to results.map { mapOf(
                    "requirement" to it.requirement.description,
                    "passed" to it.passed,
                    "reason" to it.reason,
                    "required_permissions" to it.requiredPermissions
                )}
            )
        )
    }

    override suspend fun isRuleApplicable(context: NavigationContext): Boolean {
        // This rule is applicable if there are permission requirements and target destination is not empty
        return permissionRequirements.isNotEmpty() && context.targetDestination.isNotEmpty()
    }

    override suspend fun getRuleAlternatives(context: NavigationContext): List<String> {
        val targetDestination = context.targetDestination

        // Get alternatives from failed permission requirements
        return permissionRequirements.values
            .filter { !it.checkPermission(context).passed }
            .flatMap { it.getAlternativeDestinations(context) }
            .distinct()
    }

    override fun validateRuleConfig(): RuleValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (permissionRequirements.isEmpty()) {
            warnings.add("No permission requirements defined")
        }

        // Validate each permission requirement
        permissionRequirements.forEach { (destination, requirement) ->
            when (val validation = requirement.validate()) {
                is PermissionValidationResult.Invalid -> {
                    errors.add("Invalid permission requirement for '$destination': ${validation.errors}")
                }
                is PermissionValidationResult.ValidWithWarnings -> {
                    warnings.addAll(validation.warnings.map { "Permission requirement '$destination': $it" })
                }
                else -> { /* Valid */ }
            }
        }

        return RuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    companion object {
        private const val GLOBAL_PERMISSION_KEY = "__GLOBAL__"
    }
}

/**
 * Represents a permission requirement for navigation
 */
interface PermissionRequirement {

    /**
     * Description of this permission requirement
     */
    val description: String

    /**
     * Check if the navigation context satisfies this permission requirement
     */
    suspend fun checkPermission(context: NavigationContext): PermissionCheckResult

    /**
     * Get alternative destinations if permission check fails
     */
    suspend fun getAlternativeDestinations(context: NavigationContext): List<String>

    /**
     * Validate this permission requirement
     */
    fun validate(): PermissionValidationResult
}

/**
 * Result of permission check
 */
data class PermissionCheckResult(
    val requirement: PermissionRequirement,
    val passed: Boolean,
    val reason: String,
    val requiredPermissions: Set<String>,
    val executionTimeMs: Long = 0,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Validation result for permission requirements
 */
sealed class PermissionValidationResult {
    object Valid : PermissionValidationResult()
    data class ValidWithWarnings(val warnings: List<String>) : PermissionValidationResult()
    data class Invalid(val errors: List<String>) : PermissionValidationResult()
}

/**
 * Simple permission requirement that checks for specific permissions
 */
data class SimplePermissionRequirement(
    override val description: String = "Simple permission requirement",
    val requiredPermissions: Set<String>,
    val requireAll: Boolean = true, // true = AND logic, false = OR logic
    val alternativeDestination: String? = null
) : PermissionRequirement {

    override suspend fun checkPermission(context: NavigationContext): PermissionCheckResult {
        val startTime = System.currentTimeMillis()

        val userPermissions = context.permissions
        val passed: Boolean
        val reason: String

        if (requireAll) {
            // AND logic - user must have ALL required permissions
            passed = requiredPermissions.all { userPermissions.contains(it) }
            reason = if (passed) {
                "User has all required permissions: $requiredPermissions"
            } else {
                val missingPermissions = requiredPermissions.filter { !userPermissions.contains(it) }
                "User missing required permissions: $missingPermissions"
            }
        } else {
            // OR logic - user must have AT LEAST ONE required permission
            passed = requiredPermissions.any { userPermissions.contains(it) }
            reason = if (passed) {
                val userHas = requiredPermissions.filter { userPermissions.contains(it) }
                "User has at least one required permission: $userHas"
            } else {
                "User has none of the required permissions: $requiredPermissions"
            }
        }

        return PermissionCheckResult(
            requirement = this,
            passed = passed,
            reason = reason,
            requiredPermissions = requiredPermissions,
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "require_all" to requireAll,
                "user_permissions" to userPermissions,
                "missing_permissions" to if (requireAll) {
                    requiredPermissions.filter { !userPermissions.contains(it) }
                } else emptyList<String>()
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): PermissionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (requiredPermissions.isEmpty()) {
            errors.add("Required permissions cannot be empty")
        }

        if (requiredPermissions.any { it.isBlank() }) {
            errors.add("Required permissions cannot contain blank entries")
        }

        if (requiredPermissions.size > 10) {
            warnings.add("Large number of required permissions (${requiredPermissions.size}) may impact performance")
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                PermissionValidationResult.Valid
            } else {
                PermissionValidationResult.ValidWithWarnings(warnings)
            }
        } else {
            PermissionValidationResult.Invalid(errors)
        }
    }
}

/**
 * Hierarchical permission requirement that checks permission levels
 */
data class HierarchicalPermissionRequirement(
    override val description: String = "Hierarchical permission requirement",
    val permissionHierarchy: Map<String, Set<String>>, // permission -> child permissions
    val requiredPermission: String,
    val alternativeDestination: String? = null
) : PermissionRequirement {

    override suspend fun checkPermission(context: NavigationContext): PermissionCheckResult {
        val startTime = System.currentTimeMillis()

        val userPermissions = context.permissions
        val passed = checkHierarchicalPermission(userPermissions, requiredPermission)
        val reason = if (passed) {
            "User has required permission or higher: $requiredPermission"
        } else {
            "User lacks required permission: $requiredPermission"
        }

        return PermissionCheckResult(
            requirement = this,
            passed = passed,
            reason = reason,
            requiredPermissions = setOf(requiredPermission),
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "required_permission" to requiredPermission,
                "user_permissions" to userPermissions,
                "hierarchy" to permissionHierarchy
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): PermissionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (requiredPermission.isBlank()) {
            errors.add("Required permission cannot be blank")
        }

        if (permissionHierarchy.isEmpty()) {
            warnings.add("Empty permission hierarchy")
        }

        if (!permissionHierarchy.containsKey(requiredPermission) &&
            !permissionHierarchy.values.any { it.contains(requiredPermission) }) {
            warnings.add("Required permission not found in hierarchy")
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                PermissionValidationResult.Valid
            } else {
                PermissionValidationResult.ValidWithWarnings(warnings)
            }
        } else {
            PermissionValidationResult.Invalid(errors)
        }
    }

    /**
     * Check if user has the required permission or any higher-level permission
     */
    private fun checkHierarchicalPermission(userPermissions: Set<String>, requiredPermission: String): Boolean {
        // Direct permission check
        if (userPermissions.contains(requiredPermission)) {
            return true
        }

        // Check if user has any parent permissions
        for ((parentPermission, childPermissions) in permissionHierarchy) {
            if (childPermissions.contains(requiredPermission) && userPermissions.contains(parentPermission)) {
                return true
            }
        }

        return false
    }
}

/**
 * Time-based permission requirement that checks permissions within time windows
 */
data class TimeBasedPermissionRequirement(
    override val description: String = "Time-based permission requirement",
    val requiredPermission: String,
    val validTimeRanges: List<TimeRange>,
    val timezone: String = "UTC",
    val alternativeDestination: String? = null
) : PermissionRequirement {

    override suspend fun checkPermission(context: NavigationContext): PermissionCheckResult {
        val startTime = System.currentTimeMillis()

        val userPermissions = context.permissions
        val hasPermission = userPermissions.contains(requiredPermission)
        val currentTime = System.currentTimeMillis()
        val isInValidTimeRange = validTimeRanges.any { it.contains(currentTime) }

        val passed = hasPermission && isInValidTimeRange
        val reason = when {
            !hasPermission -> "User lacks required permission: $requiredPermission"
            !isInValidTimeRange -> "Current time not in valid range. Valid ranges: $validTimeRanges"
            else -> "User has permission and time is valid"
        }

        return PermissionCheckResult(
            requirement = this,
            passed = passed,
            reason = reason,
            requiredPermissions = setOf(requiredPermission),
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "required_permission" to requiredPermission,
                "has_permission" to hasPermission,
                "current_time" to currentTime,
                "valid_time_ranges" to validTimeRanges,
                "is_in_valid_range" to isInValidTimeRange
            )
        )
    }

    override suspend fun getAlternativeDestinations(context: NavigationContext): List<String> {
        return alternativeDestination?.let { listOf(it) } ?: emptyList()
    }

    override fun validate(): PermissionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (requiredPermission.isBlank()) {
            errors.add("Required permission cannot be blank")
        }

        if (validTimeRanges.isEmpty()) {
            errors.add("Valid time ranges cannot be empty")
        }

        // Check for overlapping time ranges
        for (i in validTimeRanges.indices) {
            for (j in i + 1 until validTimeRanges.size) {
                if (validTimeRanges[i].overlaps(validTimeRanges[j])) {
                    warnings.add("Overlapping time ranges detected: ${validTimeRanges[i]} and ${validTimeRanges[j]}")
                }
            }
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                PermissionValidationResult.Valid
            } else {
                PermissionValidationResult.ValidWithWarnings(warnings)
            }
        } else {
            PermissionValidationResult.Invalid(errors)
        }
    }
}

/**
 * Represents a time range for time-based permissions
 */
data class TimeRange(
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int, // 0-23
    val endMinute: Int, // 0-59
    val daysOfWeek: Set<Int> = (0..6).toSet() // 0 = Sunday, 1 = Monday, etc.
) {

    init {
        require(startHour in 0..23) { "Start hour must be 0-23" }
        require(startMinute in 0..59) { "Start minute must be 0-59" }
        require(endHour in 0..23) { "End hour must be 0-23" }
        require(endMinute in 0..59) { "End minute must be 0-59" }
        require(daysOfWeek.all { it in 0..6 }) { "Days of week must be 0-6" }
    }

    /**
     * Check if this time range contains the given timestamp
     */
    fun contains(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1 // Convert to 0-6

        if (!daysOfWeek.contains(currentDayOfWeek)) {
            return false
        }

        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val startTimeInMinutes = startHour * 60 + startMinute
        val endTimeInMinutes = endHour * 60 + endMinute

        return if (startTimeInMinutes <= endTimeInMinutes) {
            // Same day range
            currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        } else {
            // Overnight range (e.g., 22:00 to 02:00)
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        }
    }

    /**
     * Check if this time range overlaps with another
     */
    fun overlaps(other: TimeRange): Boolean {
        // Simple overlap check - in a real implementation, this would be more sophisticated
        return this.startHour == other.startHour || this.endHour == other.endHour ||
               (this.startHour < other.startHour && this.endHour > other.startHour)
    }

    override fun toString(): String {
        val days = daysOfWeek.joinToString(",") { day ->
            when (day) {
                0 -> "Sun"
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                else -> "?"
            }
        }
        return String.format("%02d:%02d-%02d:%02d (%s)", startHour, startMinute, endHour, endMinute, days)
    }
}