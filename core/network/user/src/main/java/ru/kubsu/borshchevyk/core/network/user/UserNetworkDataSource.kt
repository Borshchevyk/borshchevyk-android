package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse

interface UserNetworkDataSource {
    suspend fun searchUsers(query: String): NetworkResult<List<UserProfileResponse>>
    suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>
    suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse>
    suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse>
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse>
}
