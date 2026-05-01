package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object used for registering a new user account.
 *
 * @property email The email address of the new user.
 * @property tag A unique handle or username for the new user.
 * @property firstName The first name of the new user.
 * @property lastName The optional last name of the new user.
 * @property passwordHash The hashed password.
 * @property publicKey The user's public key for cryptographic operations.
 * @property encryptedPrivateKey The user's private key, encrypted symmetrically with a key derived from the password.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val tag: String,
    val firstName: String,
    val lastName: String? = null,
    val passwordHash: String,
    val publicKey: String,
    val encryptedPrivateKey: String
)
