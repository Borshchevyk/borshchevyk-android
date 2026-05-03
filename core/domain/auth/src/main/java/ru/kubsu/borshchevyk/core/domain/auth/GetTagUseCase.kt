package ru.kubsu.borshchevyk.core.domain.auth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to retrieve a continuous stream of the current user's tag (username).
 *
 * This UseCase encapsulates the business logic of observing the user's tag from the repository.
 * It is useful for UI components that need to display the user's tag and react to changes.
 *
 * @property authRepository The repository handling authentication data.
 */
class GetTagUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Invokes the use case to get the user's tag flow.
     *
     * @return A [Flow] emitting the user's tag as a [String], or `null` if no user is authenticated.
     */
    operator fun invoke(): Flow<String?> {
        return authRepository.tag
    }
}
