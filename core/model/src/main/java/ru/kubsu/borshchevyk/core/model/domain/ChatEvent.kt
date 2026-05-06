package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing a typing indicator event in a chat.
 *
 * @property userId The ID of the user who started or stopped typing.
 * @property isTyping True if the user is currently typing, false otherwise.
 */
data class DomainTypingEvent(
    val userId: String,
    val isTyping: Boolean
)

/**
 * Domain model representing a reaction added or removed from a message.
 *
 * @property messageId The unique ID of the message receiving the reaction.
 * @property userId The ID of the user who reacted.
 * @property reaction The emoji or string representing the reaction.
 * @property isAdded True if the reaction was added, false if it was removed.
 */
data class DomainReactionEvent(
    val messageId: String,
    val userId: String,
    val reaction: String,
    val isAdded: Boolean
)

/**
 * Domain model representing a read receipt for a specific message.
 *
 * @property messageId The unique ID of the message that was read.
 * @property userId The ID of the user who read the message.
 */
data class DomainReadReceiptEvent(
    val messageId: String,
    val userId: String
)

/**
 * Represents the type of global chat event received.
 */
enum class GlobalChatAction {
    JOINED,
    KICKED,
    LEFT,
    DELETED,
    HISTORY_CLEARED,
    PINNED,
    UNPINNED,
    INFO_UPDATED,
    PERMISSIONS_UPDATED,
    MESSAGE,
    UNKNOWN;

    companion object {
        fun fromString(action: String): GlobalChatAction {
            return entries.find { it.name.equals(action, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Domain model representing a global event affecting a chat (e.g., title changed, avatar updated).
 *
 * @property chat The affected chat domain model.
 * @property action The action performed on the chat.
 */
data class DomainGlobalChatEvent(
    val chat: Chat,
    val action: GlobalChatAction
)

/**
 * Domain model representing the online presence status of a user.
 *
 * @property userId The unique ID of the user.
 * @property isOnline True if the user is currently online, false otherwise.
 * @property lastSeenAt The timestamp (in milliseconds) when the user was last seen.
 */
data class DomainPresenceStatus(
    val userId: String,
    val isOnline: Boolean,
    val lastSeenAt: Long?
)

/**
 * Sealed interface encompassing all specific, strictly-typed events that can occur within a chat session.
 * Used for reactive UI updates and local data synchronization.
 */
sealed interface ChatEvent {
    /** Event emitted when a new message is received or sent. */
    data class NewMessage(val message: Message) : ChatEvent
    /** Event emitted when a message is deleted. */
    data class MessageDeleted(val messageId: String) : ChatEvent
    /** Event emitted when someone changes their typing status. */
    data class Typing(val event: DomainTypingEvent) : ChatEvent
    /** Event emitted when a reaction is updated on a message. */
    data class ReactionUpdated(val event: DomainReactionEvent) : ChatEvent
    /** Event emitted when a message is pinned in the chat. */
    data class MessagePinned(val messageId: String) : ChatEvent
    /** Event emitted when a message is unpinned in the chat. */
    data class MessageUnpinned(val messageId: String) : ChatEvent
    /** Event emitted when a read receipt is received. */
    data class ReadReceipt(val event: DomainReadReceiptEvent) : ChatEvent
    /** Event emitted for global chat operations like deletion. */
    data class GlobalChatEvent(val event: DomainGlobalChatEvent) : ChatEvent
}
