package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest

interface MessageRepository {
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): Message
    suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<Message>
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
}
