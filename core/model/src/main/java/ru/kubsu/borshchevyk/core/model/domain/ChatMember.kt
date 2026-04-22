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
    val canDeleteMessages: Boolean = false,
    val canInviteUsers: Boolean = false,
    val canChangeInfo: Boolean = false,
    val historyClearedAt: String? = null
)
