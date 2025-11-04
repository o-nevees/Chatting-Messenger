package com.chatting.ui.activitys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chatting.ui.MainActivity
//import com.chatting.ui.theme.ChattingTheme // Importe o tema do seu projeto

class ExceptionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_MESSAGE = "EXTRA_ERROR_MESSAGE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pega a mensagem de erro do Intent
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "Nenhuma mensagem de erro fornecida."

        setContent {
            // Use o tema principal do seu aplicativo
          //  ChattingTheme {
                ExceptionScreen(
                    errorMessage = errorMessage,
                    onCopyClick = { copyErrorToClipboard(errorMessage) },
                    onRestartClick = { restartApp() }
                )
            }
      //  }
    }

    private fun copyErrorToClipboard(errorMessage: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("error_log", errorMessage)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Erro copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        // Garanta que 'MainActivity::class.java' é a sua activity principal
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun ExceptionScreen(
    errorMessage: String,
    onCopyClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ocorreu um Erro 😔",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Um erro inesperado aconteceu. As informações do erro estão abaixo. Por favor, reinicie o aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Caixa com a mensagem de erro técnica (com scroll)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp), // Limita a altura máxima
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace // Ideal para logs de erro
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCopyClick) {
                    Text("Copiar Erro")
                }

                Button(onClick = onRestartClick) {
                    Text("Reiniciar App")
                }
            }
        }
    }
}

