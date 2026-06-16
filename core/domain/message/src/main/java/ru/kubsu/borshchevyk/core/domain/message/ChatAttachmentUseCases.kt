package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * A wrapper class that groups all use cases related to chat attachments.
 *
 * This class provides a convenient way to inject multiple attachment-related
 * operations into ViewModels or other components, avoiding constructor parameter bloat.
 *
 * @property uploadAttachment Use case for uploading new attachments.
 * @property getAttachmentUrl Use case for retrieving the URL of an existing attachment.
 */
class ChatAttachmentUseCases @Inject constructor(
    val uploadAttachment: UploadAttachmentUseCase,
    val getAttachmentUrl: GetAttachmentUrlUseCase,
    val exportAttachment: ExportAttachmentUseCase,
    val observeAttachmentProgress: ObserveAttachmentProgressUseCase,
    val observeIncomingFiles: ObserveIncomingFilesUseCase
)
