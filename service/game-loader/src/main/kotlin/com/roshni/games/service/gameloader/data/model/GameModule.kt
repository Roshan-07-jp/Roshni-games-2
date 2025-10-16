package com.roshni.games.service.gameloader.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a game module that can be dynamically loaded
 */
@Serializable
data class GameModule(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val category: String,
    val difficulty: GameDifficulty,
    val minPlayers: Int,
    val maxPlayers: Int,
    val estimatedDuration: Int, // in minutes
    val entryPoint: String, // Main class/activity name
    val dependencies: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val downloadUrl: String? = null,
    val checksum: String? = null,
    val size: Long = 0,
    val lastUpdated: String,
    val tags: List<String> = emptyList()
)

@Serializable
enum class GameDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

/**
 * Loading state for game modules
 */
sealed class GameModuleLoadState {
    data object Idle : GameModuleLoadState()
    data object Loading : GameModuleLoadState()
    data class Success(val module: GameModule) : GameModuleLoadState()
    data class Error(val throwable: Throwable) : GameModuleLoadState()
}

/**
 * Game module manifest for plugin discovery
 */
@Serializable
data class GameModuleManifest(
    val modules: List<GameModule>,
    val version: String,
    val lastUpdated: String
)