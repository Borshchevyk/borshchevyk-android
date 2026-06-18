package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse
import javax.inject.Inject

/**
 * Proxy implementation of [UserNetworkDataSource] that switches between Ktor (Global)
 * and Mesh (P2P) data sources based on the current [TransportModeManager] state.
 */
class ProxyUserNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorUserNetworkDataSource,
    private val meshDataSource: MeshUserNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : UserNetworkDataSource {

    private val dataSource: UserNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    override suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse> =
        dataSource.getUserProfile(userIdOrTag)

    override suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> =
        dataSource.updateProfile(request)

    override suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse> =
        dataSource.updateAvatar(request)

    override suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse> =
        dataSource.getPrivacySettings()

    override suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse> =
        dataSource.updatePrivacySettings(request)
}
