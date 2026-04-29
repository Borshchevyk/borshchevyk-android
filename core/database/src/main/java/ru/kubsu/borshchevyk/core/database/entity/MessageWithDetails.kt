package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

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
