package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.User

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val authorId: String,
    val author: User?,
    val text: String,
    val createdAt: String,
    val updatedAt: String?,
    val status: MessageStatus?,
    val isDeleted: Boolean,
    val source: MessageSource,
    val isPinned: Boolean,
    val reactions: List<MessageReaction>,
    val commentsCount: Int,
    val parentMessageId: String?,
    val forwardedFromChatId: String?,
    val forwardedFromUserId: String?,
    val forwardedFromUser: User?,
    val attachments: List<Attachment>
)
