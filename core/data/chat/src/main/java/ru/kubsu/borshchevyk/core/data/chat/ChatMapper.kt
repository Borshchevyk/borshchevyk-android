package ru.kubsu.borshchevyk.core.data.chat

import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole

/**
 * Maps a network [ChatResponse] DTO to a local [ChatEntity].
 *
 * @return The mapped database entity.
 */
fun ChatResponse.toEntity(): ChatEntity = ChatEntity(
    id = id,
    type = type,
    title = if (type == ru.kubsu.borshchevyk.core.model.domain.ChatType.PRIVATE) partnerName ?: title else title,
    description = description,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerAvatarUrl = partnerAvatarUrl,
    partnerLastOnline = partnerLastOnline,
    lastMessage = lastMessage,
    unreadCount = unreadCount,
    allowedReactions = allowedReactions,
    isDeletable = isDeletable,
    isPinned = isPinned,
    createdAt = createdAt
)

/**
 * Maps a network [ChatResponse] DTO to a domain [Chat] model.
 *
 * @return The mapped domain model.
 */
fun ChatResponse.toDomain(): Chat = Chat(
    id = id,
    type = type,
    title = if (type == ru.kubsu.borshchevyk.core.model.domain.ChatType.PRIVATE) partnerName ?: title else title,
    description = description,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerAvatarUrl = partnerAvatarUrl,
    partnerLastOnline = partnerLastOnline,
    lastMessage = lastMessage,
    unreadCount = unreadCount,
    allowedReactions = allowedReactions,
    isDeletable = isDeletable,
    isPinned = isPinned,
    createdAt = createdAt
)

/**
 * Maps a local [ChatEntity] to a domain [Chat] model.
 *
 * @return The mapped domain model.
 */
fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    type = type,
    title = title,
    description = description,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerAvatarUrl = partnerAvatarUrl,
    partnerLastOnline = partnerLastOnline,
    lastMessage = lastMessage,
    unreadCount = unreadCount,
    allowedReactions = allowedReactions,
    isDeletable = isDeletable,
    isPinned = isPinned,
    createdAt = createdAt
)

/**
 * Maps a network [ChatMemberResponse] DTO to a domain [ChatMember] model.
 *
 * @return The mapped domain model.
 */
fun ChatMemberResponse.toDomain(): ChatMember = ChatMember(
    chatId = chatId,
    userId = userId,
    user = userDetails?.let {
        ru.kubsu.borshchevyk.core.model.domain.User(
            userId = it.id,
            firstName = it.firstName,
            lastName = it.lastName,
            tag = it.tag ?: "",
            avatarUrl = it.avatarUrl
        )
    },
    role = ChatMemberRole.valueOf(role),
    joinedAt = joinedAt,
    canSendMessages = canSendMessages,
    canDeleteMessages = canDeleteMessages,
    canInviteUsers = canInviteUsers,
    canChangeInfo = canChangeInfo,
    historyClearedAt = historyClearedAt
)