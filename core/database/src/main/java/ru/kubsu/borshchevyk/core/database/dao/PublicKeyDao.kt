package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.kubsu.borshchevyk.core.database.entity.PublicKeyEntity

@Dao
interface PublicKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertPublicKey(entity: PublicKeyEntity): Long

    @Query("SELECT publicKey FROM public_keys WHERE userId = :userId")
    @JvmSuppressWildcards
    suspend fun getPublicKey(userId: String): String?
}
