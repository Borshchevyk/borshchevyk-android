package ru.kubsu.borshchevyk.core.domain.auth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserIdUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<String?> {
        return authRepository.userId
    }
}
