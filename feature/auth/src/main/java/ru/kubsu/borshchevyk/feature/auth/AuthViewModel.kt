package ru.kubsu.borshchevyk.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.auth.CheckAuthStatusUseCase
import ru.kubsu.borshchevyk.core.domain.auth.LoginOnlineUseCase
import ru.kubsu.borshchevyk.core.domain.auth.RegisterOfflineUseCase
import ru.kubsu.borshchevyk.core.domain.auth.RegisterOnlineUseCase
import javax.inject.Inject

/**
 * Represents the distinct states of the Auth UI.
 *
 * @property isLoading whether a network or cryptographic operation is in progress
 * @property error an optional error message to display to the user
 * @property success whether the authentication flow completed successfully
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * ViewModel orchestrating the authentication screens.
 *
 * Responsible for delegating user intent from the UI to the domain layer
 * via UseCases. It maintains the view state in a thread-safe manner using [StateFlow].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerOffline: RegisterOfflineUseCase,
    private val registerOnline: RegisterOnlineUseCase,
    private val loginOnline: LoginOnlineUseCase,
    private val checkAuthStatus: CheckAuthStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    
    /** Public read-only state for Jetpack Compose observation. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Evaluates the startup authentication state.
     * Called when the UI is first composed.
     */
    fun checkAuth() {
        viewModelScope.launch {
            if (checkAuthStatus()) {
                _uiState.update { it.copy(success = true) }
            }
        }
    }

    /**
     * Handles the user's request to register locally for Mesh networks.
     *
     * @param tag the user's requested identity tag
     */
    fun onRegisterOffline(tag: String) {
        if (tag.isBlank()) {
            _uiState.update { it.copy(error = "Tag cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            registerOffline(tag)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false, success = true) }
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, error = err.message ?: "Failed to register offline") }
                }
        }
    }

    /**
     * Handles the user's request to create a new online account.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     * @param tag the user's requested identity tag
     */
    fun onRegisterOnline(email: String, password: String, tag: String) {
        if (email.isBlank() || password.isBlank() || tag.isBlank()) {
            _uiState.update { it.copy(error = "Fields cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            registerOnline(email, password, tag)
                .onSuccess {
                    // Registration successful, now login to satisfy the challenge and get tokens
                    onLoginOnline(email, password)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, error = err.message ?: "Failed to register online") }
                }
        }
    }

    /**
     * Handles the user's request to login to an existing online account.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     */
    fun onLoginOnline(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Fields cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginOnline(email, password)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false, success = true) }
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, error = err.message ?: "Failed to login online") }
                }
        }
    }
    
    /**
     * Clears the current error state after it has been displayed (e.g., via Snackbar).
     */
    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
