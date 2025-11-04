package com.data.source.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.source.local.db.entities.BotEntity

@Dao
interface BotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBot(bot: BotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBots(bots: List<BotEntity>) // Added for bulk sync

    @Query("SELECT * FROM bots WHERE botId = :botId")
    suspend fun getBotById(botId: String): BotEntity?

    @Query("SELECT * FROM bots")
    fun getAllBots(): LiveData<List<BotEntity>>

    
    @Query("DELETE FROM bots")
    suspend fun clearTable() // Added for full sync
}