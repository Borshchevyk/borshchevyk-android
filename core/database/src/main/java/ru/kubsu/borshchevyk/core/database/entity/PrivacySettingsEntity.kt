package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.Visibility

/**
 * Database entity representing user privacy settings.
 *
 * @property userId The unique identifier of the user these settings belong to.
 * @property emailVisibility Visibility level for the user's email.
 * @property searchByEmailVisibility Visibility level for finding the user by email.
 * @property profilePhotoVisibility Visibility level for the user's profile photo.
 * @property inviteToChatVisibility Visibility level for who can invite the user to chats.
 */
@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey val userId: String,
    val emailVisibility: Visibility,
    val searchByEmailVisibility: Visibility,
    val profilePhotoVisibility: Visibility,
    val inviteToChatVisibility: Visibility
)

/**
 * Extension to convert [PrivacySettingsEntity] to its domain model [PrivacySettings].
 */
fun PrivacySettingsEntity.toDomain(): PrivacySettings = PrivacySettings(
    userId = userId,
    emailVisibility = emailVisibility,
    searchByEmailVisibility = searchByEmailVisibility,
    profilePhotoVisibility = profilePhotoVisibility,
    inviteToChatVisibility = inviteToChatVisibility
)

/**
 * Extension to convert [PrivacySettings] domain model to its database entity [PrivacySettingsEntity].
 */
fun PrivacySettings.toEntity(): PrivacySettingsEntity = PrivacySettingsEntity(
    userId = userId,
    emailVisibility = emailVisibility,
    searchByEmailVisibility = searchByEmailVisibility,
    profilePhotoVisibility = profilePhotoVisibility,
    inviteToChatVisibility = inviteToChatVisibility
)
