package ru.kubsu.borshchevyk.feature.chat.chatlist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListIntent
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListUiState
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components.ChatItem

@Composable
internal fun ChatListScreen(
    uiState: ChatListUiState,
    onChatClick: (String) -> Unit,
    onIntent: (ChatListIntent) -> Unit,
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
                    onClick = { onChatClick(chat.id) },
                    onPinToggle = {
                        if (chat.isPinned) onIntent(ChatListIntent.UnpinChat(chat.id)) else onIntent(ChatListIntent.PinChat(chat.id))
                    }
                )
            }
        }
    }
}
