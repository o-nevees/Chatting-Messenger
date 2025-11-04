package com.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity
import com.chatting.ui.model.Conversation
import com.service.api.SearchResult

interface ChatRepository {

    val allConversations: LiveData<List<Conversation>>
    val paginationResult: LiveData<Pair<Int, Boolean>>

    fun getAllUsers(): LiveData<List<UserEntity>>
    fun getAllGroups(): LiveData<List<GroupDetailsEntity>>
    fun getAllBots(): LiveData<List<BotEntity>>
    fun getMessagesForConversation(conversationId: String): LiveData<List<MessageEntity>>
    fun getMessagesForGroup(groupId: Int): LiveData<List<MessageEntity>>

    fun markConversationMessagesAsRead(conversationId: String)
    fun markGroupAsRead(groupId: Int)
    suspend fun searchEntities(query: String): Result<List<SearchResult>>

    fun requestDataSync()
    fun onPaginationResult(itemsLoaded: Int, isLastPage: Boolean)
    fun setExternalIsLoadingInitial(liveData: MutableLiveData<Boolean>)

    suspend fun insertMessages(messages: List<MessageEntity>)
    suspend fun insertOrUpdateUser(user: UserEntity)
    suspend fun insertOrUpdateBot(bot: BotEntity)
    suspend fun insertOrUpdateUsers(users: List<UserEntity>)
    suspend fun insertOrUpdateBots(bots: List<BotEntity>)
    suspend fun insertOrUpdateGroups(groups: List<GroupDetailsEntity>)
    suspend fun insertOrUpdateGroupMembers(members: List<GroupMemberEntity>)
    suspend fun updateGroupAndMembers(groupDetails: GroupDetailsEntity, members: List<GroupMemberEntity>)
    suspend fun clearAllDataForFullSync()
    suspend fun updateMessageContent(messageId: String, newText: String, newStatus: String, newTimestamp: Long)
    suspend fun deleteMessageById(messageId: String)
    suspend fun updateMessageStatus(messageId: String, newStatus: String)
    suspend fun updateStatusToLidaForIds(ids: List<String>)

    suspend fun getUserById(userId: String): UserEntity?
    suspend fun getBotById(botId: String): BotEntity?
    suspend fun getGroupDetailsById(groupId: Int): GroupDetailsEntity?
    suspend fun getMessageByIdSync(messageId: String): MessageEntity?
}