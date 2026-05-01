package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile
import javax.inject.Inject

/**
 * Handler exclusively responsible for constructing and sending standard text and attachment messages.
 *
 * @property messageUseCases Use cases for sending the final chat message.
 * @property attachmentUseCases Use cases for uploading attachments before sending the message.
 */
class MessageSenderHandler @Inject constructor(
    private val messageUseCases: ChatMessageUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases
) {
    /**
     * Sends a new message to the specified chat, optionally including uploaded attachments or forwarded content.
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
}
