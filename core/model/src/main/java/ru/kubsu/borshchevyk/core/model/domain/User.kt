package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val email: String? = null,
    val tag: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val avatars: List<String> = emptyList()
)
