package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

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
