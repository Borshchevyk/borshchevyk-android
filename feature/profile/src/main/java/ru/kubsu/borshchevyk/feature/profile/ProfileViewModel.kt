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
import ru.kubsu.borshchevyk.core.domain.auth.GetTagUseCase
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.auth.LogoutUseCase
import ru.kubsu.borshchevyk.core.domain.message.UploadAvatarUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetPrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetUserProfileUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdateAvatarUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdatePrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdateProfileUseCase
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import javax.inject.Inject

@HiltViewModel
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
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadData()
    }

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.ReloadData -> loadData()
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
        }
    }

    private fun onUpdateAvatar(fileBytes: ByteArray, filename: String, contentType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val url = uploadAvatarUseCase(fileBytes, filename, contentType)
                val updatedUser = updateAvatarUseCase(url)
                _uiState.update { it.copy(user = updatedUser, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to update avatar"))
            }
        }
    }

    private fun sendEffect(effect: ProfileEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = getUserIdUseCase().firstOrNull()
                val tag = getTagUseCase().firstOrNull()
                
                val profile = getUserProfileUseCase(userId ?: tag ?: "")
                val settings = getPrivacySettingsUseCase()
                
                _uiState.update { 
                    it.copy(
                        userId = userId ?: "", 
                        user = profile, 
                        privacySettings = settings,
                        isLoading = false 
                    ) 
                }
            } catch (e: Exception) {
                val userId = getUserIdUseCase().firstOrNull()
                val tag = getTagUseCase().firstOrNull()
                _uiState.update { 
                    it.copy(
                        userId = userId ?: "", 
                        user = User(userId = userId ?: "", tag = tag ?: "User"), 
                        isLoading = false
                    )
                }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to load profile"))
            }
        }
    }

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

    private fun onUpdatePrivacy(
        emailVisibility: Visibility?,
        searchByEmailVisibility: Visibility?,
        profilePhotoVisibility: Visibility?,
        inviteToChatVisibility: Visibility?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updatedSettings = updatePrivacySettingsUseCase(
                    emailVisibility, searchByEmailVisibility, profilePhotoVisibility, inviteToChatVisibility
                )
                _uiState.update { it.copy(privacySettings = updatedSettings, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(ProfileEffect.ShowError(e.message ?: "Failed to update privacy settings"))
            }
        }
    }

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
}
