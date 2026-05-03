package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object containing JWT tokens upon successful challenge verification.
 *
 * @property accessToken The short-lived JWT access token for API authorization.
 * @property refreshToken The long-lived JWT refresh token to obtain new access tokens.
 */
@Serializable
data class VerifyResponse(
    val accessToken: String,
    val refreshToken: String
)
