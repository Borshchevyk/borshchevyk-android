package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.ChatType

/**
 * Summarized details of a chat, generally used when full chat information is unnecessary.
 *
 * @property id The unique identifier of the chat.
 * @property name The name of the chat, if set.
 */
@Serializable
data class ShortChatDto(
    val id: String,
    val name: String? = null
)

/**
 * The standard response object containing comprehensive details about a chat.
 *
 * @property id The unique identifier of the chat.
 * @property type The type of the chat (e.g., DIRECT, GROUP).
 * @property title The title of the chat, typically used for group chats.
 * @property description A description or about text for the chat.
 * @property createdAt The timestamp when the chat was created.
 * @property partnerId The unique identifier of the other participant in a direct chat.
 * @property partnerName The name of the other participant in a direct chat.
 * @property partnerAvatarUrl The avatar URL of the other participant in a direct chat.
 * @property partnerLastOnline The last online timestamp of the other participant in a direct chat.
 * @property allowedReactions The set of allowed reaction emojis or identifiers for this chat.
 * @property lastMessage The content of the most recent message in the chat.
 * @property unreadCount The number of unread messages for the current user.
 * @property isDeletable Indicates whether the current user can delete this chat.
 * @property isPinned Indicates whether this chat is pinned by the current user.
 */
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
    @SerialName("deletable")
    val isDeletable: Boolean = true,
    @SerialName("pinned")
    val isPinned: Boolean = false
)

/**
 * Request object used to specify a target user for an action (e.g., initiating a direct chat).
 *
 * @property targetUserId The unique identifier of the target user.
 */
@Serializable
data class TargetUserRequest(
    val targetUserId: String
)

/**
 * Request object used to create a new chat.
 *
 * @property type The type of chat to create (e.g., DIRECT, GROUP).
 * @property title The title of the chat (required for group chats).
 * @property description An optional description for the chat.
 * @property initialMemberIds An optional list of user IDs to include in the chat initially.
 */
@Serializable
data class CreateChatRequest(
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val initialMemberIds: List<String>? = null
)

/**
 * Request object used to update the permissions of a chat member.
 *
 * @property canSendMessages Updates permission to send messages.
 * @property canDeleteMessages Updates permission to delete messages.
 * @property canInviteUsers Updates permission to invite other users.
 * @property canChangeInfo Updates permission to modify chat details.
 */
@Serializable
data class UpdatePermissionsRequest(
    val canSendMessages: Boolean? = null,
    val canDeleteMessages: Boolean? = null,
    val canInviteUsers: Boolean? = null,
    val canChangeInfo: Boolean? = null
)
