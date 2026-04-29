package ru.kubsu.borshchevyk.core.network.auth

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.network.dto.ChallengeResponse
import ru.kubsu.borshchevyk.core.network.dto.LoginRequest
import ru.kubsu.borshchevyk.core.network.dto.LoginResponse
import ru.kubsu.borshchevyk.core.network.dto.RefreshRequest
import ru.kubsu.borshchevyk.core.network.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.network.dto.RegisterResponse
import ru.kubsu.borshchevyk.core.network.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.network.dto.VerifyResponse

interface AuthNetworkDataSource {
    suspend fun register(request: RegisterRequest): NetworkResult<RegisterResponse>
    suspend fun login(request: LoginRequest): NetworkResult<LoginResponse>
    suspend fun challenge(request: ChallengeRequest): NetworkResult<ChallengeResponse>
    suspend fun verify(request: VerifyRequest): NetworkResult<VerifyResponse>
    suspend fun refresh(request: RefreshRequest): NetworkResult<VerifyResponse>
}
