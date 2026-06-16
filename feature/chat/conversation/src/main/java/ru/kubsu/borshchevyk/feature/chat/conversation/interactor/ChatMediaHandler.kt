package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import javax.inject.Inject

class ChatMediaHandler @Inject constructor(
    private val attachmentUseCases: ChatAttachmentUseCases
) {
    fun observeAttachmentProgress(attachmentId: String): Flow<Float> {
        return attachmentUseCases.observeAttachmentProgress(attachmentId)
    }

    fun observeIncomingFiles(): Flow<String> {
        return attachmentUseCases.observeIncomingFiles()
    }

    suspend fun resolveAttachmentUrls(attachment: ru.kubsu.borshchevyk.core.model.domain.Attachment, isThumbnail: Boolean): Map<String, String> {
        val urls = mutableMapOf<String, String>()
        if (attachment.type == DomainAttachmentType.VIDEO || attachment.type == DomainAttachmentType.PHOTO || attachment.type == DomainAttachmentType.CIRCLE) {
            urls[attachment.id] = attachmentUseCases.getAttachmentUrl(attachment.id, false)
            urls[attachment.id + "_thumb"] = attachmentUseCases.getAttachmentUrl(attachment.id, true)
        } else {
            urls[attachment.id] = attachmentUseCases.getAttachmentUrl(attachment.id, isThumbnail)
        }
        return urls
    }

    suspend fun exportAttachment(attachmentId: String): Result<String> =
        attachmentUseCases.exportAttachment(attachmentId)
}
