package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val userId: String,
    val publicKey: String,
    val encryptedPrivateKey: String
)
