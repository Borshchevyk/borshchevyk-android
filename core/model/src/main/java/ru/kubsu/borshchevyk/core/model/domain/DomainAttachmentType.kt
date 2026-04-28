package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class DomainAttachmentType {
    PHOTO,
    VIDEO,
    VOICE,
    CIRCLE,
    FILE,
    STICKER,
    AVATAR
}
