package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

/**
 * Represents a discrete message within a Borshchevyk chat conversation.
 *
 * This is the core entity for all communication, capable of representing messages exchanged
 * over standard client-server connections as well as decentralized P2P mesh networks.
 * It tracks its origin via the [source] property, allowing the UI to differentiate between
 * global and local interactions. It logically belongs to a [ChatEntity].
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the [ChatEntity] this message belongs to.
 * @property authorId The ID of the [UserEntity] who sent the message.
 * @property text The textual content of the message.
 * @property createdAt ISO timestamp indicating when the message was initially sent.
 * @property updatedAt ISO timestamp indicating when the message was last edited (null if never edited).
 * @property status The current delivery/read status of the message (e.g., sent, delivered, read).
 * @property isDeleted Flag indicating if the message has been logically deleted by the user.
 * @property source The origin network source of the message (e.g., Global Server vs. P2P Mesh).
 * @property isPinned Flag indicating whether the message is pinned to the top of the chat.
 * @property commentsCount The number of thread comments/replies associated with this message.
 * @property parentMessageId The ID of the parent message, used if this message is a direct reply.
 * @property forwardedFromChatId The ID of the original chat if this message was forwarded.
 * @property forwardedFromUserId The ID of the original sender if this message was forwarded.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
    val updatedAt: String?,
    val status: MessageStatus?,
    val isDeleted: Boolean,
    val source: MessageSource,
    val isPinned: Boolean,
    val commentsCount: Int,
    val parentMessageId: String?,
    val forwardedFromChatId: String?,
    val forwardedFromUserId: String?
)
