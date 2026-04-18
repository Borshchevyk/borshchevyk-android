package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMemberRole {
    OWNER,
    ADMIN,
    MEMBER
}

@Serializable
data class ChatMember(
    val chatId: String,
    val userId: String,
    val role: ChatMemberRole,
    val joinedAt: String,
    val canSendMessages: Boolean = true,
    val canDeleteMessages: Boolean = true,
    val canInviteUsers: Boolean = true,
    val canChangeInfo: Boolean = true,
    val historyClearedAt: String? = null
)
