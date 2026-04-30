package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user in the application, storing profile information and identity.
 *
 * @property userId The unique identifier for the user.
 * @property email The user's email address (optional).
 * @property tag The user's unique identifier tag (e.g., @username).
 * @property firstName The user's first name (optional).
 * @property lastName The user's last name (optional).
 * @property bio The user's profile biography (optional).
 * @property avatarUrl The primary avatar image URL (optional).
 * @property avatars A list of additional avatar images for the user profile.
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
