package com.roshni.games.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roshni.games.core.database.model.ScoreEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface ScoreDao {

    @Query("SELECT * FROM scores WHERE id = :scoreId")
    fun getScore(scoreId: String): Flow<ScoreEntity?>

    @Query("SELECT * FROM scores WHERE playerId = :playerId ORDER BY score DESC, achievedAt DESC")
    fun getScoresByPlayer(playerId: String): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE gameId = :gameId ORDER BY score DESC, achievedAt DESC")
    fun getScoresByGame(gameId: String): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE playerId = :playerId AND gameId = :gameId ORDER BY score DESC")
    fun getScoresByPlayerAndGame(playerId: String, gameId: String): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE isPersonalBest = 1 ORDER BY score DESC")
    fun getPersonalBests(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE playerId = :playerId AND isPersonalBest = 1 ORDER BY score DESC")
    fun getPlayerPersonalBests(playerId: String): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE multiplayerRank IS NOT NULL ORDER BY multiplayerRank ASC, score DESC")
    fun getMultiplayerScores(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE achievedAt >= :since ORDER BY score DESC LIMIT :limit")
    fun getRecentHighScores(since: LocalDateTime, limit: Int): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE playerId = :playerId ORDER BY achievedAt DESC LIMIT 1")
    fun getLastScoreForPlayer(playerId: String): Flow<ScoreEntity?>

    @Query("SELECT MAX(score) FROM scores WHERE playerId = :playerId AND gameId = :gameId")
    fun getPersonalBestForGame(playerId: String, gameId: String): Flow<Long?>

    @Query("SELECT COUNT(*) FROM scores WHERE playerId = :playerId")
    fun getPlayerScoreCount(playerId: String): Flow<Int>

    @Query("SELECT AVG(score) FROM scores WHERE playerId = :playerId")
    fun getPlayerAverageScore(playerId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<ScoreEntity>)

    @Update
    suspend fun updateScore(score: ScoreEntity)

    @Query("UPDATE scores SET syncedAt = :syncedAt WHERE id = :scoreId")
    suspend fun markScoreAsSynced(scoreId: String, syncedAt: LocalDateTime)

    @Query("UPDATE scores SET isPersonalBest = 0 WHERE playerId = :playerId AND gameId = :gameId")
    suspend fun clearPersonalBestsForPlayerAndGame(playerId: String, gameId: String)

    @Query("UPDATE scores SET isPersonalBest = 1 WHERE id = :scoreId")
    suspend fun markAsPersonalBest(scoreId: String)

    @Query("DELETE FROM scores WHERE id = :scoreId")
    suspend fun deleteScore(scoreId: String)

    @Query("DELETE FROM scores WHERE playerId = :playerId")
    suspend fun deleteScoresForPlayer(playerId: String)

    @Query("DELETE FROM scores WHERE gameId = :gameId")
    suspend fun deleteScoresForGame(gameId: String)

    @Query("DELETE FROM scores")
    suspend fun deleteAllScores()

    @Query("SELECT EXISTS(SELECT 1 FROM scores WHERE id = :scoreId)")
    fun scoreExists(scoreId: String): Flow<Boolean>
}