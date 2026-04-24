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
            is AuthIntent.RegisterOffline -> onRegisterOffline(intent.tag)
            is AuthIntent.RegisterOnline -> onRegisterOnline(intent.email, intent.password, intent.tag, intent.firstName, intent.lastName)
            is AuthIntent.LoginOnline -> onLoginOnline(intent.email, intent.password)
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            if (checkAuthStatus()) {
                _effect.send(AuthEffect.AuthSuccess)
            }
        }
    }

    private fun onRegisterOffline(tag: String) {
        if (tag.isBlank()) {
            sendError("Tag cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            registerOffline(tag)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    _effect.send(AuthEffect.AuthSuccess)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false) }
                    sendError(err.message ?: "Failed to register offline")
                }
        }
    }

    private fun onRegisterOnline(email: String, password: String, tag: String, firstName: String, lastName: String?) {
        if (email.isBlank() || password.isBlank() || tag.isBlank() || firstName.isBlank()) {
            sendError("Mandatory fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            registerOnline(email, password, tag, firstName, lastName)
                .onSuccess {
                    onLoginOnline(email, password)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false) }
                    sendError(err.message ?: "Failed to register online")
                }
        }
    }

    private fun onLoginOnline(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            sendError("Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loginOnline(email, password)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    _effect.send(AuthEffect.AuthSuccess)
                }
                .onFailure { err ->
                    _uiState.update { state -> state.copy(isLoading = false) }
                    sendError(err.message ?: "Failed to login online")
                }
        }
    }
    
    private fun sendError(message: String) {
        viewModelScope.launch {
            _effect.send(AuthEffect.ShowError(message))
        }
    }
}
