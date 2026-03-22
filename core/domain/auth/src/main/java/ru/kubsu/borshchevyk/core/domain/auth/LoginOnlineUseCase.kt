package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for authenticating an existing user via the global server.
 *
 * Handles fetching the user's encrypted private key backup, verifying
 * the password by decrypting it, satisfying the server's cryptographic
 * challenge, and storing the resulting tokens.
 *
 * @property authRepository the repository handling domain logic
 */
class LoginOnlineUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the online login process.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     * @return a [Result] containing the server-assigned UUID on success
     */
    suspend operator fun invoke(email: String, password: String): Result<String> = runCatching {
        authRepository.loginOnline(email, password)
    }
}
