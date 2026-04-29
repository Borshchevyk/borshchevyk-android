package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class EnrichedUserResponse(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
)
