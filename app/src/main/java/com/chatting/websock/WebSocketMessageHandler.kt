package com.chatting.websock

import android.app.Application
import android.util.Log
import com.data.receiver.DataReceiver // <<< MODIFICADO

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import com.chatting.ui.utils.SecurePrefs
import org.json.JSONObject

/**
 * Ouve mensagens brutas do WebSocketClient, faz o parse inicial do comando
 * e delega a lógica de negócios para handlers específicos (como AuthHandler ou DataReceiver).
 */
class WebSocketMessageHandler(
    private val application: Application,
    private val webSocketClient: WebSocketClient,
    private val dataReceiver: DataReceiver, // <<< MODIFICADO
    private val authHandler: WebSocketActionHandler
) {
    private val tag = "WebSocketMessageHandler"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messageListenerJob: Job? = null

    fun startListening() {
        if (messageListenerJob != null && messageListenerJob!!.isActive) {
            Log.d(tag, "Already listening to WebSocket messages.")
            return
        }
        Log.i(tag, "Starting to listen for WebSocket messages...")
        messageListenerJob = webSocketClient.messages
            .onEach { rawMessage -> processRawMessage(rawMessage) }
            .catch { e -> Log.e(tag, "Error collecting WebSocket messages", e) }
            .launchIn(scope) // Launch collection in the handler's scope
    }

    fun stopListening() {
        Log.i(tag, "Stopping listening for WebSocket messages.")
        messageListenerJob?.cancel()
        messageListenerJob = null
    }

    private suspend fun processRawMessage(text: String) {
        val separatorIndex = text.indexOf(":")
        if (separatorIndex == -1) {
            Log.w(tag, "Invalid message format (no ':' separator): ${text.take(100)}")
            return
        }

        val command = text.substring(0, separatorIndex).trim()
        val payload = text.substring(separatorIndex + 1)

        if (command.isEmpty()) {
            Log.w(tag, "Empty command in message: ${text.take(100)}")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Processing command '$command'")
                when (command) {
                    // --- Lógica de Auth DELEGADA ---
                    "auth_success" -> authHandler.handleAuthSuccess()
                    "auth_fail" -> authHandler.handleAuthFail()

                    // --- Lógica de Dados DELEGADA para DataReceiver ---
                    "sync_data" -> dataReceiver.processSyncData(payload)
                    "new_message" -> dataReceiver.processIncomingMessage(command, payload)
                    "edit_msg", "edit_msg_group" -> dataReceiver.processEditMessage(command, payload)
                    "delete_msg", "delete_msg_group" -> dataReceiver.processDeleteMessage(command, payload)
                    "is_user_online" -> dataReceiver.processOnlineStatusUpdate(payload)
                    "update_message_status" -> dataReceiver.processMessageStatusUpdateFromServer(payload)
                    "message_read_receipt" -> dataReceiver.processMessageReadReceiptFromServer(payload)

                    else -> Log.w(tag, "Unhandled command: $command")
                }
            } catch (e: JSONException) {
                Log.e(tag, "JSON Error processing command '$command': ${payload.take(500)}", e)
            } catch (e: Exception) {
                Log.e(tag, "Unexpected Error processing command '$command'", e)
            }
        }
    }
}