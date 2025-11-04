package com.chatting.ui.activitys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.utils.PreferencesManager
import com.chatting.ui.viewmodel.ChatsViewModel

class DataStorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                DataStorageScreen(onNavigateUp = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageScreen(
    onNavigateUp: () -> Unit,
    chatsViewModel: ChatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var showMediaQualityDialog by remember { mutableStateOf(false) }

    // Estados para as opções de download automático
    var downloadImagesMobile by remember { mutableStateOf(preferencesManager.getAutoDownloadImagesOnMobile()) }
    var downloadAudioMobile by remember { mutableStateOf(preferencesManager.getAutoDownloadAudioOnMobile()) }
    var downloadVideoMobile by remember { mutableStateOf(preferencesManager.getAutoDownloadVideosOnMobile()) }
    var downloadDocsMobile by remember { mutableStateOf(preferencesManager.getAutoDownloadDocsOnMobile()) }

    var downloadImagesWifi by remember { mutableStateOf(preferencesManager.getAutoDownloadImagesOnWifi()) }
    var downloadAudioWifi by remember { mutableStateOf(preferencesManager.getAutoDownloadAudioOnWifi()) }
    var downloadVideoWifi by remember { mutableStateOf(preferencesManager.getAutoDownloadVideosOnWifi()) }
    var downloadDocsWifi by remember { mutableStateOf(preferencesManager.getAutoDownloadDocsOnWifi()) }
    
    val cacheSize by chatsViewModel.getCacheSize().observeAsState("Calculando...")


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados e Armazenamento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Seção de Uso de Rede
            item {
                SectionTitle("Uso de Rede")
            }
            item {
                StorageSettingItem(
                    title = "Download automático de mídia",
                    subtitle = "Escolha quando baixar mídias automaticamente"
                )
            }
            item {
                Column(Modifier.padding(start = 16.dp)) {
                    AutoDownloadSection("Ao usar dados móveis", listOf(
                        "Imagens" to downloadImagesMobile,
                        "Áudio" to downloadAudioMobile,
                        "Vídeos" to downloadVideoMobile,
                        "Documentos" to downloadDocsMobile
                    )) { type, enabled ->
                        when(type) {
                            "Imagens" -> {
                                downloadImagesMobile = enabled
                                preferencesManager.setAutoDownloadImagesOnMobile(enabled)
                            }
                            "Áudio" -> {
                                downloadAudioMobile = enabled
                                preferencesManager.setAutoDownloadAudioOnMobile(enabled)
                            }
                            "Vídeos" -> {
                                downloadVideoMobile = enabled
                                preferencesManager.setAutoDownloadVideosOnMobile(enabled)
                            }
                            "Documentos" -> {
                                downloadDocsMobile = enabled
                                preferencesManager.setAutoDownloadDocsOnMobile(enabled)
                            }
                        }
                    }
                    AutoDownloadSection("Ao usar Wi-Fi", listOf(
                        "Imagens" to downloadImagesWifi,
                        "Áudio" to downloadAudioWifi,
                        "Vídeos" to downloadVideoWifi,
                        "Documentos" to downloadDocsWifi
                    )) { type, enabled ->
                         when(type) {
                            "Imagens" -> {
                                downloadImagesWifi = enabled
                                preferencesManager.setAutoDownloadImagesOnWifi(enabled)
                            }
                            "Áudio" -> {
                                downloadAudioWifi = enabled
                                preferencesManager.setAutoDownloadAudioOnWifi(enabled)
                            }
                            "Vídeos" -> {
                                downloadVideoWifi = enabled
                                preferencesManager.setAutoDownloadVideosOnWifi(enabled)
                            }
                            "Documentos" -> {
                                downloadDocsWifi = enabled
                                preferencesManager.setAutoDownloadDocsOnWifi(enabled)
                            }
                        }
                    }
                }
            }

            // Seção de Uso do Armazenamento
            item {
                SectionTitle("Uso do Armazenamento")
            }
            item {
                StorageSettingItem(
                    title = "Limpar cache",
                    subtitle = "$cacheSize em cache",
                    onClick = { chatsViewModel.clearCache() }
                )
            }
            item {
                StorageSettingItem(
                    title = "Qualidade da mídia",
                    subtitle = "Escolha a qualidade dos arquivos de mídia a serem enviados",
                    onClick = { showMediaQualityDialog = true }
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun StorageSettingItem(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun AutoDownloadSection(title: String, options: List<Pair<String, Boolean>>, onCheckedChange: (String, Boolean) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        options.forEach { (optionTitle, isChecked) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(optionTitle, !isChecked) }
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckedChange(optionTitle, it) }
                )
                Text(text = optionTitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}