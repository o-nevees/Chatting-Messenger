package com.chatting.ui.viewmodel

// Esta classe selada representa todos os estados possíveis da barra de entrada.
sealed class ChatInputState {
    // Estado para quando o usuário pode digitar normalmente
    data class Enabled(val text: String = "") : ChatInputState()

    // Estado para quando a barra deve ser substituída por um aviso
    data class ReadOnly(val message: String) : ChatInputState()

    // Estado para quando a barra deve estar presente, mas desabilitada
    object Disabled : ChatInputState()
}