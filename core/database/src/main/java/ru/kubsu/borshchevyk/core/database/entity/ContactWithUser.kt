package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A data transfer object representing a contact combined with its associated user profile data.
 *
 * This POJO is used to fetch the latest user profile information (name, avatar, etc.)
 * directly from the [UserEntity] table while observing a [ContactEntity].
 *
 * @property contact The embedded contact entity.
 * @property user The related user entity for the contact.
 */
data class ContactWithUser(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "contact_user_id",
        entityColumn = "userId"
    )
    val user: UserEntity?
)
