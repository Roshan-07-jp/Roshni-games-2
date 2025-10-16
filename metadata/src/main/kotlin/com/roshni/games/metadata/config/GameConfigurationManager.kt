package com.roshni.games.metadata.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Centralized game configuration and metadata management system
 */
class GameConfigurationManager(
    private val context: Context
) {

    private val Context.gameConfigDataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "game_config")

    // Game feature flags
    private val featureFlags = mutableMapOf<String, Boolean>()

    // Game configurations
    private val gameConfigurations = mutableMapOf<String, GameConfiguration>()

    // Localization data
    private val localizationData = mutableMapOf<String, Map<String, String>>()

    init {
        initializeDefaultConfigurations()
        initializeFeatureFlags()
        loadConfigurations()
    }

    /**
     * Get game configuration
     */
    fun getGameConfiguration(gameId: String): GameConfiguration {
        return gameConfigurations[gameId] ?: createDefaultGameConfiguration(gameId)
    }

    /**
     * Update game configuration
     */
    suspend fun updateGameConfiguration(gameId: String, config: GameConfiguration) {
        gameConfigurations[gameId] = config

        // Save to persistent storage
        context.gameConfigDataStore.edit { preferences ->
            preferences[stringPreferencesKey("config_$gameId")] = Json.encodeToString(config)
        }

        Timber.d("Updated configuration for game: $gameId")
    }

    /**
     * Check if feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return featureFlags[feature] ?: false
    }

    /**
     * Enable/disable feature
     */
    suspend fun setFeatureEnabled(feature: String, enabled: Boolean) {
        featureFlags[feature] = enabled

        // Save to persistent storage
        context.gameConfigDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("feature_$feature")] = enabled
        }

        Timber.d("Feature $feature ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get localized string
     */
    fun getLocalizedString(key: String, language: String = getCurrentLanguage()): String {
        return localizationData[language]?.get(key) ?: localizationData["en"]?.get(key) ?: key
    }

    /**
     * Set current language
     */
    suspend fun setCurrentLanguage(language: String) {
        context.gameConfigDataStore.edit { preferences ->
            preferences[stringPreferencesKey("current_language")] = language
        }

        Timber.d("Language set to: $language")
    }

    /**
     * Get current language
     */
    private fun getCurrentLanguage(): String {
        // In real implementation, this would read from DataStore
        return "en" // Default to English
    }

    /**
     * Load game metadata for specific game
     */
    fun loadGameMetadata(gameId: String): GameMetadata? {
        // In real implementation, this would load from assets or network
        return createGameMetadata(gameId)
    }

    /**
     * Get all available games metadata
     */
    fun getAllGamesMetadata(): List<GameMetadata> {
        // Return metadata for all games in the catalog
        return listOf(
            // Puzzle games metadata
            createGameMetadata("sudoku-classic"),
            createGameMetadata("crossword-daily"),
            createGameMetadata("jigsaw-master"),
            createGameMetadata("match3-jewels"),
            createGameMetadata("physics-puzzle"),
            createGameMetadata("word-search-pro"),
            createGameMetadata("logic-grid"),
            createGameMetadata("math-puzzle"),
            createGameMetadata("sliding-puzzle"),
            createGameMetadata("tetris-modern"),
            createGameMetadata("maze-runner"),
            createGameMetadata("pattern-match"),

            // Action games metadata
            createGameMetadata("platform-jumper"),
            createGameMetadata("space-shooter"),
            createGameMetadata("racing-champion"),
            createGameMetadata("sports-soccer"),
            createGameMetadata("fighting-legends"),
            createGameMetadata("adventure-quest"),

            // Strategy games metadata
            createGameMetadata("tower-defense"),
            createGameMetadata("rts-commander"),
            createGameMetadata("chess-master"),
            createGameMetadata("card-strategy"),
            createGameMetadata("checkers-pro"),

            // Arcade games metadata
            createGameMetadata("endless-runner"),
            createGameMetadata("bullet-hell"),
            createGameMetadata("retro-arcade"),

            // Card games metadata
            createGameMetadata("solitaire-classic"),
            createGameMetadata("poker-texas"),
            createGameMetadata("blackjack-pro"),
            createGameMetadata("memory-cards"),

            // Trivia games metadata
            createGameMetadata("trivia-general"),
            createGameMetadata("science-quiz"),
            createGameMetadata("history-trivia"),
            createGameMetadata("sports-quiz"),
            createGameMetadata("entertainment-quiz"),

            // Simulation games metadata
            createGameMetadata("life-simulation"),
            createGameMetadata("business-tycoon"),
            createGameMetadata("farming-simulator"),
            createGameMetadata("city-builder"),

            // Casual games metadata
            createGameMetadata("bubble-popper"),
            createGameMetadata("time-management"),
            createGameMetadata("hidden-objects"),
            createGameMetadata("color-match")
        )
    }

    /**
     * Check for updates
     */
    suspend fun checkForUpdates(): UpdateInfo {
        // In real implementation, this would check against server
        return UpdateInfo(
            isUpdateAvailable = false,
            latestVersion = "1.0.0",
            downloadSize = 0,
            changelog = emptyList()
        )
    }

    /**
     * Get system configuration
     */
    fun getSystemConfiguration(): SystemConfiguration {
        return SystemConfiguration(
            enableAnalytics = true,
            enableCrashReporting = true,
            enableAutoUpdates = true,
            maxCacheSize = 500 * 1024 * 1024, // 500MB
            enableBackgroundSync = true,
            syncInterval = 3600000 // 1 hour
        )
    }

    private fun initializeDefaultConfigurations() {
        // Initialize default configurations for all games
        val allGames = getAllGamesMetadata()
        allGames.forEach { game ->
            gameConfigurations[game.id] = createDefaultGameConfiguration(game.id)
        }

        Timber.d("Initialized default configurations for ${allGames.size} games")
    }

    private fun initializeFeatureFlags() {
        // Initialize default feature flags
        featureFlags["enable_multiplayer"] = true
        featureFlags["enable_achievements"] = true
        featureFlags["enable_daily_challenges"] = true
        featureFlags["enable_seasonal_events"] = true
        featureFlags["enable_beta_features"] = false
        featureFlags["enable_debug_mode"] = false

        Timber.d("Initialized ${featureFlags.size} feature flags")
    }

    private fun loadConfigurations() {
        // Load configurations from persistent storage
        // In real implementation, this would read from DataStore
        Timber.d("Loaded game configurations")
    }

    private fun createDefaultGameConfiguration(gameId: String): GameConfiguration {
        return GameConfiguration(
            gameId = gameId,
            isEnabled = true,
            difficulty = 1,
            soundEnabled = true,
            musicEnabled = true,
            vibrationEnabled = true,
            autoSaveEnabled = true,
            tutorialEnabled = true,
            hintsEnabled = true,
            customSettings = emptyMap()
        )
    }

    private fun createGameMetadata(gameId: String): GameMetadata {
        // Create metadata based on game ID
        val baseMetadata = GameMetadata(
            id = gameId,
            name = gameId.replace("-", " ").split(" ").joinToString(" ") {
                it.replaceFirstChar { char -> char.uppercase() }
            },
            version = "1.0.0",
            category = determineGameCategory(gameId),
            subcategory = determineGameSubcategory(gameId),
            isActive = true,
            isNew = gameId.contains("new"),
            isFeatured = gameId in listOf("sudoku-classic", "match3-jewels", "tower-defense"),
            releaseDate = "2024-01-01",
            lastUpdated = "2024-01-01",
            downloadCount = (1000..100000).random().toLong(),
            rating = (3.5f..5.0f).random().toFloat(),
            fileSize = (10L..200L).random() * 1024 * 1024, // 10-200 MB
            supportedLanguages = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh"),
            ageRating = determineAgeRating(gameId),
            tags = determineGameTags(gameId)
        )

        return baseMetadata
    }

    private fun determineGameCategory(gameId: String): String {
        return when {
            gameId.startsWith("sudoku") || gameId.startsWith("crossword") ||
            gameId.startsWith("jigsaw") || gameId.startsWith("match3") ||
            gameId.startsWith("physics") || gameId.startsWith("word") ||
            gameId.startsWith("logic") || gameId.startsWith("math") ||
            gameId.startsWith("sliding") || gameId.startsWith("tetris") ||
            gameId.startsWith("maze") || gameId.startsWith("pattern") -> "Puzzle"

            gameId.startsWith("platform") || gameId.startsWith("space") ||
            gameId.startsWith("racing") || gameId.startsWith("sports") ||
            gameId.startsWith("fighting") || gameId.startsWith("adventure") -> "Action"

            gameId.startsWith("tower") || gameId.startsWith("rts") ||
            gameId.startsWith("chess") || gameId.startsWith("card-strategy") ||
            gameId.startsWith("checkers") -> "Strategy"

            gameId.startsWith("endless") || gameId.startsWith("bullet") ||
            gameId.startsWith("retro") -> "Arcade"

            gameId.startsWith("solitaire") || gameId.startsWith("poker") ||
            gameId.startsWith("blackjack") || gameId.startsWith("memory") -> "Card"

            gameId.startsWith("trivia") || gameId.startsWith("science") ||
            gameId.startsWith("history") || gameId.startsWith("sports") ||
            gameId.startsWith("entertainment") -> "Trivia"

            gameId.startsWith("life") || gameId.startsWith("business") ||
            gameId.startsWith("farming") || gameId.startsWith("city") -> "Simulation"

            gameId.startsWith("bubble") || gameId.startsWith("time") ||
            gameId.startsWith("hidden") || gameId.startsWith("color") -> "Casual"

            else -> "Unknown"
        }
    }

    private fun determineGameSubcategory(gameId: String): String {
        return when {
            gameId.startsWith("sudoku") -> "Logic"
            gameId.startsWith("crossword") -> "Word"
            gameId.startsWith("jigsaw") -> "Jigsaw"
            gameId.startsWith("match3") -> "Match-3"
            gameId.startsWith("physics") -> "Physics"
            gameId.startsWith("word") -> "Word"
            gameId.startsWith("logic") -> "Logic"
            gameId.startsWith("math") -> "Math"
            gameId.startsWith("sliding") -> "Logic"
            gameId.startsWith("tetris") -> "Logic"
            gameId.startsWith("maze") -> "Logic"
            gameId.startsWith("pattern") -> "Logic"

            gameId.startsWith("platform") -> "Platformers"
            gameId.startsWith("space") -> "Shooters"
            gameId.startsWith("racing") -> "Racing"
            gameId.startsWith("sports") -> "Sports"
            gameId.startsWith("fighting") -> "Fighting"
            gameId.startsWith("adventure") -> "Adventure"

            gameId.startsWith("tower") -> "Tower Defense"
            gameId.startsWith("rts") -> "RTS"
            gameId.startsWith("chess") -> "Turn-based"
            gameId.startsWith("card-strategy") -> "Card Strategy"
            gameId.startsWith("checkers") -> "Board Games"

            gameId.startsWith("endless") -> "Endless Runners"
            gameId.startsWith("bullet") -> "Bullet Hell"
            gameId.startsWith("retro") -> "Retro Games"

            gameId.startsWith("solitaire") -> "Solitaire"
            gameId.startsWith("poker") -> "Poker"
            gameId.startsWith("blackjack") -> "Blackjack"
            gameId.startsWith("memory") -> "Memory Cards"

            gameId.startsWith("trivia") -> "General Knowledge"
            gameId.startsWith("science") -> "Science"
            gameId.startsWith("history") -> "History"
            gameId.startsWith("sports") -> "Sports"
            gameId.startsWith("entertainment") -> "Entertainment"

            gameId.startsWith("life") -> "Life Sim"
            gameId.startsWith("business") -> "Business"
            gameId.startsWith("farming") -> "Farming"
            gameId.startsWith("city") -> "City Builder"

            gameId.startsWith("bubble") -> "Bubble Poppers"
            gameId.startsWith("time") -> "Time Management"
            gameId.startsWith("hidden") -> "Hidden Object"
            gameId.startsWith("color") -> "Match-3"

            else -> "General"
        }
    }

    private fun determineAgeRating(gameId: String): String {
        return when {
            gameId.contains("fighting") || gameId.contains("shooter") -> "Teen"
            gameId.contains("poker") || gameId.contains("blackjack") -> "Teen"
            else -> "Everyone"
        }
    }

    private fun determineGameTags(gameId: String): List<String> {
        val tags = mutableListOf<String>()

        // Add category-based tags
        when (determineGameCategory(gameId)) {
            "Puzzle" -> tags.addAll(listOf("brain-training", "logic", "problem-solving"))
            "Action" -> tags.addAll(listOf("fast-paced", "reflexes", "adrenaline"))
            "Strategy" -> tags.addAll(listOf("tactical", "planning", "thinking"))
            "Arcade" -> tags.addAll(listOf("classic", "retro", "endless"))
            "Card" -> tags.addAll(listOf("cards", "strategy", "luck"))
            "Trivia" -> tags.addAll(listOf("knowledge", "quiz", "education"))
            "Simulation" -> tags.addAll(listOf("realistic", "management", "simulation"))
            "Casual" -> tags.addAll(listOf("relaxing", "easy", "quick-play"))
        }

        // Add specific tags
        when {
            gameId.contains("multiplayer") -> tags.add("multiplayer")
            gameId.contains("classic") -> tags.add("classic")
            gameId.contains("modern") -> tags.add("modern")
            gameId.contains("educational") -> tags.add("educational")
            gameId.contains("competitive") -> tags.add("competitive")
        }

        return tags.distinct()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        gameConfigurations.clear()
        featureFlags.clear()
        localizationData.clear()
    }
}

