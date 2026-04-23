package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType

@Serializable
data class Attachment(
    val id: String,
    val type: AttachmentType,
    val originalFilename: String = "file",
    val extension: String = "",
    val sizeBytes: Long = 0,
    val url: String? = null,
    val thumbnailKey: String? = null,
    val updatedAt: String? = null
)
