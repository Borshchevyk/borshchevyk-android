package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object used to initiate a new call.
 *
 * @property participantIds The list of user IDs to include in the call.
 */
@Serializable
data class CreateCallRequest(
    val participantIds: List<String>
)

/**
 * Response object containing detailed information about a call.
 *
 * @property id The unique identifier of the call.
 * @property roomId An optional room identifier for multi-user calls.
 * @property initiator The summarized user details of the person who initiated the call.
 * @property status The current status of the call (e.g., INITIATED, IN_PROGRESS, ENDED).
 * @property createdAt The timestamp when the call was created.
 * @property endedAt The timestamp when the call ended, if applicable.
 * @property participants The list of users participating in the call.
 */
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

/**
 * Response object returned when successfully joining a call.
 *
 * @property token The authorization or access token required to join the media stream.
 */
@Serializable
data class JoinCallResponse(
    val token: String
)
