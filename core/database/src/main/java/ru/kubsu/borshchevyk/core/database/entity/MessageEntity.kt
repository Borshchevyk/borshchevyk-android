package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

/**
 * Represents a message within a chat conversation.
 *
 * This entity belongs to a [ChatEntity] via the `chatId` foreign key.
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the chat this message belongs to.
 * @property authorId The ID of the user who sent the message.
 * @property text The textual content of the message.
 * @property createdAt ISO timestamp when the message was sent.
 * @property updatedAt ISO timestamp when the message was last edited.
 * @property status The delivery/read status of the message.
 * @property isDeleted Whether the message has been deleted.
 * @property source The origin source of the message.
 * @property isPinned Whether the message is pinned in the chat.
 * @property commentsCount The number of thread comments for this message.
 * @property parentMessageId The ID of the original message if this is a reply.
 * @property forwardedFromChatId The ID of the original chat if forwarded.
 * @property forwardedFromUserId The ID of the original sender if forwarded.
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
