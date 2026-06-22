package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import javax.inject.Inject

/**
 * Use case for observing a combined stream of all relevant events within a specific chat.
 *
 * This use case merges multiple individual event streams (new messages, typing indicators,
 * read receipts, reactions, etc.) into a single, unified [Flow] of [ChatEvent]. This is
 * highly beneficial for ViewModels, allowing them to subscribe to one stream and handle
 * state updates reactively without managing multiple flows manually.
 *
 * @property observeNewMessagesUseCase Use case for observing new message events.
 * @property observeDeletedMessagesUseCase Use case for observing message deletion events.
 * @property observeTypingUseCase Use case for observing typing status events.
 * @property observeReactionsUseCase Use case for observing reaction updates.
 * @property observePinsUseCase Use case for observing message pin events.
 * @property observeUnpinsUseCase Use case for observing message unpin events.
 * @property observeReadReceiptsUseCase Use case for observing read receipt events.
 */
class ObserveChatEventsUseCase @Inject constructor(
    private val observeNewMessagesUseCase: ObserveNewMessagesUseCase,
    private val observeDeletedMessagesUseCase: ObserveDeletedMessagesUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val observeReactionsUseCase: ObserveReactionsUseCase,
    private val observePinsUseCase: ObservePinsUseCase,
    private val observeUnpinsUseCase: ObserveUnpinsUseCase,
    private val observeReadReceiptsUseCase: ObserveReadReceiptsUseCase,
    private val observeGlobalChatEventsUseCase: ObserveGlobalChatEventsUseCase
) {
    /**
     * Returns a merged Flow of [ChatEvent] for the specified chat.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @return A [Flow] emitting various [ChatEvent] instances in real-time.
     */
    operator fun invoke(chatId: String): Flow<ChatEvent> {
        val newMessages = observeNewMessagesUseCase().map { ChatEvent.NewMessage(it) }
        val deletedMessages = observeDeletedMessagesUseCase().map { ChatEvent.MessageDeleted(it) }
        val typing = observeTypingUseCase(chatId).map { ChatEvent.Typing(it) }
        val reactions = observeReactionsUseCase(chatId).map { ChatEvent.ReactionUpdated(it) }
        val pins = observePinsUseCase(chatId).map { ChatEvent.MessagePinned(it) }
        val unpins = observeUnpinsUseCase(chatId).map { ChatEvent.MessageUnpinned(it) }
        val readReceipts = observeReadReceiptsUseCase(chatId).map { ChatEvent.ReadReceipt(it) }
        val globalEvents = observeGlobalChatEventsUseCase().map { ChatEvent.GlobalChatEvent(it) }

        return merge(newMessages, deletedMessages, typing, reactions, pins, unpins, readReceipts, globalEvents)
    }
}
