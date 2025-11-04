package com.chatting.ui.usecase

import com.chatting.ui.model.Conversation
import com.chatting.ui.viewmodel.ChatInputState
import com.chatting.ui.model.*

// Este Caso de Uso não precisa de construtor se a lógica for simples
class GetChatInputStateUseCase {

    // O operador 'invoke' permite chamar a classe como se fosse uma função
    operator fun invoke(conversation: Conversation?): ChatInputState {
        if (conversation == null) {
            return ChatInputState.Disabled // Se a conversa ainda não carregou, desabilita
        }

        // ✅ **A LÓGICA DE NEGÓCIO VIVE AQUI, ISOLADA E TESTÁVEL**
        if (conversation.type == ConversationType.BOT && conversation.canReceiveMessages == false) {
            return ChatInputState.ReadOnly("Apenas ${conversation.displayName} pode enviar mensagens.")
        }

        // Adicione outras regras aqui no futuro:
        // if (conversation.isArchived) {
        //     return ChatInputState.ReadOnly("Este grupo está arquivado.")
        // }
        // if (user.isBlocked) {
        //     return ChatInputState.ReadOnly("Você não pode responder a esta conversa.")
        // }

        // Se nenhuma regra se aplicar, a barra de digitação está habilitada
        return ChatInputState.Enabled()
    }
}