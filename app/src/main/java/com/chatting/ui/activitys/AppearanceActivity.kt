package com.chatting.ui.activitys

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chatting.ui.MainActivity
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.theme.ThemeManager
import com.chatting.ui.utils.PreferencesManager
import com.chatting.ui.utils.mapFontSizeToSp

data class WallpaperOption(val value: String, val brush: Brush)

val predefinedWallpapers = listOf(
    WallpaperOption("#d32f2f", SolidColor(Color(0xFFd32f2f))),
    WallpaperOption("#c2185b", SolidColor(Color(0xFFc2185b))),
    WallpaperOption("#7b1fa2", SolidColor(Color(0xFF7b1fa2))),
    WallpaperOption("#512da8", SolidColor(Color(0xFF512da8))),
    WallpaperOption("#303f9f", SolidColor(Color(0xFF303f9f))),
    WallpaperOption("#0288d1", SolidColor(Color(0xFF0288d1))),
    WallpaperOption("#00796b", SolidColor(Color(0xFF00796b))),
    WallpaperOption("#388e3c", SolidColor(Color(0xFF388e3c))),
    WallpaperOption("#689f38", SolidColor(Color(0xFF689f38))),
    WallpaperOption("#afb42b", SolidColor(Color(0xFFafb42b))),
    WallpaperOption("#fbc02d", SolidColor(Color(0xFFfbc02d))),
    WallpaperOption("#ffa000", SolidColor(Color(0xFFffa000))),
    WallpaperOption("gradient_1", Brush.linearGradient(listOf(Color(0xFFF9A825), Color(0xFFF4511E)))),
    WallpaperOption("gradient_2", Brush.linearGradient(listOf(Color(0xFF007991), Color(0xFF78ffd6)))),
    WallpaperOption("gradient_3", Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))))
)

class AppearanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppearanceScreen()
                }
            }
        }
    }
}

