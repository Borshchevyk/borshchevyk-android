package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TargetUserRequest(
    val targetUserId: String
)
