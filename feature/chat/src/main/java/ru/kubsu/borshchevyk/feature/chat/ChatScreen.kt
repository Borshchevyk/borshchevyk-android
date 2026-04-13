package ru.kubsu.borshchevyk.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Chat",
                        style = BorshchevykTheme.typography.titleMedium,
                        color = BorshchevykTheme.colors.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = BorshchevykTheme.colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BorshchevykTheme.colors.background
                )
            )
        },
        bottomBar = {
            MessageInput(onSendMessage = viewModel::onSendMessage)
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        ChatScreen(
            uiState = uiState,
            onPinToggle = { msg ->
                if (msg.isPinned) viewModel.onUnpinMessage(msg.id)
                else viewModel.onPinMessage(msg.id)
            },
            onReactionToggle = { msgId, reaction ->
                viewModel.onToggleReaction(msgId, reaction)
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun ChatScreen(
    uiState: ChatUiState,
    onPinToggle: (Message) -> Unit,
    onReactionToggle: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.pinnedMessages.isNotEmpty()) {
            PinnedMessagesBanner(
                messages = uiState.pinnedMessages,
                onUnpinClick = { msg -> onPinToggle(msg) }
            )
        }
        
        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.authorId == uiState.currentUserId,
                        currentUserId = uiState.currentUserId,
                        onPinToggle = { onPinToggle(message) },
                        onReactionToggle = { reaction -> onReactionToggle(message.id, reaction) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun PinnedMessagesBanner(
    messages: List<Message>,
    onUnpinClick: (Message) -> Unit
) {
    val message = messages.lastOrNull() ?: return
    
    Surface(
        color = BorshchevykTheme.colors.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Pinned",
                tint = BorshchevykTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pinned Message",
                    style = BorshchevykTheme.typography.labelSmall,
                    color = BorshchevykTheme.colors.primary
                )
                Text(
                    text = message.text,
                    style = BorshchevykTheme.typography.bodyMedium,
                    color = BorshchevykTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    currentUserId: String,
    onPinToggle: () -> Unit,
    onReactionToggle: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 20.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                color = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant,
                shape = bubbleShape,
                shadowElevation = 1.dp,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        style = BorshchevykTheme.typography.bodyLarge,
                        color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurface
                    )
                }
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = BorshchevykTheme.colors.surface
            ) {
                DropdownMenuItem(
                    text = { Text(if (message.isPinned) "Unpin Message" else "Pin Message") },
                    onClick = {
                        showMenu = false
                        onPinToggle()
                    }
                )
                HorizontalDivider()
                val reactions = listOf("👍", "❤️", "😂", "😢", "🔥")
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable {
                                    showMenu = false
                                    onReactionToggle(emoji)
                                }
                                .padding(8.dp),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
        
        // Display Reactions
        if (message.reactions.isNotEmpty()) {
            val reactionCounts = message.reactions.groupBy { it.reaction }.mapValues { it.value.size }
            Row(
                modifier = Modifier.padding(top = 4.dp, start = if (isFromMe) 0.dp else 8.dp, end = if (isFromMe) 8.dp else 0.dp),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    val iReacted = message.reactions.any { it.reaction == emoji && it.userId == currentUserId }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (iReacted) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.surfaceVariant,
                        border = if (iReacted) androidx.compose.foundation.BorderStroke(1.dp, BorshchevykTheme.colors.primary) else null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onReactionToggle(emoji) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = count.toString(),
                                style = BorshchevykTheme.typography.labelSmall,
                                color = BorshchevykTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MessageInput(
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = BorshchevykTheme.colors.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { 
                    Text(
                        "Type a message...",
                        color = BorshchevykTheme.colors.onSurfaceVariant
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorshchevykTheme.colors.primary,
                    unfocusedBorderColor = BorshchevykTheme.colors.outline,
                    focusedContainerColor = BorshchevykTheme.colors.background,
                    unfocusedContainerColor = BorshchevykTheme.colors.background,
                    focusedTextColor = BorshchevykTheme.colors.onSurface,
                    unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                    cursorColor = BorshchevykTheme.colors.primary
                ),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant)
                    .clickable(enabled = text.isNotBlank()) {
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            text = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, 
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
