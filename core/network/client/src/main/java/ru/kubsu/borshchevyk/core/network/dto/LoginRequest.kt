package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Request object used for user authentication.
 *
 * @property email The email address of the user.
 * @property passwordHash The hashed password of the user.
 */
@Serializable
data class LoginRequest(
    val email: String,
    val passwordHash: String
)
