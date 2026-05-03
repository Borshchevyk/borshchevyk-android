package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object used to obtain a new access token using a refresh token.
 *
 * @property refreshToken The valid refresh token previously issued to the client.
 */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)
