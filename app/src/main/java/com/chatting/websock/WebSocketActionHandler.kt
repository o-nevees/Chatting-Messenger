package com.chatting.websock

import android.app.Application
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.data.source.remote.RemoteDataSource
import com.data.repository.AuthRepository
import com.chatting.ui.BuildConfig
import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Classe centralizada para lidar com ações de WebSocket, como autenticação e sincronização.
 * Instanciada pelo AppContainer e injetada onde necessário.
 */
class WebSocketActionHandler(
    private val application: Application,
    private val remoteDataSource: RemoteDataSource,
    private val webSocketClient: WebSocketClient,
    private val authRepository: AuthRepository // Agora recebe a instância da classe
) {

    private val tag = "WebSocketActionHandler"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Chamado quando o backend confirma a autenticação do WebSocket (auth_success).
     */
    fun handleAuthSuccess() {
        Log.i(tag, "WebSocket authentication successful! Requesting data sync...")
        // A autenticação foi bem-sucedida, agora aciona o data sync.
        requestDataSync()
    }

    /**
     * Chamado quando o backend recusa a autenticação (auth_fail).
     */
    fun handleAuthFail() {
        scope.launch {
            Log.w(tag, "WebSocket authentication failed. Refreshing HTTP token...")
            // A autenticação do WS falhou, provavelmente porque o auth_token HTTP expirou.
            // Tenta renovar o token HTTP.
            val refreshed = authRepository.refreshToken()

            if (!refreshed) {
                Log.e(tag, "Failed to refresh HTTP token after auth_fail. Disconnecting WebSocket.")
                // Se a renovação falhar, desconecta o WS e o AuthRepository
                // (que já deve ter sido) colocará o app em estado LOGGED_OUT.
                withContext(Dispatchers.Main) {
                    webSocketClient.disconnect()
                }
            } else {
                Log.i(tag, "HTTP Token refreshed successfully after auth_fail. WebSocket client should reconnect and re-authenticate automatically.")
                // O WebSocketClient (OkHttpWebSocketClient) deve tentar reconectar
                // e, ao reconectar (onOpen), enviará o *novo* auth_token.
            }
        }
    }

    /**
     * Envia o comando 'sync' para o WebSocket com dados expandidos do dispositivo.
     * Esta é agora a única fonte para esta ação.
     */
    fun requestDataSync() {
        scope.launch {
            Log.d(tag, "Requesting data sync...")
            val deviceId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            if (deviceId.isEmpty()) {
                Log.e(tag, "Cannot get device ID. Sync aborted.")
                return@launch
            }

            val lastEventId = SecurePrefs.getLong("last_event_id", 0L)
            val fmcToken = SecurePrefs.getString("FMC", null)

            try {
                // Payload JSON expandido para incluir mais dados para o futuro
                val requestJson = JSONObject().apply {
                    put("do", "sync")
                    put("id_device", deviceId)
                    put("fmc_token", fmcToken ?: JSONObject.NULL)
                    put("last_known_event_id_on_client", lastEventId)

                    // --- Novos dados para funcionalidades futuras ---
                    put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("os_version", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    put("app_version_name", BuildConfig.VERSION_NAME)
                    put("app_version_code", BuildConfig.VERSION_CODE)
                    put("locale", Locale.getDefault().toString()) // ex: "pt_BR"
                }

                Log.d(tag, "Sending sync request: ${requestJson.toString(2)}")
                val sent = remoteDataSource.sendMessage(requestJson)
                if (!sent) {
                    Log.e(tag, "Failed to send sync request via WebSocket (queue failure).")
                }
            } catch (e: JSONException) {
                Log.e(tag, "Error creating JSON for sync request", e)
            }
        }
    }
}