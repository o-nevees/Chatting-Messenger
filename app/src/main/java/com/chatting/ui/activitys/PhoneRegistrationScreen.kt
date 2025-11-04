package com.chatting.ui.activitys

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatting.ui.activitys.PhoneRegistrationUiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhoneRegistrationScreen(
    uiState: PhoneRegistrationUiState,
    onCountryClick: () -> Unit,
    onLocalPhoneNumberChange: (String) -> Unit,
    onStartClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RegistrationHeader()
                Spacer(modifier = Modifier.height(32.dp))
                PhoneNumberInputs(
                    uiState = uiState,
                    onCountryClick = onCountryClick,
                    onLocalPhoneNumberChange = onLocalPhoneNumberChange
                )
            }
            BottomActionSection(
                onStartClick = onStartClick,
                uiState = uiState,
                onLocalPhoneNumberChange = onLocalPhoneNumberChange
            )
        }
    }
}

@Composable
private fun RegistrationHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Verifique seu número de telefone",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "O Chatting enviará um código SMS para verificar seu número. Podem ser aplicadas taxas de mensagens e dados.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PhoneNumberInputs(
    uiState: PhoneRegistrationUiState,
    onCountryClick: () -> Unit,
    onLocalPhoneNumberChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = uiState.countryDisplayText,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCountryClick),
            label = { Text("País") },
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Selecionar País"
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PhoneNumberInputField(
            countryCode = uiState.countryCode,
            localPhoneNumber = uiState.localPhoneNumber,
            isError = uiState.phoneNumberError != null,
            onLocalPhoneNumberChange = onLocalPhoneNumberChange
        )
        if (uiState.phoneNumberError != null) {
            Text(
                text = uiState.phoneNumberError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}

@Composable
private fun PhoneNumberInputField(
    countryCode: String,
    localPhoneNumber: String,
    onLocalPhoneNumberChange: (String) -> Unit,
    isError: Boolean
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    BasicTextField(
        value = localPhoneNumber,
        onValueChange = onLocalPhoneNumberChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (countryCode.isNotEmpty()) {
                    Text(
                        text = countryCode,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 12.dp)
                            .width(1.dp),
                        color = borderColor
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (localPhoneNumber.isEmpty()) {
                        Text(
                            text = "Número de telefone",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BottomActionSection(
    onStartClick: () -> Unit,
    uiState: PhoneRegistrationUiState,
    onLocalPhoneNumberChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { 200 },
                    animationSpec = tween(durationMillis = 500, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200))
            ) {
                FloatingActionButton(
                    onClick = onStartClick,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Avançar"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ManualDialpad(
            onNumberClick = { number ->
                onLocalPhoneNumberChange(uiState.localPhoneNumber + number)
            },
            onDeleteClick = {
                if (uiState.localPhoneNumber.isNotEmpty()) {
                    onLocalPhoneNumberChange(uiState.localPhoneNumber.dropLast(1))
                }
            }
        )
    }
}

@Composable
fun ManualDialpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Espaçamento vertical entre as linhas
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Espaçamento horizontal entre os botões
        ) {
            DialpadButton("1", onNumberClick, modifier = Modifier.weight(1f))
            DialpadButton("2", onNumberClick, "ABC", modifier = Modifier.weight(1f))
            DialpadButton("3", onNumberClick, "DEF", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DialpadButton("4", onNumberClick, "GHI", modifier = Modifier.weight(1f))
            DialpadButton("5", onNumberClick, "JKL", modifier = Modifier.weight(1f))
            DialpadButton("6", onNumberClick, "MNO", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DialpadButton("7", onNumberClick, "PQRS", modifier = Modifier.weight(1f))
            DialpadButton("8", onNumberClick, "TUV", modifier = Modifier.weight(1f))
            DialpadButton("9", onNumberClick, "WXYZ", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            DialpadButton("0", onNumberClick, "+", modifier = Modifier.weight(1f))
            DialpadIconButton(
                icon = {
                    Text(
                        text = "✕",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DialpadButton(
    mainText: String,
    onNumberClick: (String) -> Unit,
    subText: String? = null,
    modifier: Modifier = Modifier
) {
    DialpadButton(
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black
                )
                subText?.let {
                    Text(
                        text = it,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray // Cor do texto secundário, para combinar com a imagem
                    )
                }
            }
        },
        onClick = { onNumberClick(mainText) },
        modifier = modifier
    )
}

@Composable
fun DialpadIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DialpadButton(
        content = icon,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun DialpadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(2.5f) // Proporção retangular
            .clip(RoundedCornerShape(8.dp)) // Cantos arredondados
            .background(Color(0xFFE0E0E0)) // Cor de fundo clara
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneRegistrationScreenPreview() {
    val uiState = PhoneRegistrationUiState(
        countryDisplayText = "Brasil",
        countryCode = "+55",
        localPhoneNumber = "99999-9999"
    )
    MaterialTheme {
        PhoneRegistrationScreen(
            uiState = uiState,
            onCountryClick = {},
            onLocalPhoneNumberChange = {},
            onStartClick = {}
        )
    }
}