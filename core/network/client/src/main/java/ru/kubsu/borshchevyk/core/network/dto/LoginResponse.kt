package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object returned upon successful login containing authentication keys.
 *
 * @property userId The unique identifier of the authenticated user.
 * @property publicKey The user's public key for cryptographic operations.
 * @property encryptedPrivateKey The user's encrypted private key.
 */
@Serializable
data class LoginResponse(
    val userId: String,
    val publicKey: String,
    val encryptedPrivateKey: String
)
