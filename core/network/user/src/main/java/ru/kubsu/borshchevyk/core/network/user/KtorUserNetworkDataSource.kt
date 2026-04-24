package ru.kubsu.borshchevyk.core.network.user

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorUserNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserNetworkDataSource {

    override suspend fun searchUsers(query: String): List<UserProfileResponse> {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/users/search") {
                parameter("q", query)
            }.body()
        }
    }

    override suspend fun getUserProfile(userIdOrTag: String): UserProfileResponse {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/users/$userIdOrTag").body()
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): UserProfileResponse {
        return withContext(ioDispatcher) {
            httpClient.patch("api/v1/users/me/profile") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun getPrivacySettings(): PrivacySettingsResponse {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/users/me/privacy").body()
        }
    }

    override suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): PrivacySettingsResponse {
        return withContext(ioDispatcher) {
            httpClient.patch("api/v1/users/me/privacy") {
                setBody(request)
            }.body()
        }
    }
}
