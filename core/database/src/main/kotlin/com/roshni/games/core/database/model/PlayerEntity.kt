package com.roshni.games.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String?,
    val avatarUrl: String?,
    val level: Int = 1,
    val experience: Long = 0,
    val totalScore: Long = 0,
    val gamesPlayed: Int = 0,
    val achievementsUnlocked: Int = 0,
    val isOnline: Boolean = false,
    val lastSeenAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)