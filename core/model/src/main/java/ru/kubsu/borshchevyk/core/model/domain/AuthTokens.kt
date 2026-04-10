package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing a set of JWT tokens issued by the server.
 *
 * These tokens are used for authenticating standard REST/WebSocket API requests
 * after a successful online login.
 *
 * @property accessToken the short-lived JWT used in Authorization headers
 * @property refreshToken the long-lived JWT used to obtain new access tokens
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
