package com.data.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.chatting.domain.MediaManager

import com.data.source.local.LocalDataSource
import com.data.source.remote.RemoteDataSource
import com.data.repository.ChatRepository
import com.service.api.ApiService
import com.service.api.NetworkConfig
import com.service.api.SearchResult

import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity
import com.chatting.ui.model.Conversation
import com.chatting.ui.model.ConversationQueryResult
import com.chatting.ui.model.ConversationType

import com.chatting.ui.utils.SecurePrefs
import com.chatting.websock.WebSocketActionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class DefaultChatRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val fileHandler: MediaManager,
    private val application: Application,
    private val webSocketActionHandler: WebSocketActionHandler,
    private val apiService: ApiService
) : ChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var externalIsLoadingInitial: MutableLiveData<Boolean>? = null
    private val _paginationResult = MutableLiveData<Pair<Int, Boolean>>()
    override val paginationResult: LiveData<Pair<Int, Boolean>> = _paginationResult
    override val allConversations: LiveData<List<Conversation>>

    companion object {
        private const val TAG = "DefaultChatRepository"
    }

    init {
        allConversations = initializeConversationsLiveData()
    }

    private fun formatLastMessageText(
        text: String?,
        type: String?,
        localPath: String?
    ): String? {
        if (type == null) return null
        return when (type) {
            "text" -> text
            "image" -> localPath?.let { "[IMAGE]$it" } ?: "📷 Imagem"
            "video" -> localPath?.let { "[VIDEO]$it" } ?: "🎥 Vídeo"
            "audio" -> "🎵 Áudio"
            "document", "file", "archive" -> "📄 Arquivo"
            else -> text ?: "📄 Arquivo"
        }
    }

    private fun initializeConversationsLiveData(): LiveData<List<Conversation>> {
        val queryResultLiveData = localDataSource.getConversations()

        return queryResultLiveData.map { queryResults ->
            queryResults.map { queryResult ->
                Conversation(
                    id = queryResult.id,
                    type = queryResult.type,
                    displayName = queryResult.displayName,
                    profilePhoto = queryResult.profilePhoto,
                    lastOnline = queryResult.lastOnline,
                    canReceiveMessages = queryResult.canReceiveMessages,
                    messageDateTimestamp = queryResult.messageDateTimestamp,
                    unreadCount = queryResult.unreadCount,
                    lastMessage = formatLastMessageText(
                        text = queryResult.lastMessageText,
                        type = queryResult.lastMessageType,
                        localPath = queryResult.lastMessageLocalPath
                    )
                )
            }
        }
    }

    override fun requestDataSync() {
        Log.d(TAG, "ChatRepository.requestDataSync() called, delegating to WebSocketActionHandler.")
        webSocketActionHandler.requestDataSync()
    }

    override fun markConversationMessagesAsRead(conversationId: String) {
        repositoryScope.launch {
            markMessagesAsReadInternal(conversationId, null)
        }
    }

    override fun markGroupAsRead(groupId: Int) {
        repositoryScope.launch {
            markMessagesAsReadInternal(null, groupId)
        }
    }

    private suspend fun markMessagesAsReadInternal(conversationId: String?, groupId: Int?) {
        val unreadMessages = if (conversationId != null) {
            localDataSource.getUnreadMessagesForConversationSync(conversationId)
        } else if (groupId != null) {
            localDataSource.getUnreadMessagesForGroupSync(groupId)
        } else {
            emptyList()
        }

        val receivedUnreadIds = unreadMessages.filter { !it.isMine && it.status != "lida" }.map { it.id }

        if (receivedUnreadIds.isEmpty()) {
             Log.d(TAG, "No unread received messages to mark as read for ${conversationId ?: "group $groupId"}.")
            return
        }

        localDataSource.updateStatusToLidaForIds(receivedUnreadIds)
        Log.d(TAG, "Marked ${receivedUnreadIds.size} messages as read locally for ${conversationId ?: "group $groupId"}.")

        sendReadReceiptToServer(conversationId, groupId, receivedUnreadIds)
    }

    private suspend fun sendReadReceiptToServer(conversationId: String?, groupId: Int?, messageIds: List<String>) {
        if (messageIds.isEmpty()) return

        try {
            val myNumber = SecurePrefs.getString("my_number", null)
            if (myNumber.isNullOrEmpty()) {
                 Log.w(TAG, "Cannot send read receipt: User number not found.")
                return
            }

            val readReceiptJson = JSONObject().apply {
                put("reader_id", myNumber)
                put("messages_ids", JSONArray(messageIds))

                if (conversationId != null) {
                    put("do", "messages_were_read")
                    put("sender_of_messages_id", conversationId)
                } else if (groupId != null) {
                    put("do", "group_messages_were_read")
                    put("group_id", groupId)
                } else {
                    Log.w(TAG, "Cannot send read receipt: Missing conversationId or groupId.")
                    return
                }
            }
             Log.d(TAG, "Sending read receipt: ${readReceiptJson.toString().take(200)}")
            val sent = remoteDataSource.sendMessage(readReceiptJson)
            if(!sent) Log.e(TAG, "Failed to send read receipt for ${conversationId ?: "group $groupId"}")

        } catch (e: JSONException) {
            Log.e(TAG, "Error creating/sending JSON for read receipt", e)
        }
    }

    override suspend fun searchEntities(query: String): Result<List<SearchResult>> {
        if (query.length < 2) {
            return Result.success(emptyList())
        }
        return try {
            val authToken = SecurePrefs.getString("auth_token", null) ?: run {
                Log.e(TAG, "Cannot search: Auth token not found.")
                return Result.failure(Exception("Autenticação necessária."))
            }

            Log.d(TAG, "Chamando API GET /search com query: '$query'")
            val response = apiService.searchEntities(
                authToken = "Bearer $authToken",
                query = query
            )

            val body = response.body()

            if (response.isSuccessful && body?.status == "success") {
                val results = body.data ?: emptyList()
                Log.d(TAG, "Busca bem-sucedida. Encontrados ${results.size} resultados para '$query'.")
                
                Result.success(results)
            } else {
                val errorMsg = body?.message ?: response.errorBody()?.string() ?: "Erro ${response.code()}"
                Log.e(TAG, "Falha na busca por '$query'. Resposta do servidor: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro de rede durante a busca por '$query'", e)
            Result.failure(Exception("Erro de rede durante a busca."))
        }
    }

    override fun getMessagesForConversation(conversationId: String): LiveData<List<MessageEntity>> = localDataSource.getMessagesForConversation(conversationId)
    override fun getMessagesForGroup(groupId: Int): LiveData<List<MessageEntity>> = localDataSource.getMessagesForGroup(groupId)
    override fun onPaginationResult(itemsLoaded: Int, isLastPage: Boolean) { _paginationResult.postValue(itemsLoaded to isLastPage) }
    override fun setExternalIsLoadingInitial(liveData: MutableLiveData<Boolean>) { this.externalIsLoadingInitial = liveData }
    override fun getAllUsers(): LiveData<List<UserEntity>> = localDataSource.getAllUsers()
    override fun getAllGroups(): LiveData<List<GroupDetailsEntity>> = localDataSource.getAllGroups()
    override fun getAllBots(): LiveData<List<BotEntity>> = localDataSource.getAllBots()
    override suspend fun insertMessages(messages: List<MessageEntity>) = localDataSource.insertMessages(messages)
    override suspend fun insertOrUpdateUser(user: UserEntity) = localDataSource.insertOrUpdateUser(user)
    override suspend fun insertOrUpdateBot(bot: BotEntity) = localDataSource.insertOrUpdateBot(bot)
    override suspend fun insertOrUpdateUsers(users: List<UserEntity>) = localDataSource.insertOrUpdateUsers(users)
    override suspend fun insertOrUpdateBots(bots: List<BotEntity>) = localDataSource.insertOrUpdateBots(bots)
    override suspend fun insertOrUpdateGroups(groups: List<GroupDetailsEntity>) = localDataSource.insertOrUpdateGroups(groups)
    override suspend fun insertOrUpdateGroupMembers(members: List<GroupMemberEntity>) = localDataSource.insertOrUpdateGroupMembers(members)
    override suspend fun updateGroupAndMembers(groupDetails: GroupDetailsEntity, members: List<GroupMemberEntity>) = localDataSource.updateGroupAndMembers(groupDetails, members)
    override suspend fun clearAllDataForFullSync() {
        localDataSource.clearMessages()
        localDataSource.clearUsers()
        localDataSource.clearGroups()
        localDataSource.clearGroupMembers()
        localDataSource.clearBots()
        Log.i(TAG, "Cleared all local data tables for full sync.")
    }
    override suspend fun updateMessageContent(messageId: String, newText: String, newStatus: String, newTimestamp: Long) {
         localDataSource.updateMessageContentAndTimestamp(messageId, newText, newStatus, newTimestamp)
    }
    override suspend fun deleteMessageById(messageId: String) = localDataSource.deleteMessageById(messageId)
    override suspend fun updateMessageStatus(messageId: String, newStatus: String) = localDataSource.updateMessageStatus(messageId, newStatus)
    override suspend fun updateStatusToLidaForIds(ids: List<String>) = localDataSource.updateStatusToLidaForIds(ids)
    override suspend fun getUserById(userId: String): UserEntity? = localDataSource.getUserById(userId)
    override suspend fun getBotById(botId: String): BotEntity? = localDataSource.getBotById(botId)
    override suspend fun getGroupDetailsById(groupId: Int): GroupDetailsEntity? = localDataSource.getGroupDetailsById(groupId)
    override suspend fun getMessageByIdSync(messageId: String): MessageEntity? = localDataSource.getMessageByIdSync(messageId)
}