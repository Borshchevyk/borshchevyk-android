package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Domain model representing a synchronization event in the CRDT/Vector Clock architecture.
 *
 * @property id Unique identifier of the event.
 * @property entityId ID of the entity being mutated (e.g., Message ID, Chat ID).
 * @property eventType Type of the event.
 * @property payload JSON payload of the event (CRDT operations).
 * @property vectorClock Vector clock representing the causal history of this event.
 * @property timestamp ISO 8601 formatted timestamp when the event was created.
 */
@Serializable
data class SyncEvent(
    val id: String,
    val entityId: String,
    val eventType: EventType,
    val payload: String,
    val vectorClock: VectorClock,
    val timestamp: String
)
