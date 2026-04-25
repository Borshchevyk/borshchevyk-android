package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
    SAVED_MESSAGES
}

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
    val createdAt: String
)
