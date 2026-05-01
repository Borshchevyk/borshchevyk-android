package ru.kubsu.borshchevyk.core.model.domain

/**
 * Request payload for updating user profile information.
 *
 * @property firstName The user's new first name, if changed.
 * @property lastName The user's new last name, if changed.
 * @property bio A short biography or status message.
 * @property avatarUrl A direct URL to the newly set avatar image.
 */
data class DomainUpdateProfileParam(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null
)

/**
 * Request payload specifically for updating the user's avatar.
 *
 * @property avatarUrl The direct URL to the new avatar image.
 */
data class DomainUpdateAvatarParam(
    val avatarUrl: String
)

/**
 * Request payload for updating the user's privacy settings.
 *
 * @property emailVisibility Defines who can see the user's email.
 * @property searchByEmailVisibility Defines who can find the user by their email.
 * @property profilePhotoVisibility Defines who can see the user's profile photo.
 * @property inviteToChatVisibility Defines who can invite the user to new chats.
 */
data class DomainUpdatePrivacySettingsParam(
    val emailVisibility: Visibility? = null,
    val searchByEmailVisibility: Visibility? = null,
    val profilePhotoVisibility: Visibility? = null,
    val inviteToChatVisibility: Visibility? = null
)

/**
 * Request payload for adding a new contact to the user's contact list.
 *
 * @property targetUserId The unique ID of the user being added as a contact.
 * @property firstName The local display first name for the contact.
 * @property lastName The local display last name for the contact.
 */
data class DomainAddContactParam(
    val targetUserId: String,
    val firstName: String,
    val lastName: String? = null
)
