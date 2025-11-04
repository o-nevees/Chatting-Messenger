package com.chatting.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.LaunchedEffect
import com.chatting.vibration.HapticType 
import com.chatting.vibration.VibrationManager

/**
 * O Composable principal que renderiza o diálogo com base no [DialogConfig].
 * Ele usa um [Dialog] base para flutuar sobre o conteúdo.
 */
@Composable
fun CustomDialog(
    config: DialogConfig,
    onDismiss: () -> Unit
) {
// Dispara a vibração apropriada quando o diálogo aparece
    LaunchedEffect(config.type) {
        when (config.type) {
            DialogType.ERROR -> VibrationManager.viber(HapticType.ERROR)
            DialogType.WARNING -> VibrationManager.viber(HapticType.WARNING)
            DialogType.INFO -> VibrationManager.viber(HapticType.CLICK) // Um clique sutil para informação
          //  DialogType.SUCCESS -> VibrationManager.viber(HapticType.SUCCESS)
            else -> { /* Não vibrar para BASIC ou CUSTOM_HEADER */ }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd // Para o botão 'X'
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Renderiza o Header (Ícone, Animação, etc.)
                    DialogHeader(config = config)

                    // 2. Renderiza o Conteúdo (Título, Subtítulo)
                    DialogContent(config = config, onDismiss = onDismiss)

                    // 3. Renderiza os Botões de Ação
                    DialogActions(config = config, onDismiss = onDismiss)
                }

                // Botão 'X' opcional
                if (config.showCloseButton) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renderiza o cabeçalho do diálogo (Animação, Ícone, etc.)
 * com base no DialogType.
 */
@Composable
private fun DialogHeader(config: DialogConfig) {
    // Só adiciona padding se houver um cabeçalho
    val hasHeaderContent = config.type != DialogType.BASIC
    if (hasHeaderContent) {
        Spacer(modifier = Modifier.height(24.dp))
    }

    when (config.type) {
        // Animações (importadas de DialogAnimations.kt)
        DialogType.SUCCESS -> SuccessCheckHeader()
        DialogType.ERROR -> ErrorXHeader()
        DialogType.WARNING -> WarningHeader()
        DialogType.INFO -> InfoIconHeader() // NOVO

        // Header Customizado
        DialogType.CUSTOM_HEADER -> {
            config.customHeader?.invoke()
        }

        // BASIC não têm cabeçalho gráfico.
        DialogType.BASIC -> { /* Não faz nada */ }
    }

    // Adiciona espaço após o header, se ele existiu
    if (hasHeaderContent) {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Renderiza o conteúdo textual (Título, Subtítulo).
 * MELHORIA: Subtítulo agora é rolável.
 */
@Composable
private fun DialogContent(config: DialogConfig, onDismiss: () -> Unit) {
    // Layout padrão para Título e Subtítulo
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // Adiciona padding superior se não houver header (ex: BASIC)
            .padding(top = if (config.type == DialogType.BASIC) 24.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DialogTitle(config.title)
        config.subtitle?.let {
            Spacer(modifier = Modifier.height(8.dp))
            // Subtítulo agora é rolável para textos longos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // Limita a altura máxima
                    .verticalScroll(rememberScrollState())
            ) {
                DialogSubtitle(it)
            }
        }
    }
}

/**
 * Renderiza os botões de ação (Positivo, Negativo).
 */
@Composable
private fun DialogActions(config: DialogConfig, onDismiss: () -> Unit) {
    if (config.positiveButton != null || config.negativeButton != null) {
        Spacer(modifier = Modifier.height(24.dp))
        // Linha de botões melhorada
        ButtonsRow(
            positive = config.positiveButton,
            negative = config.negativeButton,
            onDismiss = onDismiss
        )
        Spacer(modifier = Modifier.height(16.dp)) // Padding inferior
    }
}

// --- Componentes de UI Auxiliares ---

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun DialogSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Renderiza a linha de botões na parte inferior.
 * Alinhados à direita (end).
 */
@Composable
private fun ButtonsRow(
    positive: DialogButtonConfig?,
    negative: DialogButtonConfig?,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp), // Padding para os botões
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        negative?.let {
            TextButton(
                onClick = {
                    it.onClick()
                    onDismiss()
                }
            ) {
                Text(it.text.uppercase())
            }
            Spacer(modifier = Modifier.size(8.dp))
        }

        positive?.let {
            val buttonAction = {
                it.onClick()
                onDismiss()
            }
            // Renderiza botão preenchido ou de texto com base no config
            if (it.isFilled) {
                Button(onClick = buttonAction) {
                    Text(it.text.uppercase())
                }
            } else {
                TextButton(onClick = buttonAction) {
                    Text(it.text.uppercase())
                }
            }
        }
    }
}