package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.ChatType

@Serializable
data class ChatResponse(
    val id: String,
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val createdAt: String
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
