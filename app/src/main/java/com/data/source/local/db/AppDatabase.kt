package com.data.source.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.data.source.local.db.dao.BotDao
import com.data.source.local.db.dao.GroupDao
import com.data.source.local.db.dao.MessageDao
import com.data.source.local.db.dao.UserDao
import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity

@Database(
    entities = [
        MessageEntity::class,
        UserEntity::class,
        GroupDetailsEntity::class,
        GroupMemberEntity::class,
        BotEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun botDao(): BotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migração vazia, pois fallbackToDestructiveMigration cuidará disso.
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chatting_app_database"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}