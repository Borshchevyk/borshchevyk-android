package ru.kubsu.borshchevyk.core.data.chat

import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.database.entity.ChatWithPartner
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse

/**
 * Maps a [ChatWithPartner] database POJO to a domain [Chat] model.
 *
 * This mapping prioritizes data from the [UserEntity] relation for direct chats
 * to ensure UI reactivity when a partner updates their profile.
 *
 * @return The mapped domain model.
 */
fun ChatWithPartner.toDomain(): Chat = Chat(
    id = chat.id,
    type = chat.type,
    title = if (chat.type == ru.kubsu.borshchevyk.core.model.domain.ChatType.PRIVATE) {
        partner?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: chat.partnerName ?: chat.title
    } else {
        chat.title
    },
    description = chat.description,
    partnerId = chat.partnerId,
    partnerName = partner?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: chat.partnerName,
    partnerAvatarUrl = partner?.avatarUrl ?: chat.partnerAvatarUrl,
    partnerLastOnline = chat.partnerLastOnline,
    lastMessage = chat.lastMessage,
    unreadCount = chat.unreadCount,
    allowedReactions = chat.allowedReactions,
    isDeletable = chat.isDeletable,
    isPinned = chat.isPinned,
    createdAt = chat.createdAt
)

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
    isPinned = isPinned,
    historyClearedAt = historyClearedAt
)