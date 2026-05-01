package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

/**
 * Use case for updating the profile information of the current user.
 *
 * This use case handles the business logic for modifying user details such as
 * name, bio, and avatar. It interacts with the [UserRepository] to apply
 * and persist these changes.
 *
 * @property userRepository The repository used to manage the user's profile data.
 */
class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to update the user's profile.
     *
     * Only the provided parameters will be updated. Depending on the backend
     * implementation, passing null might signify no change or clearing the field.
     *
     * @param firstName The new first name for the user.
     * @param lastName The new last name for the user.
     * @param bio The new biography text for the user.
     * @param avatarUrl The new URL or path to the user's avatar.
     * @return The updated [User] profile reflecting the changes.
     */
    suspend operator fun invoke(firstName: String?, lastName: String?, bio: String?, avatarUrl: String?): User {
        return userRepository.updateProfile(
            DomainUpdateProfileParam(
                firstName = firstName,
                lastName = lastName,
                bio = bio,
                avatarUrl = avatarUrl
            )
        )
    }
}
