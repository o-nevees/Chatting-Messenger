package com.chatting.websock

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * Interface defining the contract for WebSocket communication.
 */
interface WebSocketClient {

    /**
     * Represents the current state of the WebSocket connection.
     */
    val connectionState: StateFlow<WebSocketState>

    /**
     * A flow emitting raw messages received from the server.
     */
    val messages: SharedFlow<String> // Use SharedFlow for multiple collectors

    /**
     * Attempts to establish a WebSocket connection.
     * Requires authentication details to be available (e.g., from SecurePrefs).
     */
    fun connect()

    /**
     * Closes the WebSocket connection gracefully.
     */
    fun disconnect()

    /**
     * Sends a JSON message to the server.
     * @param json The JSONObject to send.
     * @return True if the message was queued for sending, false otherwise (e.g., not connected).
     */
    fun sendMessage(json: JSONObject): Boolean

    /**
     * Sends a raw string message to the server.
     * @param message The string message to send.
     * @return True if the message was queued for sending, false otherwise.
     */
    fun sendMessage(message: String): Boolean
}

/**
 * Enum representing the possible states of the WebSocket connection.
 */
enum class WebSocketState {
    DISCONNECTED, // Explicitly disconnected or initial state
    CONNECTING,   // Attempting to connect or reconnect
    CONNECTED,    // Actively connected
    FAILED        // Failed to connect after retries or due to auth issues
}