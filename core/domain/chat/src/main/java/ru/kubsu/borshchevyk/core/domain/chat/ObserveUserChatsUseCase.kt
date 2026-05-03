package ru.kubsu.borshchevyk.core.domain.chat

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

/**
 * Use case for observing the current user's list of chats continuously.
 *
 * This provides a reactive flow that emits a new list of chats whenever the
 * underlying data changes (e.g., a new chat is created, a message is received,
 * or chat metadata is updated). This is the primary way to populate the main
 * chat list UI.
 *
 * @property chatRepository The repository providing the reactive stream of chats.
 */
class ObserveUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to observe the user's chats.
     *
     * @return A [Flow] emitting lists of [Chat] objects, representing the latest state.
     */
    operator fun invoke(): Flow<List<Chat>> {
        return chatRepository.observeUserChats()
    }
}
