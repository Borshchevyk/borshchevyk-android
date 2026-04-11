package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import javax.inject.Inject

class UpdatePrivacySettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(request: UpdatePrivacySettingsRequest): PrivacySettings {
        return userRepository.updatePrivacySettings(request)
    }
}
