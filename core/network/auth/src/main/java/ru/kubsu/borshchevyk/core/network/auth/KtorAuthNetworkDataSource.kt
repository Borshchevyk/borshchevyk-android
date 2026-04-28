package ru.kubsu.borshchevyk.core.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.model.dto.ChallengeResponse
import ru.kubsu.borshchevyk.core.model.dto.LoginRequest
import ru.kubsu.borshchevyk.core.model.dto.LoginResponse
import ru.kubsu.borshchevyk.core.model.dto.RefreshRequest
import ru.kubsu.borshchevyk.core.model.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.model.dto.RegisterResponse
import ru.kubsu.borshchevyk.core.model.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyResponse
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorAuthNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthNetworkDataSource {

    override suspend fun register(request: RegisterRequest): NetworkResult<RegisterResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/auth/register") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun login(request: LoginRequest): NetworkResult<LoginResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/auth/login") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun challenge(request: ChallengeRequest): NetworkResult<ChallengeResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/auth/challenge") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun verify(request: VerifyRequest): NetworkResult<VerifyResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/auth/verify") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun refresh(request: RefreshRequest): NetworkResult<VerifyResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/auth/refresh") {
                    setBody(request)
                }
            }
        }
    }
}