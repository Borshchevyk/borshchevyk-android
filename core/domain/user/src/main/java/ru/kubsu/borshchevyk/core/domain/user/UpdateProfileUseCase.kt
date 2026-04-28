package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
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
