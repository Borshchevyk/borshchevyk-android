package ru.kubsu.borshchevyk.core.network.user

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorUserNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserNetworkDataSource {

    override suspend fun searchUsers(query: String): NetworkResult<List<UserProfileResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/users/search") {
                    url { parameters.append("query", query) }
                }
            }
        }
    }

    override suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/users/$userIdOrTag")
            }
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/users/me/profile") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/users/me/avatar") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/users/me/privacy")
            }
        }
    }

    override suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/users/me/privacy") {
                    setBody(request)
                }
            }
        }
    }
}
