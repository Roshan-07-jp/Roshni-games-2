package com.roshni.games.core.navigation

/**
 * Navigation destinations for Roshni Games
 */
object NavigationDestinations {

    // Main app destinations
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME_LIBRARY = "game_library"
    const val GAME_PLAYER = "game_player"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val LEADERBOARD = "leaderboard"
    const val SEARCH = "search"
    const val SOCIAL = "social"
    const val PARENTAL_CONTROLS = "parental_controls"
    const val ACCESSIBILITY = "accessibility"

    // Dynamic feature destinations
    const val DYNAMIC_FEATURE = "dynamic_feature"

    // Game-specific destinations
    const val GAME_DETAILS = "game_details"
    const val GAME_SETTINGS = "game_settings"
    const val GAME_STATS = "game_stats"

    // Authentication destinations
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    // Onboarding destinations
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val PERMISSIONS = "permissions"

    // Error destinations
    const val ERROR = "error"
    const val MAINTENANCE = "maintenance"
}

/**
 * Navigation arguments
 */
object NavigationArguments {
    const val GAME_ID = "gameId"
    const val PLAYER_ID = "playerId"
    const val SCORE_ID = "scoreId"
    const val ACHIEVEMENT_ID = "achievementId"
    const val LEADERBOARD_TYPE = "leaderboardType"
    const val SEARCH_QUERY = "searchQuery"
    const val ERROR_MESSAGE = "errorMessage"
}

/**
 * Deep link URIs for external navigation
 */
object DeepLinks {
    const val GAME_URI = "roshnigames://game/{gameId}"
    const val PLAYER_URI = "roshnigames://player/{playerId}"
    const val LEADERBOARD_URI = "roshnigames://leaderboard/{type}"
    const val ACHIEVEMENT_URI = "roshnigames://achievement/{achievementId}"
}