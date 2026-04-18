package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val targetUserId: String = "",
    val message: MessageDto? = null
) {
    @Serializable
    data class MessageDto(
        val id: String,
        val chatId: String,
        val authorId: String,
        val text: String,
        val createdAt: String,
        @SerialName("deleted") val isDeleted: Boolean = false,
        val status: String? = null
    )
}

@Serializable
data class TypingEvent(
    val userId: String,
    val isTyping: Boolean
)

@Serializable
data class ReactionEvent(
    val messageId: String,
    val userId: String,
    val reaction: String,
    val isAdded: Boolean
)

@Serializable
data class ReadReceiptEvent(
    val userId: String,
    val messageId: String
)
