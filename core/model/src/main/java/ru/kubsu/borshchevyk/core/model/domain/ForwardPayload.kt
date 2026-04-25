package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
data class ForwardPayload(
    val text: String,
    val attachmentIds: List<String>,
    val fromChatId: String,
    val fromUserId: String,
    val authorName: String
)
