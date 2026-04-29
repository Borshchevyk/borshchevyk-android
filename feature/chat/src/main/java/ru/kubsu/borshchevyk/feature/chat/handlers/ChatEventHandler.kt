package ru.kubsu.borshchevyk.feature.chat.handlers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatHistoryUseCases
import ru.kubsu.borshchevyk.core.domain.message.ObserveChatEventsUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.feature.chat.ChatStateAction
import javax.inject.Inject

/**
 * Handles incoming chat events (WebSockets) and history updates.
 * Dispatches actions to the ViewModel's state.
 */
class ChatEventHandler @Inject constructor(
    private val historyUseCases: ChatHistoryUseCases,
    private val observeChatEventsUseCase: ObserveChatEventsUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val attachmentUseCases: ChatAttachmentUseCases
) {
    private val TAG = "ChatEventHandler"

    fun observe(
        chatId: String,
        scope: CoroutineScope,
        dispatch: (ChatStateAction) -> Unit,
        resolveAttachment: suspend (String) -> Unit
    ) {
        historyUseCases.observeChatHistory(chatId)
            .onEach { history ->
                dispatch(ChatStateAction.HistoryUpdated(history))
                history.forEach { msg ->
                    msg.attachments.forEach { att ->
                        resolveAttachment(att.id)
                    }
                }
            }
            .launchIn(scope)

        observeChatEventsUseCase(chatId)
            .onEach { event ->
                dispatch(ChatStateAction.ProcessDomainEvent(event))
                
                when (event) {
                    is ChatEvent.MessagePinned, is ChatEvent.MessageUnpinned -> {
                        try {
                            val pinned = historyUseCases.getPinnedMessages(chatId)
                            dispatch(ChatStateAction.SetPinnedMessages(pinned))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load pinned messages", e)
                        }
                    }
                    is ChatEvent.ReadReceipt -> {
                        try {
                            val readers = historyUseCases.getMessageReaders(chatId, event.event.messageId)
                            dispatch(ChatStateAction.SetReaders(event.event.messageId, readers))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load readers", e)
                        }
                    }
                    is ChatEvent.NewMessage -> {
                        getUserChatsUseCase() // Refresh chat list in background
                        event.message.attachments.forEach { attachment ->
                            resolveAttachment(attachment.id)
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(scope)
    }
}
