package com.chatting.di

import android.app.Application
import com.chatting.domain.AccountManager
import com.chatting.domain.MediaManager
import com.chatting.domain.MessageManager
import com.data.parser.MessageParser
import com.data.receiver.DataReceiver
import com.data.receiver.WebSocketDataReceiver
import com.data.repository.DefaultChatRepository

import com.data.source.local.LocalDataSource
import com.data.source.remote.RemoteDataSource
import com.data.repository.ChatRepository
import com.data.repository.AuthRepository
import com.data.source.local.db.AppDatabase

import com.data.repository.UserDataStore
import com.service.api.ApiService
import com.service.api.NetworkConfig
import com.chatting.websock.OkHttpWebSocketClient
import com.chatting.websock.WebSocketActionHandler
import com.chatting.websock.WebSocketClient
import com.chatting.websock.WebSocketMessageHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val application: Application) {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val database by lazy { AppDatabase.getDatabase(application) }
    private val messageDao by lazy { database.messageDao() }
    private val userDao by lazy { database.userDao() }
    private val groupDao by lazy { database.groupDao() }
    private val botDao by lazy { database.botDao() }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private val localDataSource by lazy { LocalDataSource(messageDao, userDao, groupDao, botDao) }
    private val remoteDataSource by lazy { RemoteDataSource(webSocketClient) }

    val webSocketClient: WebSocketClient by lazy {
        OkHttpWebSocketClient(application, applicationScope, okHttpClient)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(application, webSocketClient, apiService)
    }

    private val userDataStore by lazy { UserDataStore.getInstance(application) }
    private val messageParser: MessageParser by lazy { MessageParser() }

    val mediaManager: MediaManager by lazy {
        MediaManager(application, messageDao, okHttpClient)
    }

    val messageManager: MessageManager by lazy {
        MessageManager(
            application = application,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mediaManager = mediaManager,
            httpClient = okHttpClient
        )
    }

    val accountManager: AccountManager by lazy {
        AccountManager(
            application = application,
            apiService = apiService,
            authRepository = authRepository,
            userDataStore = userDataStore
        )
    }

    private val webSocketActionHandler: WebSocketActionHandler by lazy {
        WebSocketActionHandler(
            application = application,
            remoteDataSource = remoteDataSource,
            webSocketClient = webSocketClient,
            authRepository = authRepository
        )
    }
    
    private val webSocketDataReceiver: DataReceiver by lazy {
        WebSocketDataReceiver(
            application = application,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            messageParser = messageParser,
            fileHandler = mediaManager
        )
    }

    val chatRepository: ChatRepository by lazy {
        DefaultChatRepository(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            fileHandler = mediaManager,
            application = application,
            webSocketActionHandler = webSocketActionHandler,
            apiService = apiService
        )
    }

    val webSocketMessageHandler by lazy {
        WebSocketMessageHandler(
            application = application,
            webSocketClient = webSocketClient,
            dataReceiver = webSocketDataReceiver,
            authHandler = webSocketActionHandler
        )
    }

    fun initializeWebSocketListening() {
        webSocketMessageHandler.startListening()
    }
}