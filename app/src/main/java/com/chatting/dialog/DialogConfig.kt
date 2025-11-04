package com.chatting.dialog

import androidx.compose.runtime.Composable
// Importações de 'Color' e 'ImageVector' não são mais necessárias aqui

/**
 * Representa a configuração de um botão de ação no diálogo.
 * @param text O texto a ser exibido no botão.
 * @param isFilled Controla se o botão é preenchido (Button) ou apenas texto (TextButton).
 * @param onClick A ação a ser executada quando o botão for clicado.
 */
data class DialogButtonConfig(
    val text: String,
    val isFilled: Boolean = false,
    val onClick: () -> Unit = {} // Ação padrão vazia
)

// A data class 'DialogListItem' foi REMOVIDA

/**
 * Classe de dados unificada que armazena toda a configuração para um diálogo.
 * O DialogManager usará uma instância desta classe para saber o que exibir.
 *
 * @param type O estilo visual do diálogo (ex: BASIC, SUCCESS).
 * @param title O título principal do diálogo.
 * @param subtitle O texto de descrição abaixo do título (opcional).
 * @param positiveButton Configuração do botão de ação principal (opcional).
 * @param negativeButton Configuração do botão de ação secundário/cancelar (opcional).
 * @param customHeader Slot Composable para diálogos do tipo CUSTOM_HEADER (opcional).
 * @param showCloseButton Define se o botão 'X' de fechar deve ser exibido (default: false).
 */
data class DialogConfig(
    val type: DialogType = DialogType.BASIC,
    val title: String,
    val subtitle: String? = null,
    val positiveButton: DialogButtonConfig? = null,
    val negativeButton: DialogButtonConfig? = null,
    val customHeader: @Composable (() -> Unit)? = null,
    val showCloseButton: Boolean = false
    // Propriedades 'listItems', 'icon' e 'iconColor' REMOVIDAS
)