package ru.kubsu.borshchevyk.feature.auth

import ru.kubsu.borshchevyk.core.model.domain.AuthMode

/**
 * Represents the MVI intents (user actions or system events) for the authentication feature.
 * These intents are processed by the [AuthViewModel] to update the [AuthUiState] or trigger side effects.
 */
sealed interface AuthIntent {
    /** Intent to verify if the user is already authenticated. */
    data object CheckAuth : AuthIntent
    /** Intent to toggle between Online and Mesh authentication modes. */
    data object ToggleAuthMode : AuthIntent
    /** Intent to switch between Login and Registration forms. */
    data object ToggleLoginRegister : AuthIntent
    
    /** Intent triggered when the user types in the email field. */
    data class EmailChanged(val value: String) : AuthIntent
    /** Intent triggered when the user types in the password field. */
    data class PasswordChanged(val value: String) : AuthIntent
    /** Intent triggered when the user types in the first name field. */
    data class FirstNameChanged(val value: String) : AuthIntent
    /** Intent triggered when the user types in the last name field. */
    data class LastNameChanged(val value: String) : AuthIntent
    /** Intent triggered when the user types in the unique tag field. */
    data class TagChanged(val value: String) : AuthIntent
    
    /** Intent to submit the authentication form. */
    data object Submit : AuthIntent
    /** Intent to dismiss the general error message. */
    data object ClearError : AuthIntent
}

/**
 * Represents one-time side effects (MVI events) in the authentication feature.
 * Used for actions like navigation or showing transient UI components.
 */
sealed interface AuthEffect {
    /** Emitted when authentication is successfully completed. */
    data object AuthSuccess : AuthEffect
    /** Emitted when an error should be presented to the user. */
    data class ShowError(val message: String) : AuthEffect
}

/**
 * Represents the entire state of the authentication screen in the MVI architecture.
 *
 * @property email The current email input.
 * @property emailError Validation error message for the email input.
 * @property password The current password input.
 * @property passwordError Validation error message for the password input.
 * @property firstName The current first name input (used in registration).
 * @property firstNameError Validation error message for the first name input.
 * @property lastName The current last name input (used in registration).
 * @property tag The unique user tag input (used in both mesh and online registration).
 * @property tagError Validation error message for the tag input.
 * @property mode Current authentication mode (e.g., Online or Mesh/Offline).
 * @property isLogin Indicates if the user is in the login state or registration state.
 * @property isLoading Indicates if an authentication request is currently in progress.
 * @property generalError General error message to display on the screen, if any.
 */
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
