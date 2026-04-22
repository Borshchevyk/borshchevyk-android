package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateChatInfoRequest(
    val title: String? = null,
    val description: String? = null,
    val commentsEnabled: Boolean? = null
)