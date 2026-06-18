package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import javax.inject.Inject

/**
 * Use case for observing continuous updates to the current user's privacy settings.
 *
 * This use case provides a reactive stream (Flow) that emits the latest privacy
 * settings for a specific user whenever they change in the local database.
 *
 * @property userRepository The repository used to observe the privacy settings.
 */
class ObservePrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to start observing privacy settings.
     *
     * @param userId The unique identifier of the user.
     * @return A [Flow] that emits the [PrivacySettings] or `null` if not found.
     */
    operator fun invoke(userId: String): Flow<PrivacySettings?> {
        return userRepository.observePrivacySettings(userId)
    }
}
