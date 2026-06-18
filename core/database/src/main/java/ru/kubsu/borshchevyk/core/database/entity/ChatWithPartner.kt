package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A data transfer object representing a chat combined with its associated partner user data.
 *
 * This POJO is used to fetch the latest user profile information (name, avatar, etc.)
 * directly from the [UserEntity] table while observing a [ChatEntity].
 *
 * @property chat The embedded chat entity.
 * @property partner The related user entity for the chat partner (for direct chats).
 */
data class ChatWithPartner(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "partnerId",
        entityColumn = "userId"
    )
    val partner: UserEntity?
)
