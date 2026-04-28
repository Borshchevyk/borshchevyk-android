package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import javax.inject.Inject

class UpdatePrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
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
