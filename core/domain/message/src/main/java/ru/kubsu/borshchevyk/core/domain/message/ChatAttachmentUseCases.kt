package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ChatAttachmentUseCases @Inject constructor(
    val uploadAttachment: UploadAttachmentUseCase,
    val getAttachmentUrl: GetAttachmentUrlUseCase
)
