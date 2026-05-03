package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User

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
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val commentsByMessageId: Map<String, List<Message>> = emptyMap(),
    val readersByMessageId: Map<String, List<User>> = emptyMap(),
    val attachmentUrls: Map<String, String> = emptyMap(),
    val thumbnailUrls: Map<String, String> = emptyMap()
)

data class InputState(
    val editingMessage: Message? = null,
    val typingUsers: Set<String> = emptySet(),
    val isSending: Boolean = false,
    val forwardPayload: ForwardPayload? = null
)

sealed interface ChatUiState {
    object Loading : ChatUiState
    
    data class Error(
        val message: String
    ) : ChatUiState
    
    data class Content(
        val context: ChatContext,
        val feed: MessageFeed = MessageFeed(),
        val input: InputState = InputState(),
        val isChatDeleted: Boolean = false
    ) : ChatUiState
}
