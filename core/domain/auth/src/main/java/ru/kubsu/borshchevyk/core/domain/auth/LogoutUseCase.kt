package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for logging out the current user.
 *
 * This UseCase encapsulates the business logic for terminating the active session,
 * whether it's an online connection to the global server or an offline P2P identity.
 * It clears local tokens and sensitive session data via the repository.
 *
 * @property authRepository The repository handling authentication and user identity data.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the logout process.
     */
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
