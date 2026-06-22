package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.kubsu.borshchevyk.core.database.entity.PendingEnvelopeEntity

@Dao
interface PendingEnvelopeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(envelope: PendingEnvelopeEntity)

    @Query("SELECT * FROM pending_envelopes WHERE originEndpointId = :originEndpointId ORDER BY timestamp ASC")
    fun getPendingEnvelopesForUser(originEndpointId: String): List<PendingEnvelopeEntity>

    @Query("DELETE FROM pending_envelopes WHERE envelopeId = :envelopeId")
    fun delete(envelopeId: String)
    
    @Query("DELETE FROM pending_envelopes WHERE originEndpointId = :originEndpointId")
    fun deleteByOrigin(originEndpointId: String)
}
