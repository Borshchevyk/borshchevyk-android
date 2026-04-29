package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse

interface UserNetworkDataSource {
    suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>
    suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse>
    suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse>
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse>
}
