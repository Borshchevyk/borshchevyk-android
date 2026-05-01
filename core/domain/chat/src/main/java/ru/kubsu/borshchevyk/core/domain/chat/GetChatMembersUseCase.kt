package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import javax.inject.Inject

/**
 * Use case for retrieving a paginated list of members for a specific chat.
 *
 * This is useful for displaying the participants of a group chat. It handles
 * pagination to ensure efficient loading of large participant lists.
 *
 * @property chatRepository The repository handling the retrieval of chat members.
 */
class GetChatMembersUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to fetch chat members.
     *
     * @param chatId The unique identifier of the chat.
     * @param page The zero-based page index to retrieve. Defaults to 0.
     * @param size The number of members to retrieve per page. Defaults to 50.
     * @return A [DomainPage] containing the requested list of [ChatMember]s along with pagination metadata.
     */
    suspend operator fun invoke(chatId: String, page: Int = 0, size: Int = 50): DomainPage<ChatMember> {
        return chatRepository.getChatMembers(chatId, page, size)
    }
}
