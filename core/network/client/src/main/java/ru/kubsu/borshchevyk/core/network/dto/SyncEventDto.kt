package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.model.domain.VectorClock

/**
 * DTO representing a synchronization event for the network layer.
 */
@Serializable
data class SyncEventDto(
    val id: String,
    val entityId: String,
    val eventType: EventType,
    val payload: String,
    val vectorClock: VectorClock,
    val timestamp: String
)

fun SyncEventDto.toDomain() = ru.kubsu.borshchevyk.core.model.domain.SyncEvent(
    id = id,
    entityId = entityId,
    eventType = eventType,
    payload = payload,
    vectorClock = vectorClock,
    timestamp = timestamp
)

fun ru.kubsu.borshchevyk.core.model.domain.SyncEvent.toDto() = SyncEventDto(
    id = id,
    entityId = entityId,
    eventType = eventType,
    payload = payload,
    vectorClock = vectorClock,
    timestamp = timestamp
)
