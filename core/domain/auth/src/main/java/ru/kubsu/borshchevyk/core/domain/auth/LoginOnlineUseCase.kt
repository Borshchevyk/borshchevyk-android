package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for authenticating an existing user via the global server.
 *
 * Handles fetching the user's encrypted private key backup, verifying
 * the password by decrypting it, satisfying the server's cryptographic
 * challenge, and storing the resulting tokens.
 *
 * @property authRepository The repository handling authentication and user identity data.
 */
class LoginOnlineUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the online login process.
     *
     * @param email The user's email address.
     * @param password The user's plaintext password.
     * @return A [Result] containing the server-assigned UUID on success.
     */
    suspend operator fun invoke(email: String, password: String): Result<String> = runCatching {
        authRepository.loginOnline(email, password)
    }
}
