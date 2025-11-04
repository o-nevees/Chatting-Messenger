package com.chatting.ui.activitys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.chatting.ui.MainActivity
import com.chatting.ui.MyApp
import com.chatting.ui.R
import com.data.source.local.db.entities.UserEntity
import com.service.api.ApiService 
import com.data.repository.AuthRepository
import com.chatting.ui.utils.SecurePrefs
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

data class VerificationUiState(
    val verificationCode: String = "",
    val isLoading: Boolean = false,
    val infoMessage: String = "Enviamos um código para seu número",
    val canResendCode: Boolean = true
)

class VerificationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    fun onVerificationCodeChange(newCode: String) {
        if (newCode.length <= 6) {
            _uiState.update { it.copy(verificationCode = newCode) }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }
}

class VerifyNumberActivity : ComponentActivity() {

    private val viewModel: VerificationViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authRepository: AuthRepository 
    private val apiService: ApiService by lazy { (application as MyApp).appContainer.apiService }


    private var phoneNumber: String = ""
    private var verificationId: String? = null
    private var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

    companion object {
        private const val TAG = "VerifyNumberActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurePrefs.init(applicationContext)
        firebaseAuth = FirebaseAuth.getInstance()
        authRepository = (application as MyApp).authRepository

        phoneNumber = SecurePrefs.getString("my_number", "") ?: ""
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Erro: Número de telefone inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        sendVerificationCode(phoneNumber)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.infoMessage) {
                if (uiState.infoMessage.isNotBlank() && uiState.infoMessage != "Enviamos um código para seu número") {
                    snackbarHostState.showSnackbar(uiState.infoMessage)
                }
            }

            MyComposeApplicationTheme {
                VerificationScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onCodeChange = viewModel::onVerificationCodeChange,
                    onConfirmClick = {
                        if (uiState.verificationCode.length == 6) {
                            verifyCode(uiState.verificationCode)
                        } else {
                            viewModel.setInfoMessage("Digite o código de 6 dígitos.")
                        }
                    },
                    onResendClick = {
                        resendingToken?.let { token ->
                            resendVerificationCode(phoneNumber, token)
                        } ?: sendVerificationCode(phoneNumber)
                    }
                )
            }
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("Verification", "Verificação concluída automaticamente.")
            viewModel.setLoading(true)
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            viewModel.setLoading(false)
            Log.e("Verification", "Falha na verificação", e)
            viewModel.setInfoMessage("Falha na verificação: ${e.localizedMessage}")
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            viewModel.setLoading(false)
            verificationId = id
            resendingToken = token
            Log.d("Verification", "Código enviado. ID: $id")
            viewModel.setInfoMessage("Código de verificação enviado.")
        }
    }

    private fun sendVerificationCode(number: String) {
        viewModel.setLoading(true)
        viewModel.setInfoMessage("Enviamos um código para $number")
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(number: String, token: PhoneAuthProvider.ForceResendingToken) {
        viewModel.setLoading(true)
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode(code: String) {
        verificationId?.let { id ->
            viewModel.setLoading(true)
            val credential = PhoneAuthProvider.getCredential(id, code)
            signInWithPhoneAuthCredential(credential)
        } ?: run {
            viewModel.setInfoMessage("Erro interno. Tente reenviar o código.")
            Log.e("Verification", "Tentativa de verificação sem verificationId.")
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                firebaseAuth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                    if (tokenTask.isSuccessful) {
                        val firebaseIdToken = tokenTask.result?.token
                        if (firebaseIdToken != null) {
                            loginOnBackend(firebaseIdToken)
                        } else {
                            viewModel.setLoading(false)
                            viewModel.setInfoMessage("Erro: Não foi possível obter o token.")
                        }
                    } else {
                        viewModel.setLoading(false)
                        viewModel.setInfoMessage("Erro de autenticação. Tente novamente.")
                    }
                }
            } else {
                viewModel.setLoading(false)
                viewModel.setInfoMessage("Código inválido ou expirado.")
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

    private fun loginOnBackend(firebaseIdToken: String) {
        viewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                val deviceName = "${android.os.Build.MODEL} (${android.os.Build.BRAND})"
                val fmcToken = getFcmToken()

                Log.d(TAG, "Chamando API /auth/login...")
                val response = apiService.loginWithToken(
                    firebaseToken = firebaseIdToken,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    fmcToken = fmcToken
                )

                viewModel.setLoading(false)
                val body = response.body()

                if (response.isSuccessful && body != null && body.status == "success") {
                    val authData = body.data
                    Log.d(TAG, "Resposta /auth/login: ${body.status}, isNewUser: ${authData?.isNewUser}")

                    if (authData?.isNewUser == true) {
                        Log.d(TAG, "Usuário novo detectado. Navegando para CreateProfile.")
                        navigateToProfileCreation(phoneNumber, firebaseIdToken)
                    } else if (authData != null) {
                        val authToken = authData.authToken
                        val refreshToken = authData.refreshToken

                        if (authToken != null && refreshToken != null) {
                            Log.d(TAG, "Usuário existente. Processando login.")
                            val userData = authData.userData
                            val user = UserEntity(
                                number = phoneNumber,
                                username1 = userData?.username1 ?: "",
                                username2 = userData?.username2 ?: "",
                                profilePhoto = userData?.profilePhotoFilename ?: "",
                                lastOnline = null
                                
                            )

                            authRepository.onLoginSuccess(
                                authToken = authToken,
                                refreshToken = refreshToken,
                                user = user
                            )
                            Log.d(TAG, "Login bem-sucedido. Navegando para MainActivity.")
                            navigateToMain()
                        } else {
                            Log.e(TAG, "Erro: Resposta de login do backend inválida (sem tokens).")
                            viewModel.setInfoMessage("Erro: Resposta de login inválida.")
                        }
                    } else {
                        Log.e(TAG, "Erro: Resposta de login do backend inválida (sem dados de usuário).")
                        viewModel.setInfoMessage("Erro: Resposta de login inválida.")
                    }
                } else {
                    val errorMsg = body?.message ?: "Erro ${response.code()}"
                    Log.e(TAG, "Erro no login do backend: $errorMsg (Code: ${response.code()})")
                    viewModel.setInfoMessage("Erro no login: $errorMsg")
                }
            } catch (e: Exception) {
                viewModel.setLoading(false)
                Log.e(TAG, "Falha na chamada /auth/login", e)
                viewModel.setInfoMessage("Erro de conexão. Verifique sua internet.")
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileCreation(number: String, firebaseToken: String) {
        val intent = Intent(this, CreateProfileActivity::class.java).apply {
            putExtra("phone_number", number)
            putExtra("firebase_token", firebaseToken)
        }
        startActivity(intent)
    }
}

@Composable
fun VerificationScreen(
    uiState: VerificationUiState,
    snackbarHostState: SnackbarHostState,
    onCodeChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onResendClick: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.infoMessage,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = uiState.verificationCode,
                    onValueChange = onCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de verificação") },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(55.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("Confirmar")
                }

                Text(
                    text = "Reenviar código",
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 16.dp)
                        .clickable(enabled = uiState.canResendCode && !uiState.isLoading) { onResendClick() },
                    color = if (uiState.canResendCode && !uiState.isLoading) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerificationScreenPreview() {
    VerificationScreen(
        uiState = VerificationUiState(verificationCode = "123456"),
        snackbarHostState = remember { SnackbarHostState() },
        onCodeChange = {},
        onConfirmClick = {},
        onResendClick = {}
    )
}