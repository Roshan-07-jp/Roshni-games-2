package com.roshni.games.core.network.model

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime

data class GameDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("difficulty")
    val difficulty: String,

    @SerializedName("minPlayers")
    val minPlayers: Int,

    @SerializedName("maxPlayers")
    val maxPlayers: Int,

    @SerializedName("estimatedDuration")
    val estimatedDuration: Int,

    @SerializedName("iconUrl")
    val iconUrl: String?,

    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String?,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("isMultiplayer")
    val isMultiplayer: Boolean,

    @SerializedName("tags")
    val tags: List<String>,

    @SerializedName("createdAt")
    val createdAt: LocalDateTime,

    @SerializedName("updatedAt")
    val updatedAt: LocalDateTime
)

data class GameListDto(
    @SerializedName("games")
    val games: List<GameDto>,

    @SerializedName("totalCount")
    val totalCount: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("hasNextPage")
    val hasNextPage: Boolean
)