package com.chatting.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.chatting.domain.MediaManager
import com.chatting.domain.MessageManager
import com.data.repository.ChatRepository
import com.chatting.ui.MyApp
import com.chatting.ui.activitys.ChatItem
import com.data.source.local.db.entities.MessageEntity
import com.data.source.local.db.entities.UserEntity
import com.chatting.ui.model.Conversation
import com.chatting.ui.usecase.GetChatInputStateUseCase
import com.chatting.ui.utils.TimeFormat
import com.chatting.ui.model.*
import com.service.api.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null
)

class ChatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository = (application as MyApp).chatRepository
    private val messageManager: MessageManager = (application as MyApp).messageManager
    private val mediaManager: MediaManager = (application as MyApp).mediaManager

    enum class ConversationFilter { ALL, UNREAD, GROUPS }

    private val allConversations: LiveData<List<Conversation>>
    private val currentFilter = MutableLiveData(ConversationFilter.ALL)
    internal val searchQuery = MutableLiveData("")

    private val _searchUiState = MutableLiveData(SearchUiState())
    val searchUiState: LiveData<SearchUiState> = _searchUiState
    private var searchJob: Job? = null

    private val filteredConversations = MediatorLiveData<List<Conversation>>()
    val unreadCount: LiveData<Int>

    private val _isLoadingInitial = MutableLiveData(true)
    val isLoadingInitial: LiveData<Boolean> = _isLoadingInitial

    private val getChatInputStateUseCase = GetChatInputStateUseCase()

    private val isLoadingMore = MutableLiveData(false)
    val userInfoMap: LiveData<Map<String, UserEntity>>
    private var currentOffset = 0
    private val PAGE_SIZE = 30
    private var isLoading = false
    private var isLastPage = false
    private var initialLoadStarted = false

    val recentConversations: LiveData<List<Conversation>>
    val localSearchResults: LiveData<List<Conversation>>

    private fun filterLocalResults(conversations: List<Conversation>?, query: String?): List<Conversation> {
        if (query.isNullOrBlank() || conversations.isNullOrEmpty()) {
            return emptyList()
        }
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        return conversations.filter {
            it.displayName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
        }
    }


    init {
        allConversations = repository.allConversations
        userInfoMap = repository.getAllUsers().map { users -> users.associateBy { it.number } }

        repository.setExternalIsLoadingInitial(_isLoadingInitial)

        filteredConversations.addSource(allConversations) { applyAllFilters() }
        filteredConversations.addSource(currentFilter) { applyAllFilters() }
        filteredConversations.addSource(searchQuery) { applyAllFilters() }

        recentConversations = allConversations.map {
            it.take(5)
        }
        localSearchResults = MediatorLiveData<List<Conversation>>().apply {
            addSource(allConversations) { convs -> value = filterLocalResults(convs, searchQuery.value) }
            addSource(searchQuery) { query -> value = filterLocalResults(allConversations.value, query) }
        }
        unreadCount = allConversations.map { conversations ->
            conversations.sumOf { it.unreadCount }
        }

        searchQuery.observeForever { query ->
            performApiSearch(query)
        }

        repository.paginationResult.observeForever { result ->
            val (itemsLoaded, lastPage) = result
            isLoading = false
            isLoadingMore.postValue(false)
            isLastPage = lastPage

            if (_isLoadingInitial.value == true) {
                _isLoadingInitial.postValue(false)
            }
        }
    }

    private fun performApiSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            if (_searchUiState.value?.results?.isNotEmpty() == true || _searchUiState.value?.error != null) {
                _searchUiState.value = SearchUiState(results = emptyList())
            }
            return
        }
        _searchUiState.value = SearchUiState(isLoading = true)

        searchJob = viewModelScope.launch {
            delay(300)
            val result = repository.searchEntities(query)
            result.onSuccess { apiResults ->
                val localConversationIds = allConversations.value?.map { it.id }?.toSet() ?: emptySet()
                val filteredResults = apiResults.filter { apiResult ->
                    val potentialLocalId = when (apiResult.type) {
                        "group" -> "group_${apiResult.id}"
                        else -> apiResult.id
                    }
                    !localConversationIds.contains(potentialLocalId)
                }
                _searchUiState.postValue(SearchUiState(isLoading = false, results = filteredResults))
            }.onFailure { error ->
                _searchUiState.postValue(SearchUiState(isLoading = false, error = error.message ?: "Erro desconhecido"))
            }
        }
    }

    fun getChatInputState(conversationId: String): LiveData<ChatInputState> {
        return getConversationDetails(conversationId).distinctUntilChanged().map { conversation ->
            getChatInputStateUseCase(conversation)
        }
    }

    fun triggerInitialLoad() {
        synchronized(this) {
            if (initialLoadStarted) return
            initialLoadStarted = true
        }
        _isLoadingInitial.value = true
        isLoading = true
        currentOffset = 0
        isLastPage = false
        repository.requestDataSync()
    }

    fun isLoadingMore(): LiveData<Boolean> = isLoadingMore

    private fun applyAllFilters() {
        val conversations = allConversations.value ?: emptyList()
        val filter = currentFilter.value ?: ConversationFilter.ALL
        val query = searchQuery.value ?: ""

        val filteredByChip = when (filter) {
            ConversationFilter.UNREAD -> conversations.filter { it.unreadCount > 0 }
            ConversationFilter.GROUPS -> conversations.filter { it.type == ConversationType.GROUP }
            ConversationFilter.ALL -> conversations
        }

        val isApiSearchActiveOrHasResults = _searchUiState.value?.isLoading == true || _searchUiState.value?.results?.isNotEmpty() == true

        filteredConversations.value = if (query.isNotBlank() && !isApiSearchActiveOrHasResults) {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            filteredByChip.filter {
                it.displayName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            }
        } else {
            filteredByChip
        }
    }


    fun setFilter(filter: ConversationFilter) {
        currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun isLoading(): Boolean = isLoading
    fun isLastPage(): Boolean = isLastPage

    fun getConversations(): LiveData<List<Conversation>> = filteredConversations

    fun getUnreadConversationsCount(): LiveData<Int> = unreadCount

    fun getConversationDetails(conversationId: String): LiveData<Conversation?> {
        return allConversations.map { list ->
            list.find { it.id == conversationId }
        }
    }

    fun getProcessedMessages(conversationId: String?): LiveData<List<ChatItem>>? {
        if (conversationId == null) return null

        val rawMessagesLiveData = if (conversationId.startsWith("group_")) {
            val groupId = conversationId.substringAfter("group_").toIntOrNull()
            groupId?.let { repository.getMessagesForGroup(it) }
        } else {
            repository.getMessagesForConversation(conversationId)
        }

        return rawMessagesLiveData?.map { messages ->
            groupMessagesByDate(messages)
        }
    }
    
    fun markMessagesAsRead(conversationId: String?) {
        if (conversationId.isNullOrEmpty()) return
        if (conversationId.startsWith("group_")) {
            val groupId = conversationId.substringAfter("group_").toIntOrNull()
            groupId?.let { repository.markGroupAsRead(it) }
        } else {
            repository.markConversationMessagesAsRead(conversationId)
        }
    }

    fun sendTextMessage(targetId: String?, messageText: String?) {
        if (targetId.isNullOrEmpty() || messageText.isNullOrEmpty()) return
        if (targetId.startsWith("group_")) {
            val groupId = targetId.substringAfter("group_").toIntOrNull()
            groupId?.let { messageManager.sendGroupTextMessage(it, messageText) }
        } else {
            messageManager.sendTextMessage(targetId, messageText)
        }
    }

    fun editMessage(message: MessageEntity, newText: String) {
        messageManager.editMessage(message, newText)
    }

    fun deleteMessage(message: MessageEntity) {
        messageManager.deleteMessage(message)
    }

    fun sendFileMessage(targetId: String?, fileUri: Uri?) {
        if (targetId.isNullOrEmpty() || fileUri == null) return
        if (targetId.startsWith("group_")) {
            val groupId = targetId.substringAfter("group_").toIntOrNull()
            groupId?.let { messageManager.sendGroupFileMessage(it, fileUri) }
        } else {
            messageManager.sendFileMessage(targetId, fileUri)
        }
    }

    fun downloadFile(message: MessageEntity) {
        if (message.fileUrl.isNullOrEmpty()) return
        mediaManager.downloadFile(message)
    }

    fun getCacheSize(): LiveData<String> {
        val cacheSizeLiveData = MutableLiveData("Calculando...")
        mediaManager.getCacheSize { formattedSize ->
            cacheSizeLiveData.postValue(formattedSize)
        }
        return cacheSizeLiveData
    }

    fun clearCache() {
        mediaManager.clearCache { }
    }

    private fun groupMessagesByDate(messages: List<MessageEntity>): List<ChatItem> {
        val items = mutableListOf<ChatItem>()
        if (messages.isEmpty()) return items

        var lastHeaderDate: String? = null

        messages.forEach { message ->
            val messageDate = TimeFormat.getFormattedDateHeader(message.timestamp)

            if (messageDate != lastHeaderDate) {
                items.add(ChatItem.DateHeader(messageDate))
                lastHeaderDate = messageDate
            }
            items.add(ChatItem.Message(message))
        }
        return items
    }
}