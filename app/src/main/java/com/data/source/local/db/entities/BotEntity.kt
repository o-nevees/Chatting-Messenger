package com.data.source.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bots")
data class BotEntity(
    @PrimaryKey val botId: String,
    val botName: String?,
    val bio: String?,
    val profilePhoto: String?,
    val canReceiveMessages: Boolean
)