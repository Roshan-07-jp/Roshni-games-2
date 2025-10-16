package com.roshni.games.service.gameloader.domain.model

import com.roshni.games.service.gameloader.data.model.GameModule
import kotlinx.datetime.LocalDateTime

/**
 * Domain model for game modules
 */
data class GameModuleDomain(
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
    val entryPoint: String,
    val dependencies: List<String>,
    val metadata: Map<String, String>,
    val isActive: Boolean,
    val downloadUrl: String?,
    val size: Long,
    val lastUpdated: LocalDateTime,
    val tags: List<String>
)

/**
 * Domain enum for game difficulty
 */
enum class GameDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

/**
 * Loading state for domain layer
 */
sealed class GameModuleLoadState {
    data object Idle : GameModuleLoadState()
    data object Loading : GameModuleLoadState()
    data class Success(val module: GameModuleDomain) : GameModuleLoadState()
    data class Error(val throwable: Throwable) : GameModuleLoadState()
}

/**
 * Module loading preferences
 */
data class ModuleLoadingPreferences(
    val autoLoadOnStartup: Boolean = false,
    val preloadCommonModules: Boolean = true,
    val maxConcurrentLoads: Int = 3,
    val timeoutSeconds: Long = 30,
    val retryAttempts: Int = 3
)

/**
 * Module loading statistics
 */
data class ModuleLoadingStats(
    val totalModules: Int = 0,
    val loadedModules: Int = 0,
    val failedModules: Int = 0,
    val averageLoadTime: Long = 0, // in milliseconds
    val lastRefreshTime: LocalDateTime? = null
)

/**
 * Extension functions for converting between data and domain models
 */
fun GameModule.toDomain(): GameModuleDomain {
    return GameModuleDomain(
        id = id,
        name = name,
        version = version,
        description = description,
        author = author,
        category = category,
        difficulty = difficulty.toDomain(),
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        estimatedDuration = estimatedDuration,
        entryPoint = entryPoint,
        dependencies = dependencies,
        metadata = metadata,
        isActive = isActive,
        downloadUrl = downloadUrl,
        size = size,
        lastUpdated = kotlinx.datetime.LocalDateTime.parse(lastUpdated.replace("Z", "")),
        tags = tags
    )
}

fun com.roshni.games.service.gameloader.data.model.GameDifficulty.toDomain(): GameDifficulty {
    return when (this) {
        com.roshni.games.service.gameloader.data.model.GameDifficulty.EASY -> GameDifficulty.EASY
        com.roshni.games.service.gameloader.data.model.GameDifficulty.MEDIUM -> GameDifficulty.MEDIUM
        com.roshni.games.service.gameloader.data.model.GameDifficulty.HARD -> GameDifficulty.HARD
        com.roshni.games.service.gameloader.data.model.GameDifficulty.EXPERT -> GameDifficulty.EXPERT
    }
}

fun GameModuleDomain.toData(): GameModule {
    return GameModule(
        id = id,
        name = name,
        version = version,
        description = description,
        author = author,
        category = category,
        difficulty = difficulty.toData(),
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        estimatedDuration = estimatedDuration,
        entryPoint = entryPoint,
        dependencies = dependencies,
        metadata = metadata,
        isActive = isActive,
        downloadUrl = downloadUrl,
        size = size,
        lastUpdated = lastUpdated.toString(),
        tags = tags
    )
}

fun GameDifficulty.toData(): com.roshni.games.service.gameloader.data.model.GameDifficulty {
    return when (this) {
        GameDifficulty.EASY -> com.roshni.games.service.gameloader.data.model.GameDifficulty.EASY
        GameDifficulty.MEDIUM -> com.roshni.games.service.gameloader.data.model.GameDifficulty.MEDIUM
        GameDifficulty.HARD -> com.roshni.games.service.gameloader.data.model.GameDifficulty.HARD
        GameDifficulty.EXPERT -> com.roshni.games.service.gameloader.data.model.GameDifficulty.EXPERT
    }
}

fun com.roshni.games.service.gameloader.data.model.GameModuleLoadState.toDomain(): GameModuleLoadState {
    return when (this) {
        is com.roshni.games.service.gameloader.data.model.GameModuleLoadState.Idle -> GameModuleLoadState.Idle
        is com.roshni.games.service.gameloader.data.model.GameModuleLoadState.Loading -> GameModuleLoadState.Loading
        is com.roshni.games.service.gameloader.data.model.GameModuleLoadState.Success -> GameModuleLoadState.Success(it.module.toDomain())
        is com.roshni.games.service.gameloader.data.model.GameModuleLoadState.Error -> GameModuleLoadState.Error(it.throwable)
    }
}