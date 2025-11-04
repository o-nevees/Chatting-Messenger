package com.chatting.ui.activitys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.chatting.ui.ActiveChatManager
import com.chatting.ui.R
import com.chatting.ui.components.AvatarInitial
import com.chatting.ui.components.MessageInputBar
import com.chatting.ui.components.StyledText
import com.chatting.ui.components.TextContentType
import com.data.source.local.db.entities.MessageEntity
import com.chatting.ui.model.Conversation
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.utils.FileUtils
import com.chatting.ui.utils.PreferencesManager
import com.chatting.ui.utils.mapFontSizeToSp
import com.chatting.ui.viewmodel.ChatsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import com.data.source.local.db.entities.UserEntity
import java.util.Locale
import com.chatting.ui.viewmodel.ChatInputState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce

sealed interface ChatItem {
    data class Message(val message: MessageEntity) : ChatItem
    data class DateHeader(val date: String) : ChatItem
}

/**
 * Activity principal para exibir a tela de chat.
 */
class ChatActivity : ComponentActivity() {

    private lateinit var chatsViewModel: ChatsViewModel
    private lateinit var currentConversationId: String

    private val imageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            chatsViewModel.sendFileMessage(currentConversationId, it)
        }
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            chatsViewModel.sendFileMessage(currentConversationId, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatsViewModel = ViewModelProvider(this)[ChatsViewModel::class.java]
        currentConversationId = intent.getStringExtra("conversation_id") ?: ""

        if (currentConversationId.isEmpty()) {
            finish()
            return
        }

        setContent {
            MyComposeApplicationTheme {
                ChatScreen(
                    viewModel = chatsViewModel,
                    conversationId = currentConversationId,
                    onBackPressed = { finish() },
                    onImageAttachment = { imageLauncher.launch("image/*") },
                    onFileAttachment = { fileLauncher.launch("*/*") }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::currentConversationId.isInitialized) {
            ActiveChatManager.setActiveChat(currentConversationId)
            chatsViewModel.markMessagesAsRead(currentConversationId)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::currentConversationId.isInitialized && ActiveChatManager.activeChatId.value == currentConversationId) {
            ActiveChatManager.setActiveChat(null)
        }
    }
}

/**
 * Converte um AnnotatedString (com SpanStyles) em uma string formatada em Markdown
 * para ser enviada ao ViewModel e salva no banco de dados.
 */
private fun convertAnnotatedToMarkdown(annotatedString: AnnotatedString): String {
    val text = annotatedString.text
    val spans = annotatedString.spanStyles
    if (spans.isEmpty()) return text

    val tags = mutableListOf<Pair<Int, String>>()

    spans.forEach { range ->
        val start = range.start
        val end = range.end
        val style = range.item

        when {
            style.fontWeight == FontWeight.Bold -> {
                tags.add(start to "*")
                tags.add(end to "*")
            }
            style.fontStyle == FontStyle.Italic -> {
                tags.add(start to "_")
                tags.add(end to "_")
            }
            style.textDecoration == TextDecoration.LineThrough -> {
                tags.add(start to "~")
                tags.add(end to "~")
            }
            style.fontFamily == FontFamily.Monospace -> {
                tags.add(start to "```")
                tags.add(end to "```")
            }
        }
    }

    if (tags.isEmpty()) return text

    tags.sortByDescending { it.first }

    val sb = StringBuilder(text)
    tags.forEach { (index, tag) ->
        sb.insert(index, tag)
    }

    return sb.toString()
}


/**
 * Composable principal que constrói a UI da tela de chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatsViewModel,
    conversationId: String,
    onBackPressed: () -> Unit,
    onImageAttachment: () -> Unit,
    onFileAttachment: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    val userInfoMap by viewModel.userInfoMap.observeAsState(initial = emptyMap())
    val conversationDetails by viewModel.getConversationDetails(conversationId).observeAsState()
    val messages by (viewModel.getProcessedMessages(conversationId) ?: MutableLiveData(emptyList<ChatItem>())).observeAsState(initial = emptyList())
    val isLoadingMore by viewModel.isLoadingMore().observeAsState(initial = false)
    val inputState by viewModel.getChatInputState(conversationId).observeAsState(ChatInputState.Disabled)

    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedMessages by remember { mutableStateOf<Set<MessageEntity>>(emptySet()) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onGalleryClick = {
                showAttachmentSheet = false
                onImageAttachment()
            },
            onFileClick = {
                showAttachmentSheet = false
                onFileAttachment()
            }
        )
    }

    val onFileClick = { message: MessageEntity ->
        val localPath = message.localPath
        if (!localPath.isNullOrEmpty() && File(localPath).exists()) {
            try {
                val file = File(localPath)
                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, context.contentResolver.getType(fileUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.chat_error_open_file), Toast.LENGTH_SHORT).show()
            }
        }
        else if (!message.fileUrl.isNullOrEmpty()) {
            if(message.downloadStatus != "baixando") {
                viewModel.downloadFile(message)
                Toast.makeText(context, context.getString(R.string.chat_download_started), Toast.LENGTH_SHORT).show()
            }
        }
        else {
            Toast.makeText(context, context.getString(R.string.chat_error_file_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (selectedMessages.isNotEmpty()) {
                SelectionTopBar(
                    selectionCount = selectedMessages.size,
                    onClearSelection = { selectedMessages = emptySet() },
                    onCopy = {
                        val textToCopy = selectedMessages.sortedBy { it.timestamp }.joinToString("\n") { it.text ?: "" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("messages", textToCopy))
                        val copiedMessage = context.getString(R.string.chat_messages_copied, selectedMessages.size)
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        selectedMessages = emptySet()
                    },
                    onDelete = {
                        selectedMessages.forEach { viewModel.deleteMessage(it) }
                        selectedMessages = emptySet()
                    }
                )
            }
            else {
                ChatTopBar(
                    conversation = conversationDetails,
                    onBackPressed = onBackPressed,
                    onProfileClick = {
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            ChatBackground(wallpaperValue = preferencesManager.getChatWallpaper())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MessageList(
                    messages = messages,
                    userInfoMap = userInfoMap,
                    selectedMessages = selectedMessages,
                    fontSize = mapFontSizeToSp(preferencesManager.getMessageFontSize()),
                    bubbleColor = Color(preferencesManager.getMessageBubbleColor()),
                    onMessageSelected = { message ->
                        selectedMessages = if (selectedMessages.contains(message)) {
                            selectedMessages - message
                        } else {
                            selectedMessages + message
                        }
                    },
                    onFileClick = onFileClick,
                    modifier = Modifier.weight(1f),
                    viewModel = viewModel,
                    conversationId = conversationId,
                    isLoadingMore = isLoadingMore
                )

                when (val currentInputState = inputState) {
                    is ChatInputState.Enabled -> {
                        MessageInputBar(
                            value = textValue,
                            onValueChange = { textValue = it },
                            onSendClick = {
                                val textToSend = convertAnnotatedToMarkdown(textValue.annotatedString)
                                if (textToSend.isNotBlank()) {
                                    viewModel.sendTextMessage(conversationId, textToSend.trim())
                                    textValue = TextFieldValue("")
                                }
                            },
                            onAttachmentClick = { showAttachmentSheet = true }
                        )
                    }
                    is ChatInputState.ReadOnly -> {
                        BotReadOnlyMessage(message = currentInputState.message)
                    }
                    is ChatInputState.Disabled -> {
                    }
                }
            }
        }
    }
}

/**
 * Exibe uma barra inferior fixa quando o chat é somente leitura.
 */
@Composable
fun BotReadOnlyMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Exibe o BottomSheet para seleção de anexos (Galeria, Arquivo, Contato).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onFileClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Anexar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(icon = Icons.Default.Image, label = "Galeria", color = Color(0xFFAC43C8), onClick = onGalleryClick)
                AttachmentOption(icon = Icons.Default.Description, label = "Arquivo", color = Color(0xFF5E53C2), onClick = onFileClick)
                AttachmentOption(icon = Icons.Default.Contacts, label = "Contato", color = Color(0xFF00897B), onClick = {})
            }
        }
    }
}

