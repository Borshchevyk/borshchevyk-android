package ru.kubsu.borshchevyk.core.model.domain

data class DomainTypingEvent(
    val userId: String,
    val isTyping: Boolean
)

data class DomainReactionEvent(
    val messageId: String,
    val userId: String,
    val reaction: String,
    val isAdded: Boolean
)

data class DomainReadReceiptEvent(
    val messageId: String,
    val userId: String
)

data class DomainGlobalChatEvent(
    val chatId: String,
    val action: String
)

data class DomainPresenceStatus(
    val userId: String,
    val isOnline: Boolean,
    val lastSeenAt: Long?
)

sealed interface ChatEvent {
    data class NewMessage(val message: Message) : ChatEvent
    data class MessageDeleted(val messageId: String) : ChatEvent
    data class Typing(val event: DomainTypingEvent) : ChatEvent
    data class ReactionUpdated(val event: DomainReactionEvent) : ChatEvent
    data class MessagePinned(val messageId: String) : ChatEvent
    data class MessageUnpinned(val messageId: String) : ChatEvent
    data class ReadReceipt(val event: DomainReadReceiptEvent) : ChatEvent
}
