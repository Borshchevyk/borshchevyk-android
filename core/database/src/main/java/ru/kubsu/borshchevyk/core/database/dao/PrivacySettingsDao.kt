package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.PrivacySettingsEntity

/**
 * Data Access Object for [PrivacySettingsEntity].
 */
@Dao
interface PrivacySettingsDao {
    /**
     * Observes privacy settings for a specific user.
     */
    @Query("SELECT * FROM privacy_settings WHERE userId = :userId")
    fun observePrivacySettings(userId: String): Flow<PrivacySettingsEntity?>

    /**
     * Retrieves privacy settings for a specific user.
     */
    @Query("SELECT * FROM privacy_settings WHERE userId = :userId")
    fun getPrivacySettings(userId: String): PrivacySettingsEntity?

    /**
     * Updates or inserts privacy settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPrivacySettings(settings: PrivacySettingsEntity)

    /**
     * Deletes privacy settings for a specific user.
     */
    @Query("DELETE FROM privacy_settings WHERE userId = :userId")
    fun deletePrivacySettings(userId: String)
}
