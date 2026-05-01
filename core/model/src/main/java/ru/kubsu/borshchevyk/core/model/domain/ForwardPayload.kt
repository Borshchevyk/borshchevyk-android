package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Domain model representing the necessary payload to forward an existing message.
 *
 * Encapsulates the content and origin of the forwarded message.
 *
 * @property text The text content of the forwarded message.
 * @property attachmentIds A list of IDs of attachments included in the forwarded message.
 * @property fromChatId The ID of the chat where the message originated.
 * @property fromUserId The user ID of the original author of the message.
 * @property authorName The display name of the original author.
 */
@Serializable
data class ForwardPayload(
    val text: String,
    val attachmentIds: List<String>,
    val fromChatId: String,
    val fromUserId: String,
    val authorName: String
)
