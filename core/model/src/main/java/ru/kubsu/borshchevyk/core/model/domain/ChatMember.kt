package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Defines the roles a user can hold within a group chat or channel.
 */
@Serializable
enum class ChatMemberRole {
    /** The creator or primary owner of the chat. Has full privileges. */
    OWNER,
    /** An administrator with elevated privileges (can manage members/messages). */
    ADMIN,
    /** A regular participant in the chat. */
    MEMBER
}

/**
 * Domain model representing a participant within a specific chat.
 *
 * It contains permission flags and metadata regarding the user's relationship
 * with the group or channel.
 *
 * @property chatId The ID of the chat this membership belongs to.
 * @property userId The user ID of the participant.
 * @property user The full [User] domain object, if loaded.
 * @property role The user's role (e.g., OWNER, ADMIN, MEMBER).
 * @property joinedAt ISO 8601 formatted timestamp indicating when the user joined.
 * @property canSendMessages True if the user is allowed to send messages in this chat.
 * @property canDeleteMessages True if the user is allowed to delete messages (usually ADMIN/OWNER).
 * @property canInviteUsers True if the user has permission to invite others.
 * @property canChangeInfo True if the user has permission to edit chat title, avatar, or description.
 * @property isPinned True if the user has pinned this chat in their own view.
 * @property historyClearedAt ISO 8601 timestamp of when the user last cleared their chat history.
 */
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
    val isPinned: Boolean = false,
    val historyClearedAt: String? = null
)
