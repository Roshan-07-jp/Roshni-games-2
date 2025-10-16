package com.roshni.games.core.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.core.database.dao.TermsDao
import com.roshni.games.core.database.model.GameEntity
import com.roshni.games.core.database.model.PlayerEntity
import com.roshni.games.core.database.model.ScoreEntity
import com.roshni.games.core.database.model.TermsAcceptanceEntity
import com.roshni.games.core.database.model.TermsDocumentEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        ScoreEntity::class,
        TermsDocumentEntity::class,
        TermsAcceptanceEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class RoshniGamesDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    abstract fun gameDao(): GameDao

    abstract fun scoreDao(): ScoreDao

    abstract fun termsDao(): TermsDao

    companion object {
        const val DATABASE_NAME = "roshni_games.db"
    }
}