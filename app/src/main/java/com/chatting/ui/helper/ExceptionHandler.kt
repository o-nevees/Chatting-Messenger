package com.chatting.ui.helper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.chatting.ui.activitys.ExceptionActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class ExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        // Converte o stacktrace em uma string legível
        val stackTrace = StringWriter().also { ex.printStackTrace(PrintWriter(it)) }.toString()

        // Usa templates de string do Kotlin para uma construção mais limpa
        val errorReport = """
            ************ CAUSA DO ERRO ************
            $stackTrace
            
            ************ INFORMAÇÕES DO DISPOSITIVO ************
            Marca: ${Build.BRAND}
            Dispositivo: ${Build.DEVICE}
            Modelo: ${Build.MODEL}
            ID: ${Build.ID}
            Produto: ${Build.PRODUCT}
            
            ************ FIRMWARE ************
            SDK: ${Build.VERSION.SDK_INT}
            Release: ${Build.VERSION.RELEASE}
            Incremental: ${Build.VERSION.INCREMENTAL}
            """.trimIndent()

        // Inicia a activity que mostrará o erro
        val intent = Intent(context, ExceptionActivity::class.java).apply {
            putExtra(ExceptionActivity.EXTRA_ERROR_MESSAGE, errorReport)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Mata o processo atual para evitar um estado inconsistente do app
        Process.killProcess(Process.myPid())
        exitProcess(10) // Código de erro
    }
}