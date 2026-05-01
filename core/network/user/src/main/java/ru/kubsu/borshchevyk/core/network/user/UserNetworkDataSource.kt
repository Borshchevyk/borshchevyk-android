package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse

/**
 * Data source interface defining network operations related to user profiles and privacy settings.
 */
interface UserNetworkDataSource {
    /**
     * Fetches the profile information for a specific user.
     *
     * @param userIdOrTag The ID or unique tag of the user to fetch.
     * @return A [NetworkResult] containing the [UserProfileResponse].
     */
    suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse>

    /**
     * Updates the current user's basic profile details.
     *
     * @param request The [UpdateProfileRequest] with the changes.
     * @return A [NetworkResult] containing the updated [UserProfileResponse].
     */
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>

    /**
     * Updates the current user's active avatar.
     *
     * @param request The [UpdateAvatarRequest] containing the new avatar URL.
     * @return A [NetworkResult] containing the updated [UserProfileResponse].
     */
    suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse>

    /**
     * Fetches the current user's privacy settings.
     *
     * @return A [NetworkResult] containing the [PrivacySettingsResponse].
     */
    suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse>

    /**
     * Updates the current user's privacy settings.
     *
     * @param request The [UpdatePrivacySettingsRequest] containing the new visibility levels.
     * @return A [NetworkResult] containing the updated [PrivacySettingsResponse].
     */
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse>
}
