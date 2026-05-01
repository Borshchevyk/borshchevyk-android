package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

/**
 * Use case for observing continuous updates to a user's profile.
 *
 * This use case provides a reactive stream (Flow) that emits the latest profile
 * data for a specific user whenever it changes in the underlying data source
 * (e.g., local database or cache).
 *
 * @property userRepository The repository used to observe the user profile data.
 */
class ObserveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to start observing a user's profile.
     *
     * @param userId The unique identifier of the user to observe.
     * @return A [Flow] that emits the [User] profile or `null` if the user is not found.
     */
    operator fun invoke(userId: String): Flow<User?> {
        return userRepository.observeUserProfile(userId)
    }
}
