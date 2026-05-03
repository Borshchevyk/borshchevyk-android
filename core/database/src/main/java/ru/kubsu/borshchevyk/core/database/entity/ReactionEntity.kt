package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a user's reaction (e.g., an emoji) to a specific message in the Borshchevyk messenger.
 *
 * Reactions are synced across peers in P2P mode or via the global server. This entity is linked
 * to [MessageEntity] via a foreign key with CASCADE delete, meaning if the underlying message
 * is deleted, all its associated reactions are automatically removed from the local database.
 *
 * @property messageId The ID of the [MessageEntity] being reacted to.
 * @property userId The ID of the [UserEntity] who performed the reaction.
 * @property reaction The string representation of the reaction (typically a Unicode emoji).
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