/**
 * Item individual de opção de anexo (ícone circular e rótulo).
 */
@Composable
fun AttachmentOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}


/**
 * Aplica o papel de parede (imagem, gradiente ou cor) ao fundo do chat.
 */
@Composable
private fun ChatBackground(wallpaperValue: String) {
    if (wallpaperValue.isEmpty()) return

    when {
        wallpaperValue.startsWith("content://") -> {
            AsyncImage(
                model = wallpaperValue.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        wallpaperValue.startsWith("gradient_") -> {
            val brush = remember(wallpaperValue) {
                when (wallpaperValue) {
                    "gradient_1" -> Brush.linearGradient(listOf(Color(0xFFF9A825), Color(0xFFF4511E)))
                    "gradient_2" -> Brush.linearGradient(listOf(Color(0xFF007991), Color(0xFF78ffd6)))
                    "gradient_3" -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
                    else -> SolidColor(Color.Transparent)
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(brush))
        }
        else -> {
            val color = remember(wallpaperValue) {
                try { Color(android.graphics.Color.parseColor(wallpaperValue)) } catch (e: Exception) { Color.Transparent }
            }
            Box(modifier = Modifier.fillMaxSize().background(color))
        }
    }
}

/**
 * Barra superior padrão da tela de chat (com nome e avatar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(conversation: Conversation?, onBackPressed: () -> Unit, onProfileClick: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onProfileClick)
            ) {
                AvatarInitial(
                    name = conversation?.displayName ?: "?",
                    size = 40.dp,
                    localImagePath = conversation?.profilePhoto
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = conversation?.displayName ?: "Carregando...", fontWeight = FontWeight.Bold)
                    conversation?.lastOnline?.let {
                        if (it.isNotEmpty()) {
                             Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        }
    )
}

/**
 * Barra superior alternativa exibida durante o modo de seleção de mensagens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(selectionCount: Int, onClearSelection: () -> Unit, onCopy: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text("$selectionCount selecionada(s)") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Limpar seleção")
            }
        },
        actions = {
            IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copiar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Deletar") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    )
}

/**
 * Exibe a lista rolável de mensagens e cabeçalhos de data.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.MessageList(
    messages: List<ChatItem>,
    userInfoMap: Map<String, UserEntity>,
    selectedMessages: Set<MessageEntity>,
    fontSize: Dp,
    bubbleColor: Color,
    onMessageSelected: (MessageEntity) -> Unit,
    onFileClick: (MessageEntity) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel,
    conversationId: String,
    isLoadingMore: Boolean
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isScrolledToEnd by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 5
            }
        }
    }

    LaunchedEffect(messages) {
        if (isScrolledToEnd && messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        reverseLayout = false
    ) {
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        items(
            items = messages,
            key = { item ->
                if (item is ChatItem.Message) item.message.id else item.hashCode()
            }
        ) { item ->
            when (item) {
                is ChatItem.Message -> {
                    val message = item.message
                    val isSelected = selectedMessages.contains(message)

                    val simpleClickHandler = {
                        if (selectedMessages.isNotEmpty()) {
                            onMessageSelected(message)
                        } else if (message.type != "text") {
                            onFileClick(message)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = simpleClickHandler,
                                onLongClick = { onMessageSelected(message) }
                            )
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 8.dp)
                    ) {
                        MessageItem(
                            message = message,
                            senderName = if (!message.isMine && message.groupId != null) userInfoMap[message.senderId]?.username1 else null,
                            fontSize = fontSize,
                            bubbleColor = bubbleColor,
                            onFileClick = onFileClick
                        )
                    }
                }
                is ChatItem.DateHeader -> {
                    DateHeaderItem(text = item.date)
                }
            }
        }
    }
}


/**
 * Exibe um cabeçalho de data (ex: "Hoje", "Ontem").
 */
@Composable
fun DateHeaderItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = text.uppercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Exibe um único balão de mensagem (enviado ou recebido).
 */
@Composable
fun MessageItem(
    message: MessageEntity,
    senderName: String?,
    fontSize: Dp,
    bubbleColor: Color,
    modifier: Modifier = Modifier,
    onFileClick: (MessageEntity) -> Unit
) {
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isMine) bubbleColor else MaterialTheme.colorScheme.surfaceVariant
    val bubbleShape = if (message.isMine) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
                      else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = modifier.widthIn(max = 280.dp),
            shadowElevation = 1.dp
        ) {
            Column {
                senderName?.let {
                    Text(
                        it,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }

                when (message.type) {
                    "image" -> ImageMessageContent(message, onFileClick, fontSize)
                    "video", "audio", "file", "document", "archive" -> FileMessageContent(message, onFileClick)
                    else -> TextMessageContent(message, fontSize)
                }
            }
        }
    }
}


/**
 * Renderiza o conteúdo de uma mensagem de texto.
 */
@Composable
fun TextMessageContent(message: MessageEntity, fontSize: Dp) {
    val textColor = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)) {
        StyledText(
            text = message.text ?: "",
            contentType = TextContentType.CHAT_BUBBLE_MESSAGE,
            color = textColor,
            fontSize = fontSize.value.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatMessageTime(message.timestamp), fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
            if (message.isMine) {
                Spacer(Modifier.width(4.dp))
                MessageStatusIcon(status = message.status, isMine = message.isMine)
            }
        }
    }
}

