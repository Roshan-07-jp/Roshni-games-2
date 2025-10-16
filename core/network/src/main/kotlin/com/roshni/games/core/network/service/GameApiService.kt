package com.roshni.games.core.network.service

import com.roshni.games.core.network.model.ApiResponse
import com.roshni.games.core.network.model.GameDto
import com.roshni.games.core.network.model.GameListDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GameApiService {

    @GET("games")
    suspend fun getGames(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("isActive") isActive: Boolean? = true
    ): ApiResponse<GameListDto>

    @GET("games/{gameId}")
    suspend fun getGame(
        @Path("gameId") gameId: String
    ): ApiResponse<GameDto>

    @GET("games/featured")
    suspend fun getFeaturedGames(
        @Query("limit") limit: Int = 10
    ): ApiResponse<GameListDto>

    @GET("games/categories")
    suspend fun getGameCategories(): ApiResponse<List<String>>

    @GET("games/search")
    suspend fun searchGames(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): ApiResponse<GameListDto>

    @GET("games/{gameId}/leaderboard")
    suspend fun getGameLeaderboard(
        @Path("gameId") gameId: String,
        @Query("limit") limit: Int = 50,
        @Query("timeframe") timeframe: String? = null // daily, weekly, monthly, all-time
    ): ApiResponse<List<com.roshni.games.core.network.model.ScoreDto>>
}