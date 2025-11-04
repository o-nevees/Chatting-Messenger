package com.chatting.domain

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.data.source.local.db.dao.MessageDao
import com.data.source.local.db.entities.MessageEntity
import com.chatting.ui.utils.MediaCacheManager
import com.chatting.ui.utils.PreferencesManager
import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaManager(
    private val application: Application,
    private val messageDao: MessageDao,
    private val httpClient: OkHttpClient
) {

    private val activeDownloads = ConcurrentHashMap<String, Call>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    internal fun createLocalCopy(uri: Uri): String? {
        return try {
            val sentDir = File(application.filesDir, "Sent")
            if (!sentDir.exists()) {
                sentDir.mkdirs()
            }
            val originalFileName = getFileName(uri)
            val uniqueFileName = "${System.currentTimeMillis()}_${originalFileName}"
            val destinationFile = File(sentDir, uniqueFileName)

            application.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create local file copy", e)
            null
        }
    }

    internal fun getBytesFromUri(uri: Uri): ByteArray? {
         return try {
            application.contentResolver.openInputStream(uri)?.use { inputStream ->
                ByteArrayOutputStream().use { byteBuffer ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        byteBuffer.write(buffer, 0, len)
                    }
                    byteBuffer.toByteArray()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading bytes from URI: $uri", e)
            null
        }
    }

    internal fun getFileName(uri: Uri?): String? {
        if (uri == null) return null
         var result: String? = null
         if ("content".equals(uri.scheme, ignoreCase = true)) {
             try {
                 application.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                     if (cursor.moveToFirst()) {
                         val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                         if (columnIndex != -1) {
                            result = cursor.getString(columnIndex)
                         }
                     }
                 }
             } catch (e: Exception) {
                 Log.w(TAG, "Error querying content resolver for file name: ${e.message}")
             }
         }
         if (result == null) {
             result = uri.path
             result?.let {
                 val cut = it.lastIndexOf('/')
                 if (cut != -1) result = it.substring(cut + 1)
             }
         }
          return result?.replace("[\\\\/:*?\"<>|]".toRegex(), "_") ?: "file_${UUID.randomUUID()}"
    }

    internal fun getFileSize(uri: Uri?): Long {
        if (uri != null && "content".equals(uri.scheme, ignoreCase = true)) {
             try {
                 application.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                     if (cursor.moveToFirst()) {
                         val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                         if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                             return cursor.getLong(sizeIndex)
                         }
                     }
                 }
             } catch (e: Exception) {
                  Log.w(TAG, "Error querying content resolver for file size: ${e.message}")
             }
         } else if (uri != null && "file".equals(uri.scheme, ignoreCase = true)) {
              try {
                  uri.path?.let { File(it).length() }?.let { return it }
              } catch (e: Exception) {
                   Log.w(TAG, "Error getting file size from file URI path: ${e.message}")
              }
         }
         return 0L
    }

    fun downloadFile(message: MessageEntity) {
         val messageId = message.id
        val fileName = message.text?.takeIf { it.isNotBlank() } ?: "file_${message.id}"
        val preferencesManager = PreferencesManager(application)

        scope.launch {
            try {
                 val currentDbMessage = messageDao.getMessageByIdSync(messageId)
                 if (currentDbMessage == null) {
                     Log.w(TAG, "Download cancelled: Message $messageId not found in DB.")
                     return@launch
                 }

                 val currentStatus = currentDbMessage.downloadStatus
                 val localPath = currentDbMessage.localPath
                 val fileUrl = currentDbMessage.fileUrl

                if (currentStatus == "concluido" && !localPath.isNullOrEmpty() && File(localPath).exists()) {
                    Log.d(TAG, "Download skipped: File for message $messageId already exists at $localPath.")
                    if(currentDbMessage.downloadProgress != 100) messageDao.updateDownloadStatus(messageId, "concluido", 100)
                    return@launch
                }

                 if (fileUrl.isNullOrEmpty()) {
                     Log.e(TAG, "Download failed: No file URL for message $messageId.")
                     messageDao.updateDownloadStatus(messageId, "falhou", 0)
                     return@launch
                 }

                 if (currentStatus == "baixando" || activeDownloads.containsKey(messageId)) {
                     Log.d(TAG, "Download skipped: Already in progress for message $messageId.")
                     return@launch
                 }

                 val isWifi = true
                 val shouldAutoDownload = when (currentDbMessage.type) {
                     "image" -> if (isWifi) preferencesManager.getAutoDownloadImagesOnWifi() else preferencesManager.getAutoDownloadImagesOnMobile()
                     "audio" -> if (isWifi) preferencesManager.getAutoDownloadAudioOnWifi() else preferencesManager.getAutoDownloadAudioOnMobile()
                     "video" -> if (isWifi) preferencesManager.getAutoDownloadVideosOnWifi() else preferencesManager.getAutoDownloadVideosOnMobile()
                     "document", "file", "archive" -> if (isWifi) preferencesManager.getAutoDownloadDocsOnWifi() else preferencesManager.getAutoDownloadDocsOnMobile()
                     else -> false
                 }

                 if (!shouldAutoDownload && currentStatus == null) {
                     Log.d(TAG, "Marking file for message $messageId as pending download.")
                     messageDao.updateDownloadStatus(messageId, "pendente", 0)
                     return@launch
                 } else if (!shouldAutoDownload && currentStatus != "pendente") {
                      Log.d(TAG,"Download skipped: Auto-download disabled for type ${currentDbMessage.type} on current network.")
                      return@launch
                 }

                Log.i(TAG, "Starting download for message $messageId, URL: $fileUrl")
                messageDao.updateDownloadStatus(messageId, "baixando", 0)

                val request = Request.Builder().url(fileUrl).build()
                val call = httpClient.newCall(request)
                activeDownloads[messageId] = call

                val response = try {
                    call.await()
                } catch (e: IOException) {
                    if (e is CancellationException || e.message?.contains("Canceled") == true) {
                         Log.i(TAG, "Download cancelled for $messageId.")
                         messageDao.updateDownloadStatus(messageId, "pendente", 0)
                    } else {
                         throw e
                    }
                    return@launch
                }

                if (!response.isSuccessful) throw IOException("Download failed. Code: ${response.code}, Message: ${response.message}")

                val body = response.body ?: throw IOException("Empty response body")
                Log.d(TAG, "Response successful for $messageId. Saving file...")

                val destinationFile = saveFileToCacheWithProgress(body, fileName, messageId)
                 response.close()

                if (destinationFile != null && destinationFile.exists()) {
                     Log.i(TAG, "Download complete for $messageId. Saved to: ${destinationFile.absolutePath}")
                    messageDao.updateMessageFileLocalPath(messageId, destinationFile.absolutePath)
                    messageDao.updateDownloadStatus(messageId, "concluido", 100)
                } else {
                    throw IOException("Failed to save file to cache.")
                }

            } catch (e: Exception) {
                 if (e !is CancellationException) {
                     Log.e(TAG, "Download failed for message $messageId", e)
                     try { messageDao.updateDownloadStatus(messageId, "falhou", 0) } catch (dbE: Exception) { Log.e(TAG, "Error updating status to failed for $messageId", dbE) }
                 } else {
                     Log.i(TAG, "Download coroutine cancelled for $messageId.")
                 }
            } finally {
                activeDownloads.remove(messageId)
                 Log.d(TAG, "Download process finished for $messageId")
            }
        }
    }

    suspend fun downloadProfilePhoto(
        fileUrl: String,
        destinationPath: String,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(destinationPath)
                if (file.exists() && file.length() > 0) {
                     Log.d(TAG, "Profile photo already exists: $destinationPath")
                    withContext(Dispatchers.Main) { onComplete(destinationPath) }
                    return@withContext
                }

                file.parentFile?.mkdirs()

                Log.d(TAG, "Downloading profile photo from $fileUrl to $destinationPath")
                val request = Request.Builder().url(fileUrl).build()
                val response = httpClient.newCall(request).await()

                if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")

                response.body?.byteStream()?.use { input ->
                     FileOutputStream(file).use { output ->
                         input.copyTo(output)
                     }
                 } ?: throw IOException("Empty response body")
                 response.close()

                Log.d(TAG, "Profile photo download complete: $destinationPath")
                 withContext(Dispatchers.Main) { onComplete(destinationPath) }

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading profile photo to $destinationPath", e)
                 withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
                 try { File(destinationPath).delete() } catch (_: Exception) {}
            }
        }
    }

    private suspend fun saveFileToCacheWithProgress(body: ResponseBody, fileName: String, messageId: String): File? {
        return withContext(Dispatchers.IO) {
            val destinationFile = MediaCacheManager.getCacheFile(application, fileName)
            var currentProgress = -1
            try {
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.source().use { source ->
                    destinationFile.outputStream().use { outputStream ->
                        val buffer = okio.Buffer()
                        var read: Long
                        while (source.read(buffer, 8192L).also { read = it } != -1L) {
                            ensureActive()
                            outputStream.write(buffer.readByteArray())
                            downloadedBytes += read

                            if (totalBytes > 0) {
                                val progress = (100 * downloadedBytes / totalBytes).toInt()
                                if (progress > currentProgress && (progress % 5 == 0 || progress == 100 || progress == 0)) {
                                     currentProgress = progress
                                     scope.launch {
                                        try { messageDao.updateDownloadStatus(messageId, "baixando", progress) } catch (e: Exception) { Log.e(TAG, "Error updating progress for $messageId", e) }
                                     }
                                }
                            }
                        }
                    }
                }
                 if (currentProgress != 100) {
                      scope.launch { try { messageDao.updateDownloadStatus(messageId, "baixando", 100) } catch (e: Exception) {} }
                 }
                Log.d(TAG, "File saving complete for $messageId")
                destinationFile
            } catch (e: IOException) {
                Log.e(TAG, "Error saving file to cache for $messageId", e)
                 try { destinationFile.delete() } catch (_: Exception) {}
                null
            } catch (e: CancellationException) {
                 Log.i(TAG, "File saving cancelled for $messageId")
                 try { destinationFile.delete() } catch (_: Exception) {}
                 throw e
            }
        }
    }

    fun getCacheSize(callback: (String) -> Unit) {
        scope.launch {
            val size = MediaCacheManager.getCacheSize(application)
             withContext(Dispatchers.Main) {
                callback(android.text.format.Formatter.formatShortFileSize(application, size))
            }
        }
    }

    fun clearCache(callback: () -> Unit) {
        MediaCacheManager.clearCache(application)
         callback()
    }

    companion object {
        private const val TAG = "MediaManager"
    }

    suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                     if (!continuation.isCompleted) {
                        continuation.resume(response)
                     } else {
                         response.close()
                     }
                }

                override fun onFailure(call: Call, e: IOException) {
                     if (!call.isCanceled() && continuation.isActive) {
                         continuation.resumeWithException(e)
                     }
                }
            })
            continuation.invokeOnCancellation {
                try {
                    if (!isExecuted()) {
                        cancel()
                    }
                } catch (ex: Throwable) {
                     Log.w("OkHttpAwait", "Exception during cancellation: ${ex.message}")
                }
            }
        }
    }
}