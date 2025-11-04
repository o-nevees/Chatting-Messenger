package com.data.source.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.data.source.local.db.entities.MessageEntity
import com.chatting.ui.model.ConversationQueryResult
import com.chatting.ui.model.ConversationType

@Dao
interface MessageDao {

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isMine = 0 AND status <> 'lida'")
    suspend fun getUnreadMessagesForConversationSync(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE groupId = :groupId AND isMine = 0 AND status <> 'lida'")
    suspend fun getUnreadMessagesForGroupSync(groupId: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND status = :status ORDER BY timestamp ASC")
    fun getPendingMessagesForConversationSync(conversationId: String, status: String): List<MessageEntity>

    @Query("UPDATE messages SET status = 'lida' WHERE id IN (:messageIds)")
    suspend fun updateStatusToLidaForIds(messageIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForConversation(conversationId: String): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForConversationSync(conversationId: String): MessageEntity?

    @Query("UPDATE messages SET status = 'lida' WHERE conversationId = :conversationId AND isMine = 0 AND status != 'lida'")
    suspend fun markConversationMessagesAsRead(conversationId: String)

    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForGroup(groupId: Int): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForGroupSync(groupId: Int): MessageEntity?

    @Query("UPDATE messages SET status = 'lida' WHERE groupId = :groupId AND isMine = 0 AND status != 'lida'")
    suspend fun markGroupMessagesAsRead(groupId: Int)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageByIdSync(messageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("UPDATE messages SET status = :newStatus WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: String)

    @Query("UPDATE messages SET text = :newText, status = :newStatus, timestamp = :newTimestamp WHERE id = :messageId")
    suspend fun updateMessageContentAndTimestamp(messageId: String, newText: String, newStatus: String, newTimestamp: Long): Int

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId LIMIT 1)")
    suspend fun messageExistsSync(messageId: String): Boolean

    @Query("UPDATE messages SET localPath = :localPath WHERE id = :messageId")
    suspend fun updateMessageFileLocalPath(messageId: String, localPath: String?)

    @Query("UPDATE messages SET fileUrl = :fileUrl, fileSize = :fileSize, status = :status WHERE id = :messageId")
    suspend fun updateUploadedFileDetails(messageId: String, fileUrl: String?, fileSize: Long?, status: String)

    @Query("UPDATE messages SET downloadStatus = :status, downloadProgress = :progress WHERE id = :messageId")
    suspend fun updateDownloadStatus(messageId: String, status: String, progress: Int)

    @Query("UPDATE messages SET uploadProgress = :progress WHERE id = :messageId")
    suspend fun updateUploadProgress(messageId: String, progress: Int)

    @Query("DELETE FROM messages")
    suspend fun clearTable()

    @Transaction
    @Query("""
        WITH AllConversations AS (
            SELECT 
                number AS id, 
                'USER' AS type 
            FROM users
            
            UNION
            
            SELECT 
                botId AS id, 
                'BOT' AS type 
            FROM bots
            
            UNION
            
            SELECT 
                'group_' || groupId AS id, 
                'GROUP' AS type 
            FROM group_details
        ),
        LatestMessages AS (
            SELECT 
                m.*,
                CASE 
                    WHEN m.conversationId IS NOT NULL THEN m.conversationId 
                    ELSE 'group_' || m.groupId 
                END AS convId
            FROM messages m
            INNER JOIN (
                SELECT
                    CASE 
                        WHEN conversationId IS NOT NULL THEN conversationId 
                        ELSE 'group_' || groupId 
                    END as conversation_identifier,
                    MAX(timestamp) as max_timestamp
                FROM messages
                WHERE timestamp IS NOT NULL
                GROUP BY conversation_identifier
            ) AS latest 
            ON (CASE WHEN m.conversationId IS NOT NULL THEN m.conversationId ELSE 'group_' || m.groupId END) = latest.conversation_identifier 
            AND m.timestamp = latest.max_timestamp
        )
        SELECT
            ac.id AS id,
            ac.type AS type,
            
            COALESCE(u.username1, g.groupName, b.botName) AS displayName,
            COALESCE(u.profilePhoto, g.groupIcon, b.profilePhoto) AS profilePhoto,
            
            u.lastOnline AS lastOnline,
            b.canReceiveMessages AS canReceiveMessages,
            
            lm.text AS lastMessageText,
            lm.type AS lastMessageType,
            lm.localPath AS lastMessageLocalPath,
            lm.timestamp AS messageDateTimestamp,
            
            (
                SELECT COUNT(*)
                FROM messages m_unread
                WHERE (m_unread.conversationId = ac.id OR 'group_' || m_unread.groupId = ac.id)
                AND m_unread.isMine = 0
                AND m_unread.status <> 'lida'
            ) AS unreadCount
            
        FROM AllConversations ac
        
        LEFT JOIN users u ON ac.id = u.number AND ac.type = 'USER'
        LEFT JOIN bots b ON ac.id = b.botId AND ac.type = 'BOT'
        LEFT JOIN group_details g ON ac.id = 'group_' || g.groupId AND ac.type = 'GROUP'
        
        LEFT JOIN LatestMessages lm ON ac.id = lm.convId
        
        WHERE (displayName IS NOT NULL OR lm.id IS NOT NULL)

        ORDER BY messageDateTimestamp DESC
    """)
    fun getConversations(): LiveData<List<ConversationQueryResult>>

    @Query("SELECT COUNT(*) FROM messages WHERE (CASE WHEN conversationId IS NOT NULL THEN conversationId ELSE 'group_' || groupId END) = :conversationIdentifier AND isMine = 0 AND status <> 'lida'")
    suspend fun getUnreadCountForConversation(conversationIdentifier: String): Int
}