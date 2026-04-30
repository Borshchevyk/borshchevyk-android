package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.ChatType

/**
 * Represents a chat session, which can be either a direct conversation or a group chat.
 *
 * @property id The unique identifier for the chat.
 * @property type The type of chat (e.g., DIRECT, GROUP).
 * @property title The chat title (nullable for direct chats).
 * @property description The description or topic of the chat.
 * @property partnerId The ID of the primary chat partner (used for direct chats).
 * @property partnerName The display name of the chat partner.
 * @property partnerAvatarUrl The URL of the partner's avatar.
 * @property partnerLastOnline ISO timestamp of the partner's last seen time.
 * @property lastMessage The content of the most recent message in this chat.
 * @property unreadCount The number of unread messages.
 * @property allowedReactions The set of reactions supported by this chat.
 * @property isDeletable Whether this chat can be deleted.
 * @property isPinned Whether this chat is pinned to the top of the list.
 * @property createdAt ISO timestamp when the chat was created.
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
    val createdAt: String
)
