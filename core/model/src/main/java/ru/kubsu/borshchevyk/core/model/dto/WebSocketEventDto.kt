package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val targetUserId: String = "",
    val message: MessageDto? = null,
    val chatEvent: ChatEventDto? = null,
    val callEvent: CallEventDto? = null
) {
    @Serializable
    data class ChatEventDto(
        val chat: ShortChatDto,
        val action: String
    )
    
    @Serializable
    data class CallEventDto(
        val callId: String,
        val eventType: String, // e.g. "INITIATED", "ENDED", "ACCEPTED", "REJECTED"
        val initiator: ShortUserDto? = null,
        val actor: ShortUserDto? = null,
        val timestamp: String? = null
    )

    @Serializable
    data class MessageDto(
        val id: String,
        val chat: ShortChatDto,
        val author: ShortUserDto,
        val text: String,
        val createdAt: String,
        @SerialName("deleted") val isDeleted: Boolean = false,
        val status: String? = null,
        val forwardedFromChat: ShortChatDto? = null,
        val forwardedFromUser: ShortUserDto? = null,
        val attachments: List<MessageAttachmentResponse>? = null,
        @SerialName("attachmentIds") val attachmentIdsOld: List<MessageAttachmentResponse>? = null
    )
}

@Serializable
data class TypingEvent(
    val user: ShortUserDto,
    val isTyping: Boolean
)

@Serializable
data class ReactionEvent(
    val messageId: String,
    val user: ShortUserDto,
    val reaction: String,
    val isAdded: Boolean
)

@Serializable
data class ReadReceiptEvent(
    val user: ShortUserDto,
    val messageId: String
)

@Serializable
data class PresenceStatusResponse(
    val userId: String,
    @SerialName("online") val isOnline: Boolean,
    val lastSeenAt: Long? = null
)
