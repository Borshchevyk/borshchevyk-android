package ru.kubsu.borshchevyk.core.model.domain

data class GlobalSearchResults(
    val users: List<User> = emptyList(),
    val chats: List<Chat> = emptyList()
)