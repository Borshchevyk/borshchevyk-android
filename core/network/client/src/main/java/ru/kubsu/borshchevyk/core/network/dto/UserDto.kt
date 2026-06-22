package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.Visibility

/**
 * Summarized details of a user, typically used in lists or as references in other models.
 *
 * @property id The unique identifier of the user.
 * @property firstName The first name of the user.
 * @property lastName The last name of the user.
 * @property tag The user's handle or username.
 * @property avatarUrl The URL to the user's avatar image.
 */
@Serializable
data class ShortUserDto(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
)

val ShortUserDto.displayName: String
    get() = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim().ifBlank { displayTag }

val ShortUserDto.displayTag: String
    get() {
        val cleanTag = tag?.removePrefix("offline_user_")
        return cleanTag?.let { if (it.startsWith("@")) it else "@$it" } ?: "@unknown"
    }

/**
 * Comprehensive details about a user's profile.
 *
 * @property userId The unique identifier of the user.
 * @property email The email address of the user, visible based on privacy settings.
 * @property tag The user's unique handle.
 * @property firstName The first name of the user.
 * @property lastName The last name of the user.
 * @property bio A short biography or status message.
 * @property avatarUrl The URL of the user's current active avatar.
 * @property avatars A list of URLs or identifiers for all uploaded avatars.
 */
@Serializable
data class UserProfileResponse(
    val userId: String,
    val email: String? = null,
    val tag: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val avatars: List<String> = emptyList()
)

/**
 * Request object used to update basic profile information.
 *
 * @property firstName The new first name.
 * @property lastName The new last name.
 * @property bio The new biography text.
 * @property avatarUrl The new avatar URL to set as active.
 */
@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null
)

/**
 * Request object used to update the user's active avatar.
 *
 * @property avatarUrl The URL or key of the new avatar.
 */
@Serializable
data class UpdateAvatarRequest(
    val avatarUrl: String
)

/**
 * Response object containing the current privacy settings of a user.
 *
 * @property userId The unique identifier of the user.
 * @property emailVisibility Who can see the user's email address.
 * @property searchByEmailVisibility Who can find the user by searching their email.
 * @property profilePhotoVisibility Who can view the user's profile photo.
 * @property inviteToChatVisibility Who can invite the user to groups or chats.
 */
@Serializable
data class PrivacySettingsResponse(
    val userId: String,
    val emailVisibility: Visibility,
    val searchByEmailVisibility: Visibility,
    val profilePhotoVisibility: Visibility,
    val inviteToChatVisibility: Visibility
)

/**
 * Request object used to update a user's privacy preferences.
 *
 * @property emailVisibility The new visibility level for the email address.
 * @property searchByEmailVisibility The new visibility level for email searchability.
 * @property profilePhotoVisibility The new visibility level for the profile photo.
 * @property inviteToChatVisibility The new visibility level for chat invitations.
 */
@Serializable
data class UpdatePrivacySettingsRequest(
    val emailVisibility: Visibility? = null,
    val searchByEmailVisibility: Visibility? = null,
    val profilePhotoVisibility: Visibility? = null,
    val inviteToChatVisibility: Visibility? = null
)
