package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile

data class FailedMessageData(
    val text: String,
    val attachments: List<AttachmentFile>,
    val forwardPayload: ForwardPayload?
)

data class ChatContext(
    val chatId: String = "",
    val currentUserId: String = "",
    val isGroupChat: Boolean = false,
    val chatName: String = "",
    val chatAvatarUrl: String? = null,
    val isOnline: Boolean? = null,
    val lastSeenAt: Long? = null
)

data class MessageFeed(
    val messages: PersistentList<Message> = persistentListOf(),
    val pinnedMessages: PersistentList<Message> = persistentListOf(),
    val commentsByMessageId: PersistentMap<String, PersistentList<Message>> = persistentMapOf(),
    val readersByMessageId: PersistentMap<String, PersistentList<User>> = persistentMapOf(),
    val attachmentUrls: PersistentMap<String, String> = persistentMapOf(),
    val thumbnailUrls: PersistentMap<String, String> = persistentMapOf()
)

data class InputState(
    val editingMessage: Message? = null,
    val typingUsers: PersistentSet<String> = persistentSetOf(),
    val isSending: Boolean = false,
    val forwardPayload: ForwardPayload? = null,
    val pendingMessagesData: PersistentMap<String, FailedMessageData> = persistentMapOf()
)

sealed interface ChatUiState {
    object Loading : ChatUiState
    
    data class Error(val message: String) : ChatUiState
    
    data class Content(
        val context: ChatContext,
        val feed: MessageFeed = MessageFeed(),
        val input: InputState = InputState(),
        val isChatDeleted: Boolean = false
    ) : ChatUiState
}
