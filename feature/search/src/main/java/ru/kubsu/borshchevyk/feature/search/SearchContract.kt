package ru.kubsu.borshchevyk.feature.search

import ru.kubsu.borshchevyk.core.model.domain.User

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data class CreateChat(val userId: String) : SearchIntent
}

sealed interface SearchEffect {
    data class ShowError(val message: String) : SearchEffect
    data class NavigateToChat(val chatId: String) : SearchEffect
}

data class SearchUiState(
    val query: String = "",
    val results: List<User> = emptyList(),
    val isLoading: Boolean = false
)
