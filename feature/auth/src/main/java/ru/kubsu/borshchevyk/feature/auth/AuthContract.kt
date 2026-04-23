package ru.kubsu.borshchevyk.feature.auth

sealed interface AuthIntent {
    object CheckAuth : AuthIntent
    data class RegisterOffline(val tag: String) : AuthIntent
    data class RegisterOnline(val email: String, val password: String, val tag: String) : AuthIntent
    data class LoginOnline(val email: String, val password: String) : AuthIntent
}

sealed interface AuthEffect {
    object AuthSuccess : AuthEffect
    data class ShowError(val message: String) : AuthEffect
}

data class AuthUiState(
    val isLoading: Boolean = false
)
