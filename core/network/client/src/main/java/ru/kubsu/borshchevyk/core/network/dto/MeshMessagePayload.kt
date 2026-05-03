package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeshMessagePayload(
    val chatId: String,
    val request: SendMessageRequest
)
