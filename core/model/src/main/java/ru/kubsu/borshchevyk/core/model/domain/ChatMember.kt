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
    val user: User? = null,
    val role: ChatMemberRole,
    val joinedAt: String,
    val canSendMessages: Boolean,
    val canDeleteMessages: Boolean,
    val canInviteUsers: Boolean,
    val canChangeInfo: Boolean,
    val historyClearedAt: String? = null
)
