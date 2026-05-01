package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import javax.inject.Inject

/**
 * Use case for observing read receipt events within a specific chat.
 *
 * This use case listens for updates indicating that participants in the chat
 * have read specific messages. This is used to update the UI to show "seen"
 * statuses.
 *
 * @property messageRepository The repository providing the read receipt event stream.
 */
class ObserveReadReceiptsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of read receipt events for the specified chat.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting [DomainReadReceiptEvent] instances when messages are read.
     */
    operator fun invoke(chatId: String): Flow<DomainReadReceiptEvent> {
        return messageRepository.observeReadReceipts(chatId)
    }
}
