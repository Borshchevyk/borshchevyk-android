package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.core.model.domain.Visibility

@Serializable
data class ShortUserDto(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
)

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

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class UpdateAvatarRequest(
    val avatarUrl: String
)

@Serializable
data class PrivacySettingsResponse(
    val userId: String,
    val emailVisibility: Visibility,
    val searchByEmailVisibility: Visibility,
    val profilePhotoVisibility: Visibility,
    val inviteToChatVisibility: Visibility
)

@Serializable
data class UpdatePrivacySettingsRequest(
    val emailVisibility: Visibility? = null,
    val searchByEmailVisibility: Visibility? = null,
    val profilePhotoVisibility: Visibility? = null,
    val inviteToChatVisibility: Visibility? = null
)
