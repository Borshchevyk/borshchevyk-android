package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults
import javax.inject.Inject

/**
 * Use case for performing a global search across the application's data.
 *
 * This allows users to search for specific content, such as other users, existing chats,
 * or even specific messages, depending on the backend/mesh implementation.
 * It provides a unified search entry point.
 *
 * @property chatRepository The repository executing the search query.
 */
class GlobalSearchUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the global search.
     *
     * @param query The text query to search for.
     * @return A [GlobalSearchResults] object containing categorized lists of results (e.g., users, chats).
     */
    suspend operator fun invoke(query: String): GlobalSearchResults {
        return chatRepository.globalSearch(query)
    }
}
