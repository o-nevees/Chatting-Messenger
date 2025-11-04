package com.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import com.data.source.local.db.AppDatabase
import com.data.source.local.db.entities.UserEntity
import com.service.api.ApiService 
import com.chatting.ui.utils.SecurePrefs as ActualSecurePrefs 
import com.chatting.websock.WebSocketClient
import com.chatting.websock.WebSocketState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthState {
    LOGGED_IN,
    LOGGED_OUT
}

class AuthRepository(
    private val application: Application,
    private val webSocketClient: WebSocketClient,
    private val apiService: ApiService 
) {

    private val tag = "AuthRepository" 

    private companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    private object SecurePrefs {
        private var isInitialized = false

        fun ensureInitialized(context: Context) {
            if (!isInitialized) {
                ActualSecurePrefs.init(context.applicationContext)
                isInitialized = true
                Log.d("AuthRepository", "SecurePrefs wrapper initialized.")
            }
        }

        private fun checkInitialized() {
            if (!isInitialized) {
                Log.e("AuthRepository", "SecurePrefs accessed before initialization!", IllegalStateException("SecurePrefs not initialized!"))
            }
        }

        fun getString(key: String, defaultValue: String?): String? {
            checkInitialized()
            return ActualSecurePrefs.getString(key, defaultValue)
        }
        fun putString(key: String, value: String?) {
            checkInitialized()
            if (key == KEY_AUTH_TOKEN || key == KEY_REFRESH_TOKEN) Log.d("AuthRepository", "Writing $key to SecurePrefs.")
            if (value == null) {
                ActualSecurePrefs.remove(key)
            } else {
                ActualSecurePrefs.putString(key, value)
            }
        }
        fun remove(key: String) {
            checkInitialized()
            Log.d("AuthRepository", "Removing $key from SecurePrefs.")
            ActualSecurePrefs.remove(key)
        }
        fun getLong(key: String, defaultValue: Long): Long {
            checkInitialized()
            return ActualSecurePrefs.getLong(key, defaultValue)
        }
        fun putLong(key: String, value: Long) {
            checkInitialized()
            ActualSecurePrefs.putLong(key, value)
        }
    }

    private val _authStateFlow = MutableStateFlow(AuthState.LOGGED_OUT) 
    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    init {
        Log.d(tag, "Initializing AuthRepository class...")
        SecurePrefs.ensureInitialized(application) 

        val initialState = if (SecurePrefs.getString(KEY_AUTH_TOKEN, null) != null) {
            Log.i(tag, "Initial state determined: LOGGED_IN (found auth token).")
            AuthState.LOGGED_IN
        } else {
            Log.i(tag, "Initial state determined: LOGGED_OUT (no auth token found).")
            AuthState.LOGGED_OUT
        }
        
        if (_authStateFlow.value != initialState) {
             _authStateFlow.value = initialState
        }

        if (initialState == AuthState.LOGGED_IN) {
            Log.i(tag, "AuthRepository initialized while LOGGED_IN, attempting WebSocket connection.")
            webSocketClient.connect()
        }
        Log.d(tag, "AuthRepository initialization complete.")
    }


    fun onLoginSuccess(
        authToken: String,
        refreshToken: String,
        user: UserEntity
    ) {
        Log.i(tag, "onLoginSuccess called.")

        updateTokens(authToken, refreshToken)

        val userDataStore = UserDataStore.getInstance(application)
        userDataStore.onLoginSuccess(user)

        SecurePrefs.remove("last_event_id")

        _authStateFlow.value = AuthState.LOGGED_IN
        Log.i(tag, "Auth state set to LOGGED_IN. Attempting WebSocket connection...")
        webSocketClient.connect()
    }

    fun onLogout(context: Context) {
        Log.e(tag, "onLogout called!", RuntimeException("Stack trace for onLogout call"))
        
        Log.i(tag, "Disconnecting WebSocket.")
        webSocketClient.disconnect()

        val applicationContext = context.applicationContext as Application
        val userDataStore = UserDataStore.getInstance(applicationContext)
        userDataStore.onLogout()

        SecurePrefs.remove(KEY_AUTH_TOKEN)
        SecurePrefs.remove(KEY_REFRESH_TOKEN)
        SecurePrefs.remove("last_event_id")

        CoroutineScope(Dispatchers.IO).launch {
            Log.i(tag, "Clearing database tables on logout.")
            try {
                AppDatabase.getDatabase(applicationContext).clearAllTables()
                Log.i(tag, "Database tables cleared successfully.")
            } catch (e: Exception) {
                Log.e(tag, "Error clearing database tables on logout", e)
            }
        }

        _authStateFlow.value = AuthState.LOGGED_OUT
        Log.i(tag, "Auth state set to LOGGED_OUT.")
    }

    suspend fun refreshToken(): Boolean {
        val currentRefreshToken = SecurePrefs.getString(KEY_REFRESH_TOKEN, null)
            ?: return false.also { Log.w(tag, "Refresh token not found.") }

        return withContext(Dispatchers.IO) {
            try {
                Log.i(tag, "Attempting to refresh auth token...")
                val response = apiService.refreshToken(refreshToken = currentRefreshToken)
                val body = response.body()
                
                if (response.isSuccessful && body != null && body.status == "success") {
                    body.data?.let { data ->
                        val authToken = data.authToken
                        val refreshToken = data.refreshToken

                        if (authToken != null && refreshToken != null) {
                            updateTokens(authToken, refreshToken)
                            Log.i(tag, "Token refreshed successfully!")
                            if (webSocketClient.connectionState.value == WebSocketState.FAILED) {
                                Log.i(tag, "WebSocket was FAILED, attempting reconnect after token refresh.")
                                withContext(Dispatchers.Main) { webSocketClient.connect() }
                            }
                            true
                        } else {
                            Log.e(tag, "Refresh token response missing new tokens.")
                            false
                        }
                    } ?: false.also { Log.e(tag, "Refresh token response data block is null.") }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "Failed to refresh token. Code: ${response.code()}, Body: $errorBody")
                    if (response.code() == 401 || response.code() == 403) {
                         Log.w(tag, "Refresh token invalid/expired (HTTP ${response.code()}). Setting state to LOGGED_OUT.")
                         withContext(Dispatchers.Main) {
                             if (_authStateFlow.value != AuthState.LOGGED_OUT) {
                                _authStateFlow.value = AuthState.LOGGED_OUT
                                webSocketClient.disconnect()
                             }
                         }
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e(tag, "Network error during token refresh", e)
                false
            }
        }
    }

    private fun updateTokens(newAuthToken: String, newRefreshToken: String) {
        Log.i(tag, "Saving new tokens. Auth: ${newAuthToken.take(10)}..., Refresh: ${newRefreshToken.take(10)}...")
        SecurePrefs.putString(KEY_AUTH_TOKEN, newAuthToken)
        SecurePrefs.putString(KEY_REFRESH_TOKEN, newRefreshToken)
    }
}