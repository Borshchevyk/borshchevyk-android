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

@Serializable
data class UserProfileMeshPayload(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class UpdateChatMeshPayload(
    val chatId: String,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class DeleteChatMeshPayload(
    val chatId: String
)

@Serializable
data class ClearHistoryMeshPayload(
    val chatId: String,
    val forAll: Boolean
)

@Serializable
data class InviteUserMeshPayload(
    val chatId: String,
    val targetUserId: String
)

@Serializable
data class KickUserMeshPayload(
    val chatId: String,
    val targetUserId: String
)
