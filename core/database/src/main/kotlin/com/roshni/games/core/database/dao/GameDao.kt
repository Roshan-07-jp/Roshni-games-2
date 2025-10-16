package com.roshni.games.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roshni.games.core.database.model.GameDifficulty
import com.roshni.games.core.database.model.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGame(gameId: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE isActive = 1")
    fun getActiveGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE category = :category AND isActive = 1")
    fun getGamesByCategory(category: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE difficulty = :difficulty AND isActive = 1")
    fun getGamesByDifficulty(difficulty: GameDifficulty): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isMultiplayer = 1 AND isActive = 1")
    fun getMultiplayerGames(): Flow<List<GameEntity>>

    @Query("SELECT DISTINCT category FROM games WHERE isActive = 1")
    fun getGameCategories(): Flow<List<String>>

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' AND isActive = 1")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE :tag IN (tags) AND isActive = 1")
    fun getGamesByTag(tag: String): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET isActive = :isActive WHERE id = :gameId")
    suspend fun updateGameStatus(gameId: String, isActive: Boolean)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("SELECT COUNT(*) FROM games WHERE isActive = 1")
    fun getActiveGameCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM games WHERE id = :gameId AND isActive = 1)")
    fun gameExists(gameId: String): Flow<Boolean>
}