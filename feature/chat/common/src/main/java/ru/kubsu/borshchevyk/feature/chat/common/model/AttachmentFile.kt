package ru.kubsu.borshchevyk.feature.chat.common.model

import android.net.Uri
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

data class AttachmentFile(
    val uri: Uri,
    val sizeBytes: Long,
    val originalFilename: String,
    val contentType: String,
    val extension: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null
) {
    fun toDomainAttachmentType(): DomainAttachmentType {
        val videoExtensions = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp")
        val audioExtensions = setOf("mp3", "m4a", "wav", "ogg", "flac", "aac")
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
        val ext = extension.lowercase()

        return when {
            contentType.startsWith("image/") || imageExtensions.contains(ext) -> DomainAttachmentType.PHOTO
            contentType.startsWith("video/") || videoExtensions.contains(ext) -> DomainAttachmentType.VIDEO
            contentType.startsWith("audio/") || audioExtensions.contains(ext) -> DomainAttachmentType.VOICE
            else -> DomainAttachmentType.FILE
        }
    }
}
