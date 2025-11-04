package com.data.source.local

import androidx.lifecycle.LiveData
import com.data.source.local.db.dao.BotDao
import com.data.source.local.db.dao.GroupDao
import com.data.source.local.db.dao.MessageDao
import com.data.source.local.db.dao.UserDao
import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity
import com.chatting.ui.model.ConversationQueryResult // <<< ADICIONADO

/**
 * Gerencia o acesso aos dados locais através dos DAOs do Room.
 * É a única classe que interage diretamente com o banco de dados.
 */
class LocalDataSource(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val botDao: BotDao
) {
    // --- LiveData Observables ---
    
    // <<< NOVA FUNÇÃO >>>
    fun getConversations(): LiveData<List<ConversationQueryResult>> = messageDao.getConversations()
    
    fun getAllUsers(): LiveData<List<UserEntity>> = userDao.getAllUsers()
    fun getAllGroups(): LiveData<List<GroupDetailsEntity>> = groupDao.getAllGroupDetails()
    fun getAllBots(): LiveData<List<BotEntity>> = botDao.getAllBots()
    fun getMessagesForConversation(conversationId: String): LiveData<List<MessageEntity>> = messageDao.getMessagesForConversation(conversationId)
    fun getMessagesForGroup(groupId: Int): LiveData<List<MessageEntity>> = messageDao.getMessagesForGroup(groupId)

    // --- Suspend Functions (Sync/Internal Logic) ---
    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId) // Busca síncrona
    suspend fun getBotById(botId: String): BotEntity? = botDao.getBotById(botId) // Busca síncrona
    suspend fun getGroupDetailsById(groupId: Int): GroupDetailsEntity? = groupDao.getGroupDetailsById(groupId) // Busca síncrona
    suspend fun getMessageByIdSync(messageId: String): MessageEntity? = messageDao.getMessageByIdSync(messageId) // Busca síncrona

    suspend fun getUnreadCountForConversation(conversationIdentifier: String): Int = messageDao.getUnreadCountForConversation(conversationIdentifier)
    suspend fun getUnreadMessagesForConversationSync(conversationId: String): List<MessageEntity> = messageDao.getUnreadMessagesForConversationSync(conversationId)
    suspend fun getUnreadMessagesForGroupSync(groupId: Int): List<MessageEntity> = messageDao.getUnreadMessagesForGroupSync(groupId)
    suspend fun getPendingMessagesForConversationSync(conversationId: String, status: String): List<MessageEntity> = messageDao.getPendingMessagesForConversationSync(conversationId, status)

    // --- Insert/Update Operations ---
    suspend fun insertMessages(messages: List<MessageEntity>) = messageDao.insertAllMessages(messages)
    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun insertOrUpdateUser(user: UserEntity) = userDao.insertOrUpdateUser(user)
    suspend fun insertOrUpdateBot(bot: BotEntity) = botDao.insertOrUpdateBot(bot)
    suspend fun updateGroupAndMembers(groupDetails: GroupDetailsEntity, members: List<GroupMemberEntity>) = groupDao.updateGroupAndMembers(groupDetails, members)
    suspend fun updateMessageContentAndTimestamp(messageId: String, newText: String, newStatus: String, newTimestamp: Long) = messageDao.updateMessageContentAndTimestamp(messageId, newText, newStatus, newTimestamp)
    suspend fun updateMessageStatus(messageId: String, newStatus: String) = messageDao.updateMessageStatus(messageId, newStatus)
    suspend fun updateStatusToLidaForIds(ids: List<String>) = messageDao.updateStatusToLidaForIds(ids)
    
    suspend fun updateUploadProgress(messageId: String, progress: Int) = messageDao.updateUploadProgress(messageId, progress) // <<< ADICIONADO
    suspend fun updateUploadedFileDetails(messageId: String, fileUrl: String?, fileSize: Long?, status: String) = messageDao.updateUploadedFileDetails(messageId, fileUrl, fileSize, status) // <<< ADICIONADO


    // --- Bulk Operations for Sync ---
    suspend fun insertOrUpdateUsers(users: List<UserEntity>) = userDao.insertOrUpdateUsers(users) // Adicionar no UserDao
    suspend fun insertOrUpdateBots(bots: List<BotEntity>) = botDao.insertOrUpdateBots(bots) // Adicionar no BotDao
    suspend fun insertOrUpdateGroups(groups: List<GroupDetailsEntity>) = groupDao.insertOrUpdateGroups(groups) // Adicionar no GroupDao
    suspend fun insertOrUpdateGroupMembers(members: List<GroupMemberEntity>) = groupDao.insertGroupMembers(members) // Reutiliza existente, mas pode precisar de @Insert(onConflict = OnConflictStrategy.REPLACE) se necessário


    // --- Delete Operations ---
    suspend fun deleteMessageById(messageId: String) = messageDao.deleteMessageById(messageId)
    suspend fun clearMessages() = messageDao.clearTable()
    suspend fun clearUsers() = userDao.clearTable() // Adicionar no UserDao
    suspend fun clearGroups() = groupDao.clearGroupDetailsTable() // Adicionar no GroupDao
    suspend fun clearGroupMembers() = groupDao.clearGroupMembersTable() // Adicionar no GroupDao
    suspend fun clearBots() = botDao.clearTable() // Adicionar no BotDao
}