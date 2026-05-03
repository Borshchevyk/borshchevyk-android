package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for broadcasting the current user's typing status to a chat.
 *
 * This use case allows the client to inform other participants in a chat that
 * the user has started or stopped typing. This is essential for providing
 * real-time feedback in modern messaging applications.
 *
 * @property messageRepository The repository responsible for broadcasting the event.
 */
class SendTypingEventUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Sends a typing status event.
     *
     * @param chatId The unique identifier of the chat where the typing is occurring.
     * @param isTyping True to indicate the user is typing, false to indicate they have stopped.
     */
    suspend operator fun invoke(chatId: String, isTyping: Boolean) {
        messageRepository.sendTypingEvent(chatId, isTyping)
    }
}
