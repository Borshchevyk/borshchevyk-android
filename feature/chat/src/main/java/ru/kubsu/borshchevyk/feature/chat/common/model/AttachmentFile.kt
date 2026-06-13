package ru.kubsu.borshchevyk.feature.chat.common.model

import android.net.Uri
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

data class AttachmentFile(
    val uri: Uri,
    val bytes: ByteArray,
    val originalFilename: String,
    val contentType: String,
    val extension: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null
) {
    fun toDomainAttachmentType(): DomainAttachmentType {
        return when {
            contentType.startsWith("image/") -> DomainAttachmentType.PHOTO
            contentType.startsWith("video/") -> DomainAttachmentType.VIDEO
            contentType.startsWith("audio/") -> DomainAttachmentType.VOICE
            else -> DomainAttachmentType.FILE
        }
    }
}
