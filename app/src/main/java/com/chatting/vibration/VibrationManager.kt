package com.chatting.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Gerenciador singleton para controlar o feedback háptico (vibração) em todo o app.
 *
 * Para usar:
 * 1. Chame `VibrationManager.init(context)` na sua classe Application.
 * 2. Chame `VibrationManager.viber(HapticType.SUCCESS)` de qualquer lugar.
 *
 * REQUER A PERMISSÃO: <uses-permission android:name="android.permission.VIBRATE" />
 */
object VibrationManager {

    private const val TAG = "VibrationManager"
    private var vibrator: Vibrator? = null
    private var isInitialized = false

    /**
     * Inicializa o gerenciador com o contexto da aplicação.
     * Deve ser chamado uma vez no `MyApp.onCreate()`.
     */
    fun init(context: Context) {
        if (isInitialized) return

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Para Android 12+ (API 31+)
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            // Para versões anteriores
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }

        if (vibrator == null || !vibrator!!.hasVibrator()) {
            Log.w(TAG, "Dispositivo não possui um vibrador ou o serviço não foi encontrado.")
            vibrator = null // Garante que é nulo se não houver vibrador
        } else {
            isInitialized = true
            Log.d(TAG, "VibrationManager inicializado com sucesso.")
        }
    }

    /**
     * Dispara um padrão de vibração específico.
     *
     * @param type O tipo de feedback háptico do enum HapticType.
     */
    fun viber(type: HapticType) {
        if (!isInitialized || vibrator == null) {
            if (!isInitialized) Log.e(TAG, "VibrationManager.viber() chamado antes de init()!")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+ (Oreo) suporta VibrationEffect com amplitudes
                val effect = type.pattern.createEffect()
                vibrator?.vibrate(effect)
            } else {
                // API < 26 (Legacy) - usa apenas os tempos, sem amplitude
                @Suppress("DEPRECATION")
                vibrator?.vibrate(type.pattern.timings, -1)
            }
        } catch (e: Exception) {
            // Captura qualquer exceção (ex: app em segundo plano sem permissão)
            Log.e(TAG, "Erro ao tentar vibrar: ${e.message}", e)
        }
    }
}