package com.roshni.games.core.utils.security.permissions

/**
 * Sealed class hierarchy for different types of permissions in the Roshni Games platform.
 * Provides type-safe permission management with hierarchical relationships.
 */

// ==================== BASE PERMISSION CLASSES ====================

/**
 * Base sealed class for all permissions
 */
sealed class Permission(
    val name: String,
    val description: String,
    val category: PermissionCategory,
    val level: PermissionLevel = PermissionLevel.BASIC
) {
    override fun toString(): String = name
}

/**
 * Permission categories for organization
 */
enum class PermissionCategory(val displayName: String) {
    GAMEPLAY("Gameplay"),
    CONTENT("Content"),
    SOCIAL("Social"),
    SYSTEM("System"),
    PARENTAL_CONTROLS("Parental Controls"),
    ADMINISTRATION("Administration")
}

/**
 * Permission levels for hierarchical access control
 */
enum class PermissionLevel(val priority: Int) {
    BASIC(1),
    INTERMEDIATE(2),
    ADVANCED(3),
    ADMIN(4),
    SYSTEM(5)
}

// ==================== GAMEPLAY PERMISSIONS ====================

/**
 * Permissions related to gameplay functionality
 */
sealed class GameplayPermission(
    name: String,
    description: String,
    level: PermissionLevel = PermissionLevel.BASIC
) : Permission(name, description, PermissionCategory.GAMEPLAY, level) {

    object PLAY_GAMES : GameplayPermission(
        name = "PLAY_GAMES",
        description = "Basic permission to play games",
        level = PermissionLevel.BASIC
    )

    object SAVE_PROGRESS : GameplayPermission(
        name = "SAVE_PROGRESS",
        description = "Save game progress and achievements",
        level = PermissionLevel.BASIC
    )

    object LOAD_GAMES : GameplayPermission(
        name = "LOAD_GAMES",
        description = "Load saved games and continue progress",
        level = PermissionLevel.BASIC
    )

    object ACCESS_GAME_LIBRARY : GameplayPermission(
        name = "ACCESS_GAME_LIBRARY",
        description = "Browse and access the game library",
        level = PermissionLevel.BASIC
    )

    object PLAY_MULTIPLAYER : GameplayPermission(
        name = "PLAY_MULTIPLAYER",
        description = "Participate in multiplayer games",
        level = PermissionLevel.INTERMEDIATE
    )

    object CREATE_CUSTOM_GAMES : GameplayPermission(
        name = "CREATE_CUSTOM_GAMES",
        description = "Create custom games and levels",
        level = PermissionLevel.ADVANCED
    )

    object MANAGE_GAMES : GameplayPermission(
        name = "MANAGE_GAMES",
        description = "Manage game installations and updates",
        level = PermissionLevel.ADMIN
    )

    object ACCESS_DEVELOPER_TOOLS : GameplayPermission(
        name = "ACCESS_DEVELOPER_TOOLS",
        description = "Access game development and debugging tools",
        level = PermissionLevel.SYSTEM
    )

    companion object {
        val ALL: Set<GameplayPermission> = setOf(
            PLAY_GAMES, SAVE_PROGRESS, LOAD_GAMES, ACCESS_GAME_LIBRARY,
            PLAY_MULTIPLAYER, CREATE_CUSTOM_GAMES, MANAGE_GAMES, ACCESS_DEVELOPER_TOOLS
        )
    }
}

// ==================== CONTENT PERMISSIONS ====================

/**
 * Permissions related to content management and access
 */
