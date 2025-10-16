package com.roshni.games.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playerId"),
        Index("gameId"),
        Index("playerId", "gameId"),
        Index("score", "achievedAt")
    ]
)
data class ScoreEntity(
    @PrimaryKey
    val id: String,
    val playerId: String,
    val gameId: String,
    val score: Long,
    val level: Int = 1,
    val duration: Long, // in milliseconds
    val achievements: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val isPersonalBest: Boolean = false,
    val isMultiplayer: Boolean = false,
    val multiplayerRank: Int? = null,
    val achievedAt: LocalDateTime,
    val syncedAt: LocalDateTime? = null
)