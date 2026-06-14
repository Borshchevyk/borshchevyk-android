package ru.kubsu.borshchevyk.feature.chat.conversation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.CommentsDialog
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.MediaViewerDialog
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.MessageBubble
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.PinnedMessagesBanner
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.ReadersDialog

@Composable
internal fun ChatScreen(
    contentState: ChatUiState.Content,
    onIntent: (ChatIntent) -> Unit,
    onObserveProgress: (String) -> Flow<Float> = { flowOf(0f) },
    modifier: Modifier = Modifier
) {
    var messageIdForReaders by rememberSaveable { mutableStateOf<String?>(null) }
    var messageIdForComments by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAttachment by rememberSaveable { mutableStateOf<Attachment?>(null) }
    val listState = rememberLazyListState()

    val firstMessageId = contentState.feed.messages.firstOrNull()?.id
    LaunchedEffect(firstMessageId) {
        if (contentState.feed.messages.isNotEmpty()) {
            val isFromMe = contentState.feed.messages.first().authorId == contentState.context.currentUserId
            if (isFromMe || listState.firstVisibleItemIndex <= 2) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (contentState.feed.pinnedMessages.isNotEmpty()) {
            PinnedMessagesBanner(
                messages = contentState.feed.pinnedMessages,
                onUnpinClick = { msg -> onIntent(ChatIntent.UnpinMessage(msg.id)) }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = true
        ) {
            items(contentState.feed.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.authorId == contentState.context.currentUserId,
                    currentUserId = contentState.context.currentUserId,
                    attachmentUrls = contentState.feed.attachmentUrls,
                    thumbnailUrls = contentState.feed.thumbnailUrls,
                    onIntent = onIntent,
                    onAttachmentClick = { selectedAttachment = it },
                    onViewReadersRequested = { messageIdForReaders = it },
                    onViewCommentsRequested = { messageIdForComments = it },
                    onObserveProgress = onObserveProgress
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
            onDismiss = { messageIdForComments = null }
        )
    }

    if (selectedAttachment != null) {
        MediaViewerDialog(
            attachment = selectedAttachment!!,
            attachmentUrls = contentState.feed.attachmentUrls,
            thumbnailUrls = contentState.feed.thumbnailUrls,
            onResolveAttachmentUrl = { _, thumb -> onIntent(ChatIntent.ResolveAttachmentUrl(selectedAttachment!!, thumb)) },
            onDismiss = { selectedAttachment = null }
        )
    }
}