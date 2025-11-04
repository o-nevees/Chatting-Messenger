package com.chatting.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chatting.ui.MainActivity
import com.chatting.ui.MyApp // Import MyApp to access AppContainer
import com.chatting.ui.R
import com.chatting.websock.WebSocketClient // Import the client interface
import com.chatting.websock.WebSocketState // Import the state enum
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first // To get the current state synchronously (use with caution)
import kotlinx.coroutines.runBlocking // To get the current state synchronously (use with caution)


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

     // Lazy initialization of WebSocketClient
     private val webSocketClient: WebSocketClient by lazy {
         (applicationContext as MyApp).appContainer.webSocketClient
     }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check WebSocket state synchronously (runBlocking is generally discouraged in services,
        // but might be acceptable here for a quick state check. Consider alternative approaches
        // like having the WebSocketClient post events if this becomes problematic).
        val currentWebSocketState = runBlocking { webSocketClient.connectionState.first() }

        // If the WebSocket is connected, assume the app is active and skip notification
        if (currentWebSocketState == WebSocketState.CONNECTED) {
            Log.d(TAG, "App is likely in foreground (WebSocket connected), notification skipped.")
            return
        }

        // --- Notification Handling ---
        var notificationTitle = getString(R.string.app_name) // Default title
        var notificationBody = "Você tem uma nova mensagem." // Default body

        // Handle notification payload (sent via Firebase Console or similar)
        remoteMessage.notification?.let {
            notificationTitle = it.title ?: notificationTitle
            notificationBody = it.body ?: notificationBody
            Log.d(TAG, "Received Notification Payload - Title: ${it.title}, Body: ${it.body}")
        }

        // Handle data payload (sent from your backend)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Received Data Payload: ${remoteMessage.data}")
            // Override title/body if provided in data payload
            notificationTitle = remoteMessage.data["title"] ?: notificationTitle
            notificationBody = remoteMessage.data["body"] ?: notificationBody
            // TODO: Extract other data (like message ID, sender) if needed for richer notifications or actions
        }

        Log.d(TAG, "Showing Notification - Title: $notificationTitle, Body: $notificationBody")
        sendNotification(notificationTitle, notificationBody)
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement logic to send the token to your backend server.
        // This is crucial for your server to send targeted notifications.
        Log.d(TAG, "Sending FCM token to server (implementation needed): $token")
         // Example: Get user ID/auth token and use Retrofit/OkHttp to send the FCM token
         // val userId = SecurePrefs.getString("user_id", null)
         // if (userId != null) {
         //    CoroutineScope(Dispatchers.IO).launch {
         //       try {
         //          myApiService.updateFcmToken(userId, token)
         //          Log.i(TAG, "FCM token updated on server.")
         //       } catch (e: Exception) {
         //          Log.e(TAG, "Failed to send FCM token to server", e)
         //       }
         //    }
         // }
    }

    private fun sendNotification(messageTitle: String, messageBody: String) {
        // Intent to open MainActivity when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // TODO: Add extras if you want MainActivity to navigate somewhere specific
            // intent.putExtra("conversation_id", remoteMessage.data["conversation_id"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.app_name) // Use app name as channel ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure you have this icon
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true) // Dismiss notification on tap
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For heads-up display
            // TODO: Add actions like "Reply" if desired
            // .addAction(R.drawable.ic_reply, "Reply", replyPendingIntent)
            // TODO: Add styling like BigText or InboxStyle if needed
            // .setStyle(NotificationCompat.BigTextStyle().bigText(longMessageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.app_name) // User-visible channel name
            val channelDescription = "Notificações de novas mensagens" // User-visible description
            val importance = NotificationManager.IMPORTANCE_HIGH // High importance for chat messages
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                // Configure other channel properties (lights, vibration, etc.) if needed
                // enableLights(true)
                // lightColor = Color.BLUE
                // enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification (use a unique ID if you need to update/cancel it later)
        notificationManager.notify(0 /* Notification ID */, notificationBuilder.build())
    }
}