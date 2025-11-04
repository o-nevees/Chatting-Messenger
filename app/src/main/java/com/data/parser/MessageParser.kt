// File: app/src/main/java/com/chatting/data/parser/MessageParser.kt
package com.data.parser

import android.util.Log
import com.data.source.local.db.entities.MessageEntity
import com.chatting.ui.utils.SecurePrefs
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject // Ou importe adequado para sua injeção de dependência

/**
 * Classe responsável por analisar (parse) mensagens recebidas do servidor (formato JSON)
 * e convertê-las em entidades MessageEntity para o banco de dados local.
 */
class MessageParser @Inject constructor() { // Preparado para injeção de dependência

    private val TAG = "MessageParser"

    /**
     * Converte um JSONObject recebido do servidor em uma entidade MessageEntity.
     * Inclui a lógica de fallback para o campo 'message' que contém JSON stringificado.
     * Requer que SecurePrefs esteja inicializado para obter o número do usuário atual.
     */
    fun parseMessageFromJson(messageJson: JSONObject): MessageEntity? {
        // É crucial que SecurePrefs esteja inicializado antes desta chamada.
        val myNumber = SecurePrefs.getString("my_number", null)
        if (myNumber.isNullOrEmpty()) {
            Log.e(TAG, "Cannot parse message, user number not found in SecurePrefs.")
            // Considerar lançar uma exceção ou retornar um estado de erro
            // em vez de apenas retornar null, dependendo da sua estratégia de erro.
            return null
        }

        try {
            val sender = messageJson.getString("sender")
            val isMine = sender == myNumber

            val groupIdInt = messageJson.optInt("group_id", 0).takeIf { it > 0 }
            // Corrige a lógica: receiver deve ser nulo se for mensagem de grupo
            val receiverField = if (groupIdInt == null) messageJson.optString("receiver", null) else null


            // Determina conversationId, groupId e receiverId corretamente
            val conversationIdentifier: String?
            val groupId: Int?
            val receiverId: String? // O destinatário real da mensagem 1-1

            when {
                // Mensagem de Grupo
                groupIdInt != null -> {
                    conversationIdentifier = null // Não há conversationId para grupos
                    groupId = groupIdInt
                    receiverId = null // Não há receiverId explícito para grupos no modelo
                }
                // Mensagem 1-1 (enviada ou recebida)
                receiverField != null -> {
                    groupId = null
                    receiverId = receiverField // Armazena o destinatário real
                    // O conversationId é sempre o ID do *outro* participante
                    conversationIdentifier = if (isMine) receiverField else sender
                }
                // Caso inválido
                else -> {
                    Log.e(TAG, "Invalid message JSON: missing receiver (for 1-1) and group_id. ID: ${messageJson.optString("id", "N/A")}")
                    return null
                }
            }


            val messageType = messageJson.getString("type")
            var fileUrl: String? = null
            var fileSize: Long? = null
            var textForDisplay: String? = null
            var fileName: String? = null
            var mimeType: String? = null

            // Analisa o objeto 'content'
            val contentJson = messageJson.optJSONObject("content")
            if (contentJson != null) {
                when (messageType.lowercase()) {
                    "text" -> {
                        textForDisplay = contentJson.optString("text", "")
                    }
                    // Agrupa tipos de arquivo
                    "image", "video", "audio", "document", "file", "archive" -> {
                        textForDisplay = contentJson.optString("caption", "") // Legenda
                        fileUrl = contentJson.optString("url", null)?.takeIf { it.isNotBlank() }
                        fileSize = contentJson.optLong("size", 0L).takeIf { it > 0 }
                        fileName = contentJson.optString("filename", null)
                        mimeType = contentJson.optString("mime_type", null)
                    }
                    else -> {
                        Log.w(TAG, "Unsupported message type in content: $messageType. ID: ${messageJson.optString("id")}")
                        textForDisplay = "[Unsupported content type: $messageType]"
                    }
                }
            } else if (messageJson.has("message") && messageType.equals("text", ignoreCase = true)) {
                // Fallback para mensagens de texto onde o conteúdo está no campo 'message'
                // (pode ser string simples ou JSON stringificado)
                val messageString = messageJson.optString("message", null)
                if (messageString != null) {
                    try {
                        // Tenta analisar como JSON interno primeiro
                        val innerJson = JSONObject(messageString)
                        textForDisplay = innerJson.optString("text", "")
                        Log.w(TAG,"Message ${messageJson.optString("id")} parsed using fallback for 'message' field string (as JSON).")
                    } catch (innerE: JSONException) {
                        // Se não for JSON, usa a string diretamente
                        Log.w(TAG, "Message ${messageJson.optString("id")} field 'message' is not valid JSON, using raw string.", innerE)
                        textForDisplay = messageString
                    }
                } else {
                    Log.w(TAG,"Message ${messageJson.optString("id")} is TEXT but 'content' and 'message' fields are missing or null.")
                    textForDisplay = "[Missing text content]"
                }
            } else if (messageType != "text") {
                 Log.w(TAG,"Message ${messageJson.optString("id")} ($messageType) is missing the 'content' object.")
                 // Para arquivos sem 'content', podemos usar o 'message' como nome do arquivo se existir? Ou deixar vazio?
                 textForDisplay = messageJson.optString("message", "[Missing file content]")
            }
             else {
                 Log.w(TAG,"Message ${messageJson.optString("id")} is TEXT and missing 'content' and 'message' fields.")
                 textForDisplay = "[Missing message content]"
            }


            // Status inicial e downloadStatus
            val initialDownloadStatus = if (!isMine && fileUrl != null) "pendente" else null
            val serverStatus = messageJson.optString("status", null) // Status vindo do servidor (pode ser null)
            // Define o status inicial: usa o do servidor se existir, senão 'enviada' (se minha) ou 'recebida' (se não)
            val initialStatus = serverStatus ?: if (isMine) "enviada" else "recebida"

            return MessageEntity(
                id = messageJson.getString("id"),
                conversationId = conversationIdentifier, // Correto para 1-1, null para grupo
                groupId = groupId, // Correto para grupo, null para 1-1
                senderId = sender,
                receiverId = receiverId, // Correto para 1-1, null para grupo
                text = textForDisplay, // Contém texto ou legenda
                timestamp = messageJson.getLong("date"),
                status = initialStatus,
                isMine = isMine,
                type = messageType,
                fileUrl = fileUrl,
                fileSize = fileSize,
                localPath = null, // Será preenchido após o download
                downloadStatus = initialDownloadStatus,
                downloadProgress = 0,
                // Assume 100% de upload para mensagens recebidas ou já enviadas (não 'sending')
                uploadProgress = if (isMine && initialStatus == "sending") 0 else 100
                // Adicionar fileName e mimeType se você adicionar esses campos à MessageEntity
                // fileName = fileName,
                // mimeType = mimeType
            )
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing message JSON: ${messageJson.toString().take(500)}", e)
            return null
        } catch (e: Exception) { // Captura outras exceções inesperadas
             Log.e(TAG, "Unexpected error parsing message JSON: ${messageJson.toString().take(500)}", e)
             return null
        }
    }
}