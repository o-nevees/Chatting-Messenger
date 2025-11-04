package com.chatting.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.chatting.ui.activitys.PhoneRegistrationActivity
import com.data.repository.*

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Esta Activity agora usa Jetpack Compose para exibir uma tela de carregamento
 * enquanto decide para qual tela principal o usuário deve ser direcionado.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Define o conteúdo da UI com Jetpack Compose.
        //    Isso mostra imediatamente a tela de carregamento.
        setContent {
            LauncherScreen()
        }

        // 2. A lógica de navegação original é mantida e executada em uma coroutine.
        lifecycleScope.launch {
            // Obtém a instância do AuthRepository a partir do MyApp
            val authRepository = (application as MyApp).authRepository
            // Pega o primeiro estado emitido pelo repositório
            val authState = authRepository.authStateFlow.first()
            
            // Navega para a tela correta com base no estado de autenticação
            val destination = when (authState) {
                AuthState.LOGGED_IN -> MainActivity::class.java
                AuthState.LOGGED_OUT -> PhoneRegistrationActivity::class.java
          }
            navigateTo(destination)
        }
    }

    /**
     * Navega para a Activity de destino e finaliza a LauncherActivity.
     * @param activityClass A classe da Activity para a qual navegar.
     */
    private fun navigateTo(activityClass: Class<out Activity>) {
        startActivity(Intent(this, activityClass))
        finish() // Fecha a LauncherActivity para que ela não fique na pilha de volta
    }
}

/**
 * Um Composable que exibe um indicador de progresso circular no centro da tela.
 * Esta é a UI que o usuário vê enquanto a lógica de navegação é processada.
 */
@Composable
private fun LauncherScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}