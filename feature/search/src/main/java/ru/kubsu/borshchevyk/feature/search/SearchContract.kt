package ru.kubsu.borshchevyk.feature.search

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.User

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data class CreateChat(val userId: String) : SearchIntent
    data class JoinChat(val chatId: String) : SearchIntent
}

sealed interface SearchEffect {
    data class ShowError(val message: String) : SearchEffect
    data class NavigateToChat(val chatId: String) : SearchEffect
}

data class SearchUiState(
    val query: String = "",
    val userResults: List<User> = emptyList(),
    val chatResults: List<Chat> = emptyList(),
    val isLoading: Boolean = false
)
