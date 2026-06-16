package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.ChatType

/**
 * Represents a chat session within the Borshchevyk messenger.
 *
 * A chat can be either a direct (one-on-one) conversation or a group chat. It serves as the
 * container for [MessageEntity]s. This entity unifies chats regardless of their networking mode,
 * supporting seamless integration of both global server chats and P2P mesh network connections.
 *
 * @property id The unique identifier for the chat session.
 * @property type The classification of the chat (e.g., DIRECT, GROUP).
 * @property title The chat title (usually null for direct chats where the partner's name is used instead).
 * @property description A brief description or topic of the chat, mainly for groups.
 * @property partnerId The ID of the primary chat partner (applicable and populated only for direct chats).
 * @property partnerName The display name of the chat partner, cached for quick UI rendering.
 * @property partnerAvatarUrl The URL or local identifier of the partner's avatar.
 * @property partnerLastOnline ISO timestamp indicating when the partner was last seen online.
 * @property lastMessage A snippet or the full text of the most recent message in this chat, used in the chat list.
 * @property unreadCount The current number of unread messages for the local user in this chat.
 * @property allowedReactions The set of allowed reactions/emojis that can be used in this chat.
 * @property isDeletable Flag indicating whether the local user has permission to delete this chat.
 * @property isPinned Flag indicating whether the user has pinned this chat to the top of their chat list.
 * @property createdAt ISO timestamp indicating when the chat was created.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val type: ChatType,
    val title: String?,
    val description: String?,
    val partnerId: String?,
    val partnerName: String?,
    val partnerAvatarUrl: String?,
    val partnerLastOnline: String?,
    val lastMessage: String?,
    val unreadCount: Long,
    val allowedReactions: Set<String>?,
    val isDeletable: Boolean,
    val isPinned: Boolean,
    val createdAt: String,
    
    // CRDT LWW Timestamps for Mesh group updates
    val titleUpdatedAt: Long = 0,
    val descriptionUpdatedAt: Long = 0
)
