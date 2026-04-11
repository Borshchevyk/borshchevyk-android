package ru.kubsu.borshchevyk.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
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
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun ChatScreen(
    uiState: ChatUiState,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.messages.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.authorId == uiState.currentUserId
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun MessageBubble(
    message: Message,
    isFromMe: Boolean
) {
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
        Surface(
            color = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant,
            shape = bubbleShape,
            shadowElevation = 1.dp
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
