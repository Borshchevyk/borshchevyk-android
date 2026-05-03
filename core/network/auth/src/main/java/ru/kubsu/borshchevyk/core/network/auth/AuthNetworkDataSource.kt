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

/**
 * Data source interface defining operations for user authentication and registration.
 */
interface AuthNetworkDataSource {
    /**
     * Registers a new user account on the server.
     *
     * @param request The [RegisterRequest] payload.
     * @return A [NetworkResult] containing the [RegisterResponse].
     */
    suspend fun register(request: RegisterRequest): NetworkResult<RegisterResponse>

    /**
     * Initiates the login process using email and hashed password.
     *
     * @param request The [LoginRequest] payload.
     * @return A [NetworkResult] containing initial login data ([LoginResponse]).
     */
    suspend fun login(request: LoginRequest): NetworkResult<LoginResponse>

    /**
     * Requests a cryptographic challenge for secondary authentication validation.
     *
     * @param request The [ChallengeRequest] for the target user.
     * @return A [NetworkResult] containing the [ChallengeResponse].
     */
    suspend fun challenge(request: ChallengeRequest): NetworkResult<ChallengeResponse>

    /**
     * Submits a signed challenge to complete the authentication process.
     *
     * @param request The [VerifyRequest] containing the signature.
     * @return A [NetworkResult] containing the access and refresh tokens.
     */
    suspend fun verify(request: VerifyRequest): NetworkResult<VerifyResponse>

    /**
     * Attempts to refresh an expired access token using a valid refresh token.
     *
     * @param request The [RefreshRequest] containing the refresh token.
     * @return A [NetworkResult] containing the new access and refresh tokens.
     */
    suspend fun refresh(request: RefreshRequest): NetworkResult<VerifyResponse>
}
