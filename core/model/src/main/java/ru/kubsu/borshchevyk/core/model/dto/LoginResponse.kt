package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val userId: String,
    val publicKey: String,
    val encryptedPrivateKey: String
)
