package com.chatting.dialog

/**
 * Enum que define os tipos visuais de diálogos disponíveis.
 * Facilita a chamada sem a necessidade de importar headers de animação.
 */
enum class DialogType {
    /** (Padrão) Título e subtítulo. */
    BASIC,
    /** Slot de @Composable customizado (definido em DialogConfig.customHeader) */
    CUSTOM_HEADER,
    /** Animação de sucesso (SuccessCheckHeader) */
    SUCCESS,
    /** Animação de erro (ErrorXHeader) */
    ERROR,
    /** Animação de aviso (WarningHeader) */
    WARNING,
    /** NOVO: Animação de informação (InfoIconHeader) */
    INFO
}