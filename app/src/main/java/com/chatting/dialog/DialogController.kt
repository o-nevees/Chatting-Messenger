package com.chatting.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Observa o [DialogManager] e exibe o [CustomDialog] apropriado
 * quando o estado não for nulo.
 *
 * Coloque este Composable no topo da sua hierarquia de UI
 * (ex: dentro do setContent da sua MainActivity).
 */
@Composable
fun DialogController() {
    // Alterado de dialogData para dialogConfig
    val dialogConfig by DialogManager.dialogState.collectAsState()

    dialogConfig?.let {
        CustomDialog(
            config = it, // Passa a nova config
            onDismiss = { DialogManager.hide() }
        )
    }
}