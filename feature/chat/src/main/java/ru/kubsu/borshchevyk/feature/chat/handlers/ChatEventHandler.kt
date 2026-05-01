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

    /**
     * Observes chat history and incoming events (e.g., via WebSockets) for a specific chat.
     * Dispatches state actions when new data or events arrive and automatically resolves attachment metadata.
     *
     * @param chatId The unique identifier of the chat to observe.
     * @param scope The coroutine scope in which the flows will be launched.
     * @param dispatch Callback function to dispatch state actions back to the ViewModel.
     * @param resolveAttachment Callback function to trigger resolution (e.g., metadata/previews) of attachments.
     */
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
                
                if (event is ChatEvent.NewMessage) {
                    event.message.attachments.forEach { attachment ->
                        resolveAttachment(attachment.id)
                    }
                }
            }
            .launchIn(scope)
    }
}
