package ru.kubsu.borshchevyk.feature.chat.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.CommentsDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.MessageBubble
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.PinnedMessagesBanner
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ReadersDialog

@Composable
internal fun ChatScreen(
    contentState: ChatUiState.Content,
    onResolveAttachmentUrl: (String) -> Unit,
    onPinToggle: (Message) -> Unit,
    onReactionToggle: (String, String) -> Unit,
    onEdit: (Message) -> Unit,
    onDelete: (String, Boolean) -> Unit,
    onMessageVisible: (String) -> Unit,
    onLoadReaders: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onIntent: (ChatIntent   ) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageIdForReaders by remember { mutableStateOf<String?>(null) }
    var messageIdForComments by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (contentState.feed.pinnedMessages.isNotEmpty()) {
            PinnedMessagesBanner(
                messages = contentState.feed.pinnedMessages,
                onUnpinClick = { msg -> onPinToggle(msg) }
            )
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            val otherTypingUsers = contentState.input.typingUsers.filter { it != contentState.context.currentUserId }
            if (otherTypingUsers.isNotEmpty()) {
                item {
                    Text(
                        text = if (otherTypingUsers.size == 1) "User is typing..." else "Multiple users are typing...",
                        style = BorshchevykTheme.typography.labelSmall,
                        color = BorshchevykTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }

            items(contentState.feed.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.authorId == contentState.context.currentUserId,
                    currentUserId = contentState.context.currentUserId,
                    attachmentUrls = contentState.feed.attachmentUrls,
                    resolvedUsers = contentState.feed.resolvedUsers,
                    onResolveAttachmentUrl = onResolveAttachmentUrl,
                    onPinToggle = { onPinToggle(message) },
                    onReactionToggle = { reaction -> onReactionToggle(message.id, reaction) },
                    onEdit = { onEdit(message) },
                    onDelete = { forAll -> onDelete(message.id, forAll) },
                    onMessageVisible = { onMessageVisible(message.id) },
                    onViewReaders = {
                        onLoadReaders(message.id)
                        messageIdForReaders = message.id
                    },
                    onViewComments = {
                        onLoadComments(message.id)
                        messageIdForComments = message.id
                    },
                    onResend = {
                        onIntent(ChatIntent.ResendMessage(message.id))
                    },
                    onForward = {
                        onIntent(ChatIntent.ForwardMessage(message))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (messageIdForReaders != null) {
        ReadersDialog(
            readers = contentState.feed.readersByMessageId[messageIdForReaders],
            onDismiss = { messageIdForReaders = null }
        )
    }

    if (messageIdForComments != null) {
        CommentsDialog(
            comments = contentState.feed.commentsByMessageId[messageIdForComments],
            resolvedUsers = contentState.feed.resolvedUsers,
            onDismiss = { messageIdForComments = null }
        )
    }
}
