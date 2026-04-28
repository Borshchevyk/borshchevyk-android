package ru.kubsu.borshchevyk.core.data.chat

import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse

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
