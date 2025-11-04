// File: app/src/main/java/com/chatting/ui/ActiveChatManager.kt
package com.chatting.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para rastrear qual conversationId (ou group_id) está
 * atualmente ativo na tela do usuário.
 */
object ActiveChatManager {
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow() // Expõe como StateFlow somente leitura

    fun setActiveChat(conversationId: String?) {
        _activeChatId.value = conversationId
    }
}