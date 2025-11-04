package com.chatting.ui.utils

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {

    /**
     * Verifica de forma robusta se um arquivo/recurso representado por uma Uri
     * existe e é acessível no disco.
     * Esta anotação @JvmStatic é crucial para que o método possa ser chamado
     * de forma estática a partir de código Java (ex: FileHandler.java).
     */
    @JvmStatic
    fun fileExists(context: Context, localPath: String?): Boolean {
        if (localPath.isNullOrEmpty()) {
            return false
        }

        return try {
            val uri = Uri.parse(localPath)
            // Para URIs de 'file://', checamos a existência do arquivo diretamente.
            if ("file" == uri.scheme) {
                val file = uri.path?.let { File(it) }
                file != null && file.exists() && file.length() > 0
            } else {
                // Para 'content://', a única forma confiável de saber se existe
                // é tentar abrir. Se não lançar exceção, ele existe.
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    // O arquivo existe e o stream foi aberto. O 'use' garante que será fechado.
                    return true
                }
                // Se o InputStream for nulo, o arquivo não existe ou não há permissão.
                false
            }
        } catch (e: Exception) {
            // Qualquer exceção (FileNotFound, SecurityException, etc.) significa que o arquivo não é acessível.
            false
        }
    }
}