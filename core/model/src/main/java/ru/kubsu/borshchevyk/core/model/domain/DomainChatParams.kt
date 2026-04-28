package ru.kubsu.borshchevyk.core.model.domain

data class DomainCreateChatParam(
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val initialMemberIds: List<String>? = null
)

data class DomainUpdateChatInfoParam(
    val title: String? = null,
    val description: String? = null
)

data class DomainTargetUserParam(
    val targetUserId: String
)

data class DomainUpdatePermissionsParam(
    val userId: String,
    val canSendMessages: Boolean? = null,
    val canDeleteMessages: Boolean? = null,
    val canInviteUsers: Boolean? = null,
    val canChangeInfo: Boolean? = null
)
