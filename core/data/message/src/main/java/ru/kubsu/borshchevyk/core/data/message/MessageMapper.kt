package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto

fun MessageResponse.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chat.id,
    authorId = author.id,
    author = User(
        userId = author.id,
        firstName = author.firstName,
        lastName = author.lastName,
        tag = author.tag ?: "",
        avatarUrl = author.avatarUrl
    ),
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = pinnedAt != null,
    reactions = reactions?.map { MessageReaction(userId = it.userId, reaction = it.reaction) } ?: emptyList(),
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChat?.id,
    forwardedFromUserId = forwardedFromUser?.id,
    forwardedFromUser = forwardedFromUser?.let { 
        User(
            userId = it.id,
            firstName = it.firstName,
            lastName = it.lastName,
            tag = it.tag ?: "",
            avatarUrl = it.avatarUrl
        )
    },
    attachments = attachments?.map {
        Attachment(
            id = it.id,
            type = it.type?.toDomain() ?: DomainAttachmentType.FILE,
            originalFilename = it.originalFilename ?: "file",
            extension = it.extension ?: "",
            sizeBytes = it.sizeBytes ?: 0L,
            thumbnailKey = it.thumbnailKey,
            updatedAt = it.updatedAt,
            width = it.width,
            height = it.height,
            duration = it.duration
        )
    } ?: attachmentIdsOld?.map { Attachment(id = it, type = DomainAttachmentType.FILE) 
    } ?: emptyList()
)

fun NotificationDto.MessageDto.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chat.id,
    authorId = author.id,
    author = User(
        userId = author.id,
        firstName = author.firstName,
        lastName = author.lastName,
        tag = author.tag ?: "",
        avatarUrl = author.avatarUrl
    ),
    text = text,
    createdAt = createdAt,
    updatedAt = null,
    status = status?.let { MessageStatus.valueOf(it) },
    isDeleted = isDeleted,
    source = MessageSource.ONLINE,
    isPinned = false,
    reactions = emptyList(),
    commentsCount = 0,
    parentMessageId = null,
    forwardedFromChatId = forwardedFromChat?.id,
    forwardedFromUserId = forwardedFromUser?.id,
    forwardedFromUser = forwardedFromUser?.let { 
        User(
            userId = it.id,
            firstName = it.firstName,
            lastName = it.lastName,
            tag = it.tag ?: "",
            avatarUrl = it.avatarUrl
        )
    },
    attachments = attachments?.map {
        Attachment(
            id = it.id,
            type = it.type?.toDomain() ?: DomainAttachmentType.FILE,
            originalFilename = it.originalFilename ?: "file",
            extension = it.extension ?: "",
            sizeBytes = it.sizeBytes ?: 0L,
            thumbnailKey = it.thumbnailKey,
            updatedAt = it.updatedAt,
            width = it.width,
            height = it.height,
            duration = it.duration
        )
    } ?: attachmentIdsOld?.map { 
        Attachment(id = it.id, type = DomainAttachmentType.FILE) 
    } ?: emptyList()
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    chatId = chatId,
    authorId = authorId,
    author = author,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = isPinned,
    reactions = reactions,
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChatId,
    forwardedFromUserId = forwardedFromUserId,
    forwardedFromUser = forwardedFromUser,
    attachments = attachments
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    authorId = authorId,
    author = author,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = isPinned,
    reactions = reactions,
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChatId,
    forwardedFromUserId = forwardedFromUserId,
    forwardedFromUser = forwardedFromUser,
    attachments = attachments
)

private fun AttachmentType.toDomain(): DomainAttachmentType = when (this) {
    AttachmentType.PHOTO -> DomainAttachmentType.PHOTO
    AttachmentType.VIDEO -> DomainAttachmentType.VIDEO
    AttachmentType.VOICE -> DomainAttachmentType.VOICE
    AttachmentType.CIRCLE -> DomainAttachmentType.CIRCLE
    AttachmentType.FILE -> DomainAttachmentType.FILE
    AttachmentType.STICKER -> DomainAttachmentType.STICKER
    AttachmentType.AVATAR -> DomainAttachmentType.AVATAR
}
