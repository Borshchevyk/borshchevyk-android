package ru.kubsu.borshchevyk.feature.chat.conversation.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.User

@Immutable
data class MessageUiModel(
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
    val reactions: PersistentList<MessageReaction>,
    val commentsCount: Int = 0,
    val parentMessageId: String? = null,
    val forwardedFromChatId: String? = null,
    val forwardedFromUserId: String? = null,
    val forwardedFromUser: User? = null,
    val attachments: PersistentList<Attachment>
)

fun Message.toUiModel(): MessageUiModel = MessageUiModel(
    id = id,
    chatId = chatId,
    authorId = authorId,
    author = author,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = isPinned,
    reactions = reactions.toPersistentList(),
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChatId,
    forwardedFromUserId = forwardedFromUserId,
    forwardedFromUser = forwardedFromUser,
    attachments = attachments.toPersistentList()
)

fun MessageUiModel.toDomain(): Message = Message(
    id = id,
    chatId = chatId,
    authorId = authorId,
    author = author,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = isPinned,
    reactions = reactions,
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChatId,
    forwardedFromUserId = forwardedFromUserId,
    forwardedFromUser = forwardedFromUser,
    attachments = attachments
)
