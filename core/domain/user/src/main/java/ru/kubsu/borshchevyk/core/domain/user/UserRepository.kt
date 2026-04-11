package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest

interface UserRepository {
    suspend fun searchUsers(query: String): List<User>
    suspend fun getUserProfile(userIdOrTag: String): User
    suspend fun updateProfile(request: UpdateProfileRequest): User
    suspend fun getPrivacySettings(): PrivacySettings
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): PrivacySettings
}
