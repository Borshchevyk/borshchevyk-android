package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing the aggregated results of a global search query.
 *
 * Contains matching users and groups/channels found across the network or local database.
 *
 * @property users A list of users matching the search criteria.
 * @property chats A list of chats (groups, channels) matching the search criteria.
 */
data class GlobalSearchResults(
    val users: List<User> = emptyList(),
    val chats: List<Chat> = emptyList()
)