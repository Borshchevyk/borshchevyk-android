package ru.kubsu.borshchevyk.core.model.domain

/**
 * Request payload for creating a new chat.
 *
 * @property type The type of chat to create (e.g., PRIVATE, GROUP).
 * @property title The title of the chat (required for GROUP, ignored for PRIVATE).
 * @property description An optional description for the chat.
 * @property initialMemberIds A list of user IDs to include initially in the group chat.
 */
data class DomainCreateChatParam(
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val initialMemberIds: List<String>? = null
)

/**
 * Request payload for updating metadata of an existing chat.
 *
 * @property title The new title to apply, if provided.
 * @property description The new description to apply, if provided.
 */
data class DomainUpdateChatInfoParam(
    val title: String? = null,
    val description: String? = null
)

/**
 * Generic request payload containing a single target user ID.
 *
 * Often used for adding/removing users from a group, or making someone an admin.
 *
 * @property targetUserId The ID of the target user to perform the action on.
 */
data class DomainTargetUserParam(
    val targetUserId: String
)

/**
 * Request payload for updating a chat member's permissions.
 *
 * @property userId The ID of the member whose permissions are being updated.
 * @property canSendMessages Whether the user is allowed to send messages.
 * @property canDeleteMessages Whether the user is allowed to delete messages.
 * @property canInviteUsers Whether the user is allowed to invite others to the chat.
 * @property canChangeInfo Whether the user is allowed to change chat metadata.
 */
data class DomainUpdatePermissionsParam(
    val userId: String,
    val canSendMessages: Boolean? = null,
    val canDeleteMessages: Boolean? = null,
    val canInviteUsers: Boolean? = null,
    val canChangeInfo: Boolean? = null
)
