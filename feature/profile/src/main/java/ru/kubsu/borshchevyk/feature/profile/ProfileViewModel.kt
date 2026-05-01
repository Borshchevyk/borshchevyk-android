package ru.kubsu.borshchevyk.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import ru.kubsu.borshchevyk.core.domain.auth.GetTagUseCase
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.auth.LogoutUseCase
import ru.kubsu.borshchevyk.core.domain.message.UploadAvatarUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetPrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetUserProfileUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdateAvatarUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdatePrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdateProfileUseCase
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview

/**
 * ViewModel for managing the user's profile screen.
 * Handles loading user data, privacy settings, updating profile info, avatar, and logout.
 *
 * @property getUserIdUseCase Use case to retrieve the current user's ID.
 * @property getTagUseCase Use case to retrieve the current user's tag.
 * @property getUserProfileUseCase Use case to fetch the user's profile data.
 * @property updateProfileUseCase Use case to update the user's profile information.
 * @property uploadAvatarUseCase Use case to upload a new avatar image.
 * @property updateAvatarUseCase Use case to update the avatar URL in the user's profile.
 * @property getPrivacySettingsUseCase Use case to fetch current privacy settings.
 * @property updatePrivacySettingsUseCase Use case to update privacy settings on the server.
 * @property logoutUseCase Use case to log the user out of the application.
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class ProfileViewModel @Inject constructor(
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getTagUseCase: GetTagUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val updateAvatarUseCase: UpdateAvatarUseCase,
    private val getPrivacySettingsUseCase: GetPrivacySettingsUseCase,
    private val updatePrivacySettingsUseCase: UpdatePrivacySettingsUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    
    /**
     * The single source of truth for the profile screen's UI state.
     */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    
    /**
     * A flow of one-time events (effects) such as navigation or showing error messages.
     */
    val effect = _effect.receiveAsFlow()

    private val privacyUpdateFlow = MutableSharedFlow<PrivacySettings>(replay = 1)

    init {
        loadData()
        observePrivacyUpdates()
    }

    /**
     * Observes updates to privacy settings.
     */
    private fun observePrivacyUpdates() {
        privacyUpdateFlow
            .debounce(1000L)
            .onEach { settings ->
                performUpdatePrivacy(settings)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles UI intents.
     *
     * @param intent The intent to handle.
     */
    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Refresh -> loadData(isRefreshing = true)
            is ProfileIntent.UpdateProfile -> onUpdateProfile(intent.firstName, intent.lastName, intent.bio)
            is ProfileIntent.UpdateAvatar -> onUpdateAvatar(intent.fileBytes, intent.filename, intent.contentType)
            is ProfileIntent.UpdatePrivacy -> onUpdatePrivacy(
                intent.emailVisibility,
                intent.searchByEmailVisibility,
                intent.profilePhotoVisibility,
                intent.inviteToChatVisibility
            )
            is ProfileIntent.Logout -> onLogout()
            is ProfileIntent.OpenEditProfile -> sendEffect(ProfileEffect.NavigateToEditProfile)
            is ProfileIntent.OpenEditPrivacy -> sendEffect(ProfileEffect.NavigateToEditPrivacy)
            is ProfileIntent.ConsumeEffect -> { /* Handled by Flow collection */ }
        }
    }

    /**
     * Loads profile data.
     *
     * @param isRefreshing Whether the loading is triggered by a refresh action.
     */
    private fun loadData(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefreshing, isRefreshing = isRefreshing) }
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val tag = getTagUseCase().firstOrNull() ?: ""
                
                val profile = getUserProfileUseCase(userId.ifBlank { tag })
                val settings = getPrivacySettingsUseCase()
                
                _uiState.update { 
                    it.copy(
                        userId = userId, 
                        user = profile, 
                        privacySettings = settings,
                        isLoading = false,
                        isRefreshing = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to load profile"))
            }
        }
    }

    /**
     * Updates the user's avatar by uploading the file and updating the profile.
     *
     * @param fileBytes The byte array of the image file.
     * @param filename The name of the file being uploaded.
     * @param contentType The MIME type of the file (e.g., "image/jpeg").
     */
    private fun onUpdateAvatar(fileBytes: ByteArray, filename: String, contentType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAvatar = true) }
            try {
                val url = uploadAvatarUseCase(fileBytes, filename, contentType)
                val updatedUser = updateAvatarUseCase(url)
                _uiState.update { it.copy(user = updatedUser, isUpdatingAvatar = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingAvatar = false) }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to update avatar"))
            }
        }
    }

    /**
     * Updates the user's profile information.
     *
     * @param firstName The user's new first name.
     * @param lastName The user's new last name.
     * @param bio The user's new biography or description.
     */
    private fun onUpdateProfile(firstName: String, lastName: String, bio: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updatedUser = updateProfileUseCase(
                    firstName = firstName, 
                    lastName = lastName.ifBlank { null }, 
                    bio = bio.ifBlank { null }, 
                    avatarUrl = null
                )
                _uiState.update { it.copy(user = updatedUser, isLoading = false) }
                sendEffect(ProfileEffect.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to update profile"))
            }
        }
    }

    /**
     * Updates the user's privacy settings optimistically and emits to the update flow.
     *
     * @param emailVisibility The new visibility setting for the user's email.
     * @param searchByEmailVisibility The new visibility setting for searching the user by email.
     * @param profilePhotoVisibility The new visibility setting for the user's profile photo.
     * @param inviteToChatVisibility The new visibility setting for who can invite the user to a chat.
     */
    private fun onUpdatePrivacy(
        emailVisibility: Visibility?,
        searchByEmailVisibility: Visibility?,
        profilePhotoVisibility: Visibility?,
        inviteToChatVisibility: Visibility?
    ) {
        val currentSettings = _uiState.value.privacySettings ?: return
        val newSettings = currentSettings.copy(
            emailVisibility = emailVisibility ?: currentSettings.emailVisibility,
            searchByEmailVisibility = searchByEmailVisibility ?: currentSettings.searchByEmailVisibility,
            profilePhotoVisibility = profilePhotoVisibility ?: currentSettings.profilePhotoVisibility,
            inviteToChatVisibility = inviteToChatVisibility ?: currentSettings.inviteToChatVisibility
        )
        
        // Optimistic update
        _uiState.update { it.copy(privacySettings = newSettings) }
        
        viewModelScope.launch {
            privacyUpdateFlow.emit(newSettings)
        }
    }

    /**
     * Performs the network request to update privacy settings.
     *
     * @param settings The new privacy settings to synchronize.
     */
    private suspend fun performUpdatePrivacy(settings: PrivacySettings) {
        try {
            val updatedSettings = updatePrivacySettingsUseCase(
                settings.emailVisibility,
                settings.searchByEmailVisibility,
                settings.profilePhotoVisibility,
                settings.inviteToChatVisibility
            )
            _uiState.update { it.copy(privacySettings = updatedSettings) }
        } catch (e: Exception) {
            sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to sync privacy settings"))
            // Revert on error if needed, but for now we just show error
        }
    }

    /**
     * Logs the user out of the application and clears local data.
     */
    private fun onLogout() {
        viewModelScope.launch {
            try {
                logoutUseCase()
                sendEffect(ProfileEffect.LogoutSuccess)
            } catch(e: Exception) {
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to logout"))
            }
        }
    }

    /**
     * Sends a one-time effect to the UI.
     *
     * @param effect The effect to be sent and handled by the UI.
     */
    private fun sendEffect(effect: ProfileEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
