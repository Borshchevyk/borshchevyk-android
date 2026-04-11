package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import javax.inject.Inject

class GetPrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): PrivacySettings {
        return userRepository.getPrivacySettings()
    }
}