sealed class ContentPermission(
    name: String,
    description: String,
    level: PermissionLevel = PermissionLevel.BASIC
) : Permission(name, description, PermissionCategory.CONTENT, level) {

    object VIEW_CONTENT : ContentPermission(
        name = "VIEW_CONTENT",
        description = "View games, media, and other content",
        level = PermissionLevel.BASIC
    )

    object DOWNLOAD_CONTENT : ContentPermission(
        name = "DOWNLOAD_CONTENT",
        description = "Download games and content for offline use",
        level = PermissionLevel.BASIC
    )

    object CREATE_CONTENT : ContentPermission(
        name = "CREATE_CONTENT",
        description = "Create user-generated content and reviews",
        level = PermissionLevel.INTERMEDIATE
    )

    object SHARE_CONTENT : ContentPermission(
        name = "SHARE_CONTENT",
        description = "Share content with other users",
        level = PermissionLevel.INTERMEDIATE
    )

    object RATE_CONTENT : ContentPermission(
        name = "RATE_CONTENT",
        description = "Rate and review games and content",
        level = PermissionLevel.INTERMEDIATE
    )

    object UPLOAD_CONTENT : ContentPermission(
        name = "UPLOAD_CONTENT",
        description = "Upload custom content and game assets",
        level = PermissionLevel.ADVANCED
    )

    object MANAGE_CONTENT : ContentPermission(
        name = "MANAGE_CONTENT",
        description = "Moderate and manage platform content",
        level = PermissionLevel.ADMIN
    )

    object ACCESS_PREMIUM_CONTENT : ContentPermission(
        name = "ACCESS_PREMIUM_CONTENT",
        description = "Access premium and exclusive content",
        level = PermissionLevel.ADVANCED
    )

    companion object {
        val ALL: Set<ContentPermission> = setOf(
            VIEW_CONTENT, DOWNLOAD_CONTENT, CREATE_CONTENT, SHARE_CONTENT,
            RATE_CONTENT, UPLOAD_CONTENT, MANAGE_CONTENT, ACCESS_PREMIUM_CONTENT
        )
    }
}

// ==================== SOCIAL PERMISSIONS ====================

/**
 * Permissions related to social features and interactions
 */
sealed class SocialPermission(
    name: String,
    description: String,
    level: PermissionLevel = PermissionLevel.BASIC
) : Permission(name, description, PermissionCategory.SOCIAL, level) {

    object VIEW_PROFILE : SocialPermission(
        name = "VIEW_PROFILE",
        description = "View user profiles and basic information",
        level = PermissionLevel.BASIC
    )

    object EDIT_PROFILE : SocialPermission(
        name = "EDIT_PROFILE",
        description = "Edit own profile and settings",
        level = PermissionLevel.BASIC
    )

    object SEND_MESSAGES : SocialPermission(
        name = "SEND_MESSAGES",
        description = "Send messages to other users",
        level = PermissionLevel.INTERMEDIATE
    )

    object JOIN_COMMUNITIES : SocialPermission(
        name = "JOIN_COMMUNITIES",
        description = "Join gaming communities and groups",
        level = PermissionLevel.INTERMEDIATE
    )

    object CREATE_COMMUNITIES : SocialPermission(
        name = "CREATE_COMMUNITIES",
        description = "Create new gaming communities",
        level = PermissionLevel.ADVANCED
    )

    object MANAGE_SOCIAL : SocialPermission(
        name = "MANAGE_SOCIAL",
        description = "Moderate social features and communities",
        level = PermissionLevel.ADMIN
    )

    object ACCESS_SOCIAL_ANALYTICS : SocialPermission(
        name = "ACCESS_SOCIAL_ANALYTICS",
        description = "Access social interaction analytics",
        level = PermissionLevel.ADMIN
    )

    object MANAGE_FRIENDS : SocialPermission(
        name = "MANAGE_FRIENDS",
        description = "Manage friend connections and relationships",
        level = PermissionLevel.INTERMEDIATE
    )

    companion object {
        val ALL: Set<SocialPermission> = setOf(
            VIEW_PROFILE, EDIT_PROFILE, SEND_MESSAGES, JOIN_COMMUNITIES,
            CREATE_COMMUNITIES, MANAGE_SOCIAL, ACCESS_SOCIAL_ANALYTICS, MANAGE_FRIENDS
        )
    }
}

// ==================== SYSTEM PERMISSIONS ====================

/**
 * Permissions related to system administration and management
 */
