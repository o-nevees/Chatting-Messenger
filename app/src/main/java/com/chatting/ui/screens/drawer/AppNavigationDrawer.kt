package com.chatting.ui.screens.drawer // Pacote corrigido

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue 
import androidx.compose.runtime.remember 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.data.repository.UserDataStore
import com.chatting.ui.activitys.SettingsActivity
import com.chatting.ui.components.AvatarInitial
import com.chatting.ui.components.StyledText
import com.chatting.ui.theme.ThemeManager
import com.chatting.ui.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Define a estrutura de um item de navegação no Drawer.
 */
internal data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isScreen: Boolean = true // Indica se é uma tela ou uma ação (ex: SettingsActivity)
)

/**
 * Composable principal que envolve o conteúdo da tela (ex: MainScreen)
 * com um ModalNavigationDrawer.
 *
 * @param drawerState O estado do drawer (aberto/fechado), controlado pela tela principal.
 * @param gesturesEnabled Define se o drawer pode ser aberto por gestos.
 * @param selectedRoute A rota/tela atualmente selecionada.
 * @param onRouteSelected Callback quando um novo item de navegação é clicado.
 * @param onLogout Callback quando o botão de logout é clicado.
 * @param content O conteúdo principal da tela (geralmente um Scaffold) a ser exibido.
 */
@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    selectedRoute: String,
    onRouteSelected: (route: String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    // Lista de itens de navegação.
    // Pode ser expandida facilmente no futuro.
    val navItems = listOf(
        NavItem("home", "Início", Icons.Default.Home),
        NavItem("settings", "Configurações", Icons.Default.Settings, isScreen = false),
        NavItem("about", "Sobre", Icons.Default.Info)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            AppDrawerContent(
                navItems = navItems,
                selectedRoute = selectedRoute,
                onRouteSelected = onRouteSelected,
                onLogout = onLogout
            )
        },
        content = content // O Scaffold e o resto da MainScreen são passados aqui
    )
}

/**
 * Define o conteúdo interno do ModalDrawerSheet.
 */
@Composable
private fun AppDrawerContent(
    navItems: List<NavItem>,
    selectedRoute: String,
    onRouteSelected: (route: String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        windowInsets = WindowInsets(0, 0, 0, 0) // Remove insets extras
    ) {
        DrawerHeader()
        Spacer(Modifier.height(12.dp))

        // Itens de Navegação principais
        navItems.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = { StyledText(item.label) },
                selected = item.route == selectedRoute,
                onClick = { onRouteSelected(item.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Item de Logout
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") },
            label = { StyledText("Logout") },
            selected = false,
            onClick = onLogout,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

/**
 * O cabeçalho do Navigation Drawer.
 * Exibe o avatar, nome, status e o botão de tema.
 */
@Composable
private fun DrawerHeader() {
    val context = LocalContext.current
    // Obtém dados do usuário logado
    val userDataStore = remember { UserDataStore.getInstance(context.applicationContext as android.app.Application) }
    val loggedInUser by userDataStore.loggedInUser.collectAsStateWithLifecycle()

    val userName = loggedInUser?.username1 ?: "Usuário"
    val userStatus = loggedInUser?.number ?: "Offline"
    val localPhotoPath = loggedInUser?.profilePhoto

    // Obtém o modo de tema atual
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemInDarkMode = isSystemInDarkTheme()
    val isDarkMode = when (themeMode) {
        PreferencesManager.THEME_LIGHT -> false
        PreferencesManager.THEME_DARK -> true
        else -> isSystemInDarkMode
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(WindowInsets.statusBars.asPaddingValues()) // Aplica padding da status bar
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp)
        ) {
            // Informações do Usuário
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                AvatarInitial(
                    name = userName,
                    size = 64.dp,
                    localImagePath = localPhotoPath
                )
                Spacer(modifier = Modifier.height(8.dp))
                StyledText(
                    text = userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                StyledText(
                    text = userStatus,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botão de Tema
            AnimatedContent(
                targetState = isDarkMode,
                modifier = Modifier.align(Alignment.TopEnd),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200))) togetherWith
                            (fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200)))
                },
                label = "ThemeToggleAnimation"
            ) { isDark ->
                IconButton(onClick = {
                    val newTheme = if (isDark) PreferencesManager.THEME_LIGHT else PreferencesManager.THEME_DARK
                    ThemeManager.setTheme(context, newTheme)
                    // Não precisa fechar o drawer aqui, o onRouteSelected fará isso
                }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                        contentDescription = "Mudar Tema",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}