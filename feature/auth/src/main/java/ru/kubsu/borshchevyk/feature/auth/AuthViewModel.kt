package ru.kubsu.borshchevyk.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.auth.CheckAuthStatusUseCase
import ru.kubsu.borshchevyk.core.domain.auth.LoginOnlineUseCase
import ru.kubsu.borshchevyk.core.domain.auth.RegisterOfflineUseCase
import ru.kubsu.borshchevyk.core.domain.auth.RegisterOnlineUseCase
import ru.kubsu.borshchevyk.core.model.domain.AuthMode
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerOffline: RegisterOfflineUseCase,
    private val registerOnline: RegisterOnlineUseCase,
    private val loginOnline: LoginOnlineUseCase,
    private val checkAuthStatus: CheckAuthStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.CheckAuth -> checkAuth()
            is AuthIntent.ToggleAuthMode -> toggleAuthMode()
            is AuthIntent.ToggleLoginRegister -> toggleLoginRegister()
            is AuthIntent.EmailChanged -> _uiState.update { it.copy(email = intent.value, emailError = null) }
            is AuthIntent.PasswordChanged -> _uiState.update { it.copy(password = intent.value, passwordError = null) }
            is AuthIntent.FirstNameChanged -> _uiState.update { it.copy(firstName = intent.value, firstNameError = null) }
            is AuthIntent.LastNameChanged -> _uiState.update { it.copy(lastName = intent.value) }
            is AuthIntent.TagChanged -> _uiState.update { it.copy(tag = intent.value, tagError = null) }
            is AuthIntent.Submit -> submit()
            is AuthIntent.ClearError -> _uiState.update { it.copy(generalError = null) }
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            if (checkAuthStatus()) {
                _effect.send(AuthEffect.AuthSuccess)
            }
        }
    }

    private fun toggleAuthMode() {
        _uiState.update { state ->
            val nextMode = if (state.mode == AuthMode.ONLINE) AuthMode.OFFLINE else AuthMode.ONLINE
            state.copy(mode = nextMode, generalError = null)
        }
    }

    private fun toggleLoginRegister() {
        _uiState.update { it.copy(isLogin = !it.isLogin, generalError = null) }
    }

    private fun submit() {
        if (!validate()) return

        val state = _uiState.value
        when {
            state.mode == AuthMode.OFFLINE -> onRegisterOffline(state.tag)
            state.isLogin -> onLoginOnline(state.email, state.password)
            else -> onRegisterOnline(state.email, state.password, state.tag, state.firstName, state.lastName)
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        val state = _uiState.value

        if (state.mode == AuthMode.ONLINE) {
            if (state.email.isBlank() || !state.email.contains("@")) {
                _uiState.update { it.copy(emailError = "Invalid email address") }
                isValid = false
            }
            if (state.password.length < 6) {
                _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
                isValid = false
            }
            if (!state.isLogin) {
                if (state.firstName.isBlank()) {
                    _uiState.update { it.copy(firstNameError = "First name is required") }
                    isValid = false
                }
                if (state.tag.isBlank()) {
                    _uiState.update { it.copy(tagError = "Unique tag is required") }
                    isValid = false
                }
            }
        } else {
            if (state.tag.isBlank()) {
                _uiState.update { it.copy(tagError = "Unique tag is required") }
                isValid = false
            }
        }

        return isValid
    }

    private fun onRegisterOffline(tag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            registerOffline(tag)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    _effect.send(AuthEffect.AuthSuccess)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, generalError = err.message) }
                    _effect.send(AuthEffect.ShowError(err.message ?: "Failed to register offline"))
                }
        }
    }

    private fun onRegisterOnline(email: String, password: String, tag: String, firstName: String, lastName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            registerOnline(email, password, tag, firstName, lastName.takeIf { it?.isNotBlank() == true })
                .onSuccess {
                    onLoginOnline(email, password)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, generalError = err.message) }
                    _effect.send(AuthEffect.ShowError(err.message ?: "Failed to register online"))
                }
        }
    }

    private fun onLoginOnline(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loginOnline(email, password)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    _effect.send(AuthEffect.AuthSuccess)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false, generalError = err.message) }
                    _effect.send(AuthEffect.ShowError(err.message ?: "Failed to login online"))
                }
        }
    }
}
