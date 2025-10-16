package com.roshni.games.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val difficulty: GameDifficulty,
    val minPlayers: Int = 1,
    val maxPlayers: Int = 4,
    val estimatedDuration: Int, // in minutes
    val iconUrl: String?,
    val thumbnailUrl: String?,
    val isActive: Boolean = true,
    val isMultiplayer: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

enum class GameDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}