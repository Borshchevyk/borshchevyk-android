package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Defines the standard visibility levels for user profile information.
 */
@Serializable
enum class Visibility {
    /** Visible to anyone on the network. */
    EVERYONE,
    /** Visible only to users in the mutual contact list. */
    CONTACTS,
    /** Hidden from everyone except the user themselves. */
    NOBODY
}

/**
 * Domain model representing a user's privacy preferences.
 *
 * Dictates who can see certain parts of the user's profile and who can interact with them.
 *
 * @property userId The ID of the user these settings belong to.
 * @property emailVisibility Determines who can see the user's email address.
 * @property searchByEmailVisibility Determines who can discover the user via email search.
 * @property profilePhotoVisibility Determines who can view the user's profile picture.
 * @property inviteToChatVisibility Determines who can invite the user to new groups/channels.
 */
@Serializable
data class PrivacySettings(
    val userId: String,
    val emailVisibility: Visibility = Visibility.NOBODY,
    val searchByEmailVisibility: Visibility = Visibility.EVERYONE,
    val profilePhotoVisibility: Visibility = Visibility.EVERYONE,
    val inviteToChatVisibility: Visibility = Visibility.EVERYONE
)
