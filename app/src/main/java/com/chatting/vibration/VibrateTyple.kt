package com.chatting.vibration

import android.os.VibrationEffect

/**
 * Define os diferentes tipos de feedback háptico (vibração) disponíveis no app.
 *
 * Cada tipo encapsula:
 * @param timings O padrão de milissegundos (off, on, off, on...).
 * @param amplitudes A força da vibração para cada "on" no padrão (0-255). 0 = sem vibração.
 */
@Suppress("ArrayInDataClass") // É aceitável para constantes de efeito de vibração
data class HapticPattern(
    val timings: LongArray,
    val amplitudes: IntArray? = null // Nulo para usar amplitude padrão
) {
    /**
     * Cria um VibrationEffect para APIs 26+.
     * Se as amplitudes não forem fornecidas ou não forem suportadas, cria um padrão simples.
     */
    fun createEffect(): VibrationEffect {
        return if (amplitudes != null && amplitudes.size == timings.size) {
            // Cria uma forma de onda com amplitudes personalizadas
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            // Cria uma forma de onda simples (sem amplitude)
            VibrationEffect.createWaveform(timings, -1)
        }
    }
}

/**
 * Enum dos tipos de vibração pré-definidos que podem ser chamados.
 */
enum class HapticType(val pattern: HapticPattern) {

    /** Um "tick" muito curto e nítido. Ótimo para cliques de botões. */
    CLICK(
        HapticPattern(
            timings = longArrayOf(0, 20), // 0ms off, 20ms on
            amplitudes = intArrayOf(0, 100) // Força média-baixa
        )
    ),

    /** Vibração curta para confirmar uma ação bem-sucedida. */
    SUCCESS(
        HapticPattern(
            timings = longArrayOf(0, 50), // 0ms off, 50ms on
            amplitudes = intArrayOf(0, 150) // Força média
        )
    ),

    /** Vibração dupla rápida para avisos ou notificações no app. */
    WARNING(
        HapticPattern(
            timings = longArrayOf(0, 70, 50, 70), // on, off, on
            amplitudes = intArrayOf(0, 180, 0, 180) // Força média-alta
        )
    ),

    /** Vibração longa e forte para indicar um erro claro. */
    ERROR(
        HapticPattern(
            timings = longArrayOf(0, 250), // 0ms off, 250ms on
            amplitudes = intArrayOf(0, 255) // Força máxima
        )
    ),

    /** Padrão de notificação padrão (vibra-pausa-vibra). */
    NOTIFICATION(
        HapticPattern(
            timings = longArrayOf(0, 200, 100, 200), // on, off, on
            amplitudes = intArrayOf(0, 200, 0, 200) // Força alta
        )
    )
}