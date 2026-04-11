package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(request: UpdateProfileRequest): User {
        return userRepository.updateProfile(request)
    }
}
