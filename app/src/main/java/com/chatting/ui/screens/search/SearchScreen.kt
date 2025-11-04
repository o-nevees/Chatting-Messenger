package com.chatting.ui.screens.search

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatting.ui.activitys.ChatActivity
import com.chatting.ui.components.AvatarInitial
import com.chatting.ui.model.Conversation
import com.chatting.ui.model.ConversationType
import com.service.api.SearchResult

import com.chatting.ui.utils.TimeFormat // Importado para formatar lastOnline
import com.chatting.ui.viewmodel.ChatsViewModel
import com.chatting.ui.viewmodel.SearchUiState

/**
 * Tela principal para exibição dos resultados de busca local e global.
 * Organiza os resultados em seções distintas.
 */
@Composable
fun SearchScreen(
    searchQuery: String,
    searchUiState: SearchUiState,
    localSearchResults: List<Conversation>,
    recentConversations: List<Conversation>,
    onResultClick: (id: String) -> Unit, // Callback para notificar o fechamento da busca
    chatsViewModel: ChatsViewModel // ViewModel pode ser usado para futuras interações
) {
    val context = LocalContext.current

    // Função interna para navegação, evitando repetição
    fun navigateToChat(conversationId: String) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("conversation_id", conversationId)
        }
        context.startActivity(intent)
        onResultClick(conversationId) // Notifica que um item foi clicado
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp) // Espaço no final da lista
    ) {
        // --- Seção Recentes (Apenas se a busca estiver VAZIA) ---
        if (searchQuery.isBlank() && recentConversations.isNotEmpty()) { //
            item(key = "recent_section") {
                RecentConversationsSection(
                    conversations = recentConversations,
                    onConversationClick = { conversation ->
                        navigateToChat(conversation.id)
                    }
                )
            }
        }

        // --- Seções de Resultados (Apenas se houver busca ATIVA) ---
        if (searchQuery.isNotBlank()) { //

            // --- Seção Contatos (Resultados Locais) ---
            // <<< MODIFICAÇÃO: Mostra APENAS se houver resultados locais >>>
            if (localSearchResults.isNotEmpty()) { //
                item(key = "local_header") {
                    SearchResultSectionHeader("Conversas Locais") // Título mais descritivo
                }
                // Não precisa de EmptyResultMessage aqui, pois a seção só aparece se houver resultados.
                items(localSearchResults, key = { "local_${it.id}" }) { conversation -> //
                    LocalResultItem(
                        conversation = conversation,
                        onClick = { navigateToChat(conversation.id) }
                    )
                }
            }
            // --- Seção Resultados Globais (Resultados da API) ---
            // <<< MODIFICAÇÃO: Mostra APENAS se NÃO houver resultados locais >>>
            else { // Implícito: localSearchResults.isEmpty()
                item(key = "global_divider") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                }
                item(key = "global_header") {
                    SearchResultSectionHeader("Resultados Globais")
                }
                // Lógica interna da seção global (loading, error, results, empty) permanece
                when {
                    searchUiState.isLoading -> { //
                        item(key = "global_loading") { LoadingIndicator() }
                    }
                    searchUiState.error != null -> { //
                        item(key = "global_error") { ErrorResultMessage("Erro na busca: ${searchUiState.error}") }
                    }
                    searchUiState.results.isNullOrEmpty() -> { //
                        item(key = "global_empty") { EmptyResultMessage("Nenhum resultado global encontrado.") }
                    }
                    else -> {
                        items(searchUiState.results, key = { "global_${it.type}_${it.id}" }) { result -> //
                            GlobalResultItem(
                                result = result,
                                onClick = {
                                    // Adapta o ID se for grupo antes de navegar
                                    val conversationId = if (result.type == "group") "group_${result.id}" else result.id //
                                    navigateToChat(conversationId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- O restante das funções Composable (RecentConversationsSection, RecentConversationItem, etc.) ---
// --- permanecem iguais às da resposta anterior. ---


/**
 * Seção horizontal para exibir conversas recentes quando a busca está vazia.
 */
@Composable
fun RecentConversationsSection(
    conversations: List<Conversation>,
    onConversationClick: (Conversation) -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "Recentes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(conversations, key = { "recent_${it.id}" }) { conversation -> //
                RecentConversationItem(conversation, onConversationClick)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp)) // Divisor após a seção
    }
}

/**
 * Item individual para a lista de conversas recentes.
 */
@Composable
fun RecentConversationItem(
    conversation: Conversation,
    onClick: (Conversation) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick(conversation) } //
            .padding(vertical = 4.dp)
    ) {
        AvatarInitial(
            name = conversation.displayName ?: "?", //
            size = 56.dp, //
            localImagePath = conversation.profilePhoto //
        )
        Spacer(modifier = Modifier.height(4.dp)) //
        Text(
            text = conversation.displayName ?: "", //
            fontSize = 12.sp, //
            maxLines = 1, //
            overflow = TextOverflow.Ellipsis, //
            textAlign = TextAlign.Center, //
            color = MaterialTheme.colorScheme.onSurfaceVariant //
        )
    }
}

/**
 * Cabeçalho para as seções de resultados (Local e Global).
 */
@Composable
fun SearchResultSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium, //
        fontWeight = FontWeight.SemiBold, //
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), //
        color = MaterialTheme.colorScheme.onSurfaceVariant //
    )
}

