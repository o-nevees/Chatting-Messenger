package com.chatting.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.CalendarView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.chatting.ui.R
import com.chatting.ui.viewmodel.CreateProfileUiState
import com.chatting.ui.viewmodel.CreateProfileViewModel
import com.chatting.ui.viewmodel.UsernameValidationState
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CreateProfileScreen(
    viewModel: CreateProfileViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Lida com o botão "voltar" do sistema
    BackHandler(enabled = uiState.currentStep > 0) {
        viewModel.onPreviousStep()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Perfil") },
                navigationIcon = {
                    if (uiState.currentStep > 0) {
                        IconButton(onClick = viewModel::onPreviousStep) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentStep = uiState.currentStep,
                isNextEnabled = uiState.isNextButtonEnabled,
                onNextClick = {
                    if (uiState.currentStep == 3) onComplete() else viewModel.onNextStep()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // Anima a transição entre os passos
                AnimatedContent(targetState = uiState.currentStep) { step ->
                    when (step) {
                        0 -> NameStep(uiState = uiState, onNameChange = viewModel::onNameChange)
                        1 -> BirthdateStep(uiState = uiState, onBirthdateChange = viewModel::onBirthdateChange)
                        2 -> UsernameStep(uiState = uiState, onUsernameChange = viewModel::onUsernameChange)
                        3 -> PhotoStep(uiState = uiState, onProfileImageChange = viewModel::onProfileImageChange)
                    }
                }
            }
        }
    }
}

// --- Passos Individuais ---

@Composable
fun NameStep(uiState: CreateProfileUiState, onNameChange: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Qual é o seu nome?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text("Este nome será visível para seus contatos.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Nome Completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
    }
}

@Composable
fun BirthdateStep(uiState: CreateProfileUiState, onBirthdateChange: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onBirthdateChange(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // Impede data futura
            show()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Qual a sua data de nascimento?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        OutlinedTextField(
            value = uiState.birthdate,
            onValueChange = {},
            label = { Text("Data de Nascimento") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker() },
            readOnly = true,
            enabled = false, // Desabilita para forçar o clique
            colors = OutlinedTextFieldDefaults.colors(
                 disabledTextColor = MaterialTheme.colorScheme.onSurface,
                 disabledBorderColor = MaterialTheme.colorScheme.outline,
                 disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
        )
    }
}


@Composable
fun UsernameStep(uiState: CreateProfileUiState, onUsernameChange: (String) -> Unit) {
    val (helperText, helperColor, trailingIcon) = when (uiState.usernameValidationState) {
        UsernameValidationState.Idle -> Triple(null, Color.Gray, null)
        UsernameValidationState.TooShort -> Triple("Mínimo de 5 caracteres", MaterialTheme.colorScheme.error, Icons.Default.Close)
        UsernameValidationState.Checking -> Triple("Verificando...", Color.Gray, null)
        UsernameValidationState.Available -> Triple("Disponível!", MaterialTheme.colorScheme.primary, Icons.Default.Check)
        UsernameValidationState.Taken -> Triple("Este nome de usuário já está em uso", MaterialTheme.colorScheme.error, Icons.Default.Close)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Escolha um nome de usuário", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Nome de usuário") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = uiState.usernameValidationState == UsernameValidationState.Taken || uiState.usernameValidationState == UsernameValidationState.TooShort,
            supportingText = {
                if (helperText != null) {
                    Text(text = helperText, color = helperColor)
                }
            },
            trailingIcon = {
                if (uiState.usernameValidationState == UsernameValidationState.Checking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (trailingIcon != null) {
                    Icon(trailingIcon, contentDescription = null, tint = helperColor)
                }
            }
        )
    }
}

@Composable
fun PhotoStep(uiState: CreateProfileUiState, onProfileImageChange: (Uri?) -> Unit) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onProfileImageChange(uri) }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Adicione uma foto de perfil", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

        // Lógica para exibir a imagem selecionada ou um placeholder
        val painter = if (uiState.profileImageUri != null) {
            rememberAsyncImagePainter(model = uiState.profileImageUri)
        } else {
            painterResource(id = R.drawable.attach_file_24px) // Substitua por seu placeholder
        }

        Image(
            painter = painter,
            contentDescription = "Foto de Perfil",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Escolher Foto")
        }
        TextButton(onClick = { onProfileImageChange(null) }) {
            Text("Pular por enquanto")
        }
    }
}

// --- Componentes de UI ---

@Composable
fun BottomNavigationBar(
    currentStep: Int,
    isNextEnabled: Boolean,
    onNextClick: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNextClick,
                enabled = isNextEnabled
            ) {
                Text(if (currentStep == 3) "Concluir" else "Avançar")
            }
        }
    }
}