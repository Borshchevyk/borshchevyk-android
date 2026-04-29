package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GlobalSearchResponse(
    val users: List<ShortUserDto> = emptyList(),
    val chats: List<ShortChatDto> = emptyList()
)