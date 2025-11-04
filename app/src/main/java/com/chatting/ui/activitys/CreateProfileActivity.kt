package com.chatting.ui.activitys

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.chatting.domain.AccountManager
import com.chatting.ui.MainActivity
import com.service.api.ApiService
import com.data.repository.AuthRepository
import com.chatting.ui.screens.CreateProfileScreen
import com.chatting.ui.viewmodel.CreateProfileViewModel
import com.chatting.ui.viewmodel.CreateProfileViewModelFactory
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import com.data.source.local.db.entities.UserEntity
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import com.chatting.ui.theme.MyComposeApplicationTheme
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.service.api.NetworkConfig
import com.service.api.UploadPhotoData
import com.data.repository.ProgressRequestBody
import com.chatting.ui.MyApp
import okhttp3.RequestBody.Companion.asRequestBody

class CreateProfileActivity : ComponentActivity() {

    private val apiService: ApiService by lazy { (application as MyApp).appContainer.apiService }
    
    private val viewModel: CreateProfileViewModel by viewModels {
        CreateProfileViewModelFactory(apiService)
    }

    private val accountManager: AccountManager by lazy { (application as MyApp).accountManager }

    private var firebaseToken: String? = null
    private var phoneNumber: String? = null

    companion object {
        private const val TAG = "CreateProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseToken = intent.getStringExtra("firebase_token")
        phoneNumber = intent.getStringExtra("phone_number")

        if (firebaseToken.isNullOrEmpty() || phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Erro: Dados de verificação inválidos.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
             MyComposeApplicationTheme {
                CreateProfileScreen(
                    viewModel = viewModel,
                    onComplete = { attemptCreateProfile() }
                )
            }
        }
    }

    private fun attemptCreateProfile() {
        lifecycleScope.launch {
            viewModel.setLoading(true)
            
            try {
                val fcmToken = getFcmToken()
                val uiState = viewModel.uiState.value
                val deviceName = "${android.os.Build.MODEL} (${android.os.Build.BRAND})"
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""

                val result = accountManager.createProfile(
                    firebaseToken = firebaseToken!!,
                    name = uiState.name,
                    username = uiState.username,
                    birthdate = uiState.birthdate,
                    deviceName = deviceName,
                    deviceId = deviceId,
                    fcmToken = fcmToken,
                    profileImageUri = uiState.profileImageUri,
                    phoneNumber = phoneNumber!!
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Criação de perfil e login bem-sucedidos. Navegando para MainActivity.")
                    navigateToMain()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    Log.e(TAG, "Erro ao criar perfil: $errorMsg")
                    Toast.makeText(this@CreateProfileActivity, "Erro ao criar perfil: $errorMsg", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Falha inesperada em attemptCreateProfile", e)
                Toast.makeText(this@CreateProfileActivity, "Erro inesperado.", Toast.LENGTH_SHORT).show()
            } finally {
                viewModel.setLoading(false)
            }
        }
    }

     private suspend fun getFcmToken(): String? {
         return try {
             FirebaseMessaging.getInstance().token.await()
         } catch (e: Exception) {
             Log.w(TAG, "Fetching FCM registration token failed", e)
             null
         }
     }

     private fun navigateToMain() {
         val intent = Intent(this, MainActivity::class.java).apply {
             flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
         }
         startActivity(intent)
         finish()
     }
}