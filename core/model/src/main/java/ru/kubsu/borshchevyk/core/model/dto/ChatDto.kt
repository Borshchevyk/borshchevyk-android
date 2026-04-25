package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.ChatType

@Serializable
data class ShortChatDto(
    val id: String,
    val name: String? = null
)

@Serializable
data class ChatResponse(
    val id: String,
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val createdAt: String,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerAvatarUrl: String? = null,
    val partnerLastOnline: String? = null,
    val allowedReactions: Set<String>? = null,
    val lastMessage: String? = null,
    val unreadCount: Long = 0,
    val isDeletable: Boolean = true
)

@Serializable
data class TargetUserRequest(
    val targetUserId: String
)

@Serializable
data class CreateChatRequest(
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val initialMemberIds: List<String>? = null
)

@Serializable
data class UpdatePermissionsRequest(
    val canSendMessages: Boolean? = null,
    val canDeleteMessages: Boolean? = null,
    val canInviteUsers: Boolean? = null,
    val canChangeInfo: Boolean? = null
)
