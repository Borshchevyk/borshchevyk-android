package ru.kubsu.borshchevyk.core.domain.auth

import javax.inject.Inject

/**
 * UseCase for registering a new user identity in Offline (Mesh) mode.
 *
 * This operation is fully local and ensures the required cryptographic
 * material is provisioned within the Android Keystore.
 *
 * @property authRepository The repository handling authentication and user identity data.
 */
class RegisterOfflineUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the offline registration.
     *
     * @param tag The user's requested identity tag.
     * @return A [Result] containing the generated local ID on success.
     */
    suspend operator fun invoke(tag: String): Result<String> = runCatching {
        authRepository.registerOffline(tag)
    }
}
