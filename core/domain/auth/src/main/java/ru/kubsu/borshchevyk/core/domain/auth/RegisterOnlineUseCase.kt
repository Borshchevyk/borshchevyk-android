package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for registering a new user identity in Online (Server) mode.
 *
 * It initiates the network request to register the user, backing up the
 * encrypted private key to the server, and secures the key locally.
 *
 * @property authRepository the repository handling domain logic
 */
class RegisterOnlineUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the online registration.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     * @param tag the user's requested identity tag
     * @return a [Result] containing the server-assigned UUID on success
     */
    suspend operator fun invoke(email: String, password: String, tag: String): Result<String> = runCatching {
        authRepository.registerOnline(email, password, tag)
    }
}
