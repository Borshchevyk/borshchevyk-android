package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.entity.AttachmentEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageWithDetails
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.network.dto.AttachmentType
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto

fun MessageResponse.toDomain(): Message = Message(
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

fun NotificationDto.MessageDto.toDomain(): Message = Message(
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

fun MessageWithDetails.toDomain(): Message = Message(
    id = message.id,
    chatId = message.chatId,
    authorId = message.authorId,
    author = author?.let { 
        User(
            userId = it.userId, 
            email = it.email, 
            tag = it.tag, 
            firstName = it.firstName, 
            lastName = it.lastName, 
            bio = it.bio, 
            avatarUrl = it.avatarUrl, 
            avatars = it.avatars
        ) 
    },
    text = message.text,
    createdAt = message.createdAt,
    updatedAt = message.updatedAt,
    status = message.status,
    isDeleted = message.isDeleted,
    source = message.source,
    isPinned = message.isPinned,
    reactions = reactions.map { MessageReaction(userId = it.userId, reaction = it.reaction) },
    commentsCount = message.commentsCount,
    parentMessageId = message.parentMessageId,
    forwardedFromChatId = message.forwardedFromChatId,
    forwardedFromUserId = message.forwardedFromUserId,
    forwardedFromUser = forwardedFromUser?.let {
        User(
            userId = it.userId, 
            email = it.email, 
            tag = it.tag, 
            firstName = it.firstName, 
            lastName = it.lastName, 
            bio = it.bio, 
            avatarUrl = it.avatarUrl, 
            avatars = it.avatars
        )
    },
    attachments = attachments.map {
        Attachment(
            id = it.id,
            type = it.type,
            originalFilename = it.originalFilename,
            extension = it.extension,
            sizeBytes = it.sizeBytes,
            thumbnailKey = it.thumbnailKey,
            updatedAt = it.updatedAt,
            width = it.width,
            height = it.height,
            duration = it.duration?.toDouble()
        )
    }
)

fun Message.toMessageEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    authorId = authorId,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isDeleted = isDeleted,
    source = source,
    isPinned = isPinned,
    commentsCount = commentsCount,
    parentMessageId = parentMessageId,
    forwardedFromChatId = forwardedFromChatId,
    forwardedFromUserId = forwardedFromUserId
)

fun Message.toAuthorEntity(): UserEntity? = author?.let {
    UserEntity(
        userId = it.userId,
        email = it.email,
        tag = it.tag,
        firstName = it.firstName,
        lastName = it.lastName,
        bio = it.bio,
        avatarUrl = it.avatarUrl,
        avatars = it.avatars
    )
}

fun Message.toForwardedUserEntity(): UserEntity? = forwardedFromUser?.let {
    UserEntity(
        userId = it.userId,
        email = it.email,
        tag = it.tag,
        firstName = it.firstName,
        lastName = it.lastName,
        bio = it.bio,
        avatarUrl = it.avatarUrl,
        avatars = it.avatars
    )
}

fun Message.toAttachmentEntities(): List<AttachmentEntity> = attachments.map {
    AttachmentEntity(
        id = it.id,
        messageId = id,
        type = it.type,
        originalFilename = it.originalFilename,
        extension = it.extension,
        sizeBytes = it.sizeBytes,
        thumbnailKey = it.thumbnailKey,
        updatedAt = it.updatedAt,
        width = it.width,
        height = it.height,
        duration = it.duration?.toInt()
    )
}

fun Message.toReactionEntities(): List<ReactionEntity> = reactions.map {
    ReactionEntity(
        messageId = id,
        userId = it.userId,
        reaction = it.reaction
    )
}

private fun AttachmentType.toDomain(): DomainAttachmentType = when (this) {
    AttachmentType.PHOTO -> DomainAttachmentType.PHOTO
    AttachmentType.VIDEO -> DomainAttachmentType.VIDEO
    AttachmentType.VOICE -> DomainAttachmentType.VOICE
    AttachmentType.CIRCLE -> DomainAttachmentType.CIRCLE
    AttachmentType.FILE -> DomainAttachmentType.FILE
    AttachmentType.STICKER -> DomainAttachmentType.STICKER
    AttachmentType.AVATAR -> DomainAttachmentType.AVATAR
}
