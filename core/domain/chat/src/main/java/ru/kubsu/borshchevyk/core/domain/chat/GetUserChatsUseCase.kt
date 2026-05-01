package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

/**
 * Use case for retrieving the current list of chats for the authenticated user.
 *
 * Unlike the observing use case which returns a reactive stream, this use case
 * fetches a single, one-time snapshot of the user's chats. This is useful for
 * initial loading or one-off data requests where a continuous flow is not needed.
 *
 * @property chatRepository The repository providing the list of user chats.
 */
class GetUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to fetch the user's chats.
     *
     * @return A list of [Chat] objects representing the user's current active conversations.
     */
    suspend operator fun invoke(): List<Chat> {
        return chatRepository.getUserChats()
    }
}
