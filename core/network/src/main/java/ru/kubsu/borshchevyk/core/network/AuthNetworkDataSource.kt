package ru.kubsu.borshchevyk.core.network

import ru.kubsu.borshchevyk.core.model.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.model.dto.ChallengeResponse
import ru.kubsu.borshchevyk.core.model.dto.LoginRequest
import ru.kubsu.borshchevyk.core.model.dto.LoginResponse
import ru.kubsu.borshchevyk.core.model.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.model.dto.RegisterResponse
import ru.kubsu.borshchevyk.core.model.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyResponse

interface AuthNetworkDataSource {
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun challenge(request: ChallengeRequest): ChallengeResponse
    suspend fun verify(request: VerifyRequest): VerifyResponse
}
