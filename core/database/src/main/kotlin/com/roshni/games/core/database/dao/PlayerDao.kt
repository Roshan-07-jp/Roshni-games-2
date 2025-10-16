package com.roshni.games.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roshni.games.core.database.model.PlayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE id = :playerId")
    fun getPlayer(playerId: String): Flow<PlayerEntity?>

    @Query("SELECT * FROM players")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE isOnline = 1")
    fun getOnlinePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY totalScore DESC LIMIT :limit")
    fun getTopPlayers(limit: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE level >= :minLevel ORDER BY experience DESC")
    fun getPlayersByLevel(minLevel: Int): Flow<List<PlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE players SET level = :level, experience = :experience, totalScore = :totalScore, gamesPlayed = :gamesPlayed, achievementsUnlocked = :achievementsUnlocked, updatedAt = :updatedAt WHERE id = :playerId")
    suspend fun updatePlayerStats(
        playerId: String,
        level: Int,
        experience: Long,
        totalScore: Long,
        gamesPlayed: Int,
        achievementsUnlocked: Int,
        updatedAt: LocalDateTime
    )

    @Query("UPDATE players SET isOnline = :isOnline, lastSeenAt = :lastSeenAt WHERE id = :playerId")
    suspend fun updatePlayerOnlineStatus(
        playerId: String,
        isOnline: Boolean,
        lastSeenAt: LocalDateTime
    )

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: String)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @Query("SELECT COUNT(*) FROM players")
    fun getPlayerCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM players WHERE id = :playerId)")
    fun playerExists(playerId: String): Flow<Boolean>
}