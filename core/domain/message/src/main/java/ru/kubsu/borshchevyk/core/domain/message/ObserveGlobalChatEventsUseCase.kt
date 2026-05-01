package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import javax.inject.Inject

/**
 * Use case for observing global events that affect chats across the application.
 *
 * This use case provides a stream for events that aren't tied to a single specific
 * chat interaction, but rather system-wide occurrences, such as new chats being
 * created, global updates, or system announcements.
 *
 * @property messageRepository The repository providing the global event stream.
 */
class ObserveGlobalChatEventsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves a reactive stream of global chat events.
     *
     * @return A [Flow] emitting [DomainGlobalChatEvent] objects as they occur.
     */
    operator fun invoke(): Flow<DomainGlobalChatEvent> {
        return messageRepository.observeChatEvents()
    }
}
