package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.ContactEntity

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE owner_id = :ownerId ORDER BY added_at DESC")
    fun observeContacts(ownerId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE owner_id = :ownerId ORDER BY added_at DESC")
    fun getContacts(ownerId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE owner_id = :ownerId AND contact_user_id = :contactUserId LIMIT 1")
    fun getContact(ownerId: String, contactUserId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE owner_id = :ownerId AND contact_user_id = :contactUserId")
    fun deleteContact(ownerId: String, contactUserId: String)

    @Query("DELETE FROM contacts WHERE owner_id = :ownerId")
    fun clearContacts(ownerId: String)
}
