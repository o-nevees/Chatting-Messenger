package com.chatting.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chatting.ui.activitys.ChatActivity
import com.chatting.ui.activitys.SettingsActivity
import com.chatting.ui.components.AvatarInitial
import com.chatting.ui.components.StyledText
import com.chatting.ui.components.TextContentType
import com.chatting.ui.model.*
import com.data.repository.UserDataStore
import com.chatting.ui.screens.AboutScreen
import com.chatting.ui.screens.drawer.AppNavigationDrawer 
import com.chatting.ui.screens.search.SearchScreen
import com.chatting.ui.theme.ThemeManager

import com.chatting.ui.utils.PreferencesManager
import com.chatting.ui.utils.TimeFormat
import com.chatting.ui.viewmodel.ChatsViewModel
import com.chatting.ui.viewmodel.SearchUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File


// REMOVIDO: data class NavItem(val route: String, val label: String, val icon: ImageVector)

private sealed class ParsedMessageType {
    object Text : ParsedMessageType()
    data class Media(val path: String, val description: String) : ParsedMessageType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scope: CoroutineScope, // Mantido para controlar o drawer e o scaffold
    drawerState: DrawerState, // Mantido para controlar o drawer
    appBarTitle: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Estados que controlam a UI principal
    var selectedRoute by remember { mutableStateOf("home") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedConversations by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedConversations.isNotEmpty()

    // ViewModel e estados de busca
    val chatsViewModel: ChatsViewModel = viewModel()
    val searchQuery by chatsViewModel.searchQuery.observeAsState("")
    val searchUiState by chatsViewModel.searchUiState.observeAsState(initial = SearchUiState())
    val localSearchResults by chatsViewModel.localSearchResults.observeAsState(initial = emptyList())
    val recentConversations by chatsViewModel.recentConversations.observeAsState(initial = emptyList())


    // *** ALTERAÇÃO PRINCIPAL ***
    // O ModalNavigationDrawer foi substituído pelo novo AppNavigationDrawer.
    // O Scaffold e seu conteúdo agora são passados como o lambda 'content'
    // para o AppNavigationDrawer.
    AppNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSelectionMode && !isSearchActive,
        selectedRoute = selectedRoute,
        onRouteSelected = { newRoute ->
            scope.launch { drawerState.close() }
            if (newRoute == "settings") {
                // Ação de abrir Activity
                context.startActivity(Intent(context, SettingsActivity::class.java))
            } else {
                // Ação de mudar a tela interna
                selectedRoute = newRoute
            }
        },
        onLogout = {
            scope.launch { drawerState.close() }
            selectedConversations = emptySet()
            onLogout()
        }
    ) {
        // O conteúdo que o AppNavigationDrawer irá "envelopar" é o Scaffold:
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = {
                        if (targetState) {
                            (slideInVertically(initialOffsetY = { -it }) + fadeIn()) togetherWith (slideOutVertically(targetOffsetY = { it }) + fadeOut())
                        } else {
                            (slideInVertically(initialOffsetY = { it }) + fadeIn()) togetherWith (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
                        }
                    },
                    label = "TopBarAnimation"
                ) { selectionActive ->
                    if (selectionActive) {
                        SelectionTopBar(
                            selectionCount = selectedConversations.size,
                            onClearSelection = { selectedConversations = emptySet() },
                            onPin = {
                                Toast.makeText(context, "Fixar (não implementado)", Toast.LENGTH_SHORT).show()
                                selectedConversations = emptySet()
                            },
                            onArchive = {
                                Toast.makeText(context, "Arquivar (não implementado)", Toast.LENGTH_SHORT).show()
                                selectedConversations = emptySet()
                            }
                        )
                    } else {
                        MainTopAppBar(
                            title = appBarTitle,
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { chatsViewModel.setSearchQuery(it) },
                            onSearchRequested = { isSearchActive = true },
                            onSearchCancelled = {
                                isSearchActive = false
                                chatsViewModel.setSearchQuery("")
                            },
                            onMenuClicked = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isSearchActive) {
                    SearchScreen(
                        searchQuery = searchQuery,
                        searchUiState = searchUiState,
                        localSearchResults = localSearchResults,
                        recentConversations = recentConversations,
                        chatsViewModel = chatsViewModel,
                        onResultClick = { _ ->
                            isSearchActive = false
                            chatsViewModel.setSearchQuery("")
                        }
                    )
                } else {
                    when (selectedRoute) {
                        "home" -> HomeScreen(
                            chatsViewModel = chatsViewModel,
                            selectedConversations = selectedConversations,
                            onSelectionChange = { newSelection -> selectedConversations = newSelection }
                        )
                        "about" -> AboutScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectionCount: Int,
    onClearSelection: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectionCount selecionada(s)", fontWeight = FontWeight.Normal) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Limpar seleção")
            }
        },
        actions = {
            IconButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = "Fixar")
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Arquivar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchRequested: () -> Unit,
    onSearchCancelled: () -> Unit,
    onMenuClicked: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                            fadeOut(animationSpec = tween(90))
                }, label = "TitleSearchAnimation"
            ) { isSearching ->
                if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { StyledText("Pesquisar...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                    )
                } else {
                    StyledText(title, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = onSearchCancelled) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar Pesquisa")
                }
            } else {
                IconButton(onClick = onMenuClicked) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        },
        actions = {
            if (isSearchActive && searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar Pesquisa")
                }
            }
            else if (!isSearchActive) {
                 IconButton(onClick = onSearchRequested) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                }
            }
        }
    )
}

