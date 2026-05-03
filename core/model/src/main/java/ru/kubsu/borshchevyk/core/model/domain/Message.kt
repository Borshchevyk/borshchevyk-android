package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Indicates whether a message was sent over the standard server network or a P2P mesh network.
 */
@Serializable
enum class MessageSource {
    /** Message sent via the centralized global server. */
    ONLINE,
    /** Message sent via an offline P2P mesh network. */
    OFFLINE
}

/**
 * Domain model representing a single reaction attached to a message.
 *
 * @property userId The ID of the user who added the reaction.
 * @property reaction The emoji or short string representing the reaction itself.
 */
@Serializable
data class MessageReaction(
    val userId: String,
    val reaction: String
)

/**
 * Domain model representing a chat message.
 *
 * Supports text, attachments, reactions, threads (comments), and forwarding.
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the chat where the message belongs.
 * @property authorId The user ID of the person who authored the message.
 * @property author The populated [User] object of the author, if available.
 * @property text The main text content of the message.
 * @property createdAt ISO 8601 formatted timestamp of when the message was sent.
 * @property updatedAt ISO 8601 formatted timestamp of when the message was last edited.
 * @property status The delivery status of the message (e.g., READ, DELIVERED).
 * @property isDeleted True if the message has been soft-deleted.
 * @property source Indicates if the message came from an ONLINE server or OFFLINE mesh.
 * @property isPinned True if the message is pinned within the chat.
 * @property reactions A list of [MessageReaction] added to this message.
 * @property commentsCount The number of replies/comments in the thread for this message.
 * @property parentMessageId If this is a reply, the ID of the parent message being replied to.
 * @property forwardedFromChatId The ID of the chat this message was originally forwarded from.
 * @property forwardedFromUserId The ID of the user who originally wrote the forwarded message.
 * @property forwardedFromUser The [User] object of the original author, if available.
 * @property attachments A list of [Attachment] included with this message.
 */
@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val authorId: String,
    val author: User? = null,
    val text: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val status: MessageStatus? = null,
    val isDeleted: Boolean = false,
    val source: MessageSource = MessageSource.ONLINE,
    val isPinned: Boolean = false,
    val reactions: List<MessageReaction> = emptyList(),
    val commentsCount: Int = 0,
    val parentMessageId: String? = null,
    val forwardedFromChatId: String? = null,
    val forwardedFromUserId: String? = null,
    val forwardedFromUser: User? = null,
    val attachments: List<Attachment> = emptyList()
)
