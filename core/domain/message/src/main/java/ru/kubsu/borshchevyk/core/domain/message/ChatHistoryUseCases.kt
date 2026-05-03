package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * A wrapper class that groups all use cases related to chat history management and retrieval.
 *
 * This class aggregates various operations for observing, syncing, and querying historical
 * chat data, making it easier to inject into ViewModels.
 *
 * @property observeChatHistory Use case for observing the real-time or cached chat history.
 * @property syncChatHistory Use case for explicitly synchronizing chat history with the remote server or network.
 * @property getPinnedMessages Use case for retrieving a list of pinned messages within a chat.
 * @property getMessageReaders Use case for fetching the list of users who have read a specific message.
 * @property getMessageComments Use case for retrieving comments or thread replies for a specific message.
 */
class ChatHistoryUseCases @Inject constructor(
    val observeChatHistory: ObserveChatHistoryUseCase,
    val syncChatHistory: SyncChatHistoryUseCase,
    val getPinnedMessages: GetPinnedMessagesUseCase,
    val getMessageReaders: GetMessageReadersUseCase,
    val getMessageComments: GetMessageCommentsUseCase
)
