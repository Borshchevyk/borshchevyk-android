package ru.kubsu.borshchevyk.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.auth.GetTagUseCase
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.auth.LogoutUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetPrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetUserProfileUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdatePrivacySettingsUseCase
import ru.kubsu.borshchevyk.core.domain.user.UpdateProfileUseCase
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import javax.inject.Inject

data class ProfileUiState(
    val userId: String = "",
    val user: User? = null,
    val privacySettings: PrivacySettings? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getTagUseCase: GetTagUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getPrivacySettingsUseCase: GetPrivacySettingsUseCase,
    private val updatePrivacySettingsUseCase: UpdatePrivacySettingsUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun onUpdateProfile(firstName: String, lastName: String, bio: String) {
        viewModelScope.launch {
            try {
                val updatedUser = updateProfileUseCase(
                    UpdateProfileRequest(firstName = firstName, lastName = lastName, bio = bio)
                )
                _uiState.update { it.copy(user = updatedUser) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onUpdatePrivacy(request: UpdatePrivacySettingsRequest) {
        viewModelScope.launch {
            try {
                val updatedSettings = updatePrivacySettingsUseCase(request)
                _uiState.update { it.copy(privacySettings = updatedSettings) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
