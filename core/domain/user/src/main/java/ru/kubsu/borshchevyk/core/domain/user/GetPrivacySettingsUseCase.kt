package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import javax.inject.Inject

/**
 * Use case for retrieving the privacy settings of the current user.
 *
 * This use case handles the business logic required to fetch the user's privacy
 * configuration, such as profile visibility or online status rules. It abstracts
 * the interaction with the [UserRepository].
 *
 * @property userRepository The repository used to access user-related data.
 */
class GetPrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to fetch privacy settings.
     *
     * @return The [PrivacySettings] for the current user.
     */
    suspend operator fun invoke(): PrivacySettings {
        return userRepository.getPrivacySettings()
    }
}
