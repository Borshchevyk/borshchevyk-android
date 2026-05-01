package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Defines the various types of attachments supported within the application domain.
 *
 * Used to conditionally render UI components (like video players vs image viewers)
 * based on the file content.
 */
@Serializable
enum class DomainAttachmentType {
    /** Standard image file (e.g., JPEG, PNG). */
    PHOTO,
    /** Standard video file (e.g., MP4). */
    VIDEO,
    /** Voice message recorded within the app. */
    VOICE,
    /** Video message (circle video) recorded within the app. */
    CIRCLE,
    /** Generic file document (e.g., PDF, DOCX, ZIP). */
    FILE,
    /** Animated or static sticker image. */
    STICKER,
    /** User or group profile picture. */
    AVATAR
}
