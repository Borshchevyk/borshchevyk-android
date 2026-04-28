package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.ChatType

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
