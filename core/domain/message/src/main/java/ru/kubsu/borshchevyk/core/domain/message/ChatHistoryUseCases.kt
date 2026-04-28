package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ChatHistoryUseCases @Inject constructor(
    val observeChatHistory: ObserveChatHistoryUseCase,
    val syncChatHistory: SyncChatHistoryUseCase,
    val getPinnedMessages: GetPinnedMessagesUseCase,
    val getMessageReaders: GetMessageReadersUseCase,
    val getMessageComments: GetMessageCommentsUseCase
)
