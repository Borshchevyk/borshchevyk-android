package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

/**
 * Use case for fetching a specific user's profile information.
 *
 * This use case provides a way to retrieve a user's details based on their unique
 * identifier or their tag (e.g., @username). It handles the communication with the
 * [UserRepository].
 *
 * @property userRepository The repository used to retrieve the user's profile.
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to get a user's profile.
     *
     * @param userIdOrTag The unique identifier or tag of the user.
     * @return The [User] profile data.
     */
    suspend operator fun invoke(userIdOrTag: String): User {
        return userRepository.getUserProfile(userIdOrTag)
    }
}