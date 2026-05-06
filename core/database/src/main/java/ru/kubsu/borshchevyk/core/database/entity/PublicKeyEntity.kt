package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the raw X.509 Base64 encoded public keys for users.
 * Kept separate from [UserEntity] to isolate security credentials from UI profile data.
 */
@Entity(tableName = "public_keys")
data class PublicKeyEntity(
    @PrimaryKey val userId: String,
    val publicKey: String
)
