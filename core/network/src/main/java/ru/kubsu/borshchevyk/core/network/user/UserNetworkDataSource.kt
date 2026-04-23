package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse

interface UserNetworkDataSource {
    suspend fun searchUsers(query: String): List<UserProfileResponse>
    suspend fun getUserProfile(userIdOrTag: String): UserProfileResponse
    suspend fun updateProfile(request: UpdateProfileRequest): UserProfileResponse
    suspend fun getPrivacySettings(): PrivacySettingsResponse
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): PrivacySettingsResponse
}
