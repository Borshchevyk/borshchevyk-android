package ru.kubsu.borshchevyk.feature.auth

import ru.kubsu.borshchevyk.core.model.domain.AuthMode

sealed interface AuthIntent {
    data object CheckAuth : AuthIntent
    data object ToggleAuthMode : AuthIntent
    data object ToggleLoginRegister : AuthIntent
    
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class FirstNameChanged(val value: String) : AuthIntent
    data class LastNameChanged(val value: String) : AuthIntent
    data class TagChanged(val value: String) : AuthIntent
    
    data object Submit : AuthIntent
    data object ClearError : AuthIntent
}

sealed interface AuthEffect {
    data object AuthSuccess : AuthEffect
    data class ShowError(val message: String) : AuthEffect
}

data class AuthUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val firstName: String = "",
    val firstNameError: String? = null,
    val lastName: String = "",
    val tag: String = "",
    val tagError: String? = null,
    
    val mode: AuthMode = AuthMode.ONLINE,
    val isLogin: Boolean = true,
    val isLoading: Boolean = false,
    val generalError: String? = null
)