/**
 * Renderiza o conteúdo de uma mensagem de imagem, incluindo overlays de status.
 */
@Composable
fun ImageMessageContent(message: MessageEntity, onImageClick: (MessageEntity) -> Unit, fontSize: Dp) {
    val imageModel = remember(message.localPath, message.downloadStatus) {
        message.localPath?.let { File(it) } ?: message.fileUrl
    }
    val textColor = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(message) }
                .widthIn(max = 260.dp)
                .wrapContentHeight()
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = message.text ?: "Imagem",
                placeholder = rememberVectorPainter(Icons.Default.Image),
                error = rememberVectorPainter(Icons.Default.BrokenImage),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 350.dp),
                contentScale = ContentScale.Fit
            )

            val isUploading = message.uploadProgress in 1..99
            val isDownloading = message.downloadStatus == "baixando"
            val downloadFailed = message.downloadStatus == "falhou"
            val needsDownload = message.localPath.isNullOrEmpty() && message.downloadStatus != "concluido"

            if (isUploading || isDownloading || downloadFailed || needsDownload) {
                 Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(progress = { message.uploadProgress / 100f }, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${message.uploadProgress}%", color = Color.White, fontSize = 12.sp)
                        }
                        isDownloading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            if (message.downloadProgress > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${message.downloadProgress}%", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        downloadFailed -> Icon(Icons.Default.ErrorOutline, contentDescription = "Falha no download", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        needsDownload -> Icon(Icons.Default.Download, contentDescription = "Baixar", modifier = Modifier.size(48.dp), tint = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(topStart = 8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatMessageTime(message.timestamp), fontSize = 11.sp, color = Color.White)
                if (message.isMine) {
                    Spacer(Modifier.width(4.dp))
                    MessageStatusIcon(status = message.status, isMine = message.isMine)
                }
            }
        }

        if (!message.text.isNullOrBlank()) {
            StyledText(
                text = message.text!!,
                contentType = TextContentType.CHAT_BUBBLE_MESSAGE,
                color = textColor,
                fontSize = fontSize.value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 12.dp, end = 12.dp, bottom = 4.dp)
            )
        } else {
             Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


/**
 * Renderiza o conteúdo de uma mensagem de arquivo genérico.
 */
@Composable
fun FileMessageContent(message: MessageEntity, onFileClick: (MessageEntity) -> Unit) {
    val context = LocalContext.current
    val textColor = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val fileExists = remember(message.localPath, message.downloadStatus) {
        FileUtils.fileExists(context, message.localPath)
    }

    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onFileClick(message) }
                .padding(bottom = 4.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                val isUploading = message.uploadProgress in 1..99
                val isDownloading = message.downloadStatus == "baixando"
                val downloadFailed = message.downloadStatus == "falhou"

                when {
                    isUploading -> CircularProgressIndicator(progress = { message.uploadProgress / 100f })
                    isDownloading -> CircularProgressIndicator()
                    downloadFailed -> Icon(Icons.Default.ErrorOutline, "Falha no download", tint = MaterialTheme.colorScheme.error)
                    fileExists -> Icon(Icons.Default.InsertDriveFile, "Arquivo", tint = textColor)
                    else -> Icon(Icons.Default.Download, "Baixar arquivo", tint = textColor)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    message.text ?: "Arquivo",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Text(
                    formatFileSize(message.fileSize ?: 0L),
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatMessageTime(message.timestamp), fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
            if (message.isMine) {
                Spacer(Modifier.width(4.dp))
                MessageStatusIcon(status = message.status, isMine = message.isMine)
            }
        }
    }
}

/**
 * Exibe o ícone de status da mensagem (enviando, entregue, lida, etc.).
 */
@Composable
fun MessageStatusIcon(status: String?, isMine: Boolean) {
    val baseColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (status == "lida") MaterialTheme.colorScheme.tertiary else baseColor.copy(alpha = 0.7f)

    val icon = when (status) {
        "sending" -> Icons.Default.Schedule
        "enviada" -> Icons.Default.Done
        "recebida" -> Icons.Default.DoneAll
        "lida" -> Icons.Default.DoneAll
        "falhou", "failed_edit" -> Icons.Default.ErrorOutline
        else -> null
    }

    icon?.let {
        Icon(
            it,
            contentDescription = null,
            tint = if (status == "falhou" || status == "failed_edit") MaterialTheme.colorScheme.error else iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Formata um timestamp para exibição da hora (HH:mm).
 */
private fun formatMessageTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Formata um tamanho de arquivo em bytes para KB, MB, GB.
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
