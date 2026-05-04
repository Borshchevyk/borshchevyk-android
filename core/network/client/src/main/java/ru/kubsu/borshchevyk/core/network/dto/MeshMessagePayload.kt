package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeshMessagePayload(
    val chatId: String,
    val request: SendMessageRequest,
    val attachments: List<AttachmentResponse>? = null
)

@Serializable
data class ReactionMeshPayload(
    val chatId: String,
    val messageId: String,
    val reaction: String
)

@Serializable
data class PinMeshPayload(
    val chatId: String,
    val messageId: String
)

@Serializable
data class ReadReceiptMeshPayload(
    val chatId: String,
    val messageId: String
)
