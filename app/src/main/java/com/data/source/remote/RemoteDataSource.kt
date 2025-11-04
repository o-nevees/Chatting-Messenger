package com.data.source.remote

import com.chatting.websock.WebSocketClient // Importa a interface
import org.json.JSONObject
import javax.inject.Inject // Example using Hilt/Dagger, adjust if using manual DI

/**
 * Manages access to remote data sources, primarily via WebSocket.
 * Delegates message sending to the injected WebSocketClient.
 */
// Use constructor injection (example with Hilt/Dagger, adapt for manual DI)
class RemoteDataSource @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    fun sendMessage(json: JSONObject): Boolean {
        return webSocketClient.sendMessage(json)
    }

     fun sendRawMessage(message: String): Boolean {
         return webSocketClient.sendMessage(message)
     }

    // Future API calls via Retrofit can also be added here.
    // private val apiService: ApiService
}