package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MessageSource {
    ONLINE,
    OFFLINE
}

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
    val isDeleted: Boolean = false,
    val source: MessageSource = MessageSource.ONLINE
)
