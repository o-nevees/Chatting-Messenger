package com.chatting.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Especificação de animação de mola "elástica" para entrada
val bouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * ANIMAÇÃO MELHORADA: Sucesso
 * 1. Círculo verde "nasce" com elasticidade.
 * 2. *Após* o círculo aparecer, o ícone de "check" "nasce" dentro dele.
 */
@Composable
fun SuccessCheckHeader() {
    val greenBackground = Color(0xFF66BB6A) // Verde
    val circleScale = remember { Animatable(0.3f) }
    val iconScale = remember { Animatable(0.0f) } // Começa invisível

    LaunchedEffect(Unit) {
        // 1. Círculo cresce
        circleScale.animateTo(
            targetValue = 1.0f,
            animationSpec = bouncySpring
        )
        // 2. Ícone cresce (após o círculo)
        iconScale.animateTo(
            targetValue = 1.0f,
            animationSpec = bouncySpring
        )
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = circleScale.value
                scaleY = circleScale.value
            }
            .clip(CircleShape)
            .background(greenBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Sucesso",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
        )
    }
}

/**
 * ANIMAÇÃO MELHORADA: Erro
 * 1. Círculo vermelho "nasce" com elasticidade.
 * 2. Ícone "X" aparece.
 * 3. *Após* tudo aparecer, o diálogo inteiro "treme" para dar ênfase ao erro.
 */
@Composable
fun ErrorXHeader() {
    val redBackground = Color(0xFFEF5350) // Vermelho
    val circleScale = remember { Animatable(0.3f) }
    val iconScale = remember { Animatable(0.0f) }
    val shakeOffset = remember { Animatable(0f) } // Para a animação de "tremer"

    LaunchedEffect(Unit) {
        // 1. Círculo cresce
        circleScale.animateTo(1.0f, animationSpec = bouncySpring)
        // 2. Ícone cresce
        iconScale.animateTo(1.0f, animationSpec = bouncySpring)
        // 3. Espera um pouco e "treme"
        delay(100)
        shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                -10f at 50
                10f at 100
                -10f at 150
                10f at 200
                -5f at 250
                5f at 300
                0f at 350
            }
        )
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = circleScale.value
                scaleY = circleScale.value
                translationX = shakeOffset.value // Aplica o "tremor"
            }
            .clip(CircleShape)
            .background(redBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Erro",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
        )
    }
}

/**
 * ANIMAÇÃO MELHORADA: Aviso (Warning)
 * 1. Círculo laranja "nasce" com elasticidade.
 * 2. Ícone "!" "nasce" dentro dele.
 * 3. *Após* a entrada, o *ícone* (não o círculo) começa a pulsar infinitamente.
 */
@Composable
fun WarningHeader() {
    val orangeBackground = Color(0xFFFFA726) // Laranja
    val circleScale = remember { Animatable(0.3f) }
    val iconBaseScale = remember { Animatable(0.0f) }

    // Animação de pulso infinito (só começa após a entrada)
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val iconPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_pulse_scale"
    )

    LaunchedEffect(Unit) {
        // 1. Círculo cresce
        circleScale.animateTo(1.0f, animationSpec = bouncySpring)
        // 2. Ícone cresce
        iconBaseScale.animateTo(1.0f, animationSpec = bouncySpring)
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = circleScale.value
                scaleY = circleScale.value
            }
            .clip(CircleShape)
            .background(orangeBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            // Ícone "PriorityHigh" é mais forte que "Warning"
            imageVector = Icons.Default.PriorityHigh,
            contentDescription = "Aviso",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    // Combina a escala de entrada com a escala do pulso
                    val finalScale = iconBaseScale.value * iconPulseScale
                    scaleX = finalScale
                    scaleY = finalScale
                }
        )
    }
}

/**
 * NOVA ANIMAÇÃO: Informação
 * 1. Círculo azul "nasce" com elasticidade.
 * 2. Ícone "i" "nasce" com uma leve rotação e elasticidade.
 */
@Composable
fun InfoIconHeader() {
    val blueBackground = Color(0xFF42A5F5) // Azul
    val circleScale = remember { Animatable(0.3f) }
    val iconScale = remember { Animatable(0.0f) }
    val iconRotation = remember { Animatable(-90f) } // Começa "deitado"

    LaunchedEffect(Unit) {
        // 1. Círculo cresce
        circleScale.animateTo(1.0f, animationSpec = bouncySpring)
        // 2. Ícone cresce e gira
        iconScale.animateTo(1.0f, animationSpec = bouncySpring)
        iconRotation.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = circleScale.value
                scaleY = circleScale.value
            }
            .clip(CircleShape)
            .background(blueBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Informação",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                    rotationZ = iconRotation.value
                }
        )
    }
}