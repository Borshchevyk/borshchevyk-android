package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.model.domain.VectorClock

/**
 * Room entity representing an outbox synchronization event.
 * Events are stored here temporarily when generated locally while offline,
 * and then pushed to the server via SyncManager.
 */
@Entity(tableName = "sync_outbox")
data class SyncEventEntity(
    @PrimaryKey
    val id: String,
    val entityId: String,
    val eventType: EventType,
    val payload: String,
    val vectorClock: VectorClock,
    val timestamp: String
)
