package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing a signaling event for an audio or video call.
 *
 * Handles WebRTC or generic call signaling between users.
 *
 * @property type The type of the call event (e.g., "offer", "answer", "ice_candidate").
 * @property callId The unique identifier for the call session.
 * @property initiatorId The user ID of the person who initiated the call or event.
 * @property participants A list of user IDs involved in the call.
 */
data class DomainCallEvent(
    val type: String,
    val callId: String,
    val initiatorId: String,
    val participants: List<String>
)
