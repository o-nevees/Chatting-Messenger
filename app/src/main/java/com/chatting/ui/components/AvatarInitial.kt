package com.chatting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun AvatarInitial(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    textColor: Color = Color.White,
    localImagePath: String? = null // Removido o parâmetro imageUrl
) {
    val gradientBrush = remember(name) { createGradientBrush(name) }
    val initials = remember(name) { extractInitials(name) }

    // Verifica se o arquivo local existe e tem conteúdo.
    val localFile = if (!localImagePath.isNullOrBlank()) File(localImagePath) else null
    val localFileExists = localFile?.exists() == true && localFile.length() > 0

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = gradientBrush)
    ) {
        // Se o arquivo local existir, exibe a imagem.
        if (localFileExists) {
            AsyncImage(
                model = localFile,
                contentDescription = "Avatar de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            // Caso contrário, exibe as iniciais.
            val fontSize = (size.value * if (initials.length == 1) 0.5 else 0.4).sp

            Text(
                text = initials,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * MELHORIA: Extrai as iniciais de forma mais inteligente.
 * - Trata nomes com hífen (ex: "Mary-Anne" -> "MA").
 * - Ignora partes do nome que não começam com letras.
 */
private fun extractInitials(name: String): String {
    val cleanName = name.trim().replace('-', ' ')
    if (cleanName.isEmpty()) return "?"

    val words = cleanName.split(" ").filter { it.isNotEmpty() && it.first().isLetter() }

    return when (words.size) {
        0 -> "?"
        1 -> words[0].first().uppercase()
        else -> "${words[0].first().uppercase()}${words[1].first().uppercase()}"
    }
}

/**
 * NOVO: Gera um Brush de gradiente determinístico a partir do nome.
 */
private fun createGradientBrush(name: String): Brush {
    // MELHORIA: Paleta de cores expandida com pares para gradientes.
    val colorPairs = listOf(
        Pair(Color(0xFFF857A6), Color(0xFFFF5858)), // Rosa -> Vermelho
        Pair(Color(0xFF43E97B), Color(0xFF38F9D7)), // Verde -> Ciano
        Pair(Color(0xFF764BA2), Color(0xFF667EEA)), // Roxo -> Azul
        Pair(Color(0xFFFFC371), Color(0xFFFF5F6D)), // Laranja -> Rosa
        Pair(Color(0xFF00C6FF), Color(0xFF0072FF)), // Azul Claro -> Azul Escuro
        Pair(Color(0xFFF4D03F), Color(0xFF16A085)), // Amarelo -> Verde Mar
        Pair(Color(0xFFD4145A), Color(0xFFFBB03B)), // Magenta -> Laranja
        Pair(Color(0xFF4facfe), Color(0xFF00f2fe)), // Céu Azul
        Pair(Color(0xFFff7e5f), Color(0xFFfeb47b)), // Laranja Pêssego
        Pair(Color(0xFF6a11cb), Color(0xFF2575fc)), // Roxo Royal
        Pair(Color(0xFFa8e063), Color(0xFF56ab2f)), // Verde Limão
        Pair(Color(0xFFa1c4fd), Color(0xFFc2e9fb)), // Azul Suave
        Pair(Color(0xFFe684ae), Color(0xFF79cbca)), // Doce Manhã
        Pair(Color(0xFF8e2de2), Color(0xFF4a00e0)), // Violeta
        Pair(Color(0xFFff0844), Color(0xFFffb199)), // Vermelho Coral
        Pair(Color(0xFF02aab0), Color(0xFF00cdac))  // Verde Oceano
    )

    val index = (name.hashCode().absoluteValue) % colorPairs.size
    val selectedPair = colorPairs[index]

    return Brush.linearGradient(
        colors = listOf(selectedPair.first, selectedPair.second)
    )
}