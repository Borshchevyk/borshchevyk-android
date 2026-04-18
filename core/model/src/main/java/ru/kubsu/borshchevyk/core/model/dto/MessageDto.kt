package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.MessageSource

@Serializable
data class MessageReactionResponse(
    val userId: String,
    val reaction: String
)

@Serializable
data class MessageResponse(
    val id: String,
    val chatId: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
    @SerialName("deleted") val isDeleted: Boolean = false,
    val source: MessageSource,
    val pinnedAt: String? = null,
    val pinnedBy: String? = null,
    val reactions: List<MessageReactionResponse>? = null,
    val commentsCount: Int = 0,
    val parentMessageId: String? = null,
    val forwardedFromChatId: String? = null,
    val forwardedFromUserId: String? = null
)

@Serializable
data class SendMessageRequest(
    val text: String,
    val source: MessageSource? = null,
    val parentMessageId: String? = null
)

@Serializable
data class EditMessageRequest(
    val text: String
)
