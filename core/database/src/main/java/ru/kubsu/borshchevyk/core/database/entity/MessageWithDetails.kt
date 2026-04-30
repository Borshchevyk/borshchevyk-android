package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A POJO representing a [MessageEntity] along with its related data for display or business logic.
 *
 * Uses Room's `@Relation` to fetch associated author, forwarded user, reactions, and attachments
 * in a single complex query.
 *
 * @property message The core [MessageEntity].
 * @property author The [UserEntity] who sent the message.
 * @property forwardedFromUser The [UserEntity] from whom the message was originally forwarded (if any).
 * @property reactions A list of [ReactionEntity] objects associated with this message.
 * @property attachments A list of [AttachmentEntity] objects associated with this message.
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
