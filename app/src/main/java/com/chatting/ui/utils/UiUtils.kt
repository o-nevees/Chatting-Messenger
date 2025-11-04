package com.chatting.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Função centralizada para mapear o tamanho da fonte
fun mapFontSizeToSp(size: Int): Dp {
    return when (size) {
        PreferencesManager.FONT_SIZE_SMALL -> 12.dp
        PreferencesManager.FONT_SIZE_NORMAL -> 14.dp
        PreferencesManager.FONT_SIZE_LARGE -> 16.dp
        PreferencesManager.FONT_SIZE_EXTRA_LARGE -> 18.dp
        else -> 14.dp
    }
}