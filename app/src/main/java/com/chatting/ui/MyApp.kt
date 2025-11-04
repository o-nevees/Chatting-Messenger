package com.chatting.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.chatting.di.AppContainer
import com.chatting.domain.AccountManager
import com.chatting.domain.MediaManager
import com.chatting.domain.MessageManager
import com.chatting.ui.helper.ExceptionHandler
import com.data.repository.*
import com.chatting.ui.theme.ThemeManager
import com.chatting.ui.utils.SecurePrefs
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.chatting.vibration.VibrationManager

class MyApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    val chatRepository: ChatRepository
        get() = appContainer.chatRepository

    val authRepository: AuthRepository
        get() = appContainer.authRepository
        
    val messageManager: MessageManager
        get() = appContainer.messageManager
        
    val mediaManager: MediaManager
        get() = appContainer.mediaManager
        
    val accountManager: AccountManager
        get() = appContainer.accountManager

    companion object {
        private const val TAG = "MyApp"
        fun getRepository(context: Context): ChatRepository {
            return (context.applicationContext as MyApp).chatRepository
        }
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            if (authRepository.authStateFlow.value == AuthState.LOGGED_IN) {
                Log.d(TAG, "App in foreground AND Logged In. Attempting WebSocket connection...")
                try {
                    appContainer.webSocketClient.connect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting WebSocket onStart", e)
                }
            } else {
                 Log.d(TAG, "App in foreground but Logged Out. Skipping WebSocket connection.")
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            Log.d(TAG, "App in background. Disconnecting WebSocket.")
            try {
                appContainer.webSocketClient.disconnect()
            } catch (e: Exception) {
                 Log.e(TAG, "Error disconnecting WebSocket onStop", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate started.")

        SecurePrefs.init(this)
        Log.d(TAG, "SecurePrefs initialized.")
        
        VibrationManager.init(this)

        appContainer = AppContainer(this)
        Log.d(TAG, "AppContainer created.")
        
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))
        Log.d(TAG, "ExceptionHandler set.")

        FirebaseApp.initializeApp(this)
        Log.d(TAG, "FirebaseApp initialized.")

        ThemeManager.init(this)
        Log.d(TAG, "ThemeManager initialized.")

        appContainer.initializeWebSocketListening()
        Log.d(TAG, "WebSocketMessageHandler listener started.")

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        Log.d(TAG, "LifecycleObserver added.")

        getAndStoreFCMToken()
        Log.d(TAG, "onCreate finished.")
    }

    private fun getAndStoreFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null) {
                    SecurePrefs.putString("FMC", token)
                    Log.d(TAG, "FCM Token obtained and saved: ${token.take(10)}...")
                } else {
                    Log.w(TAG, "FCM Token obtained but was null.")
                }
            } else {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }
}