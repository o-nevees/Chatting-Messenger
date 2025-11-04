package com.chatting.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private enum class SpanFormat {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    MONOSPACE
}

private fun isStyleApplied(
    annotatedString: AnnotatedString,
    selection: TextRange,
    style: SpanFormat
): Boolean {
    if (selection.collapsed) return false

    val spansAtStart = annotatedString.spanStyles.filter {
        it.start <= selection.start && it.end > selection.start
    }

    return when (style) {
        SpanFormat.BOLD -> spansAtStart.any { it.item.fontWeight == FontWeight.Bold }
        SpanFormat.ITALIC -> spansAtStart.any { it.item.fontStyle == FontStyle.Italic }
        SpanFormat.STRIKETHROUGH -> spansAtStart.any { it.item.textDecoration == TextDecoration.LineThrough }
        SpanFormat.MONOSPACE -> spansAtStart.any { it.item.fontFamily == FontFamily.Monospace }
    }
}

private fun applySpanStyle(
    value: TextFieldValue,
    format: SpanFormat,
    monoBackgroundColor: Color
): TextFieldValue {
    val selection = value.selection
    if (selection.collapsed) return value

    val isAlreadyApplied = isStyleApplied(value.annotatedString, selection, format)

    val styleToApply = when (format) {
        SpanFormat.BOLD -> SpanStyle(fontWeight = if (isAlreadyApplied) null else FontWeight.Bold)
        SpanFormat.ITALIC -> SpanStyle(fontStyle = if (isAlreadyApplied) null else FontStyle.Italic)
        SpanFormat.STRIKETHROUGH -> SpanStyle(textDecoration = if (isAlreadyApplied) null else TextDecoration.LineThrough)
        SpanFormat.MONOSPACE -> if (isAlreadyApplied) {
            SpanStyle(fontFamily = null, background = Color.Transparent)
        } else {
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = monoBackgroundColor
            )
        }
    }

    val styleToRemove = when (format) {
        SpanFormat.BOLD -> SpanStyle(fontWeight = null)
        SpanFormat.ITALIC -> SpanStyle(fontStyle = null)
        SpanFormat.STRIKETHROUGH -> SpanStyle(textDecoration = null)
        SpanFormat.MONOSPACE -> SpanStyle(fontFamily = null, background = Color.Transparent)
    }

    val finalStyle = if (isAlreadyApplied) styleToRemove else styleToApply

    val newAnnotatedString = AnnotatedString.Builder(value.annotatedString).apply {
        addStyle(SpanStyle(), selection.start, selection.end)
        addStyle(finalStyle, selection.start, selection.end)
    }.toAnnotatedString()

    return value.copy(
        annotatedString = newAnnotatedString,
        selection = selection
    )
}

@Composable
private fun TextFormattingMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onFormat: (SpanFormat) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Negrito") },
            onClick = {
                onFormat(SpanFormat.BOLD)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Itálico") },
            onClick = {
                onFormat(SpanFormat.ITALIC)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Tachado") },
            onClick = {
                onFormat(SpanFormat.STRIKETHROUGH)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Monoespaçado") },
            onClick = {
                onFormat(SpanFormat.MONOSPACE)
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit
) {
    var showFormattingMenu by remember { mutableStateOf(false) }
    val monoBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.SentimentSatisfied, "Emoji") }

            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem") },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent
                )
            )

            Box {
                IconButton(onClick = { showFormattingMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Formatar Texto")
                }
                TextFormattingMenu(
                    expanded = showFormattingMenu,
                    onDismiss = { showFormattingMenu = false },
                    onFormat = { format ->
                        onValueChange(applySpanStyle(value, format, monoBackgroundColor))
                    }
                )
            }

            IconButton(onClick = onAttachmentClick) { Icon(Icons.Default.Attachment, "Anexar") }
            IconButton(onClick = onSendClick, enabled = value.text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
            }
        }
    }
}