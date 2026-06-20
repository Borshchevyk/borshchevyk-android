package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.kubsu.borshchevyk.core.database.entity.SyncEventEntity

@Dao
interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSyncEvent(event: SyncEventEntity): Long

    @Query("SELECT * FROM sync_outbox ORDER BY timestamp ASC")
    fun getUnsyncedEvents(): List<SyncEventEntity>

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    fun deleteEvents(ids: List<String>): Int
    
    @Query("DELETE FROM sync_outbox")
    fun clearOutbox(): Int
}
