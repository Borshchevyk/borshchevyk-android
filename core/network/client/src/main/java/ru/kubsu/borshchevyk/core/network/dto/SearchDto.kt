package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object containing results of a global search across users and chats.
 *
 * @property users A list of users matching the search criteria.
 * @property chats A list of chats or groups matching the search criteria.
 */
@Serializable
data class GlobalSearchResponse(
    val users: List<ShortUserDto> = emptyList(),
    val chats: List<ShortChatDto> = emptyList()
)