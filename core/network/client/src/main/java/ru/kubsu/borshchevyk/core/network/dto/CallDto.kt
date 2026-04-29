package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateCallRequest(
    val participantIds: List<String>
)

@Serializable
data class CallResponse(
    val id: String,
    val roomId: String? = null,
    val initiator: ShortUserDto,
    val status: String,
    val createdAt: String,
    val endedAt: String? = null,
    val participants: List<ShortUserDto> = emptyList()
)

@Serializable
data class JoinCallResponse(
    val token: String
)
