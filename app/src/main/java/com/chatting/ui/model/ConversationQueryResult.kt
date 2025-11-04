package com.chatting.ui.model

import com.chatting.ui.model.ConversationType

/**
 * POJO (Plain Old Java Object) para armazenar o resultado da query complexa de conversas do Room.
 * Esta classe é então mapeada para a classe 'Conversation' da UI no repositório.
 */
data class ConversationQueryResult(
    // --- Identificação da Conversa ---
    val id: String,
    val type: ConversationType, // O SQL usará 'USER', 'GROUP', 'BOT'

    // --- Detalhes da Entidade (User, Group, Bot) ---
    val displayName: String?,
    val profilePhoto: String?,
    
    // --- Detalhes Específicos (User, Bot) ---
    val lastOnline: String?,
    val canReceiveMessages: Boolean?,

    // --- Detalhes da Última Mensagem (Message) ---
    val lastMessageText: String?, // m.text
    val lastMessageType: String?, // m.type
    val lastMessageLocalPath: String?, // m.localPath
    val messageDateTimestamp: Long?, // m.timestamp

    // --- Contagem de Não Lidas (Calculado) ---
    val unreadCount: Int
)