/**
 * Game configuration
 */
@Serializable
data class GameConfiguration(
    val gameId: String,
    val isEnabled: Boolean = true,
    val difficulty: Int = 1,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val tutorialEnabled: Boolean = true,
    val hintsEnabled: Boolean = true,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Game metadata
 */
@Serializable
data class GameMetadata(
    val id: String,
    val name: String,
    val version: String,
    val category: String,
    val subcategory: String,
    val isActive: Boolean = true,
    val isNew: Boolean = false,
    val isFeatured: Boolean = false,
    val releaseDate: String,
    val lastUpdated: String,
    val downloadCount: Long = 0,
    val rating: Float = 0.0f,
    val fileSize: Long = 0,
    val supportedLanguages: List<String> = emptyList(),
    val ageRating: String = "Everyone",
    val tags: List<String> = emptyList(),
    val description: String = "",
    val developer: String = "Roshni Games",
    val publisher: String = "Roshni Games"
)

/**
 * System configuration
 */
@Serializable
data class SystemConfiguration(
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true,
    val enableAutoUpdates: Boolean = true,
    val maxCacheSize: Long = 500 * 1024 * 1024, // 500MB
    val enableBackgroundSync: Boolean = true,
    val syncInterval: Long = 3600000 // 1 hour
)

/**
 * Update information
 */
@Serializable
data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "1.0.0",
    val downloadSize: Long = 0,
    val changelog: List<String> = emptyList(),
    val isMandatory: Boolean = false
)