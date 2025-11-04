package com.chatting.ui.activitys

import android.os.Bundle
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatting.ui.components.AvatarInitial
import com.data.repository.UserDataStore
import com.chatting.ui.theme.MyComposeApplicationTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                SettingsScreen(onNavigateUp = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateUp: () -> Unit) {

    val context = LocalContext.current
    val userDataStore = remember { UserDataStore.getInstance(context.applicationContext as android.app.Application) }
    val loggedInUser by userDataStore.loggedInUser.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            item {
                ProfileSection(
                    name = loggedInUser?.username1 ?: "Carregando...",
                    phoneNumber = loggedInUser?.number ?: "",
                    profilePhotoPath = loggedInUser?.profilePhoto,
                    onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
           
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                SettingItem(
                    icon = Icons.Default.Palette,
                    title = "Aparência",
                    onClick = { context.startActivity(Intent(context, AppearanceActivity::class.java)) }
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.Lock,
                    title = "Privacidade",
                    onClick = { context.startActivity(Intent(context, PrivacyActivity::class.java)) }
                )
            }
             item {
                SettingItem(
                    icon = Icons.Default.Folder,
                    title = "Dados e armazenamento",
                    onClick = { context.startActivity(Intent(context, DataStorageActivity::class.java)) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
            }
             item {
                SettingItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Ajuda",
                    onClick = { /* TODO: Navegar para tela de ajuda */ }
                )
            }
             item {
                SettingItem(
                    icon = Icons.Default.People,
                    title = "Convidar amigos",
                    onClick = { /* TODO: Abrir tela para convidar amigos */ }
                )
            }
        }
    }
}

@Composable
fun ProfileSection(name: String, phoneNumber: String, profilePhotoPath: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarInitial(
            name = name,
            size = 60.dp,
            localImagePath = profilePhotoPath
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 17.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MyComposeApplicationTheme {
        SettingsScreen(onNavigateUp = {})
    }
}