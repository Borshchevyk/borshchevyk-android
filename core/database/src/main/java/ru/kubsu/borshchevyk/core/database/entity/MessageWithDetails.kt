package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A POJO representing a [MessageEntity] along with its fully resolved related data.
 *
 * This aggregate class is crucial for the Borshchevyk UI layer, providing a complete picture
 * of a message, whether it was received via the global server or a P2P mesh network.
 * It uses Room's `@Relation` to fetch the associated author, forwarded user, reactions,
 * and attachments in a single complex query, ensuring efficient data loading for chat screens.
 *
 * @property message The core [MessageEntity] containing text, status, and metadata.
 * @property author The [UserEntity] who sent the message.
 * @property forwardedFromUser The [UserEntity] from whom the message was originally forwarded (if any).
 * @property reactions A list of [ReactionEntity] objects associated with this message.
 * @property attachments A list of [AttachmentEntity] objects representing files/media attached to this message.
 */
data class MessageWithDetails(
    @Embedded val message: MessageEntity,
    
    @Relation(
        parentColumn = "authorId",
        entityColumn = "userId"
    )
    val author: UserEntity?,
    
    @Relation(
        parentColumn = "forwardedFromUserId",
        entityColumn = "userId"
    )
    val forwardedFromUser: UserEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val reactions: List<ReactionEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentEntity>
)