/**
 * Item para exibir um resultado de busca local (conversa existente).
 * Agora exibe o subtítulo formatado corretamente.
 */
@Composable
fun LocalResultItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val subtitle = remember(conversation) { getSubtitleForLocalConversation(conversation) }
    BaseResultItem(
        name = conversation.displayName ?: "Desconhecido", //
        subtitle = subtitle,
        localImagePath = conversation.profilePhoto, //
        onClick = onClick
    )
}

/**
 * Item para exibir um resultado de busca global (da API).
 * Agora exibe o subtítulo vindo da API.
 */
@Composable
fun GlobalResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current //
    val localImagePath = result.photo//

    BaseResultItem(
        name = result.name ?: "Desconhecido", //
        // Usa o subtítulo vindo da API diretamente
        subtitle = result.subtitle, //
        localImagePath = localImagePath,
        onClick = onClick
    )
}


/**
 * Composable base reutilizável para itens de resultado (Local e Global).
 */
@Composable
private fun BaseResultItem(
    name: String,
    subtitle: String?,
    localImagePath: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth() //
            .clickable(onClick = onClick) //
            .padding(horizontal = 16.dp, vertical = 10.dp), //
        verticalAlignment = Alignment.CenterVertically //
    ) {
        AvatarInitial(
            name = name,
            size = 48.dp, //
            localImagePath = localImagePath
        )
        Spacer(modifier = Modifier.width(16.dp)) //
        Column(modifier = Modifier.weight(1f)) { //
            Text(
                text = name,
                fontWeight = FontWeight.Medium, //
                maxLines = 1, //
                overflow = TextOverflow.Ellipsis, //
                fontSize = 16.sp //
            )
            // Exibe o subtítulo se ele não for nulo ou vazio
            if (!subtitle.isNullOrBlank()) { //
                Text(
                    text = subtitle,
                    fontSize = 14.sp, //
                    color = MaterialTheme.colorScheme.onSurfaceVariant, //
                    maxLines = 1, //
                    overflow = TextOverflow.Ellipsis //
                )
            }
        }
    }
}

/**
 * Mensagem exibida quando uma seção de busca não retorna resultados.
 */
@Composable
fun EmptyResultMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth() //
            .padding(horizontal = 16.dp, vertical = 24.dp), //
        verticalAlignment = Alignment.CenterVertically, //
        horizontalArrangement = Arrangement.Center //
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff, //
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, //
            modifier = Modifier.size(20.dp) //
        )
        Spacer(modifier = Modifier.width(8.dp)) //
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant, //
            style = MaterialTheme.typography.bodyMedium //
        )
    }
}

/**
 * Mensagem exibida quando ocorre um erro na busca global.
 */
@Composable
fun ErrorResultMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth() //
            .padding(horizontal = 16.dp, vertical = 24.dp), //
        verticalAlignment = Alignment.CenterVertically, //
        horizontalArrangement = Arrangement.Center //
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber, //
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error, //
            modifier = Modifier.size(20.dp) //
        )
        Spacer(modifier = Modifier.width(8.dp)) //
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error, //
            style = MaterialTheme.typography.bodyMedium //
        )
    }
}

/**
 * Indicador de carregamento para a busca global.
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth() //
            .padding(16.dp), //
        contentAlignment = Alignment.Center //
    ) {
        CircularProgressIndicator() //
    }
}

// --- Funções Auxiliares ---

/**
 * Determina o subtítulo a ser exibido para uma Conversation local,
 * formatando o status online do usuário.
 */
private fun getSubtitleForLocalConversation(conversation: Conversation): String? {
    return when (conversation.type) { //
        ConversationType.USER -> conversation.lastOnline?.let { formatLastOnlineStatus(it) } ?: "Offline" //
        ConversationType.BOT -> conversation.id // Mostra o ID do Bot como subtítulo //
        ConversationType.GROUP -> { //
            // TODO: Buscar a contagem de membros do grupo localmente se necessário
            // Por enquanto, apenas "Grupo"
             "Grupo"
             // Exemplo futuro: "${viewModel.getGroupMemberCount(conversation.id)} membro(s)"
        }
    }
}

/**
 * Formata o status 'lastOnline' (pode ser "Online", timestamp ISO ou null).
 * TODO: Implementar lógica de formatação de data/hora mais amigável ("visto por último...")
 */
private fun formatLastOnlineStatus(lastOnline: String): String {
    return when {
        lastOnline.equals("Online", ignoreCase = true) -> "Online" //
        // Tenta formatar como data/hora, senão retorna o status original ou "Offline"
        else -> {
            // Aqui você pode adicionar a lógica para converter a string ISO 8601
            // em algo como "visto por último às HH:mm" ou "visto ontem", etc.
            // Por simplicidade, retornaremos "Offline" se não for "Online".
            // Use TimeFormat.getFormattedTimestamp se o backend retornar timestamp numérico.
             "Offline" // Simplificado por enquanto
        }
    }
}

/**
 * ATENÇÃO: É necessário atualizar a data class `SearchResult` no arquivo
 * `app/src/main/java/com/chatting/ui/network/ApiModels.kt` para incluir
 * o campo `subtitle`:
 */
/*
// Em ApiModels.kt, atualize SearchResult:
data class SearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("subtitle") val subtitle: String? // <<< ADICIONAR ESTE CAMPO
)
*/