sealed class SystemPermission(
    name: String,
    description: String,
    level: PermissionLevel = PermissionLevel.BASIC
) : Permission(name, description, PermissionCategory.SYSTEM, level) {

    object BASIC_ACCESS : SystemPermission(
        name = "BASIC_ACCESS",
        description = "Basic system access and navigation",
        level = PermissionLevel.BASIC
    )

    object USER : SystemPermission(
        name = "USER",
        description = "Standard user privileges",
        level = PermissionLevel.BASIC
    )

    object PREMIUM_USER : SystemPermission(
        name = "PREMIUM_USER",
        description = "Premium user privileges and features",
        level = PermissionLevel.INTERMEDIATE
    )

    object MODERATOR : SystemPermission(
        name = "MODERATOR",
        description = "Content moderation and community management",
        level = PermissionLevel.ADVANCED
    )

    object ADMIN : SystemPermission(
        name = "ADMIN",
        description = "Full administrative access",
        level = PermissionLevel.ADMIN
    )

    object SYSTEM : SystemPermission(
        name = "SYSTEM",
        description = "System-level access and configuration",
        level = PermissionLevel.SYSTEM
    )

    object ACCESS_SETTINGS : SystemPermission(
        name = "ACCESS_SETTINGS",
        description = "Access application settings",
        level = PermissionLevel.BASIC
    )

    object MANAGE_USERS : SystemPermission(
        name = "MANAGE_USERS",
        description = "Manage user accounts and permissions",
        level = PermissionLevel.ADMIN
    )

    object ACCESS_LOGS : SystemPermission(
        name = "ACCESS_LOGS",
        description = "Access system and security logs",
        level = PermissionLevel.ADMIN
    )

    object SYSTEM_CONFIGURATION : SystemPermission(
        name = "SYSTEM_CONFIGURATION",
        description = "Modify system configuration and settings",
        level = PermissionLevel.SYSTEM
    )

    companion object {
        val ALL: Set<SystemPermission> = setOf(
            BASIC_ACCESS, USER, PREMIUM_USER, MODERATOR, ADMIN, SYSTEM,
            ACCESS_SETTINGS, MANAGE_USERS, ACCESS_LOGS, SYSTEM_CONFIGURATION
        )
    }
}

// ==================== PARENTAL CONTROL PERMISSIONS ====================

/**
 * Permissions related to parental controls and child safety
 */
sealed class ParentalControlPermission(
    name: String,
    description: String,
    level: PermissionLevel = PermissionLevel.BASIC
) : Permission(name, description, PermissionCategory.PARENTAL_CONTROLS, level) {

    object SET_PARENTAL_CONTROLS : ParentalControlPermission(
        name = "SET_PARENTAL_CONTROLS",
        description = "Configure parental control settings",
        level = PermissionLevel.INTERMEDIATE
    )

    object OVERRIDE_PARENTAL_CONTROLS : ParentalControlPermission(
        name = "OVERRIDE_PARENTAL_CONTROLS",
        description = "Temporarily override parental control restrictions",
        level = PermissionLevel.ADVANCED
    )

    object MANAGE_CHILD_ACCOUNTS : ParentalControlPermission(
        name = "MANAGE_CHILD_ACCOUNTS",
        description = "Manage child accounts and their restrictions",
        level = PermissionLevel.INTERMEDIATE
    )

    object VIEW_PARENTAL_REPORTS : ParentalControlPermission(
        name = "VIEW_PARENTAL_REPORTS",
        description = "View parental control reports and activity logs",
        level = PermissionLevel.INTERMEDIATE
    )

    object APPROVE_CONTENT : ParentalControlPermission(
        name = "APPROVE_CONTENT",
        description = "Approve specific content for child accounts",
        level = PermissionLevel.INTERMEDIATE
    )

    companion object {
        val ALL: Set<ParentalControlPermission> = setOf(
            SET_PARENTAL_CONTROLS, OVERRIDE_PARENTAL_CONTROLS,
            MANAGE_CHILD_ACCOUNTS, VIEW_PARENTAL_REPORTS, APPROVE_CONTENT
        )
    }
}

// ==================== UTILITY FUNCTIONS ====================

