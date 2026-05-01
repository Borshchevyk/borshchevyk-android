package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user within the Borshchevyk ecosystem, storing profile information and identity.
 *
 * This entity stores details about interlocutors, friends, and the local user themselves.
 * User profiles can originate from the global centralized server or be discovered organically
 * via nearby peers in a P2P mesh network.
 *
 * @property userId The unique identifier for the user.
 * @property email The user's email address (optional, may not be available for P2P-only peers).
 * @property tag The user's unique identifier tag or handle (e.g., @username).
 * @property firstName The user's chosen first name (optional).
 * @property lastName The user's chosen last name (optional).
 * @property bio The user's profile biography or "about me" section (optional).
 * @property avatarUrl The URL or local cache key for the primary avatar image (optional).
 * @property avatars A list of URLs or keys for additional historical or alternative avatar images.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val email: String?,
    val tag: String,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val avatars: List<String>
)
