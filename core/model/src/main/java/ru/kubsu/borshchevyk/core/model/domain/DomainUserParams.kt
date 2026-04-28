package ru.kubsu.borshchevyk.core.model.domain

data class DomainUpdateProfileParam(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null
)

data class DomainUpdateAvatarParam(
    val avatarUrl: String
)

data class DomainUpdatePrivacySettingsParam(
    val emailVisibility: Visibility? = null,
    val searchByEmailVisibility: Visibility? = null,
    val profilePhotoVisibility: Visibility? = null,
    val inviteToChatVisibility: Visibility? = null
)

data class DomainAddContactParam(
    val targetUserId: String,
    val firstName: String,
    val lastName: String? = null
)
