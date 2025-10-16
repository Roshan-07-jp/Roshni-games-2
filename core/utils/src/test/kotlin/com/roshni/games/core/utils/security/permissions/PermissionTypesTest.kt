package com.roshni.games.core.utils.security.permissions

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PermissionTypesTest {

    @Test
    fun `test GameplayPermission sealed class`() {
        // Test that all permissions are properly defined
        val allGameplayPermissions = GameplayPermission.ALL

        assertTrue(allGameplayPermissions.contains(GameplayPermission.PLAY_GAMES))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.SAVE_PROGRESS))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.LOAD_GAMES))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.ACCESS_GAME_LIBRARY))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.PLAY_MULTIPLAYER))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.CREATE_CUSTOM_GAMES))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.MANAGE_GAMES))
        assertTrue(allGameplayPermissions.contains(GameplayPermission.ACCESS_DEVELOPER_TOOLS))

        // Test permission properties
        assertEquals("PLAY_GAMES", GameplayPermission.PLAY_GAMES.name)
        assertEquals("Gameplay", GameplayPermission.PLAY_GAMES.category.displayName)
    }

    @Test
    fun `test ContentPermission sealed class`() {
        // Test that all permissions are properly defined
        val allContentPermissions = ContentPermission.ALL

        assertTrue(allContentPermissions.contains(ContentPermission.VIEW_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.DOWNLOAD_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.CREATE_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.SHARE_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.RATE_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.UPLOAD_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.MANAGE_CONTENT))
        assertTrue(allContentPermissions.contains(ContentPermission.ACCESS_PREMIUM_CONTENT))

        // Test permission properties
        assertEquals("VIEW_CONTENT", ContentPermission.VIEW_CONTENT.name)
        assertEquals("Content", ContentPermission.VIEW_CONTENT.category.displayName)
    }

    @Test
    fun `test SocialPermission sealed class`() {
        // Test that all permissions are properly defined
        val allSocialPermissions = SocialPermission.ALL

        assertTrue(allSocialPermissions.contains(SocialPermission.VIEW_PROFILE))
        assertTrue(allSocialPermissions.contains(SocialPermission.EDIT_PROFILE))
        assertTrue(allSocialPermissions.contains(SocialPermission.SEND_MESSAGES))
        assertTrue(allSocialPermissions.contains(SocialPermission.JOIN_COMMUNITIES))
        assertTrue(allSocialPermissions.contains(SocialPermission.CREATE_COMMUNITIES))
        assertTrue(allSocialPermissions.contains(SocialPermission.MANAGE_SOCIAL))
        assertTrue(allSocialPermissions.contains(SocialPermission.ACCESS_SOCIAL_ANALYTICS))
        assertTrue(allSocialPermissions.contains(SocialPermission.MANAGE_FRIENDS))

        // Test permission properties
        assertEquals("VIEW_PROFILE", SocialPermission.VIEW_PROFILE.name)
        assertEquals("Social", SocialPermission.VIEW_PROFILE.category.displayName)
    }

    @Test
    fun `test SystemPermission sealed class`() {
        // Test that all permissions are properly defined
        val allSystemPermissions = SystemPermission.ALL

        assertTrue(allSystemPermissions.contains(SystemPermission.BASIC_ACCESS))
        assertTrue(allSystemPermissions.contains(SystemPermission.USER))
        assertTrue(allSystemPermissions.contains(SystemPermission.PREMIUM_USER))
        assertTrue(allSystemPermissions.contains(SystemPermission.MODERATOR))
        assertTrue(allSystemPermissions.contains(SystemPermission.ADMIN))
        assertTrue(allSystemPermissions.contains(SystemPermission.SYSTEM))
        assertTrue(allSystemPermissions.contains(SystemPermission.ACCESS_SETTINGS))
        assertTrue(allSystemPermissions.contains(SystemPermission.MANAGE_USERS))
        assertTrue(allSystemPermissions.contains(SystemPermission.ACCESS_LOGS))
        assertTrue(allSystemPermissions.contains(SystemPermission.SYSTEM_CONFIGURATION))

        // Test permission properties
        assertEquals("ADMIN", SystemPermission.ADMIN.name)
        assertEquals("System", SystemPermission.ADMIN.category.displayName)
    }

    @Test
    fun `test ParentalControlPermission sealed class`() {
        // Test that all permissions are properly defined
        val allParentalPermissions = ParentalControlPermission.ALL

        assertTrue(allParentalPermissions.contains(ParentalControlPermission.SET_PARENTAL_CONTROLS))
        assertTrue(allParentalPermissions.contains(ParentalControlPermission.OVERRIDE_PARENTAL_CONTROLS))
        assertTrue(allParentalPermissions.contains(ParentalControlPermission.MANAGE_CHILD_ACCOUNTS))
        assertTrue(allParentalPermissions.contains(ParentalControlPermission.VIEW_PARENTAL_REPORTS))
        assertTrue(allParentalPermissions.contains(ParentalControlPermission.APPROVE_CONTENT))

        // Test permission properties
        assertEquals("SET_PARENTAL_CONTROLS", ParentalControlPermission.SET_PARENTAL_CONTROLS.name)
        assertEquals("Parental Controls", ParentalControlPermission.SET_PARENTAL_CONTROLS.category.displayName)
    }

    @Test
    fun `test getPermissionsByCategory function`() {
        // Test gameplay permissions
        val gameplayPermissions = getPermissionsByCategory(PermissionCategory.GAMEPLAY)
        assertTrue(gameplayPermissions.isNotEmpty())
        assertTrue(gameplayPermissions.all { it is GameplayPermission })

        // Test content permissions
        val contentPermissions = getPermissionsByCategory(PermissionCategory.CONTENT)
        assertTrue(contentPermissions.isNotEmpty())
        assertTrue(contentPermissions.all { it is ContentPermission })

        // Test social permissions
        val socialPermissions = getPermissionsByCategory(PermissionCategory.SOCIAL)
        assertTrue(socialPermissions.isNotEmpty())
        assertTrue(socialPermissions.all { it is SocialPermission })

        // Test system permissions
        val systemPermissions = getPermissionsByCategory(PermissionCategory.SYSTEM)
        assertTrue(systemPermissions.isNotEmpty())
        assertTrue(systemPermissions.all { it is SystemPermission })

        // Test parental controls permissions
        val parentalPermissions = getPermissionsByCategory(PermissionCategory.PARENTAL_CONTROLS)
        assertTrue(parentalPermissions.isNotEmpty())
        assertTrue(parentalPermissions.all { it is ParentalControlPermission })
    }

    @Test
    fun `test getPermissionsByLevel function`() {
        // Test basic level permissions
        val basicPermissions = getPermissionsByLevel(PermissionLevel.BASIC)
        assertTrue(basicPermissions.isNotEmpty())
        assertTrue(basicPermissions.all { it.level >= PermissionLevel.BASIC })

        // Test admin level permissions
        val adminPermissions = getPermissionsByLevel(PermissionLevel.ADMIN)
        assertTrue(adminPermissions.isNotEmpty())
        assertTrue(adminPermissions.all { it.level >= PermissionLevel.ADMIN })

        // Admin permissions should be subset of basic permissions
        assertTrue(basicPermissions.containsAll(adminPermissions))
    }

    @Test
    fun `test parsePermission function`() {
        // Test parsing valid permissions
        val gameplayPermission = parsePermission("PLAY_GAMES")
        assertNotNull(gameplayPermission)
        assertIs<GameplayPermission>(gameplayPermission)
        assertEquals("PLAY_GAMES", gameplayPermission.name)

        val contentPermission = parsePermission("VIEW_CONTENT")
        assertNotNull(contentPermission)
        assertIs<ContentPermission>(contentPermission)
        assertEquals("VIEW_CONTENT", contentPermission.name)

        val systemPermission = parsePermission("ADMIN")
        assertNotNull(systemPermission)
        assertIs<SystemPermission>(systemPermission)
        assertEquals("ADMIN", systemPermission.name)

        // Test parsing invalid permission
        val invalidPermission = parsePermission("INVALID_PERMISSION")
        assertEquals(null, invalidPermission)
    }

    @Test
    fun `test getPermissionHierarchy function`() {
        val hierarchy = getPermissionHierarchy()

        // Test that hierarchy is not empty
        assertTrue(hierarchy.isNotEmpty())

        // Test admin hierarchy
        val adminChildren = hierarchy[SystemPermission.ADMIN.name]
        assertNotNull(adminChildren)
        assertTrue(adminChildren.contains(SystemPermission.MODERATOR.name))
        assertTrue(adminChildren.contains(SystemPermission.USER.name))
        assertTrue(adminChildren.contains(SystemPermission.BASIC_ACCESS.name))

        // Test moderator hierarchy
        val moderatorChildren = hierarchy[SystemPermission.MODERATOR.name]
        assertNotNull(moderatorChildren)
        assertTrue(moderatorChildren.contains(SystemPermission.USER.name))
        assertTrue(moderatorChildren.contains(SystemPermission.BASIC_ACCESS.name))

        // Test gameplay hierarchy
        val manageGamesChildren = hierarchy[GameplayPermission.MANAGE_GAMES.name]
        assertNotNull(manageGamesChildren)
        assertTrue(manageGamesChildren.contains(GameplayPermission.PLAY_GAMES.name))
        assertTrue(manageGamesChildren.contains(GameplayPermission.SAVE_PROGRESS.name))
    }

    @Test
    fun `test isHierarchicalPermission function`() {
        // Test admin -> user hierarchy
        assertTrue(isHierarchicalPermission(SystemPermission.ADMIN.name, SystemPermission.USER.name))
        assertTrue(isHierarchicalPermission(SystemPermission.ADMIN.name, SystemPermission.BASIC_ACCESS.name))

        // Test moderator -> user hierarchy
        assertTrue(isHierarchicalPermission(SystemPermission.MODERATOR.name, SystemPermission.USER.name))

        // Test gameplay hierarchy
        assertTrue(isHierarchicalPermission(
            GameplayPermission.MANAGE_GAMES.name,
            GameplayPermission.PLAY_GAMES.name
        ))

        // Test content hierarchy
        assertTrue(isHierarchicalPermission(
            ContentPermission.MANAGE_CONTENT.name,
            ContentPermission.VIEW_CONTENT.name
        ))

        // Test non-hierarchical relationship
        assertFalse(isHierarchicalPermission(
            GameplayPermission.PLAY_GAMES.name,
            SystemPermission.ADMIN.name
        ))

        // Test same permission
        assertFalse(isHierarchicalPermission(
            GameplayPermission.PLAY_GAMES.name,
            GameplayPermission.PLAY_GAMES.name
        ))
    }

    @Test
    fun `test getChildPermissions function`() {
        // Test admin children
        val adminChildren = getChildPermissions(SystemPermission.ADMIN.name)
        assertTrue(adminChildren.isNotEmpty())
        assertTrue(adminChildren.contains(SystemPermission.MODERATOR.name))
        assertTrue(adminChildren.contains(SystemPermission.USER.name))

        // Test moderator children
        val moderatorChildren = getChildPermissions(SystemPermission.MODERATOR.name)
        assertTrue(moderatorChildren.isNotEmpty())
        assertTrue(moderatorChildren.contains(SystemPermission.USER.name))

        // Test leaf permission (no children)
        val basicChildren = getChildPermissions(SystemPermission.BASIC_ACCESS.name)
        assertTrue(basicChildren.isEmpty())

        // Test invalid permission
        val invalidChildren = getChildPermissions("INVALID_PERMISSION")
        assertTrue(invalidChildren.isEmpty())
    }

    @Test
    fun `test getParentPermissions function`() {
        // Test user parents
        val userParents = getParentPermissions(SystemPermission.USER.name)
        assertTrue(userParents.isNotEmpty())
        assertTrue(userParents.contains(SystemPermission.ADMIN.name))
        assertTrue(userParents.contains(SystemPermission.MODERATOR.name))
        assertTrue(userParents.contains(SystemPermission.PREMIUM_USER.name))

        // Test basic access parents
        val basicParents = getParentPermissions(SystemPermission.BASIC_ACCESS.name)
        assertTrue(basicParents.isNotEmpty())
        assertTrue(basicParents.contains(SystemPermission.ADMIN.name))
        assertTrue(basicParents.contains(SystemPermission.MODERATOR.name))
        assertTrue(basicParents.contains(SystemPermission.PREMIUM_USER.name))
        assertTrue(basicParents.contains(SystemPermission.USER.name))

        // Test root permission (no parents)
        val adminParents = getParentPermissions(SystemPermission.ADMIN.name)
        assertTrue(adminParents.isEmpty())

        // Test invalid permission
        val invalidParents = getParentPermissions("INVALID_PERMISSION")
        assertTrue(invalidParents.isEmpty())
    }

    @Test
    fun `test permission level hierarchy`() {
        // Test that permission levels are properly ordered
        assertTrue(PermissionLevel.BASIC.priority < PermissionLevel.INTERMEDIATE.priority)
        assertTrue(PermissionLevel.INTERMEDIATE.priority < PermissionLevel.ADVANCED.priority)
        assertTrue(PermissionLevel.ADVANCED.priority < PermissionLevel.ADMIN.priority)
        assertTrue(PermissionLevel.ADMIN.priority < PermissionLevel.SYSTEM.priority)

        // Test that admin permissions have higher level than basic permissions
        assertTrue(GameplayPermission.MANAGE_GAMES.level > GameplayPermission.PLAY_GAMES.level)
        assertTrue(ContentPermission.MANAGE_CONTENT.level > ContentPermission.VIEW_CONTENT.level)
        assertTrue(SocialPermission.MANAGE_SOCIAL.level > SocialPermission.VIEW_PROFILE.level)
        assertTrue(SystemPermission.ADMIN.level > SystemPermission.USER.level)
    }

    @Test
    fun `test permission categories are distinct`() {
        val categories = PermissionCategory.values()
        val categoryNames = categories.map { it.displayName }

        // All category names should be unique
        assertEquals(categories.size, categoryNames.distinct().size)

        // Should have expected categories
        assertTrue(categoryNames.contains("Gameplay"))
        assertTrue(categoryNames.contains("Content"))
        assertTrue(categoryNames.contains("Social"))
        assertTrue(categoryNames.contains("System"))
        assertTrue(categoryNames.contains("Parental Controls"))
        assertTrue(categoryNames.contains("Administration"))
    }

    @Test
    fun `test permission toString method`() {
        // Test that toString returns the permission name
        assertEquals("PLAY_GAMES", GameplayPermission.PLAY_GAMES.toString())
        assertEquals("VIEW_CONTENT", ContentPermission.VIEW_CONTENT.toString())
        assertEquals("ADMIN", SystemPermission.ADMIN.toString())
        assertEquals("SET_PARENTAL_CONTROLS", ParentalControlPermission.SET_PARENTAL_CONTROLS.toString())
    }

    @Test
    fun `test permission collections are immutable`() {
        // Test that the companion object sets are not empty
        assertTrue(GameplayPermission.ALL.isNotEmpty())
        assertTrue(ContentPermission.ALL.isNotEmpty())
        assertTrue(SocialPermission.ALL.isNotEmpty())
        assertTrue(SystemPermission.ALL.isNotEmpty())
        assertTrue(ParentalControlPermission.ALL.isNotEmpty())

        // Test that we have a reasonable number of permissions in each category
        assertTrue(GameplayPermission.ALL.size >= 5)
        assertTrue(ContentPermission.ALL.size >= 5)
        assertTrue(SocialPermission.ALL.size >= 5)
        assertTrue(SystemPermission.ALL.size >= 5)
        assertTrue(ParentalControlPermission.ALL.size >= 3)
    }
}