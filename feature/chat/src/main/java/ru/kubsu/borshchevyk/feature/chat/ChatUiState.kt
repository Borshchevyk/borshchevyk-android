package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.Message

data class ChatContext(
    val chatId: String = "",
    val currentUserId: String = "",
    val isGroupChat: Boolean = false
)

data class MessageFeed(
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val commentsByMessageId: Map<String, List<Message>> = emptyMap(),
    val readersByMessageId: Map<String, List<String>> = emptyMap(),
    val attachmentUrls: Map<String, String> = emptyMap()
)

data class InputState(
    val editingMessage: Message? = null,
    val typingUsers: Set<String> = emptySet(),
    val isSending: Boolean = false
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
        val showSettings: Boolean = false,
        val isChatDeleted: Boolean = false
    ) : ChatUiState
}
