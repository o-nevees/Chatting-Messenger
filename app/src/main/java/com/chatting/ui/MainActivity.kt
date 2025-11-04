package com.chatting.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.data.repository.AuthRepository
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.websock.WebSocketState
import kotlinx.coroutines.launch
import com.chatting.dialog.DialogManager
import com.chatting.dialog.DialogController
import com.chatting.dialog.DialogConfig
import com.chatting.dialog.DialogType
import com.chatting.ui.screens.MainScreen
import com.chatting.dialog.DialogButtonConfig

class MainActivity : ComponentActivity() {

    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val message = if (isGranted) "Permissão de notificação concedida" else "Permissão de notificação negada"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as MyApp).appContainer
        val webSocketClient = appContainer.webSocketClient
        // Obtém a instância do AuthRepository
        val authRepository = (application as MyApp).authRepository

        requestNotificationPermission()

        setContent {
            MyComposeApplicationTheme {
                val currentStatus by webSocketClient.connectionState.collectAsState()
                val appBarTitle = produceAppBarTitle(status = currentStatus)
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        scope = scope,
                        drawerState = drawerState,
                        appBarTitle = appBarTitle,
                        onLogout = {
                            // Chama o logout na instância do AuthRepository
                            authRepository.onLogout(applicationContext)
                            finishAffinity()
                        }
                    )
                }
                
                DialogController()
            }

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        backToast?.cancel()
                        finishAffinity()
                    } else {
                        backToast = Toast.makeText(baseContext, "Pressione novamente para sair", Toast.LENGTH_SHORT)
                        backToast?.show()
                    }
                    backPressedTime = System.currentTimeMillis()
                }
            })
        }
    }

    @Composable
    private fun produceAppBarTitle(status: WebSocketState): String {
        val appName = getString(R.string.app_name)
        val connectingStatus = getString(R.string.status_connecting)
        val connectionFailedStatus = "Falha na conexão"

        return when (status) {
            WebSocketState.CONNECTED -> appName
            WebSocketState.FAILED -> connectionFailedStatus
            WebSocketState.DISCONNECTED -> appName
            WebSocketState.CONNECTING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "connecting_dots")
                val dotCount by infiniteTransition.animateValue(
                    initialValue = 0,
                    targetValue = 4,
                    typeConverter = Int.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "dot_count"
                )
                val dots = ".".repeat(dotCount % 4)
                "$connectingStatus$dots"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
    }
    
    fun showSuccessDialog() {
        DialogManager.show(
            DialogConfig(
                type = DialogType.SUCCESS,
                title = "Sucesso!",
                subtitle = "Sua operação foi concluída com êxito.",
                positiveButton = DialogButtonConfig(text = "OK", isFilled = true)
            )
        )
    }
    
    fun showErrorDialog() {
        DialogManager.show(
            DialogConfig(
                type = DialogType.ERROR,
                title = "Oops!",
                subtitle = "Algo deu errado. Tente novamente.",
                positiveButton = DialogButtonConfig(text = "Fechar", isFilled = false)
            )
        )
    }

    fun showWarningDialog() {
         DialogManager.show(
            DialogConfig(
                type = DialogType.WARNING,
                title = "Atenção",
                subtitle = "Você tem certeza que deseja executar esta ação?",
                positiveButton = DialogButtonConfig(text = "Confirmar", isFilled = true),
                negativeButton = DialogButtonConfig(text = "Cancelar")
            )
        )
    }

    fun showInfoDialog() {
         DialogManager.show(
            DialogConfig(
                type = DialogType.INFO,
                title = "Atualização Disponível",
                subtitle = "Uma nova versão do aplicativo está pronta para ser instalada.",
                positiveButton = DialogButtonConfig(text = "Atualizar"),
                negativeButton = DialogButtonConfig(text = "Agora não")
            )
        )
    }
    
     private fun requestNotificationPermission() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                 } else {
                      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                 }
             } else {
             }
         }
     }


    override fun onDestroy() {
        super.onDestroy()
    }
}