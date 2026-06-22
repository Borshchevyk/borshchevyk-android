package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A data transfer object representing a chat member combined with their user profile data.
 *
 * This POJO ensures that chat member lists in the UI update reactively when
 * a member's name or avatar changes.
 *
 * @property member The embedded chat member entity.
 * @property user The related user entity for this member.
 */
data class ChatMemberWithUser(
    @Embedded val member: ChatMemberEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val user: UserEntity?
)
