package com.chatting.domain

import android.app.Application
import android.net.Uri
import android.util.Log
import com.data.repository.AuthRepository
import com.data.repository.UserDataStore
import com.data.source.local.db.entities.UserEntity
import com.service.api.ApiResponse
import com.service.api.ApiService
import com.service.api.CreateProfileResponseData
import com.service.api.SimpleApiResponse
import com.service.api.UploadPhotoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.chatting.ui.utils.SecurePrefs

class AccountManager(
    private val application: Application,
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val userDataStore: UserDataStore
) {

    companion object {
        private const val TAG = "AccountManager"
    }

    suspend fun createProfile(
        firebaseToken: String,
        name: String,
        username: String,
        birthdate: String,
        deviceName: String,
        deviceId: String,
        fcmToken: String?,
        profileImageUri: Uri?,
        phoneNumber: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Chamando API /profile/create...")
            val response = apiService.createProfile(
                firebaseToken = firebaseToken,
                name = name,
                username = username,
                birthdate = birthdate,
                deviceName = deviceName,
                deviceId = deviceId,
                fmcToken = fcmToken
            )

            val body = response.body()

            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                val authData = body.data
                val authToken = authData?.authToken
                val refreshToken = authData?.refreshToken

                Log.d(TAG, "Perfil criado com sucesso no backend.")

                if (authToken != null && refreshToken != null) {
                    val uploadedPhotoResult = if (profileImageUri != null) {
                        uploadProfilePhoto(authToken, profileImageUri).getOrNull()
                    } else {
                        null
                    }

                    val user = UserEntity(
                        number = phoneNumber,
                        username1 = name,
                        username2 = username,
                        profilePhoto = uploadedPhotoResult?.filename,
                        lastOnline = null
                    )

                    authRepository.onLoginSuccess(
                        authToken = authToken,
                        refreshToken = refreshToken,
                        user = user
                    )
                    Log.d(TAG, "Login após criação de perfil bem-sucedido.")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Erro: Resposta de criação de perfil inválida (sem tokens).")
                    Result.failure(Exception("Resposta de autenticação inválida."))
                }
            } else {
                val message = body?.message ?: "Erro ${response.code()}"
                Log.e(TAG, "Erro ao criar perfil no backend: $message (Code: ${response.code()})")
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha na chamada /profile/create", e)
            Result.failure(Exception("Erro de conexão."))
        }
    }

    suspend fun uploadProfilePhoto(
        authToken: String,
        uri: Uri
    ): Result<UploadPhotoData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando upload da foto de perfil: $uri")
        val tempFile = createTempFileFromUri(uri)
        if (tempFile == null) {
            Log.e(TAG, "Não foi possível criar arquivo temporário para upload.")
            return@withContext Result.failure(IOException("Não foi possível criar arquivo temporário."))
        }

        try {
            val mediaType = application.contentResolver.getType(uri)?.toMediaTypeOrNull() ?: "image/*".toMediaTypeOrNull()
            val requestFile = tempFile.asRequestBody(mediaType)

            val photoPart = MultipartBody.Part.createFormData(
                "profile_photo",
                tempFile.name,
                requestFile
            )

            Log.d(TAG, "Chamando API /uploads/profilePhoto...")
            val response = apiService.uploadProfilePhoto(
                authToken = "Bearer $authToken",
                photo = photoPart
            )

            val body = response.body()
            val data = body?.data

            if (response.isSuccessful && body?.status == "success" && data != null) {
                Log.i(TAG, "Upload da foto de perfil bem-sucedido. URL: ${data.url}")
                Result.success(data)
            } else {
                val errorMsg = body?.message ?: response.errorBody()?.string() ?: "Erro ${response.code()}"
                Log.e(TAG, "Erro no upload da foto: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro de conexão ao enviar foto.", e)
            Result.failure(Exception("Erro de conexão ao enviar foto."))
        } finally {
            tempFile.delete()
        }
    }

    suspend fun saveProfile(
        name: String,
        username: String,
        bio: String
    ): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val authToken = SecurePrefs.getString("auth_token", null)
                ?: return@withContext Result.failure(Exception("Erro de autenticação."))

            Log.d(TAG, "Chamando API PUT /profile...")
            val response = apiService.updateProfile(
                authToken = "Bearer $authToken",
                name = name,
                username = username,
                bio = bio
            )

            val body = response.body()
            if (response.isSuccessful && body?.status == "success") {
                Log.i(TAG, "Perfil atualizado com sucesso no backend.")
                Result.success(body!!)
            } else {
                val errorMsg = body?.message ?: response.errorBody()?.string() ?: "Erro ${response.code()}"
                Log.e(TAG, "Falha ao atualizar perfil: $errorMsg (Code: ${response.code()})")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro de rede ao salvar perfil", e)
            Result.failure(Exception("Erro de conexão ao salvar perfil."))
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("upload_", ".tmp", application.cacheDir)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao criar/copiar arquivo temporário", e)
            null
        }
    }
}