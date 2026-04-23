package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ChatHistoryUseCases @Inject constructor(
    val loadChatHistory: LoadChatHistoryUseCase,
    val getPinnedMessages: GetPinnedMessagesUseCase,
    val getMessageReaders: GetMessageReadersUseCase,
    val getMessageComments: GetMessageCommentsUseCase
)
