package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL
}

@Serializable
data class Chat(
    val id: String,
    val type: ChatType,
    val title: String? = null,
    val description: String? = null,
    val partnerId: String? = null,
    val partnerAvatarUrl: String? = null,
    val partnerLastOnline: String? = null,
    val allowedReactions: Set<String>? = null,
    val createdAt: String
)
