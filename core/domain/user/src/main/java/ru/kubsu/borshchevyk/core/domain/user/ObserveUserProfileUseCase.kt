package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: String): Flow<User?> {
        return userRepository.observeUserProfile(userId)
    }
}
