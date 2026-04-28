package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile
import javax.inject.Inject

class MessageSenderHandler @Inject constructor(
    private val messageUseCases: ChatMessageUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases
) {
    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachments: List<AttachmentFile>,
        forwardPayload: ForwardPayload?
    ): Message {
        val attachmentIds = mutableListOf<String>()
        if (forwardPayload != null) {
            attachmentIds.addAll(forwardPayload.attachmentIds)
        }

        if (attachments.isNotEmpty()) {
            val uploadedIds = attachments.map { file ->
                val type = when {
                    file.contentType.startsWith("image/") -> AttachmentType.PHOTO
                    file.contentType.startsWith("video/") -> AttachmentType.VIDEO
                    file.contentType.startsWith("audio/") -> AttachmentType.VOICE
                    else -> AttachmentType.FILE
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

        return messageUseCases.sendMessage(
            chatId = chatId,
            text = finalText,
            attachmentIds = finalAttachmentIds,
            forwardedFromChatId = forwardPayload?.fromChatId,
            forwardedFromUserId = forwardPayload?.fromUserId
        )
    }
}
