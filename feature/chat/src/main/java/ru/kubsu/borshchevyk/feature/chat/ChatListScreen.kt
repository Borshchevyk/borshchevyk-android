package ru.kubsu.borshchevyk.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListRoute(
    onChatClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Chats", 
                        style = BorshchevykTheme.typography.titleLarge,
                        color = BorshchevykTheme.colors.onSurface
                    ) 
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BorshchevykTheme.colors.onSurface
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Profile",
                            tint = BorshchevykTheme.colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BorshchevykTheme.colors.background,
                    titleContentColor = BorshchevykTheme.colors.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateChatDialog = true },
                containerColor = BorshchevykTheme.colors.primary,
                contentColor = BorshchevykTheme.colors.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        ChatListScreen(
            uiState = uiState,
            onChatClick = onChatClick,
            modifier = Modifier.padding(padding)
        )
        
        if (showCreateChatDialog) {
            CreateChatDialog(
                onDismiss = { showCreateChatDialog = false },
                onCreate = { peerId ->
                    showCreateChatDialog = false
                    viewModel.onCreatePrivateChat(peerId) { newChatId ->
                        onChatClick(newChatId)
                    }
                }
            )
        }
    }
}

@Composable
internal fun ChatListScreen(
    uiState: ChatListUiState,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.chats.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.chats, key = { it.id }) { chat ->
                ChatItem(
                    chat = chat,
                    onClick = { onChatClick(chat.id) } 
                )
            }
        }
    }
}

@Composable
internal fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val displayName = chat.title ?: "Private Chat"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BorshchevykTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = BorshchevykTheme.typography.titleMedium,
                color = BorshchevykTheme.colors.primary
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = BorshchevykTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.description ?: "Say hi!",
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var peerId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        titleContentColor = BorshchevykTheme.colors.onSurface,
        textContentColor = BorshchevykTheme.colors.onSurfaceVariant,
        title = { 
            Text(
                "Start new chat",
                style = BorshchevykTheme.typography.titleMedium
            ) 
        },
        text = {
            OutlinedTextField(
                value = peerId,
                onValueChange = { peerId = it },
                label = { Text("User ID") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorshchevykTheme.colors.primary,
                    unfocusedBorderColor = BorshchevykTheme.colors.outline,
                    focusedTextColor = BorshchevykTheme.colors.onSurface,
                    unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                    cursorColor = BorshchevykTheme.colors.primary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(peerId) },
                enabled = peerId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BorshchevykTheme.colors.primary,
                    contentColor = BorshchevykTheme.colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BorshchevykTheme.colors.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
