package ru.kubsu.borshchevyk.core.model.domain

data class DomainAttachmentResponse(
    val id: String,
    val type: DomainAttachmentType?,
    val originalFilename: String?,
    val extension: String?,
    val sizeBytes: Long?,
    val thumbnailKey: String?,
    val updatedAt: String?,
    val width: Int?,
    val height: Int?,
    val duration: Double?
)
