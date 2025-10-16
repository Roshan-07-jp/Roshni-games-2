package com.roshni.games.gamecatalog.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a game category with its metadata and associated games
 */
@Serializable
data class GameCategory(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: String,
    val gameCount: Int,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList()
)

/**
 * Comprehensive game definition with all metadata
 */
@Serializable
data class GameDefinition(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val category: GameCategoryType,
    val subcategory: String,
    val difficulty: GameDifficulty,
    val minPlayers: Int,
    val maxPlayers: Int,
    val estimatedDuration: Int, // in minutes
    val ageRating: AgeRating,
    val gameType: GameType,
    val gameMode: GameMode,
    val isOnline: Boolean,
    val isOffline: Boolean,
    val iconUrl: String,
    val thumbnailUrl: String,
    val screenshots: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val features: List<GameFeature> = emptyList(),
    val requirements: GameRequirements,
    val version: String,
    val isActive: Boolean = true,
    val isNew: Boolean = false,
    val isFeatured: Boolean = false,
    val releaseDate: String,
    val lastUpdated: String,
    val developer: String,
    val publisher: String,
    val rating: Float = 0.0f,
    val ratingCount: Int = 0,
    val downloadCount: Long = 0,
    val fileSize: Long = 0, // in bytes
    val moduleId: String, // Dynamic feature module ID
    val entryPoint: String, // Main class/activity name
    val dependencies: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Game category types
 */
@Serializable
enum class GameCategoryType(val displayName: String, val description: String) {
    PUZZLE("Puzzle Games", "Brain teasers, logic puzzles, and mind-bending challenges"),
    ACTION("Action Games", "Fast-paced games requiring quick reflexes and precision"),
    STRATEGY("Strategy Games", "Games requiring tactical thinking and long-term planning"),
    ARCADE("Arcade Games", "Classic arcade-style games and endless challenges"),
    CARD("Card Games", "Traditional and digital card games for all players"),
    TRIVIA("Trivia & Quiz", "Knowledge-based games testing various subjects"),
    SIMULATION("Simulation Games", "Realistic simulations of real-world activities"),
    CASUAL("Casual Games", "Easy-to-play games perfect for short sessions")
}

/**
 * Game difficulty levels
 */
@Serializable
enum class GameDifficulty(val displayName: String, val description: String) {
    EASY("Easy", "Perfect for beginners and casual players"),
    MEDIUM("Medium", "Balanced challenge for most players"),
    HARD("Hard", "Challenging gameplay for experienced players"),
    EXPERT("Expert", "Extremely difficult, for skilled players only")
}

/**
 * Age ratings for games
 */
@Serializable
enum class AgeRating(val displayName: String, val minAge: Int) {
    EVERYONE("Everyone", 0),
    EVERYONE_10("Everyone 10+", 10),
    TEEN("Teen", 13),
    MATURE("Mature", 17),
    ADULTS_ONLY("Adults Only", 18)
}

/**
 * Game types
 */
@Serializable
enum class GameType(val displayName: String) {
    SINGLE_PLAYER("Single Player"),
    MULTIPLAYER("Multiplayer"),
    CO_OP("Cooperative"),
    COMPETITIVE("Competitive"),
    TEAM_BASED("Team Based")
}

/**
 * Game modes
 */
@Serializable
enum class GameMode(val displayName: String) {
    CAMPAIGN("Campaign"),
    ENDLESS("Endless"),
    TIME_TRIAL("Time Trial"),
    CHALLENGE("Challenge"),
    TOURNAMENT("Tournament"),
    SANDBOX("Sandbox"),
    STORY("Story Mode"),
    SURVIVAL("Survival")
}

/**
 * Game features
 */
@Serializable
enum class GameFeature(val displayName: String) {
    ACHIEVEMENTS("Achievements"),
    LEADERBOARDS("Leaderboards"),
    CLOUD_SAVE("Cloud Save"),
    OFFLINE_MODE("Offline Mode"),
    MULTIPLAYER("Multiplayer"),
    DAILY_CHALLENGES("Daily Challenges"),
    SEASONAL_EVENTS("Seasonal Events"),
    CUSTOMIZATION("Customization"),
    TUTORIAL("Tutorial"),
    HINTS("Hints")
}

/**
 * Game requirements
 */
@Serializable
data class GameRequirements(
    val minAndroidVersion: String,
    val recommendedAndroidVersion: String,
    val minRam: Int, // in MB
    val recommendedRam: Int, // in MB
    val minStorage: Long, // in MB
    val recommendedStorage: Long, // in MB
    val requiresInternet: Boolean = false,
    val requiresLocation: Boolean = false,
    val requiresCamera: Boolean = false,
    val requiresMicrophone: Boolean = false,
    val supportedLanguages: List<String> = emptyList()
)

/**
 * Complete game catalog
 */
@Serializable
data class GameCatalog(
    val version: String,
    val lastUpdated: String,
    val categories: List<GameCategory>,
    val games: List<GameDefinition>,
    val totalGames: Int,
    val featuredGames: List<String>, // Game IDs
    val newGames: List<String>, // Game IDs
    val metadata: Map<String, String> = emptyMap()
)