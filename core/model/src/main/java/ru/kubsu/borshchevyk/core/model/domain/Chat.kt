package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Defines the available types of chats supported by the system.
 */
@Serializable
enum class ChatType {
    /** A 1-on-1 direct conversation between two users. */
    PRIVATE,
    /** A group conversation with multiple members. */
    GROUP,
    /** A broadcast channel where only admins can post messages. */
    CHANNEL,
    /** A special private chat serving as the user's personal cloud/storage. */
    SAVED_MESSAGES
}

/**
 * Domain model representing a chat conversation.
 *
 * This can be a private 1-on-1 chat, a group, or a channel. It holds metadata
 * about the chat, the partner (for private chats), and overall state like pinned status.
 *
 * @property id Unique identifier for the chat.
 * @property type The type of the chat (PRIVATE, GROUP, etc.).
 * @property title The title of the chat (mostly for GROUP/CHANNEL).
 * @property description A brief description of the chat.
 * @property partnerId The unique user ID of the conversation partner (for PRIVATE chats).
 * @property partnerName The display name of the conversation partner.
 * @property partnerAvatarUrl The URL pointing to the partner's avatar image.
 * @property partnerLastOnline ISO 8601 formatted timestamp representing when the partner was last seen.
 * @property lastMessage Preview text of the most recent message in the chat.
 * @property unreadCount The number of unread messages for the current user in this chat.
 * @property allowedReactions The set of allowed reaction emojis for this chat. Null implies all are allowed.
 * @property isDeletable Indicates whether the current user has permission to delete this chat.
 * @property isPinned Indicates whether this chat is pinned to the top of the user's chat list.
 * @property createdAt ISO 8601 formatted timestamp when the chat was created.
 */
@Serializable
data class Chat(
    val id: String,
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerAvatarUrl: String? = null,
    val partnerLastOnline: String? = null,
    val lastMessage: String? = null,
    val unreadCount: Long = 0,
    val allowedReactions: Set<String>? = null,
    val isDeletable: Boolean = true,
    val isPinned: Boolean = false,
    val createdAt: String
)
