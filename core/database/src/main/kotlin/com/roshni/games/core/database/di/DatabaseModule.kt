package com.roshni.games.core.database.di

import android.content.Context
import androidx.room.Room
import com.roshni.games.core.database.dao.GameDao
import com.roshni.games.core.database.dao.PlayerDao
import com.roshni.games.core.database.dao.ScoreDao
import com.roshni.games.core.database.database.RoshniGamesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RoshniGamesDatabase {
        // Database passphrase for encryption
        val passphrase = "roshni_games_encryption_key_2024".toByteArray()

        val factory = SupportOpenHelperFactory(
            passphrase,
            null,
            false
        )

        return Room.databaseBuilder(
            context,
            RoshniGamesDatabase::class.java,
            "roshni_games.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration() // TODO: Implement proper migrations
            .build()
    }

    @Provides
    @Singleton
    fun provideGameDao(database: RoshniGamesDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    @Singleton
    fun providePlayerDao(database: RoshniGamesDatabase): PlayerDao {
        return database.playerDao()
    }

    @Provides
    @Singleton
    fun provideScoreDao(database: RoshniGamesDatabase): ScoreDao {
        return database.scoreDao()
    }
}