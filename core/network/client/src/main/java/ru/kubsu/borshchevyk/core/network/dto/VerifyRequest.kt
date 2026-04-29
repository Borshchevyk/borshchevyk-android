package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequest(
    val userId: String,
    val signature: String
)
