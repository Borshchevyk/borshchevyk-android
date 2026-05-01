package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase to determine if the user has an active identity or session.
 *
 * This is primarily used during app startup to route the user either to
 * the Auth screen or directly to the Messenger interface.
 *
 * @property authRepository The repository handling authentication and user identity data.
 */
class CheckAuthStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Evaluates the current authentication state.
     *
     * @return `true` if a session or local identity exists, `false` otherwise.
     */
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}
