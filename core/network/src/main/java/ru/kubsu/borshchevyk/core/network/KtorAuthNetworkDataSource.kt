package ru.kubsu.borshchevyk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ru.kubsu.borshchevyk.core.model.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.model.dto.ChallengeResponse
import ru.kubsu.borshchevyk.core.model.dto.LoginRequest
import ru.kubsu.borshchevyk.core.model.dto.LoginResponse
import ru.kubsu.borshchevyk.core.model.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.model.dto.RegisterResponse
import ru.kubsu.borshchevyk.core.model.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyResponse
import javax.inject.Inject

class KtorAuthNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : AuthNetworkDataSource {

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return httpClient.post("api/v1/auth/register") {
            setBody(request)
        }.body()
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        return httpClient.post("api/v1/auth/login") {
            setBody(request)
        }.body()
    }

    override suspend fun challenge(request: ChallengeRequest): ChallengeResponse {
        return httpClient.post("api/v1/auth/challenge") {
            setBody(request)
        }.body()
    }

    override suspend fun verify(request: VerifyRequest): VerifyResponse {
        return httpClient.post("api/v1/auth/verify") {
            setBody(request)
        }.body()
    }
}
