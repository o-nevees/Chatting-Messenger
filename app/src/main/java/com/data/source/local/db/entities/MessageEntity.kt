package com.data.source.local.db.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["groupId"]),
        Index(value = ["id"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey
    var id: String,
    var conversationId: String?,
    var groupId: Int?,
    var senderId: String,
    var receiverId: String?,
    var text: String?,
    var timestamp: Long?,
    var status: String?,
    var isMine: Boolean,
    var type: String,
    var fileUrl: String?,
    var fileSize: Long?,
    var localPath: String?,

    @ColumnInfo(name = "downloadStatus")
    var downloadStatus: String? = null,

    @ColumnInfo(name = "downloadProgress", defaultValue = "0")
    var downloadProgress: Int = 0,
    
    @ColumnInfo(name = "uploadProgress", defaultValue = "0")
    var uploadProgress: Int = 0

) : Parcelable {
    init {
        require(conversationId != null || groupId != null) {
            "A mensagem deve ter um conversationId ou um groupId"
        }
    }
}