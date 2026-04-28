package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

class UpdateAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(avatarUrl: String): User {
        return userRepository.updateAvatar(DomainUpdateAvatarParam(avatarUrl))
    }
}
