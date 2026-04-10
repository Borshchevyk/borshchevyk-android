package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.MessageSource

@Serializable
data class MessageResponse(
    val id: String,
    val chatId: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
    val isDeleted: Boolean,
    val source: MessageSource
)

@Serializable
data class SendMessageRequest(
    val text: String,
    val source: MessageSource? = null
)
