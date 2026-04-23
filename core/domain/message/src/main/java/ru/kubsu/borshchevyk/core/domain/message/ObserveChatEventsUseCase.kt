package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import javax.inject.Inject

class ObserveChatEventsUseCase @Inject constructor(
    private val observeNewMessagesUseCase: ObserveNewMessagesUseCase,
    private val observeDeletedMessagesUseCase: ObserveDeletedMessagesUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val observeReactionsUseCase: ObserveReactionsUseCase,
    private val observePinsUseCase: ObservePinsUseCase,
    private val observeUnpinsUseCase: ObserveUnpinsUseCase,
    private val observeReadReceiptsUseCase: ObserveReadReceiptsUseCase
) {
    operator fun invoke(chatId: String): Flow<ChatEvent> {
        val newMessages = observeNewMessagesUseCase().map { ChatEvent.NewMessage(it) }
        val deletedMessages = observeDeletedMessagesUseCase().map { ChatEvent.MessageDeleted(it) }
        val typing = observeTypingUseCase(chatId).map { ChatEvent.Typing(it) }
        val reactions = observeReactionsUseCase(chatId).map { ChatEvent.ReactionUpdated(it) }
        val pins = observePinsUseCase(chatId).map { ChatEvent.MessagePinned(it) }
        val unpins = observeUnpinsUseCase(chatId).map { ChatEvent.MessageUnpinned(it) }
        val readReceipts = observeReadReceiptsUseCase(chatId).map { ChatEvent.ReadReceipt(it) }

        return merge(newMessages, deletedMessages, typing, reactions, pins, unpins, readReceipts)
    }
}
