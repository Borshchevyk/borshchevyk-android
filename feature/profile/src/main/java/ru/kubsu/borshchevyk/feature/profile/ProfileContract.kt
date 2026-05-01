package ru.kubsu.borshchevyk.feature.profile

import androidx.compose.runtime.Immutable
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.domain.Visibility

/**
 * Represents the intents (actions) that can be triggered from the Profile UI.
 */
sealed interface ProfileIntent {
    /** Refreshes the profile and privacy settings data. */
    data object Refresh : ProfileIntent
    
    /**
     * Triggers an update to the user's basic profile information.
     * @param firstName The user's new first name.
     * @param lastName The user's new last name.
     * @param bio The user's new biography/status.
     */
    data class UpdateProfile(val firstName: String, val lastName: String, val bio: String) : ProfileIntent
    
    /**
     * Triggers an upload of a new avatar image.
     * @param fileBytes The byte array of the image file.
     * @param filename The name of the image file.
     * @param contentType The MIME type of the image.
     */
    data class UpdateAvatar(val fileBytes: ByteArray, val filename: String, val contentType: String) : ProfileIntent
    
    /**
     * Triggers an update to the user's privacy settings.
     * @param emailVisibility Who can see the user's email.
     * @param searchByEmailVisibility Who can search for the user by email.
     * @param profilePhotoVisibility Who can see the user's profile photo.
     * @param inviteToChatVisibility Who can invite the user to chats.
     */
    data class UpdatePrivacy(
        val emailVisibility: Visibility? = null,
        val searchByEmailVisibility: Visibility? = null,
        val profilePhotoVisibility: Visibility? = null,
        val inviteToChatVisibility: Visibility? = null
    ) : ProfileIntent
    
    /** Initiates the user logout process. */
    data object Logout : ProfileIntent
    
    /** Requests navigation to the Edit Profile screen. */
    data object OpenEditProfile : ProfileIntent
    
    /** Requests navigation to the Edit Privacy screen. */
    data object OpenEditPrivacy : ProfileIntent
    
    /** Acknowledges that an effect has been consumed. */
    data object ConsumeEffect : ProfileIntent
}

/**
 * Represents one-time side effects (like navigation or showing a toast) originating from the ProfileViewModel.
 */
sealed interface ProfileEffect {
    /** Shows an error message to the user. */
    data class ShowError(val message: String) : ProfileEffect
    
    /** Navigates back to the previous screen. */
    data object NavigateBack : ProfileEffect
    
    /** Navigates to the Edit Profile screen. */
    data object NavigateToEditProfile : ProfileEffect
    
    /** Navigates to the Edit Privacy screen. */
    data object NavigateToEditPrivacy : ProfileEffect
    
    /** Emitted when the user successfully logs out. */
    data object LogoutSuccess : ProfileEffect
}

/**
 * Represents the immutable UI state of the Profile screen.
 *
 * @property userId The ID of the current user.
 * @property user The user's profile data.
 * @property privacySettings The user's privacy configurations.
 * @property isLoading Indicates if a blocking load operation is in progress.
 * @property isRefreshing Indicates if a background refresh is in progress.
 * @property isUpdatingAvatar Indicates if the avatar is currently being uploaded.
 */
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
