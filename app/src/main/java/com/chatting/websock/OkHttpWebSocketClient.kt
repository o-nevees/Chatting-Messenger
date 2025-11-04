package com.chatting.websock

import android.app.Application
import android.util.Log
import com.service.api.NetworkConfig
import com.data.repository.AuthRepository
import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Implementation of WebSocketClient using OkHttp.
 * Manages connection state, message sending/receiving, and reconnection logic.
 */
class OkHttpWebSocketClient(
    private val application: Application, // Needed for SecurePrefs context
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val okHttpClient: OkHttpClient // <-- CLIENTE UNIFICADO INJETADO
) : WebSocketClient {

    private val tag = "OkHttpWebSocketClient"

    private val _connectionState = MutableStateFlow(WebSocketState.DISCONNECTED)
    override val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    // Use SharedFlow for messages to support multiple collectors if needed in the future
    private val _messages = MutableSharedFlow<String>(
        replay = 0, // No replay needed for messages
        extraBufferCapacity = 64, // Buffer capacity
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Strategy for buffer overflow
    )
    override val messages: SharedFlow<String> = _messages.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null
    private var isManuallyDisconnected = false
    private var reconnectAttempts = 0

    // REMOVIDO: O OkHttpClient interno foi removido.
    // private val okHttpClient: OkHttpClient by lazy { ... }

    init {
        // Initialize SecurePrefs if not already done (consider doing this reliably in Application#onCreate)
        SecurePrefs.init(application.applicationContext)
    }

    override fun connect() {
        externalScope.launch {
            synchronized(this) {
                if (_connectionState.value == WebSocketState.CONNECTING || _connectionState.value == WebSocketState.CONNECTED) {
                    Log.d(tag, "Connection attempt ignored: already connecting or connected.")
                    return@launch
                }
                isManuallyDisconnected = false // Reset manual flag on connect attempt
                Log.i(tag, "Initiating WebSocket connection...")
                updateState(WebSocketState.CONNECTING)
            }
            performConnection()
        }
    }

    private suspend fun performConnection() {
        val authToken = SecurePrefs.getString("auth_token", null)
        if (authToken == null) {
            Log.w(tag, "Connection blocked: authToken not found.")
            updateState(WebSocketState.FAILED)
            // Optionally notify AuthRepository or trigger token refresh here
            return
        }

        try {
            val request = Request.Builder().url(NetworkConfig.WEBSOCKET_URL).build()
            // Ensure previous socket is cancelled before creating a new one
            synchronized(this) {
                webSocket?.cancel() // Cancel ongoing connection or existing socket
                // <-- USA O CLIENTE UNIFICADO INJETADO
                webSocket = okHttpClient.newWebSocket(request, SocketListener())
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initiating WebSocket connection: ${e.message}", e)
            handleDisconnection(isError = true, code = -1, reason = e.message ?: "Client-side exception")
        }
    }

    override fun disconnect() {
        externalScope.launch {
            synchronized(this) {
                Log.i(tag, "Manual disconnection requested.")
                isManuallyDisconnected = true
                reconnectAttempts = 0 // Reset attempts on manual disconnect
                if (_connectionState.value != WebSocketState.DISCONNECTED) {
                    // REMOVED: sendDisconnectMessage() // Attempt to notify server
                    webSocket?.close(1000, "Client disconnecting") // Close gracefully
                    webSocket = null
                    updateState(WebSocketState.DISCONNECTED)
                }
            }
        }
    }

    override fun sendMessage(json: JSONObject): Boolean {
        return sendMessage(json.toString())
    }

    override fun sendMessage(message: String): Boolean {
        return synchronized(this) {
            if (_connectionState.value != WebSocketState.CONNECTED) {
                Log.w(tag, "Cannot send message. WebSocket is not connected (State: ${_connectionState.value}). Message: ${message.take(100)}")
                return@synchronized false
            }
            Log.d(tag, "Sending: ${message.take(500)}${if (message.length > 500) "..." else ""}")
            val sent = webSocket?.send(message) ?: false
            if (!sent) {
                Log.e(tag, "Failed to queue message for sending. The connection might have dropped.")
                // Consider triggering a reconnect or notifying failure
            }
            sent
        }
    }

    private fun updateState(newState: WebSocketState) {
        if (_connectionState.value == newState) return
        // Avoid logging transitions from CONNECTING back to CONNECTING during retries if desired
        // if (_connectionState.value == WebSocketState.CONNECTING && newState == WebSocketState.CONNECTING) return

        Log.i(tag, "Connection state changed from ${_connectionState.value} to $newState")
        _connectionState.value = newState

        // If connection fails, trigger AuthRepository to handle potential logout etc.
        if (newState == WebSocketState.FAILED) {
            Log.e(tag, "WebSocket connection failed permanently.")
            // Consider notifying AuthRepository or another component
            // Example: AuthRepository.handleWebSocketFailure()
        }
    }

    private fun handleDisconnection(isError: Boolean, code: Int, reason: String?) {
        synchronized(this) {
            // Only proceed if not already manually disconnected or failed
            if (isManuallyDisconnected || _connectionState.value == WebSocketState.DISCONNECTED || _connectionState.value == WebSocketState.FAILED) {
                Log.d(tag, "handleDisconnection ignored: Already disconnected or failed. isManual=$isManuallyDisconnected, state=${_connectionState.value}")
                // Ensure socket is nullified if it wasn't already
                 if (webSocket != null && code != -1) { // -1 indicates client-side exception before socket creation
                     webSocket?.cancel() // Ensure resources are released
                     webSocket = null
                 }
                return
            }

            webSocket = null // Nullify the socket

            Log.w(tag, "WebSocket disconnected. isError=$isError, Code=$code, Reason=${reason ?: "N/A"}")

            // Decide whether to reconnect based on error and attempts
            if (isError || code != 1000) { // Reconnect on errors or non-graceful closures
                scheduleReconnect()
            } else {
                // Graceful closure (code 1000), treat as disconnected unless connection was expected
                updateState(WebSocketState.DISCONNECTED)
            }
        }
    }

    private fun scheduleReconnect() {
        synchronized(this) {
             if (isManuallyDisconnected || _connectionState.value == WebSocketState.FAILED) {
                 Log.d(tag, "Reconnect cancelled: Manually disconnected or already failed.")
                return
            }
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                Log.e(tag, "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS). Giving up.")
                updateState(WebSocketState.FAILED)
                return
            }
        }

        reconnectAttempts++
        val delayMs = min(INITIAL_RECONNECT_DELAY * 2.0.pow(reconnectAttempts - 1).toLong(), MAX_RECONNECT_DELAY)

        Log.i(tag, "Scheduling reconnect attempt ${reconnectAttempts + 1}/$MAX_RECONNECT_ATTEMPTS in ${delayMs}ms")
        updateState(WebSocketState.CONNECTING) // Show connecting state during delay

        externalScope.launch {
            delay(delayMs)
            // Double-check state before attempting connection again
             if (!isManuallyDisconnected && _connectionState.value != WebSocketState.CONNECTED && _connectionState.value != WebSocketState.FAILED) {
                Log.d(tag, "Executing scheduled reconnect attempt ${reconnectAttempts + 1}")
                performConnection()
            } else {
                 Log.d(tag, "Reconnect attempt ${reconnectAttempts + 1} aborted due to state change.")
            }
        }
    }

    // REMOVED: private fun sendDisconnectMessage() { ... }

    // --- WebSocket Listener ---

    private inner class SocketListener : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) {
            Log.i(tag, "WebSocket Connected! Response: ${response.code}")
            synchronized(this@OkHttpWebSocketClient) {
                // Guard against race conditions if disconnect was called during onOpen
                if (isManuallyDisconnected) {
                    Log.w(tag, "onOpen detected manual disconnect flag. Closing...")
                    ws.close(1000, "Client disconnecting immediately after open")
                    return
                }
                 webSocket = ws // Assign the active socket
                reconnectAttempts = 0 // Reset on successful connection
                updateState(WebSocketState.CONNECTED)
            }
            sendAuthentication()
        }

        private fun sendAuthentication() {
            try {
                val authToken = SecurePrefs.getString("auth_token", null)
                if (authToken != null) {
                    val authJson = JSONObject().apply {
                        put("do", "auth")
                        put("token", authToken)
                    }
                     // Send message via the main class method which checks state
                    val sent = sendMessage(authJson)
                    if (sent) Log.i(tag,"Sent authentication token.") else Log.e(tag, "Failed to send auth token immediately after open.")

                } else {
                    Log.e(tag, "onOpen: authToken became null. Disconnecting.")
                    // Use the main disconnect method (will run on externalScope)
                    disconnect()
                }
            } catch (e: JSONException) {
                Log.e(tag, "Error creating authentication JSON", e)
                 // Use the main disconnect method
                disconnect()
            }
        }

         override fun onMessage(ws: WebSocket, text: String) {
             Log.v(tag, "Raw Message Received: ${text.take(500)}${if (text.length > 500) "..." else ""}")
             // Emit the raw message to the flow for the handler to process
             val emitted = _messages.tryEmit(text)
             if (!emitted) {
                 Log.w(tag, "Failed to emit message to flow, buffer might be full or no collectors.")
                 // Handle buffer overflow - maybe log more details or increase buffer
             }
         }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.w(tag, "WebSocket Peer Closing: Code=$code, Reason=$reason")
            // Server initiated closing. Handle disconnection logic.
            handleDisconnection(isError = code != 1000, code = code, reason = reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.w(tag, "WebSocket Closed by Peer: Code=$code, Reason=$reason")
            // Handle disconnection logic, might already be handled by onClosing.
             handleDisconnection(isError = code != 1000, code = code, reason = reason)
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            // Log full error details, including response if available
            val responseInfo = response?.let { " Code=${it.code} Message=${it.message}" } ?: " N/A"
            Log.e(tag, "WebSocket Failure: ${t.message}. Response:$responseInfo", t)
            // Treat failure as an error requiring reconnection attempt.
             handleDisconnection(isError = true, code = response?.code ?: -1, reason = t.message)
        }
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY = 2000L // 2 seconds
        private const val MAX_RECONNECT_DELAY = 30000L // 30 seconds
    }
}