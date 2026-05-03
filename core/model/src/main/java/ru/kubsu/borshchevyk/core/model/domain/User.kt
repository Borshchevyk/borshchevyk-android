package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Domain model representing a network user.
 *
 * @property userId The unique identifier for the user.
 * @property email The user's email address, if available and permitted by privacy settings.
 * @property tag A unique, human-readable tag or username (e.g., @johndoe).
 * @property firstName The user's first name.
 * @property lastName The user's last name.
 * @property bio A short biography or status message.
 * @property avatarUrl A direct URL to the user's current profile picture.
 * @property avatars A list of historical or secondary avatar URLs for the user.
 */
@Serializable
data class User(
    val userId: String,
    val email: String? = null,
    val tag: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val avatars: List<String> = emptyList()
)