/**
 * Get all permissions of a specific category
 */
fun getPermissionsByCategory(category: PermissionCategory): Set<Permission> {
    return when (category) {
        PermissionCategory.GAMEPLAY -> GameplayPermission.ALL
        PermissionCategory.CONTENT -> ContentPermission.ALL
        PermissionCategory.SOCIAL -> SocialPermission.ALL
        PermissionCategory.SYSTEM -> SystemPermission.ALL
        PermissionCategory.PARENTAL_CONTROLS -> ParentalControlPermission.ALL
        PermissionCategory.ADMINISTRATION -> SystemPermission.ALL.filter { it.level >= PermissionLevel.ADMIN }
    }
}

/**
 * Get all permissions of a specific level or higher
 */
fun getPermissionsByLevel(minLevel: PermissionLevel): Set<Permission> {
    return setOf(
        GameplayPermission.ALL,
        ContentPermission.ALL,
        SocialPermission.ALL,
        SystemPermission.ALL,
        ParentalControlPermission.ALL
    ).flatten().filter { it.level >= minLevel }
}

/**
 * Check if a permission string matches a specific permission type
 */
fun parsePermission(permissionString: String): Permission? {
    return setOf(
        GameplayPermission.ALL,
        ContentPermission.ALL,
        SocialPermission.ALL,
        SystemPermission.ALL,
        ParentalControlPermission.ALL
    ).flatten().firstOrNull { it.name == permissionString }
}

/**
 * Get permission hierarchy (which permissions imply others)
 */
fun getPermissionHierarchy(): Map<String, Set<String>> {
    return mapOf(
        SystemPermission.ADMIN.name to setOf(
            SystemPermission.MODERATOR.name,
            SystemPermission.PREMIUM_USER.name,
            SystemPermission.USER.name,
            SystemPermission.BASIC_ACCESS.name
        ),
        SystemPermission.MODERATOR.name to setOf(
            SystemPermission.PREMIUM_USER.name,
            SystemPermission.USER.name,
            SystemPermission.BASIC_ACCESS.name
        ),
        SystemPermission.PREMIUM_USER.name to setOf(
            SystemPermission.USER.name,
            SystemPermission.BASIC_ACCESS.name
        ),
        SystemPermission.USER.name to setOf(SystemPermission.BASIC_ACCESS.name),
        GameplayPermission.MANAGE_GAMES.name to setOf(
            GameplayPermission.PLAY_GAMES.name,
            GameplayPermission.SAVE_PROGRESS.name,
            GameplayPermission.LOAD_GAMES.name,
            GameplayPermission.ACCESS_GAME_LIBRARY.name
        ),
        ContentPermission.MANAGE_CONTENT.name to setOf(
            ContentPermission.VIEW_CONTENT.name,
            ContentPermission.DOWNLOAD_CONTENT.name,
            ContentPermission.CREATE_CONTENT.name,
            ContentPermission.SHARE_CONTENT.name,
            ContentPermission.RATE_CONTENT.name
        ),
        SocialPermission.MANAGE_SOCIAL.name to setOf(
            SocialPermission.VIEW_PROFILE.name,
            SocialPermission.EDIT_PROFILE.name,
            SocialPermission.SEND_MESSAGES.name,
            SocialPermission.JOIN_COMMUNITIES.name
        )
    )
}

/**
 * Check if a permission is hierarchical to another permission
 */
fun isHierarchicalPermission(parentPermission: String, childPermission: String): Boolean {
    val hierarchy = getPermissionHierarchy()
    return hierarchy[parentPermission]?.contains(childPermission) ?: false
}

/**
 * Get all child permissions for a given permission
 */
fun getChildPermissions(permission: String): Set<String> {
    val hierarchy = getPermissionHierarchy()
    return hierarchy[permission] ?: emptySet()
}

/**
 * Get all parent permissions for a given permission
 */
fun getParentPermissions(permission: String): Set<String> {
    val hierarchy = getPermissionHierarchy()
    return hierarchy.entries.filter { it.value.contains(permission) }.map { it.key }.toSet()
}