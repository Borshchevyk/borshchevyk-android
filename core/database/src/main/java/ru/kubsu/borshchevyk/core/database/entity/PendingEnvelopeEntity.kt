package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_envelopes")
data class PendingEnvelopeEntity(
    @PrimaryKey
    val envelopeId: String,
    val originEndpointId: String,
    val action: String,
    val payload: String,
    val signature: String?,
    val timestamp: Long = System.currentTimeMillis()
)
