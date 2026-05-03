package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.MessageSource

/**
 * Represents a user's reaction to a specific message.
 *
 * @property userId The unique identifier of the user who added the reaction.
 * @property reaction The emoji or string representation of the reaction.
 */
@Serializable
data class MessageReactionResponse(
    val userId: String,
    val reaction: String
)

/**
 * Represents an attachment (e.g., image, video, file) sent within a message.
 *
 * @property id The unique identifier of the attachment.
 * @property type The type of the attachment (e.g., IMAGE, VIDEO, FILE).
 * @property originalFilename The original name of the file before upload.
 * @property extension The file extension (e.g., "png", "pdf").
 * @property sizeBytes The size of the file in bytes.
 * @property thumbnailKey An optional identifier to retrieve a thumbnail for the attachment.
 * @property updatedAt The timestamp when the attachment was last updated.
 * @property width The width of the visual attachment, if applicable.
 * @property height The height of the visual attachment, if applicable.
 * @property duration The duration of audio or video attachments in seconds.
 */
@Serializable
data class MessageAttachmentResponse(
    val id: String,
    val type: AttachmentType? = null,
    val originalFilename: String? = null,
    val extension: String? = null,
    val sizeBytes: Long? = null,
    @SerialName("thumbnailId") val thumbnailKey: String? = null,
    val updatedAt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)

/**
 * The standard response object containing complete details about a message.
 *
 * @property id The unique identifier of the message.
 * @property chat Summarized details of the chat where the message resides.
 * @property author Summarized details of the user who authored the message.
 * @property text The text content of the message.
 * @property createdAt The timestamp when the message was initially sent.
 * @property updatedAt The timestamp when the message was last edited.
 * @property isDeleted Indicates if the message has been deleted.
 * @property source The origin of the message (e.g., SERVER, P2P).
 * @property status The current state of the message (e.g., SENT, DELIVERED, READ).
 * @property pinnedAt The timestamp when the message was pinned, if it is pinned.
 * @property pinnedBy The ID of the user who pinned the message, if applicable.
 * @property reactions A list of reactions added to this message by users.
 * @property commentsCount The number of replies or comments in the thread of this message.
 * @property parentMessageId The ID of the parent message if this is a reply.
 * @property forwardedFromChat Details of the original chat if this message was forwarded.
 * @property forwardedFromUser Details of the original author if this message was forwarded.
 * @property attachments A list of detailed file attachments included in the message.
 * @property attachmentIdsOld A legacy list of string attachment IDs for backward compatibility.
 */
@Serializable
data class MessageResponse(
    val id: String,
    val chat: ShortChatDto,
    val author: ShortUserDto,
    val text: String,
    val createdAt: String,
    val updatedAt: String? = null,
    @SerialName("deleted") val isDeleted: Boolean = false,
    val source: MessageSource,
    val status: ru.kubsu.borshchevyk.core.model.domain.MessageStatus? = null,
    val pinnedAt: String? = null,
    val pinnedBy: String? = null,
    val reactions: List<MessageReactionResponse>? = null,
    val commentsCount: Int = 0,
    val parentMessageId: String? = null,
    val forwardedFromChat: ShortChatDto? = null,
    val forwardedFromUser: ShortUserDto? = null,
    val attachments: List<MessageAttachmentResponse>? = null,
    @SerialName("attachmentIds") val attachmentIdsOld: List<String>? = null // Support old string format
)

/**
 * Request object used when sending a new message to a chat.
 *
 * @property text The actual content of the message.
 * @property source The origin of the message (e.g., specific mesh network or server).
 * @property parentMessageId Optional ID of a message being replied to.
 * @property attachmentIds Optional list of previously uploaded attachment IDs to link to this message.
 * @property forwardedFromChatId Optional ID of a chat to forward a message from.
 * @property forwardedFromUserId Optional ID of a user whose message is being forwarded.
 */
@Serializable
data class SendMessageRequest(
    val text: String,
    val source: MessageSource? = null,
    val parentMessageId: String? = null,
    val attachmentIds: List<String>? = null,
    val forwardedFromChatId: String? = null,
    val forwardedFromUserId: String? = null
)

/**
 * Request object used to modify an existing message.
 *
 * @property text The new text content to replace the old message text.
 */
@Serializable
data class EditMessageRequest(
    val text: String
)
