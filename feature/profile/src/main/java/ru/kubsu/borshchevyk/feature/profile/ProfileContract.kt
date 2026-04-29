package ru.kubsu.borshchevyk.feature.profile

import androidx.compose.runtime.Immutable
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.domain.Visibility

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data class UpdateProfile(val firstName: String, val lastName: String, val bio: String) : ProfileIntent
    data class UpdateAvatar(val fileBytes: ByteArray, val filename: String, val contentType: String) : ProfileIntent
    data class UpdatePrivacy(
        val emailVisibility: Visibility? = null,
        val searchByEmailVisibility: Visibility? = null,
        val profilePhotoVisibility: Visibility? = null,
        val inviteToChatVisibility: Visibility? = null
    ) : ProfileIntent
    data object Logout : ProfileIntent
    data object OpenEditProfile : ProfileIntent
    data object OpenEditPrivacy : ProfileIntent
    data object ConsumeEffect : ProfileIntent
}

sealed interface ProfileEffect {
    data class ShowError(val message: String) : ProfileEffect
    data object NavigateBack : ProfileEffect
    data object NavigateToEditProfile : ProfileEffect
    data object NavigateToEditPrivacy : ProfileEffect
    data object LogoutSuccess : ProfileEffect
}

@Immutable
data class ProfileUiState(
    val userId: String = "",
    val user: User? = null,
    val privacySettings: PrivacySettings? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUpdatingAvatar: Boolean = false
) {
    val displayName: String
        get() = user?.let {
            val full = "${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
            full.ifBlank { it.tag }
        } ?: "User"

    val initial: String
        get() = displayName.firstOrNull()?.uppercase() ?: "?"
}
