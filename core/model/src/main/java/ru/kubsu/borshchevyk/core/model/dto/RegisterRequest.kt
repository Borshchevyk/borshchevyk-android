package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val tag: String,
    val passwordHash: String,
    val publicKey: String,
    val encryptedPrivateKey: String
)
