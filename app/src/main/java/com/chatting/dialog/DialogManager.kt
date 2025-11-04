package com.chatting.dialog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Gerenciador singleton para controlar a exibição de diálogos customizados.
 *
 * Para usar:
 * 1. Chame `DialogManager.show(DialogConfig(...))` de qualquer lugar.
 * 2. Certifique-se de que `DialogController()` está sendo chamado no topo da sua
 * hierarquia de UI (ex: em MainActivity).
 */
object DialogManager {

    // Alterado de DialogData? para DialogConfig?
    private val _dialogState = MutableStateFlow<DialogConfig?>(null)
    val dialogState = _dialogState.asStateFlow()

    /**
     * Solicita a exibição de um novo diálogo.
     * @param config A configuração completa do diálogo a ser exibido.
     */
    fun show(config: DialogConfig) {
        _dialogState.update { config }
    }

    /**
     * Fecha o diálogo atualmente exibido.
     */
    fun hide() {
        _dialogState.update { null }
    }
}