package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
    val accessToken: String,
    val refreshToken: String
)
