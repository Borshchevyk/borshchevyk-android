package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import javax.inject.Inject

class ChatMediaHandler @Inject constructor(
    private val attachmentUseCases: ChatAttachmentUseCases
) {
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
