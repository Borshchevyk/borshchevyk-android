package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity

/**
 * Represents a many-to-many relationship mapping which users have read which messages.
 */
@Entity(
    tableName = "message_readers",
    primaryKeys = ["messageId", "userId"]
)
data class MessageReaderEntity(
    val messageId: String,
    val userId: String,
    val readAt: String? = null
)