@Composable
fun MainAppearanceScreen() {
    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> AppearanceScreen(onNavigateTo = { screen -> currentScreen = screen })
        "chat_customization" -> ChatCustomizationScreen(
            onBackPressed = { currentScreen = "main" },
            onNavigateTo = { screen -> currentScreen = screen }
        )
        "wallpaper_selection" -> WallpaperSelectionScreen(onBackPressed = { currentScreen = "chat_customization" })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    val currentTheme by ThemeManager.themeMode.collectAsState()
    val currentLanguage = remember { mutableStateOf(preferencesManager.getLanguage()) }
    val currentFontSize = remember { mutableStateOf(preferencesManager.getMessageFontSize()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aparência") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Idioma",
                subtitle = getLanguageName(currentLanguage.value),
                onClick = { showLanguageDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Tema",
                subtitle = getThemeName(currentTheme),
                onClick = { showThemeDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.ChatBubble,
                title = "Cor e papel de parede do chat",
                subtitle = "Personalize o fundo e as cores",
                onClick = { onNavigateTo("chat_customization") }
            )
            SettingsItem(
                icon = Icons.Default.FormatSize,
                title = "Tamanho da fonte da mensagem",
                subtitle = getFontSizeName(currentFontSize.value),
                onClick = { showFontSizeDialog = true }
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                ThemeManager.setTheme(context, theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage.value,
            onLanguageSelected = { language ->
                currentLanguage.value = language
                preferencesManager.setLanguage(language)

                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                (context as? Activity)?.finish()

                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showFontSizeDialog) {
        FontSizeSelectionDialog(
            currentSize = currentFontSize.value,
            onSizeSelected = { size ->
                currentFontSize.value = size
                preferencesManager.setMessageFontSize(size)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCustomizationScreen(onBackPressed: () -> Unit, onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var showColorDialog by remember { mutableStateOf(false) }
    var bubbleColor by remember { mutableStateOf(Color(preferencesManager.getMessageBubbleColor())) }
    var wallpaper by remember { mutableStateOf(preferencesManager.getChatWallpaper()) }
    var darkenWallpaper by remember { mutableStateOf(false) }
    val fontSize = mapFontSizeToSp(preferencesManager.getMessageFontSize())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cor e Papel de Parede") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            ChatPreview(wallpaperValue = wallpaper, bubbleColor = bubbleColor, fontSize = fontSize)
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                CustomizationItem(
                    title = "Cor do chat",
                    onClick = { showColorDialog = true },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(bubbleColor, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                )
                CustomizationItem(
                    title = "Redefinir as cores dos chats",
                    onClick = {
                        val defaultBubbleColor = 0xFF0288D1.toInt()
                        preferencesManager.setMessageBubbleColor(defaultBubbleColor)
                        bubbleColor = Color(defaultBubbleColor)
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                CustomizationItem(title = "Escolher o papel de parede", onClick = { onNavigateTo("wallpaper_selection") })
                CustomizationItem(
                    title = "Escurecer o papel de parede no modo escuro",
                    onClick = { darkenWallpaper = !darkenWallpaper },
                    trailingContent = { Switch(checked = darkenWallpaper, onCheckedChange = { darkenWallpaper = it }) }
                )
                CustomizationItem(
                    title = "Remover os papéis de parede",
                    onClick = {
                        val defaultWallpaper = ""
                        preferencesManager.setChatWallpaper(defaultWallpaper)
                        wallpaper = defaultWallpaper
                    }
                )
            }
        }
    }
    if (showColorDialog) {
        ColorSelectionDialog(
            onDismiss = { showColorDialog = false },
            onColorSelected = { color ->
                preferencesManager.setMessageBubbleColor(color.toArgb())
                bubbleColor = color
                showColorDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSelectionScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flag)
                preferencesManager.setChatWallpaper(it.toString())
                onBackPressed()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cor e papel de parede do chat") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { imagePickerLauncher.launch("image/*") }.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Escolher uma foto", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Text("Pré-definidos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 16.dp))
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(predefinedWallpapers) { wallpaper ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(wallpaper.brush).clickable {
                                    preferencesManager.setChatWallpaper(wallpaper.value)
                                    onBackPressed()
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationItem(title: String, onClick: () -> Unit, trailingContent: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        trailingContent?.invoke()
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatPreview(wallpaperValue: String, bubbleColor: Color, fontSize: Dp) {
    val themeBackgroundColor = MaterialTheme.colorScheme.background

    val backgroundModifier = remember(wallpaperValue, themeBackgroundColor) {
        when {
            wallpaperValue.isEmpty() -> Modifier.background(themeBackgroundColor)
            wallpaperValue.startsWith("content://") -> Modifier
            wallpaperValue.startsWith("gradient_") -> {
                val brush = when (wallpaperValue) {
                    "gradient_1" -> Brush.linearGradient(listOf(Color(0xFFF9A825), Color(0xFFF4511E)))
                    "gradient_2" -> Brush.linearGradient(listOf(Color(0xFF007991), Color(0xFF78ffd6)))
                    "gradient_3" -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
                    else -> SolidColor(Color.Transparent)
                }
                Modifier.background(brush)
            }
            else -> {
                val color = try { Color(android.graphics.Color.parseColor(wallpaperValue)) } catch (e: Exception) { Color.Transparent }
                Modifier.background(color)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .then(backgroundModifier)
    ) {
        if (wallpaperValue.startsWith("content://")) {
            AsyncImage(
                model = wallpaperValue,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "Olá! Como vai?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = fontSize.value.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier.background(bubbleColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "Tudo bem por aqui!", color = Color.White, fontSize = fontSize.value.sp)
                }
            }
        }
    }
}

@Composable
fun FontSizeSelectionDialog(currentSize: Int, onSizeSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val fontSizes = mapOf(
        PreferencesManager.FONT_SIZE_SMALL to "Pequeno",
        PreferencesManager.FONT_SIZE_NORMAL to "Normal",
        PreferencesManager.FONT_SIZE_LARGE to "Grande",
        PreferencesManager.FONT_SIZE_EXTRA_LARGE to "Extra grande"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tamanho da fonte da mensagem") },
        text = {
            Column {
                fontSizes.forEach { (size, name) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSizeSelected(size) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentSize == size, onClick = { onSizeSelected(size) })
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ThemeSelectionDialog(currentTheme: Int, onThemeSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher tema") },
        text = {
            Column {
                ThemeOption(title = "Claro", isSelected = currentTheme == PreferencesManager.THEME_LIGHT, onClick = { onThemeSelected(PreferencesManager.THEME_LIGHT) })
                ThemeOption(title = "Escuro", isSelected = currentTheme == PreferencesManager.THEME_DARK, onClick = { onThemeSelected(PreferencesManager.THEME_DARK) })
                ThemeOption(title = "Padrão do sistema", isSelected = currentTheme == PreferencesManager.THEME_SYSTEM, onClick = { onThemeSelected(PreferencesManager.THEME_SYSTEM) })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun ThemeOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title)
    }
}

@Composable
fun LanguageSelectionDialog(currentLanguage: String, onLanguageSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val languages = listOf("pt" to "Português", "en" to "English", "es" to "Español")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar idioma") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected(code) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentLanguage == code, onClick = { onLanguageSelected(code) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun ColorSelectionDialog(onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    val colors = listOf(
        Color(0xFF0288D1), Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFFFBC02D),
        Color(0xFF7B1FA2), Color(0xFF546E7A), Color(0xFF5D4037), Color(0xFF00796B),
        Color(0xFFC2185B), Color(0xFF303F9F), Color(0xFF689F38), Color(0xFFFFA000)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolha uma cor") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(colors) { color ->
                    Box(modifier = Modifier.size(48.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { onColorSelected(color) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

fun getThemeName(theme: Int): String {
    return when (theme) {
        PreferencesManager.THEME_LIGHT -> "Claro"
        PreferencesManager.THEME_DARK -> "Escuro"
        else -> "Padrão do sistema"
    }
}

fun getLanguageName(language: String): String {
    return when (language) {
        "pt" -> "Português"
        "en" -> "English"
        "es" -> "Español"
        else -> "Padrão do sistema"
    }
}

fun getFontSizeName(size: Int): String {
    return when (size) {
        PreferencesManager.FONT_SIZE_SMALL -> "Pequeno"
        PreferencesManager.FONT_SIZE_NORMAL -> "Normal"
        PreferencesManager.FONT_SIZE_LARGE -> "Grande"
        PreferencesManager.FONT_SIZE_EXTRA_LARGE -> "Extra grande"
        else -> "Normal"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyComposeApplicationTheme {
        MainAppearanceScreen()
    }
}