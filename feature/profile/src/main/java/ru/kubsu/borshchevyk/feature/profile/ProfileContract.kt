package ru.kubsu.borshchevyk.feature.profile

import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.domain.Visibility

sealed interface ProfileIntent {
    object ReloadData : ProfileIntent
    data class UpdateProfile(val firstName: String, val lastName: String, val bio: String) : ProfileIntent
    data class UpdateAvatar(val fileBytes: ByteArray, val filename: String, val contentType: String) : ProfileIntent
    data class UpdatePrivacy(
        val emailVisibility: Visibility? = null,
        val searchByEmailVisibility: Visibility? = null,
        val profilePhotoVisibility: Visibility? = null,
        val inviteToChatVisibility: Visibility? = null
    ) : ProfileIntent
    object Logout : ProfileIntent
    object OpenEditProfile : ProfileIntent
    object OpenEditPrivacy : ProfileIntent
}

sealed interface ProfileEffect {
    data class ShowError(val message: String) : ProfileEffect
    object NavigateBack : ProfileEffect
    object NavigateToEditProfile : ProfileEffect
    object NavigateToEditPrivacy : ProfileEffect
    object LogoutSuccess : ProfileEffect
}

data class ProfileUiState(
    val userId: String = "",
    val user: User? = null,
    val privacySettings: PrivacySettings? = null,
    val isLoading: Boolean = false
)
