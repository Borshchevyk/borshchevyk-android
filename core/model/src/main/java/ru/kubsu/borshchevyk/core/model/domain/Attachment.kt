package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    val type: DomainAttachmentType,
    val originalFilename: String = "file",
    val extension: String = "",
    val sizeBytes: Long = 0,
    val url: String? = null,
    val thumbnailKey: String? = null,
    val updatedAt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)
