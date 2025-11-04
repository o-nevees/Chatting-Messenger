package com.data.receiver

import android.app.Application
import android.util.Log
import com.chatting.domain.MediaManager
import com.data.parser.MessageParser
import com.data.source.local.LocalDataSource
import com.data.source.remote.RemoteDataSource
import com.service.api.NetworkConfig
import com.chatting.ui.ActiveChatManager
import com.data.source.local.db.entities.BotEntity
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity

import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class WebSocketDataReceiver @Inject constructor(
    private val application: Application,
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val messageParser: MessageParser,
    private val fileHandler: MediaManager
) : DataReceiver {

    private val tag = "WebSocketDataReceiver"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun processSyncData(jsonData: String) {
        try {
            Log.d(tag, "Processing sync_data: ${jsonData.take(500)}...")
            val data = JSONObject(jsonData)
            val syncType = data.optString("type", "unknown")
            val newLastEventId = data.optLong("last_event_id", 0L)
            val activeChatId = ActiveChatManager.activeChatId.first()

            when (syncType) {
                "full_sync" -> handleFullSync(data, activeChatId)
                "event_sync" -> handleEventSync(data, activeChatId)
                else -> Log.w(tag, "Unknown sync type received: $syncType")
            }

            val currentLastEventId = SecurePrefs.getLong("last_event_id", 0L)
            if (newLastEventId > currentLastEventId) {
                SecurePrefs.putLong("last_event_id", newLastEventId)
                Log.i(tag, "Sync finished. Updated local last_event_id to $newLastEventId")
            } else if (syncType == "full_sync" && newLastEventId < currentLastEventId) {
                SecurePrefs.putLong("last_event_id", newLastEventId)
                Log.w(tag,"Full Sync received older last_event_id ($newLastEventId < $currentLastEventId). Resetting local value.")
            } else {
                 Log.i(tag, "Sync finished. Local last_event_id ($currentLastEventId) remains unchanged or is ahead.")
            }

        } catch (e: JSONException) {
            Log.e(tag, "JSON error processing 'sync_data': ${e.message}", e)
        } catch (e: Exception) {
            Log.e(tag, "Fatal error processing 'sync_data'", e)
        }
    }

    private suspend fun handleFullSync(data: JSONObject, activeChatId: String?) {
        Log.i(tag, "--- Starting Full Sync Processing ---")
        clearAllDataForFullSync()

        val users = parseJsonArray(data.optJSONArray("users")) { parseUserEntity(it) }
        val bots = parseJsonArray(data.optJSONArray("bots")) { parseBotEntity(it) }
        val groups = parseJsonArray(data.optJSONArray("groups")) { parseGroupDetailsEntity(it) }
        val groupMembers = parseJsonArray(data.optJSONArray("groupMembers")) { parseGroupMemberEntity(it) }

        if (users.isNotEmpty()) localDataSource.insertOrUpdateUsers(users)
        if (bots.isNotEmpty()) localDataSource.insertOrUpdateBots(bots)
        if (groups.isNotEmpty()) localDataSource.insertOrUpdateGroups(groups)
        if (groupMembers.isNotEmpty()) localDataSource.insertOrUpdateGroupMembers(groupMembers)
        Log.i(tag, "Full Sync: Inserted entities - Users:${users.size}, Bots:${bots.size}, Groups:${groups.size}, Members:${groupMembers.size}")

        val conversationsArray = data.optJSONArray("conversations")
        val allMessagesToInsert = mutableListOf<MessageEntity>()
        val readReceiptMap = mutableMapOf<String, MutableList<String>>()

        conversationsArray?.let { convs ->
            for (i in 0 until convs.length()) {
                try {
                    val messagesArray = convs.getJSONObject(i).optJSONArray("messages")
                    messagesArray?.let { msgs ->
                        for (j in 0 until msgs.length()) {
                            messageParser.parseMessageFromJson(msgs.getJSONObject(j))?.let { message ->
                                val messageConversationId = message.conversationId ?: "group_${message.groupId}"
                                if (!message.isMine && messageConversationId == activeChatId && message.status != "lida") {
                                    message.status = "lida"
                                    val key = message.conversationId ?: message.groupId.toString()
                                    readReceiptMap.getOrPut(key) { mutableListOf() }.add(message.id)
                                }
                                allMessagesToInsert.add(message)
                            }
                        }
                    }
                } catch (e: JSONException) { Log.e(tag, "Error parsing conversation at index $i in Full Sync", e) }
            }
        }
        if (allMessagesToInsert.isNotEmpty()) {
            localDataSource.insertMessages(allMessagesToInsert)
            Log.i(tag, "Full Sync: Inserted ${allMessagesToInsert.size} messages.")
        }

        sendAccumulatedReadReceipts(readReceiptMap)

        Log.i(tag, "--- Full Sync Processing Completed ---")
    }

    private suspend fun handleEventSync(data: JSONObject, activeChatId: String?) {
         Log.i(tag, "--- Starting Event Sync Processing ---")
         val readReceiptMap = mutableMapOf<String, MutableList<String>>()

         val users = parseJsonArray(data.optJSONArray("users")) { parseUserEntity(it) }
         val bots = parseJsonArray(data.optJSONArray("bots")) { parseBotEntity(it) }
         val groups = parseJsonArray(data.optJSONArray("groups")) { parseGroupDetailsEntity(it) }
         val groupMembers = parseJsonArray(data.optJSONArray("groupMembers")) { parseGroupMemberEntity(it) }

         if (users.isNotEmpty()) localDataSource.insertOrUpdateUsers(users)
         if (bots.isNotEmpty()) localDataSource.insertOrUpdateBots(bots)
         if (groups.isNotEmpty()) localDataSource.insertOrUpdateGroups(groups)
         if (groupMembers.isNotEmpty()) localDataSource.insertOrUpdateGroupMembers(groupMembers)

         if (users.isNotEmpty() || bots.isNotEmpty() || groups.isNotEmpty() || groupMembers.isNotEmpty()) {
             Log.i(tag, "Event Sync: Updated entities - Users:${users.size}, Bots:${bots.size}, Groups:${groups.size}, Members:${groupMembers.size}")
         }

        val eventsArray = data.optJSONArray("events") ?: JSONArray()

        Log.i(tag, "Event Sync: Processing ${eventsArray.length()} events.")
        for (i in 0 until eventsArray.length()) {
            try {
                val event = eventsArray.getJSONObject(i)
                val eventType = event.getString("event_type")
                val eventData = event.getJSONObject("event_data")
                val entityId = event.optString("entity_id", null)

                when (eventType) {
                    "new_message" -> {
                        messageParser.parseMessageFromJson(eventData)?.let { message ->
                            
                            val messageConversationId = message.conversationId ?: "group_${message.groupId}"

                            if (!message.isMine && messageConversationId == activeChatId && message.status != "lida") {
                                message.status = "lida"
                                val key = message.conversationId ?: message.groupId.toString()
                                readReceiptMap.getOrPut(key) { mutableListOf() }.add(message.id)
                            }
                            localDataSource.insertMessages(listOf(message))
                            Log.d(tag, "Event: Processed new_message ${message.id} with status ${message.status}")
                        }
                    }
                    "message_edited" -> {
                        val messageId = eventData.optString("id")
                        val newText = eventData.optString("message", null)
                        val newTimestamp = eventData.optLong("date_now", System.currentTimeMillis())

                        if (!messageId.isNullOrEmpty() && newText != null) {
                           localDataSource.updateMessageContentAndTimestamp(messageId, newText, "editada", newTimestamp)
                           Log.d(tag, "Event: Processed message_edited $messageId")
                        } else { Log.w(tag, "Event: Invalid message_edited data: $eventData") }
                    }
                    "message_deleted" -> {
                         val messageId = eventData.optString("id")
                         if (!messageId.isNullOrEmpty()) {
                            localDataSource.deleteMessageById(messageId)
                            Log.d(tag, "Event: Processed message_deleted $messageId")
                         } else { Log.w(tag, "Event: Invalid message_deleted data: $eventData") }
                    }
                    "profile_updated", "group_updated", "group_created", "members_added", "members_removed" -> {
                        Log.d(tag, "Event: Processed '$eventType' for Entity ID: $entityId (Data likely updated from main event_sync payload).")
                    }
                    else -> Log.w(tag, "Event: Unhandled event type '$eventType'")
                }
            } catch (e: JSONException) {
                 Log.e(tag, "Error processing event at index $i in Event Sync", e)
            }
        }
         sendAccumulatedReadReceipts(readReceiptMap)
         Log.i(tag, "--- Event Sync Processing Completed ---")
    }

    override suspend fun processIncomingMessage(command: String, jsonData: String) {
        try {
            val messageJson = JSONObject(jsonData)
            messageParser.parseMessageFromJson(messageJson)?.let { message ->
                
                val activeChatId = ActiveChatManager.activeChatId.first()
                val messageConversationId = message.conversationId ?: "group_${message.groupId}"

                if (!message.isMine && messageConversationId == activeChatId && message.status != "lida") {
                    Log.d(tag, "Incoming message ${message.id} is for the active chat '$activeChatId'. Marking as read immediately.")
                    message.status = "lida"
                    sendReadReceiptToServer(message.conversationId, message.groupId, listOf(message.id))
                }

                localDataSource.insertMessages(listOf(message))
                Log.d(tag, "Processed incoming message ($command): ${message.id} with final status '${message.status}'")

                if (!message.isMine && message.fileUrl != null && message.localPath == null) {
                    fileHandler.downloadFile(message)
                }
            }
        } catch (e: JSONException) {
            Log.e(tag, "JSON error processing incoming message ($command): ${jsonData.take(500)}", e)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error processing incoming message ($command)", e)
        }
    }

    override suspend fun processEditMessage(command: String, jsonData: String) {
        try {
            val messageJson = JSONObject(jsonData)
            val messageId = messageJson.getString("id")
            val newText = messageJson.getString("message")
            val newTimestamp = messageJson.getLong("date_now")

            localDataSource.updateMessageContentAndTimestamp(messageId, newText, "editada", newTimestamp)
            Log.d(tag, "Processed message edit ($command) for ID: $messageId")
        } catch (e: JSONException) {
            Log.e(tag, "JSON error processing edit message ($command): ${jsonData.take(500)}", e)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error processing edit message ($command)", e)
        }
    }

    override suspend fun processDeleteMessage(command: String, jsonData: String) {
         try {
             val messageJson = JSONObject(jsonData)
             val messageId = messageJson.getString("id")

             localDataSource.deleteMessageById(messageId)
             Log.d(tag, "Processed message delete ($command) for ID: $messageId")
         } catch (e: JSONException) {
             Log.e(tag, "JSON error processing delete message ($command): ${jsonData.take(500)}", e)
         } catch (e: Exception) {
             Log.e(tag, "Unexpected error processing delete message ($command)", e)
         }
    }

    override suspend fun processMessageStatusUpdateFromServer(payload: String) {
        val json = JSONObject(payload)
        val messageId = json.getString("id")
        val newStatus = json.getString("status")
        
        val currentMessage = localDataSource.getMessageByIdSync(messageId)
        val myNumber = SecurePrefs.getString("my_number", null)

        if (currentMessage == null || !currentMessage.isMine || myNumber == null) {
             Log.d(tag,"Ignoring server status update for message $messageId: Not found, not mine, or user number missing.")
            return
        }

        val currentStatusOrder = statusOrder(currentMessage.status)
        val newStatusOrder = statusOrder(newStatus)

        val shouldUpdate = newStatusOrder > currentStatusOrder || newStatus.equals("falhou", ignoreCase = true)

        if (shouldUpdate) {
            localDataSource.updateMessageStatus(messageId, newStatus)
            Log.i(tag, "Updated status for sent message $messageId from '${currentMessage.status}' to '$newStatus' based on server update.")
        } else {
            Log.d(tag, "Ignoring server status update for sent message $messageId to '$newStatus' (Current: '${currentMessage.status}', Order: $currentStatusOrder >= $newStatusOrder)")
        }
    }

    override suspend fun processMessageReadReceiptFromServer(payload: String) {
         val json = JSONObject(payload)
         val messageIdsArray = json.getJSONArray("messages_ids")
         val readerId = json.getString("reader_id")
         val myNumber = SecurePrefs.getString("my_number", null)

         if (myNumber.isNullOrEmpty() || readerId == myNumber) {
              Log.d(tag, "Ignoring read receipt: Missing own number or it's my own receipt from $readerId.")
             return
         }

        val idsList = mutableListOf<String>()
        for (i in 0 until messageIdsArray.length()) {
            idsList.add(messageIdsArray.getString(i))
        }

         if (idsList.isNotEmpty()) {
             val messagesToUpdate = idsList
                 .mapNotNull { localDataSource.getMessageByIdSync(it) }
                 .filter { it.isMine && statusOrder(it.status) < statusOrder("lida") }
                 .map { it.id }

             if (messagesToUpdate.isNotEmpty()) {
                 localDataSource.updateStatusToLidaForIds(messagesToUpdate)
                 Log.i(tag, "Processed read receipt from $readerId. Marked my messages as read: ${messagesToUpdate.joinToString()}")
             } else {
                 Log.d(tag, "Read receipt received from $readerId, but contained no relevant messages needing status update.")
             }
         } else {
             Log.w(tag, "Received empty read receipt from $readerId.")
         }
    }

    override suspend fun processOnlineStatusUpdate(payload: String) {
         try {
            val json = JSONObject(payload)
            val number = json.getString("number")
            val status = json.getString("status")
            
            val user = localDataSource.getUserById(number)
            user?.let {
                if (it.lastOnline != status) {
                    localDataSource.insertOrUpdateUser(it.copy(lastOnline = status))
                    Log.d(tag, "Updated online status for user $number to '$status' in local DB.")
                } else {
                    Log.v(tag, "Online status for user $number ('$status') is already up-to-date locally.")
                }
            } ?: run {
                Log.w(tag, "Received online status update for unknown user $number. Requesting details...")
                
            }
            
            Log.d(tag, "Processed online status for $number: $status")
        } catch (e: JSONException) {
            Log.e(tag, "Error processing 'is_user_online' payload: $payload", e)
        }
    }

    private fun statusOrder(status: String?): Int {
        return when (status?.lowercase()) {
            "sending" -> 0
            "enviada" -> 1
            "recebida" -> 2
            "lida" -> 3
            "falhou", "failed_edit" -> -1
            else -> -2
        }
    }

    private suspend fun sendReadReceiptToServer(conversationId: String?, groupId: Int?, messageIds: List<String>) {
        if (messageIds.isEmpty()) return

        try {
            val myNumber = SecurePrefs.getString("my_number", null)
            if (myNumber.isNullOrEmpty()) {
                 Log.w(tag, "Cannot send read receipt: User number not found.")
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
                    Log.w(tag, "Cannot send read receipt: Missing conversationId or groupId.")
                    return
                }
            }
             Log.d(tag, "Sending read receipt: ${readReceiptJson.toString().take(200)}")
            val sent = remoteDataSource.sendMessage(readReceiptJson)
            if(!sent) Log.e(tag, "Failed to send read receipt for ${conversationId ?: "group $groupId"}")

        } catch (e: JSONException) {
            Log.e(tag, "Error creating/sending JSON for read receipt", e)
        }
    }
    
    private suspend fun sendAccumulatedReadReceipts(readReceiptMap: Map<String, List<String>>) {
        if (readReceiptMap.isEmpty()) return

        Log.d(tag, "Sending accumulated read receipts for ${readReceiptMap.size} conversations/groups.")
        readReceiptMap.forEach { (key, ids) ->
            if (ids.isNotEmpty()) {
                val groupId = key.toIntOrNull()
                val conversationId = if (groupId == null) key else null
                sendReadReceiptToServer(conversationId, groupId, ids)
            }
        }
    }

    private inline fun <T> parseJsonArray(jsonArray: JSONArray?, parser: (JSONObject) -> T?): List<T> {
        val list = mutableListOf<T>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                try {
                    parser(it.getJSONObject(i))?.let { parsedItem -> list.add(parsedItem) }
                } catch (e: JSONException) {
                    Log.e(tag, "Error parsing item at index $i in JSONArray", e)
                }
            }
        }
        return list
    }

    private fun parseUserEntity(json: JSONObject): UserEntity? {
        return try {
            UserEntity(
                number = json.getString("number"),
                username1 = json.optString("username1", null),
                username2 = json.optString("username2", null),
                profilePhoto = json.optString("profile_photo", null)?.takeIf { it.isNotBlank() },
                lastOnline = json.optString("last_online", null)
            )
        } catch (e: JSONException) { Log.e(tag, "Error parsing UserEntity JSON: $json", e); null }
    }

     private fun parseBotEntity(json: JSONObject): BotEntity? {
         return try {
             BotEntity(
                 botId = json.getString("id"),
                 botName = json.optString("displayName", null),
                 bio = json.optString("bio", null),
                 profilePhoto = json.optString("profilePhoto", null)?.takeIf { it.isNotBlank() },
                 canReceiveMessages = json.optBoolean("can_receive_messages", true)
             )
         } catch (e: JSONException) { Log.e(tag, "Error parsing BotEntity JSON: $json", e); null }
     }

     private fun parseGroupDetailsEntity(json: JSONObject): GroupDetailsEntity? {
         return try {
             GroupDetailsEntity(
                 groupId = json.getInt("group_id"),
                 groupName = json.optString("groupName", null),
                 groupIcon = json.optString("groupIcon", null)?.takeIf { it.isNotBlank() }
             )
         } catch (e: JSONException) { Log.e(tag, "Error parsing GroupDetailsEntity JSON: $json", e); null }
     }

     private fun parseGroupMemberEntity(json: JSONObject): GroupMemberEntity? {
          return try {
              GroupMemberEntity(
                  groupId = json.getInt("group_id"),
                  userNumber = json.getString("user_number")
              )
          } catch (e: JSONException) { Log.e(tag, "Error parsing GroupMemberEntity JSON: $json", e); null }
      }
     
     private suspend fun clearAllDataForFullSync() {
        localDataSource.clearMessages()
        localDataSource.clearUsers()
        localDataSource.clearGroups()
        localDataSource.clearGroupMembers()
        localDataSource.clearBots()
        Log.i(tag, "Cleared all local data tables for full sync.")
    }
}