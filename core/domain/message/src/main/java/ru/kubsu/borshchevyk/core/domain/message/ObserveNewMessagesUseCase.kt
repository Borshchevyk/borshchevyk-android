package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

/**
 * Use case for observing newly arrived messages across the application.
 *
 * This use case provides a reactive stream that emits every new message received
 * by the client, regardless of which chat it belongs to. This is typically used
 * for global notification handling or badge updating.
 *
 * @property messageRepository The repository providing the new message stream.
 */
class ObserveNewMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of newly received messages.
     *
     * @return A [Flow] emitting [Message] objects as they arrive.
     */
    operator fun invoke(): Flow<Message> {
        return messageRepository.observeNewMessages()
    }
}
