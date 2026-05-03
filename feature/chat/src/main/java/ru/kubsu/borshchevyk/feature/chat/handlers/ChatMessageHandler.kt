package ru.kubsu.borshchevyk.feature.chat.handlers

import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile
import javax.inject.Inject

/**
 * Handles message-related actions like sending, editing, and deleting.
 * This encapsulates the business logic of message lifecycle within the chat.
 *
 * @property messageUseCases Domain use cases for common message operations.
 * @property attachmentUseCases Domain use cases for uploading and managing attachments.
 */
class ChatMessageHandler @Inject constructor(
    private val messageUseCases: ChatMessageUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases
) {
    /**
     * Sends a new message to the specified chat, optionally including attachments or forwarded content.
     *
     * @param chatId The unique identifier of the target chat.
     * @param text The text content of the message.
     * @param attachments A list of local files to be uploaded and attached to the message.
     * @param forwardPayload Optional data if this message is being forwarded from another chat.
     */
    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachments: List<AttachmentFile>,
        forwardPayload: ForwardPayload?
    ) {
        val attachmentIds = mutableListOf<String>()
        if (forwardPayload != null) {
            attachmentIds.addAll(forwardPayload.attachmentIds)
        }

        if (attachments.isNotEmpty()) {
            val uploadedIds = attachments.map { file ->
                val type = when {
                    file.contentType.startsWith("image/") -> DomainAttachmentType.PHOTO
                    file.contentType.startsWith("video/") -> DomainAttachmentType.VIDEO
                    file.contentType.startsWith("audio/") -> DomainAttachmentType.VOICE
                    else -> DomainAttachmentType.FILE
                }
                attachmentUseCases.uploadAttachment(
                    fileBytes = file.bytes,
                    originalFilename = file.originalFilename,
                    contentType = file.contentType,
                    extension = file.extension,
                    type = type,
                    width = file.width,
                    height = file.height,
                    duration = file.duration?.toDouble()
                )
            }
            attachmentIds.addAll(uploadedIds)
        }

        val finalAttachmentIds = if (attachmentIds.isNotEmpty()) attachmentIds else null
        val finalText = if (text.isNotBlank()) text else forwardPayload?.text ?: ""

        messageUseCases.sendMessage(
            chatId = chatId,
            text = finalText,
            attachmentIds = finalAttachmentIds,
            forwardedFromChatId = forwardPayload?.fromChatId,
            forwardedFromUserId = forwardPayload?.fromUserId
        )
    }

    /**
     * Edits the text content of an existing message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to edit.
     * @param newText The new text content for the message.
     */
    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        messageUseCases.editMessage(chatId, messageId, newText)
    }

    /**
     * Deletes a message from the chat, optionally for all participants.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to delete.
     * @param forAll If `true`, the message will be deleted for all members of the chat.
     */
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        messageUseCases.deleteMessage(chatId, messageId, forAll)
    }

    /**
     * Marks a specific message as read by the current user.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to mark as read.
     */
    suspend fun markAsRead(chatId: String, messageId: String) {
        messageUseCases.markMessageAsRead(chatId, messageId)
    }

    /**
     * Sends a typing event to notify other chat participants that the user is currently typing.
     *
     * @param chatId The unique identifier of the chat.
     * @param isTyping `true` if the user started typing, `false` if they stopped.
     */
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        messageUseCases.sendTypingEvent(chatId, isTyping)
    }

    /**
     * Adds a reaction to a specific message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message to react to.
     * @param reaction The string representation (e.g., emoji) of the reaction.
     */
    suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        messageUseCases.addReaction(chatId, messageId, reaction)
    }

    /**
     * Removes an existing reaction from a specific message.
     *
     * @param chatId The unique identifier of the chat containing the message.
     * @param messageId The unique identifier of the message.
     * @param reaction The string representation (e.g., emoji) of the reaction to remove.
     */
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        messageUseCases.removeReaction(chatId, messageId, reaction)
    }

    /**
     * Pins a specific message to the top of the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message to pin.
     */
    suspend fun pinMessage(chatId: String, messageId: String) {
        messageUseCases.pinMessage(chatId, messageId)
    }

    /**
     * Unpins a previously pinned message from the chat.
     *
     * @param chatId The unique identifier of the chat.
     * @param messageId The unique identifier of the message to unpin.
     */
    suspend fun unpinMessage(chatId: String, messageId: String) {
        messageUseCases.unpinMessage(chatId, messageId)
    }
}
