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

/**
 * Represents the immutable UI state of the Search screen.
 *
 * @property query The current text in the search input field.
 * @property userResults The list of users matching the search query or recent users.
 * @property chatResults The list of public chats matching the search query.
 * @property isLoading Indicates whether a search operation is currently in progress.
 * @property error An optional error message if the search or action failed.
 */
data class SearchUiState(
    val query: String = "",
    val userResults: List<User> = emptyList(),
    val chatResults: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
