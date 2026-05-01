package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for registering a new user identity in Online (Server) mode.
 *
 * It initiates the network request to register the user, backing up the
 * encrypted private key to the server, and secures the key locally.
 *
 * @property authRepository The repository handling authentication and user identity data.
 */
class RegisterOnlineUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the online registration.
     *
     * @param email The user's email address.
     * @param password The user's plaintext password.
     * @param tag The user's requested identity tag.
     * @param firstName The user's first name.
     * @param lastName The user's last name (optional).
     * @return A [Result] containing the server-assigned UUID on success.
     */
    suspend operator fun invoke(email: String, password: String, tag: String, firstName: String, lastName: String?): Result<String> = runCatching {
        authRepository.registerOnline(email, password, tag, firstName, lastName)
    }
}
