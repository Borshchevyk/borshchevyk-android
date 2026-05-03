package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import javax.inject.Inject

/**
 * Use case for updating the privacy settings of the current user.
 *
 * This use case encapsulates the business logic for modifying how the user's
 * profile and contact information are exposed to others in the network.
 * It interacts with the [UserRepository] to persist these changes.
 *
 * @property userRepository The repository used to manage the user's data.
 */
class UpdatePrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Executes the use case to update privacy settings.
     *
     * Only the provided non-null parameters will be updated. Null values
     * indicate that the corresponding setting should remain unchanged.
     *
     * @param emailVisibility Defines who can see the user's email address.
     * @param searchByEmailVisibility Defines who can find the user by searching for their email.
     * @param profilePhotoVisibility Defines who can view the user's profile photo.
     * @param inviteToChatVisibility Defines who can invite the user to a chat.
     * @return The updated [PrivacySettings] reflecting the applied changes.
     */
    suspend operator fun invoke(
        emailVisibility: Visibility? = null,
        searchByEmailVisibility: Visibility? = null,
        profilePhotoVisibility: Visibility? = null,
        inviteToChatVisibility: Visibility? = null
    ): PrivacySettings {
        return userRepository.updatePrivacySettings(
            DomainUpdatePrivacySettingsParam(
                emailVisibility = emailVisibility,
                searchByEmailVisibility = searchByEmailVisibility,
                profilePhotoVisibility = profilePhotoVisibility,
                inviteToChatVisibility = inviteToChatVisibility
            )
        )
    }
}
