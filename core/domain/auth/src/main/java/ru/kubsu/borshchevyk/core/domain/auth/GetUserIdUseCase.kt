package ru.kubsu.borshchevyk.core.domain.auth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to retrieve a continuous stream of the current user's unique identifier.
 *
 * This UseCase encapsulates the business logic of observing the user's ID from the repository.
 * It is commonly used when the current user's ID is required for data queries or relationship mapping.
 *
 * @property authRepository The repository handling authentication data.
 */
class GetUserIdUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Invokes the use case to get the user ID flow.
     *
     * @return A [Flow] emitting the user's ID as a [String], or `null` if no user is authenticated.
     */
    operator fun invoke(): Flow<String?> {
        return authRepository.userId
    }
}
