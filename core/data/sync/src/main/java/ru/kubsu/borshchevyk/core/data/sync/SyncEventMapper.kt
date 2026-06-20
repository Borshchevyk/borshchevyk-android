package ru.kubsu.borshchevyk.core.data.sync

import ru.kubsu.borshchevyk.core.database.entity.SyncEventEntity
import ru.kubsu.borshchevyk.core.model.domain.SyncEvent

fun SyncEventEntity.toDomain() = SyncEvent(
    id = id,
    entityId = entityId,
    eventType = eventType,
    payload = payload,
    vectorClock = vectorClock,
    timestamp = timestamp
)

fun SyncEvent.toEntity() = SyncEventEntity(
    id = id,
    entityId = entityId,
    eventType = eventType,
    payload = payload,
    vectorClock = vectorClock,
    timestamp = timestamp
)
