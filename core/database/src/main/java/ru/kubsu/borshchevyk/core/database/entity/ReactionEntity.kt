package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a user reaction to a specific message.
 *
 * This entity is linked to [MessageEntity] via a foreign key with CASCADE delete, meaning
 * if the message is deleted, all its reactions are also removed.
 *
 * @property messageId The ID of the message being reacted to.
 * @property userId The ID of the user who performed the reaction.
 * @property reaction The emoji or identifier representing the reaction.
 */
@Entity(
    tableName = "reactions",
    primaryKeys = ["messageId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
)
data class ReactionEntity(
    val messageId: String,
    val userId: String,
    val reaction: String
)
