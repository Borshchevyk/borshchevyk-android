package ru.kubsu.borshchevyk.feature.chat.chatlist.interactor

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.chat.ObserveUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveGlobalChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveNewMessagesUseCase
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class ChatListEventHandler @Inject constructor(
    private val observeUserChatsUseCase: ObserveUserChatsUseCase,
    private val observeNewMessagesUseCase: ObserveNewMessagesUseCase,
    private val observeGlobalChatEventsUseCase: ObserveGlobalChatEventsUseCase
) {
    fun observeChats(): Flow<List<Chat>> = observeUserChatsUseCase()
    fun observeNewMessages(): Flow<Message> = observeNewMessagesUseCase()
    fun observeGlobalEvents(): Flow<DomainGlobalChatEvent> = observeGlobalChatEventsUseCase()
}
