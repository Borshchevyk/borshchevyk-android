package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

/**
 * Use case for updating the current user's avatar.
 *
 * This use case handles the business logic involved in changing the user's profile
 * picture. It encapsulates the interaction with the [UserRepository] to persist
 * the new avatar.
 *
 * @property userRepository The repository used to update the user's profile data.
 */
class UpdateAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to update the user's avatar.
     *
     * @param avatarUrl The URL or path to the new avatar image.
     * @return The updated [User] profile reflecting the new avatar.
     */
    suspend operator fun invoke(avatarUrl: String): User {
        return userRepository.updateAvatar(DomainUpdateAvatarParam(avatarUrl))
    }
}
