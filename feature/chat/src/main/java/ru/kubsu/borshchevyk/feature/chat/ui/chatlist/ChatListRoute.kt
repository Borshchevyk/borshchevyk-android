package ru.kubsu.borshchevyk.feature.chat.ui.chatlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatListViewModel
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components.CreateGroupChatDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components.JoinChatDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListRoute(
    forwardPayloadJson: String? = null,
    onCancelForward: () -> Unit = {},
    onChatClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                if (forwardPayloadJson != null) {
                    androidx.compose.material3.Surface(
                        color = BorshchevykTheme.colors.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select chat to forward message",
                                style = BorshchevykTheme.typography.bodyMedium,
                                color = BorshchevykTheme.colors.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onCancelForward) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel forwarding", tint = BorshchevykTheme.colors.onPrimaryContainer)
                            }
                        }
                    }
                }
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
                        IconButton(onClick = { showJoinDialog = true }) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Join by Link",
                                tint = BorshchevykTheme.colors.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BorshchevykTheme.colors.background,
                        titleContentColor = BorshchevykTheme.colors.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                containerColor = BorshchevykTheme.colors.primary,
                contentColor = BorshchevykTheme.colors.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "New Group")
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

        if (showCreateGroupDialog) {
            CreateGroupChatDialog(
                onDismiss = { showCreateGroupDialog = false },
                onCreate = { title, desc ->
                    showCreateGroupDialog = false
                    viewModel.onCreateGroupChat(title, desc) { newChatId ->
                        onChatClick(newChatId)
                    }
                }
            )
        }

        if (showJoinDialog) {
            JoinChatDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { link ->
                    showJoinDialog = false
                    viewModel.onJoinChat(link) { newChatId ->
                        onChatClick(newChatId)
                    }
                }
            )
        }
    }
}
