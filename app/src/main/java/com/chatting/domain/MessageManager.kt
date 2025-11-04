package com.chatting.domain

import android.app.Application
import android.net.Uri
import android.util.Log
import com.data.source.local.LocalDataSource
import com.data.source.remote.RemoteDataSource
import com.data.source.local.db.entities.MessageEntity
import com.data.repository.ProgressRequestBody
import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MessageManager(
    private val application: Application,
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val mediaManager: MediaManager,
    private val httpClient: OkHttpClient
) {
    private val TAG = "MessageManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun sendTextMessage(receiverId: String, messageText: String) {
        scope.launch {
            val myNumber = SecurePrefs.getString("my_number", "") ?: return@launch
            val message = createMessageEntity(myNumber, receiverId, null, messageText, "text")

            localDataSource.insertMessage(message)
            sendMessagePayloadToServer(message, messageText)
        }
    }

    fun sendGroupTextMessage(groupId: Int, messageText: String) {
        scope.launch {
            val myNumber = SecurePrefs.getString("my_number", "") ?: return@launch
            val message = createMessageEntity(myNumber, null, groupId, messageText, "text")

            localDataSource.insertMessage(message)

            try {
                val contentJson = JSONObject().apply { put("text", messageText) }
                val messageJson = createBaseMessageJson("send_to_group", message).apply {
                    put("content", contentJson)
                }
                val sent = remoteDataSource.sendMessage(messageJson)
                if (!sent) { handleSendFailure(message.id) }
            } catch (e: JSONException) {
                handleJsonError(e, message.id)
            }
        }
    }

    fun sendFileMessage(receiverNumber: String, fileUri: Uri?) {
        handleFileMessage(receiverNumber, null, fileUri)
    }

    fun sendGroupFileMessage(groupId: Int, fileUri: Uri?) {
        handleFileMessage(null, groupId, fileUri)
    }

    private fun handleFileMessage(receiverNumber: String?, groupId: Int?, fileUri: Uri?) {
        scope.launch {
            val myNumber = SecurePrefs.getString("my_number", "")
            if (myNumber.isNullOrEmpty() || fileUri == null) {
                Log.w(TAG, "Envio de arquivo cancelado: número ou URI ausente.")
                return@launch
            }

            val persistentLocalPath = mediaManager.createLocalCopy(fileUri)
            if (persistentLocalPath == null) {
                Log.e(TAG, "Falha ao criar cópia local persistente para $fileUri")
                return@launch
            }

            val mimeType = application.contentResolver.getType(fileUri)
            val messageType = determineMessageType(mimeType)
            val originalFileName = mediaManager.getFileName(fileUri) ?: "Arquivo"

            val placeholderMessage = createMessageEntity(
                senderId = myNumber,
                receiverId = receiverNumber,
                groupId = groupId,
                text = originalFileName,
                type = messageType
            ).apply {
                this.localPath = persistentLocalPath
                this.fileSize = mediaManager.getFileSize(fileUri)
                this.status = "sending"
                this.uploadProgress = 0
            }

            localDataSource.insertMessage(placeholderMessage)
            Log.d(TAG, "Placeholder de arquivo salvo localmente: ${placeholderMessage.id}, Path: $persistentLocalPath")

            uploadAndSendWebSocketConfirmation(
                placeholder = placeholderMessage,
                originalFileUri = fileUri,
                originalFileName = originalFileName,
                mimeType = mimeType,
                myNumber = myNumber
            )
        }
    }

    private suspend fun uploadAndSendWebSocketConfirmation(
        placeholder: MessageEntity,
        originalFileUri: Uri,
        originalFileName: String,
        mimeType: String?,
        myNumber: String
    ) {
        val messageId = placeholder.id
        var success = false
        Log.d(TAG, "Iniciando upload para msg ID: $messageId, Arquivo Original: $originalFileName")

        try {
            val fileBytes = mediaManager.getBytesFromUri(originalFileUri) ?: throw IOException("Não foi possível ler os bytes do arquivo do URI.")
            val fileBody = fileBytes.toRequestBody(mimeType?.toMediaTypeOrNull())

            var lastUploadProgress = -1
            val progressFileBody = ProgressRequestBody(fileBody) { bytesWritten, contentLength ->
                if (contentLength > 0) {
                    val percentage = (100 * bytesWritten / contentLength).toInt()
                    if (percentage > lastUploadProgress && (percentage % 5 == 0 || percentage == 100)) {
                        lastUploadProgress = percentage
                        scope.launch(Dispatchers.IO) {
                            try {
                                localDataSource.updateUploadProgress(messageId, percentage)
                            } catch (dbE: Exception) {
                                Log.e(TAG, "Erro ao atualizar progresso do upload para $messageId", dbE)
                            }
                        }
                    }
                }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", originalFileName, progressFileBody)
                .build()

            val authToken = SecurePrefs.getString("auth_token", null) ?: throw IOException("Token de autenticação não encontrado.")
            val request = Request.Builder()
                .url("https://appshub.shop/upload_handler.php")
                .header("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            Log.d(TAG, "Fazendo upload do arquivo para $messageId...")
            val response = httpClient.newCall(request).await()
            Log.d(TAG, "Resposta do upload para $messageId: Code=${response.code}")

            if (!response.isSuccessful) throw IOException("Falha no upload. Code: ${response.code}, Message: ${response.message}")

            val responseBodyString = response.body?.string() ?: throw IOException("Resposta do servidor vazia.")
            response.close()

            val uploadResponse = JSONObject(responseBodyString)

            if (uploadResponse.optString("status") == "success") {
                val fileUrl = uploadResponse.getString("file_url")
                val serverFileSize = uploadResponse.getLong("file_size")
                val serverFileName = uploadResponse.optString("file_name", originalFileName)
                val detectedMimeType = uploadResponse.optString("mime_type", mimeType)

                Log.i(TAG,"Upload bem-sucedido para $messageId. URL: $fileUrl")
                localDataSource.updateUploadedFileDetails(messageId, fileUrl, serverFileSize, "enviada")
                if (lastUploadProgress != 100) localDataSource.updateUploadProgress(messageId, 100)

                val messageContent = JSONObject().apply {
                    put("caption", placeholder.text?.takeIf { it.isNotBlank() && it != originalFileName } ?: "")
                    put("url", fileUrl)
                    put("size", serverFileSize)
                    put("filename", serverFileName)
                    put("mime_type", detectedMimeType)
                }

                val isGroup = placeholder.groupId != null
                val action = if (isGroup) "send_to_group" else "send_to"
                val messageJson = createBaseMessageJson(action, placeholder).apply {
                    put("content", messageContent)
                }

                Log.d(TAG,"Enviando confirmação WebSocket para $messageId...")
                if (!remoteDataSource.sendMessage(messageJson)) {
                    Log.e(TAG, "Falha ao enviar mensagem WebSocket após upload bem-sucedido para $messageId.")
                } else {
                    Log.d(TAG,"Confirmação WebSocket enviada para $messageId.")
                }
                success = true

            } else {
                throw IOException("Erro no upload do servidor: ${uploadResponse.optString("message", "Erro desconhecido")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha no upload/envio do arquivo para a mensagem $messageId", e)
             try { localDataSource.updateMessageStatus(messageId, "falhou") } catch (dbE: Exception) { Log.e(TAG, "Erro ao atualizar status para falhou para $messageId", dbE)}
             success = false
        } finally {
            Log.d(TAG, "Processo de Upload/Envio finalizado para $messageId com sucesso=$success")
        }
    }

    fun deleteMessage(message: MessageEntity) {
        scope.launch {
            localDataSource.deleteMessageById(message.id)
            Log.d(TAG, "Deleted message ${message.id} locally.")

            val isGroup = message.groupId != null
            try {
                val receiverTarget = when {
                    isGroup -> "group_${message.groupId}"
                    message.isMine -> message.receiverId
                    else -> message.senderId
                } ?: message.senderId

                val deleteJson = JSONObject().apply {
                    put("do", if (isGroup) "delete_msg_group" else "delete_msg")
                    put("id", message.id)
                    put("sender", message.senderId)
                    put("receiver", receiverTarget)
                }
                val sent = remoteDataSource.sendMessage(deleteJson)
                if (!sent) Log.e(TAG, "Failed to send delete_msg command for ${message.id}")
            } catch (e: JSONException) {
                Log.e(TAG, "Error creating JSON for delete_msg command", e)
            }
        }
    }

    fun editMessage(message: MessageEntity, newText: String) {
        if (newText.isBlank() || message.type != "text") {
            Log.w(TAG, "Edit ignored: New text is blank or message type is not 'text'.")
            return
        }

        scope.launch {
            val isGroup = message.groupId != null
            val dateNow = System.currentTimeMillis()

            localDataSource.updateMessageContentAndTimestamp(message.id, newText, "editada", dateNow)
            Log.d(TAG, "Edited message ${message.id} locally to '$newText'.")

            try {
                val receiverTarget = when {
                    isGroup -> "group_${message.groupId}"
                    message.isMine -> message.receiverId
                    else -> message.senderId
                } ?: message.senderId

                val editJson = JSONObject().apply {
                    put("do", if (isGroup) "edit_msg_group" else "edit_msg")
                    put("id", message.id)
                    put("message", newText)
                    put("sender", message.senderId)
                    put("receiver", receiverTarget)
                    put("date_now", dateNow)
                }
                val sent = remoteDataSource.sendMessage(editJson)
                if(!sent) {
                    Log.e(TAG, "Failed to send edit_msg command for ${message.id}. Reverting status.")
                    localDataSource.updateMessageStatus(message.id, "failed_edit")
                }

            } catch (e: JSONException) {
                Log.e(TAG, "Error creating JSON for edit_msg command", e)
                localDataSource.updateMessageStatus(message.id, "failed_edit")
            }
        }
    }

    private suspend fun sendMessagePayloadToServer(message: MessageEntity, contentText: String) {
        try {
            val contentJson = JSONObject().apply { put("text", contentText) }
            val messageJson = createBaseMessageJson("send_to", message).apply {
                put("content", contentJson)
            }
            val sent = remoteDataSource.sendMessage(messageJson)
            if (!sent) { handleSendFailure(message.id) }
        } catch (e: JSONException) {
            handleJsonError(e, message.id)
        }
    }

    private suspend fun handleSendFailure(messageId: String) {
        Log.e(TAG, "Falha ao enfileirar mensagem $messageId para envio (problema no WebSocket). Marcando como falha.")
        localDataSource.updateMessageStatus(messageId, "falhou")
    }

    private fun handleJsonError(e: JSONException, messageId: String) {
        Log.e(TAG, "Erro ao criar JSON para mensagem $messageId", e)
        scope.launch { localDataSource.updateMessageStatus(messageId, "falhou") }
    }

    private fun createMessageEntity(senderId: String, receiverId: String?, groupId: Int?, text: String?, type: String): MessageEntity {
        val conversationIdentifier = when {
            groupId != null -> "group_$groupId"
            receiverId != null -> receiverId
            else -> senderId
        }

        return MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = if (groupId == null) conversationIdentifier else null,
            groupId = groupId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = "sending",
            isMine = senderId == SecurePrefs.getString("my_number", ""),
            type = type,
            fileUrl = null, fileSize = null, localPath = null,
            downloadStatus = null, downloadProgress = 0, uploadProgress = 0
        )
    }

    private fun createBaseMessageJson(action: String, message: MessageEntity): JSONObject {
        val receiverTarget = when {
            message.groupId != null -> "group_${message.groupId}"
            message.isMine -> message.receiverId
            else -> message.senderId
        } ?: message.senderId

        return JSONObject().apply {
            put("do", action)
            put("id", message.id)
            put("sender", message.senderId)
            put("receiver", receiverTarget)
            put("date", message.timestamp)
            put("type", message.type)
        }
    }


    private fun determineMessageType(mimeType: String?): String {
        return when {
            mimeType == null -> "file"
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            mimeType == "application/pdf" -> "document"
            mimeType.startsWith("application/vnd.openxmlformats-officedocument") -> "document"
            mimeType.startsWith("application/msword") || mimeType == "application/vnd.ms-excel" || mimeType == "application/vnd.ms-powerpoint" -> "document"
            mimeType == "application/zip" || mimeType == "application/x-rar-compressed" || mimeType == "application/x-7z-compressed" -> "archive"
            mimeType.startsWith("text/") -> "document"
            else -> "file"
        }
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled() && continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            })
            continuation.invokeOnCancellation {
                try {
                    if (!isExecuted()) {
                         cancel()
                    }
                } catch (ex: Throwable) {
                     Log.w("OkHttpAwait", "Exception during cancellation: ${ex.message}")
                }
            }
        }
    }
}