// REMOVIDO: @OptIn(ExperimentalAnimationApi::class) @Composable fun DrawerHeader(...)

@Composable
fun HomeScreen(
    chatsViewModel: ChatsViewModel = viewModel(),
    selectedConversations: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    val conversations by chatsViewModel.getConversations().observeAsState()
    val unreadCount by chatsViewModel.getUnreadConversationsCount().observeAsState(initial = 0)
    val isLoading by chatsViewModel.isLoadingInitial.observeAsState(initial = true)

    var selectedFilter by remember { mutableStateOf(ChatsViewModel.ConversationFilter.ALL) }

    LaunchedEffect(Unit) {
        chatsViewModel.triggerInitialLoad()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = selectedConversations.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FilterChips(
                selectedFilter = selectedFilter,
                unreadCount = unreadCount ?: 0,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    chatsViewModel.setFilter(filter)
                }
            )
        }

        val showSkeleton = isLoading && conversations.isNullOrEmpty()
        val showEmptyState = !isLoading && conversations.isNullOrEmpty()

        when {
            showSkeleton -> SkeletonConversationList()
            showEmptyState -> {
                when (selectedFilter) {
                    ChatsViewModel.ConversationFilter.ALL -> EmptyState(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Nenhuma conversa",
                        description = "Parece que você ainda não tem conversas. Que tal iniciar uma?",
                        showButton = true
                    )
                    ChatsViewModel.ConversationFilter.UNREAD -> EmptyState(
                        icon = Icons.Default.MarkChatRead,
                        title = "Tudo em dia!",
                        description = "Você não possui nenhuma mensagem não lida no momento."
                    )
                    ChatsViewModel.ConversationFilter.GROUPS -> EmptyState(
                        icon = Icons.Default.Groups,
                        title = "Nenhum grupo",
                        description = "Você ainda não faz parte de nenhum grupo. Crie um ou peça para ser adicionado!"
                    )
                }
            }
            else -> {
                ConversationList(
                    conversations = conversations.orEmpty(),
                    isLoadingMore = false,
                    onLoadMore = { },
                    selectedConversations = selectedConversations,
                    onSelectionChange = onSelectionChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: ChatsViewModel.ConversationFilter,
    unreadCount: Int,
    onFilterSelected: (ChatsViewModel.ConversationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == ChatsViewModel.ConversationFilter.ALL,
            onClick = { onFilterSelected(ChatsViewModel.ConversationFilter.ALL) },
            label = { StyledText("Todas") },
            shape = CircleShape
        )

        val unreadLabel = if (unreadCount > 0) "Não lidas $unreadCount" else "Não lidas"
        FilterChip(
            selected = selectedFilter == ChatsViewModel.ConversationFilter.UNREAD,
            onClick = { onFilterSelected(ChatsViewModel.ConversationFilter.UNREAD) },
            label = { StyledText(unreadLabel) },
            shape = CircleShape
        )

        FilterChip(
            selected = selectedFilter == ChatsViewModel.ConversationFilter.GROUPS,
            onClick = { onFilterSelected(ChatsViewModel.ConversationFilter.GROUPS) },
            label = { StyledText("Grupos") },
            shape = CircleShape
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationList(
    conversations: List<Conversation>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    selectedConversations: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = conversations,
            key = { it.id },
            contentType = { "conversation" }
        ) { conversation ->
            val isSelected = selectedConversations.contains(conversation.id)

            ConversationItem(
                conversation = conversation,
                isSelected = isSelected,
                onItemClick = {
                    if (selectedConversations.isNotEmpty()) {
                        if (isSelected) {
                            onSelectionChange(selectedConversations - conversation.id)
                        } else {
                            onSelectionChange(selectedConversations + conversation.id)
                        }
                    } else {
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("conversation_id", conversation.id)
                        }
                        context.startActivity(intent)
                    }
                },
                onItemLongClick = {
                    if (isSelected) {
                        onSelectionChange(selectedConversations - conversation.id)
                    } else {
                        onSelectionChange(selectedConversations + conversation.id)
                    }
                }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    val isScrolledToEnd = !listState.canScrollForward
    LaunchedEffect(isScrolledToEnd) {
        if (isScrolledToEnd && !isLoadingMore) {
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    val isUnread = conversation.unreadCount > 0

    val parsedMessage = remember(conversation.lastMessage) {
        val lastMessage = conversation.lastMessage ?: ""
        when {
            lastMessage.startsWith("[IMAGE]") -> ParsedMessageType.Media(lastMessage.removePrefix("[IMAGE]"), "📷 Imagem")
            lastMessage.startsWith("[VIDEO]") -> ParsedMessageType.Media(lastMessage.removePrefix("[VIDEO]"), "🎥 Vídeo")
            lastMessage.startsWith("[AUDIO]") -> ParsedMessageType.Media(lastMessage.removePrefix("[AUDIO]"), "🎵 Áudio")
            lastMessage.startsWith("[FILE]") -> ParsedMessageType.Media(lastMessage.removePrefix("[FILE]"), "📄 Arquivo")
            else -> ParsedMessageType.Text
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarInitial(
                name = conversation.displayName ?: "?",
                size = 56.dp,
                localImagePath = conversation.profilePhoto
            )

            if (conversation.type == ConversationType.USER && conversation.lastOnline != null) {
                val isOnline = "Online".equals(conversation.lastOnline, ignoreCase = true)
                 val indicatorColor = if (isOnline) Color(0xFF4CAF50) else Color.Gray

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconVector: ImageVector? = when (conversation.type) {
                    ConversationType.GROUP -> Icons.Default.Groups
                    ConversationType.BOT -> Icons.Default.SmartToy
                    else -> null
                }
                iconVector?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = conversation.type.name,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                StyledText(
                    text = conversation.displayName ?: "Usuário",
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.defaultMinSize(minHeight = 20.dp)
            ) {
                when (parsedMessage) {
                    is ParsedMessageType.Media -> {
                         if (parsedMessage.path.isNotEmpty() && (conversation.lastMessage?.startsWith("[IMAGE]") == true || conversation.lastMessage?.startsWith("[VIDEO]") == true)) {
                            AsyncImage(
                                model = File(parsedMessage.path),
                                contentDescription = "Prévia da última mensagem",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                             val mediaIcon = when (conversation.lastMessage?.substringBefore("]")?.removePrefix("[")) {
                                 "AUDIO" -> Icons.Default.Audiotrack
                                 "FILE" -> Icons.Default.Description
                                 else -> Icons.Default.Attachment
                             }
                             Icon(
                                 imageVector = mediaIcon,
                                 contentDescription = parsedMessage.description,
                                 modifier = Modifier.size(16.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                             Spacer(modifier = Modifier.width(6.dp))
                        }
                        StyledText(
                            text = parsedMessage.description,
                            contentType = TextContentType.LAST_MESSAGE_PREVIEW,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is ParsedMessageType.Text -> {
                        StyledText(
                            text = conversation.lastMessage ?: "",
                            contentType = TextContentType.LAST_MESSAGE_PREVIEW,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StyledText(
                text = TimeFormat.getFormattedTimestamp(conversation.messageDateTimestamp),
                fontSize = 12.sp,
                color = if (isUnread) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (isUnread && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    StyledText(
                        text = conversation.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isSelected) {
                 Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         Icons.Default.Check,
                         contentDescription = "Selecionada",
                         tint = MaterialTheme.colorScheme.onPrimary,
                         modifier = Modifier.size(16.dp)
                     )
                 }
            }
            else {
                 Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SkeletonConversationList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(10) {
            SkeletonConversationItem()
        }
    }
}

@Composable
fun SkeletonConversationItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.6f)
                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .fillMaxWidth(0.9f)
                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    showButton: Boolean = false
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { startAnimation = true }

    AnimatedVisibility(
        visible = startAnimation,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(500)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                StyledText(
                    text = title,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                StyledText(
                    text = description,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    maxLines = 3
                )
                if (showButton) {
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = {
                        Toast.makeText(context, "Abrindo tela de contatos...", Toast.LENGTH_SHORT).show()
                    }) {
                        StyledText(
                            text = "INICIAR NOVA CONVERSA",
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}