// Crie um novo arquivo: com/chatting/ui/utils/MediaCacheManager.kt
package com.chatting.ui.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

/**
 * Gerencia o cache de mídias de mensagens (imagens, vídeos, documentos, etc.).
 * Armazena arquivos brutos em um diretório de cache privado do aplicativo.
 */
object MediaCacheManager {
    private const val TAG = "MediaCacheManager"
    private const val CACHE_DIR_NAME = "media_cache" // Diretório específico para mídias

    /**
     * Retorna o arquivo de cache para um nome de arquivo específico.
     * Não cria o arquivo, apenas o objeto File que aponta para o local.
     */
    fun getCacheFile(context: Context, fileName: String): File {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, fileName)
    }

    /**
     * Verifica se um arquivo de mídia já existe no cache em disco.
     */
    fun isCached(context: Context, fileName: String): Boolean {
        val cacheFile = getCacheFile(context, fileName)
        return cacheFile.exists() && cacheFile.length() > 0
    }

    /**
     * Salva um InputStream (vindo de um download) em um arquivo no cache.
     * Retorna o arquivo salvo ou null em caso de falha.
     */
    fun saveToCache(context: Context, inputStream: InputStream, fileName: String): File? {
        val cacheFile = getCacheFile(context, fileName)
        return try {
            cacheFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Log.d(TAG, "Mídia salva no cache: $fileName")
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar mídia no cache: $fileName", e)
            // Se der erro, apaga o arquivo parcial
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            null
        } finally {
            inputStream.close()
        }
    }
    
    /**
     * Limpa todo o cache de mídias.
     */
    fun clearCache(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
                Log.d(TAG, "Cache de mídias limpo completamente.")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar o cache de mídias.", e)
            }
        }
    }

    /**
     * Calcula e retorna o tamanho total do cache de mídias em bytes.
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao calcular o tamanho do cache de mídias.", e)
            0L
        }
    }
}