package com.chatting.ui.model

import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity

// Data class não-entidade para representar uma conversa na UI
data class Conversation(
    val id: String,
    val type: ConversationType,
    val displayName: String?,
    val lastMessage: String?,
    val messageDateTimestamp: Long?,
    val profilePhoto: String?,
    
    val unreadCount: Int,
    val lastOnline: String? = null,
    val canReceiveMessages: Boolean? = null
) {
    
}