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
 */
class ChatMessageHandler @Inject constructor(
    private val messageUseCases: ChatMessageUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases
) {
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

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        messageUseCases.editMessage(chatId, messageId, newText)
    }

    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        messageUseCases.deleteMessage(chatId, messageId, forAll)
    }

    suspend fun markAsRead(chatId: String, messageId: String) {
        messageUseCases.markMessageAsRead(chatId, messageId)
    }

    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        messageUseCases.sendTypingEvent(chatId, isTyping)
    }

    suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        messageUseCases.addReaction(chatId, messageId, reaction)
    }

    suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        messageUseCases.removeReaction(chatId, messageId, reaction)
    }

    suspend fun pinMessage(chatId: String, messageId: String) {
        messageUseCases.pinMessage(chatId, messageId)
    }

    suspend fun unpinMessage(chatId: String, messageId: String) {
        messageUseCases.unpinMessage(chatId, messageId)
    }
}
