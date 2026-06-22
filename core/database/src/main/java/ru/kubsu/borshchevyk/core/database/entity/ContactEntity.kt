package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "contact_user_id")
    val contactUserId: String,
    @ColumnInfo(name = "contact_first_name")
    val contactFirstName: String,
    @ColumnInfo(name = "contact_last_name")
    val contactLastName: String?,
    @ColumnInfo(name = "added_at")
    val addedAt: String
)
