package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A universal wrapper for all events received via the single WebSocket connection.
 */
@Serializable
data class AppEventDto(
    val eventType: String,
    val payload: JsonElement
)

/**
 * A generalized envelope for incoming WebSocket notifications.
 *
 * @property targetUserId The ID of the user this notification is intended for.
 * @property message Payload for incoming message events.
 * @property chatEvent Payload for chat-level events (e.g., creation, updates).
 * @property callEvent Payload for call-related events.
 */
@Serializable
data class NotificationDto(
    val targetUserId: String = "",
    val message: MessageDto? = null,
    val chatEvent: ChatEventDto? = null,
    val callEvent: CallEventDto? = null
) {
    /**
     * Represents an event affecting a chat as a whole.
     *
     * @property chat The short representation of the affected chat.
     * @property action The type of action (e.g., "CREATED", "UPDATED").
     */
    @Serializable
    data class ChatEventDto(
        val chat: ChatResponse,
        val action: String
    )
    
    /**
     * Represents an event within a WebRTC call lifecycle.
     *
     * @property callId The unique identifier of the call session.
     * @property eventType The type of event (e.g., "INITIATED", "ENDED", "ACCEPTED", "REJECTED").
     * @property initiator The user who started the call.
     * @property actor The user who triggered the current event.
     * @property timestamp When the event occurred.
     */
    @Serializable
    data class CallEventDto(
        val callId: String,
        val eventType: String, // e.g. "INITIATED", "ENDED", "ACCEPTED", "REJECTED"
        val initiator: ShortUserDto? = null,
        val actor: ShortUserDto? = null,
        val timestamp: String? = null
    )

    /**
     * Represents a single chat message received via WebSocket.
     *
     * @property id The unique identifier of the message.
     * @property chat The chat this message belongs to.
     * @property author The user who sent the message.
     * @property text The text content of the message.
     * @property createdAt When the message was sent.
     * @property isDeleted Whether the message has been deleted.
     * @property status The current delivery/read status.
     * @property forwardedFromChat If forwarded, the original chat.
     * @property forwardedFromUser If forwarded, the original author.
     * @property attachments List of attached media.
     * @property attachmentIdsOld Legacy list of attachment metadata.
     */
    @Serializable
    data class MessageDto(
        val id: String,
        val chat: ShortChatDto,
        val author: ShortUserDto,
        val text: String? = null,
        val createdAt: String,
        val updatedAt: String? = null,
        @SerialName("deleted") val isDeleted: Boolean = false,
        val status: String? = null,
        val source: String? = null,
        val forwardedFromChat: ShortChatDto? = null,
        val forwardedFromUser: ShortUserDto? = null,
        val attachments: List<MessageAttachmentResponse>? = null,
        @SerialName("attachmentIds") val attachmentIdsOld: List<MessageAttachmentResponse>? = null
    )
}

/**
 * Event indicating that a user has started or stopped typing in a chat.
 *
 * @property chatId The chat where the user is typing.
 * @property user The user whose typing status has changed.
 * @property isTyping True if the user is currently typing, false otherwise.
 */
@Serializable
data class TypingEvent(
    val chatId: String,
    val user: ShortUserDto,
    @SerialName("isTyping") val isTyping: Boolean = false
)

/**
 * Event indicating a reaction added or removed from a message.
 *
 * @property chatId The chat where the reaction happened.
 * @property messageId The unique identifier of the message the reaction is applied to.
 * @property user The user who added or removed the reaction.
 * @property reaction The emoji or string representing the reaction.
 * @property isAdded True if the reaction was added, false if it was removed.
 */
@Serializable
data class ReactionEvent(
    val chatId: String,
    val messageId: String,
    val user: ShortUserDto,
    val reaction: String,
    @SerialName("isAdded") val isAdded: Boolean = true
)

/**
 * Event indicating that a specific message has been read by a user.
 *
 * @property chatId The chat where the message was read.
 * @property user The user who read the message.
 * @property messageId The unique identifier of the message that was read.
 */
@Serializable
data class ReadReceiptEvent(
    val chatId: String,
    val user: ShortUserDto,
    val messageId: String
)

/**
 * Event or response detailing a user's presence status (online/offline).
 *
 * @property userId The unique identifier of the user.
 * @property isOnline True if the user is currently online.
 * @property lastSeenAt The timestamp (in epoch milliseconds) when the user was last seen online.
 */
@Serializable
data class PresenceStatusResponse(
    val userId: String,
    @SerialName("isOnline") val isOnline: Boolean = false,
    val lastSeenAt: Long? = null
)

/**
 * Control message used in the Mesh network to indicate a user is still online.
 */
@Serializable
data class PresencePingDto(
    val userId: String,
    val timestamp: Long
)

/**
 * Event indicating that a message has been edited in the Mesh network.
 * We need both messageId and text because Mesh lacks URL parameters.
 */
@Serializable
data class EditMessageEvent(
    val messageId: String,
    val text: String
)

