package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest

interface MessageRepository {
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): Message
    suspend fun editMessage(chatId: String, messageId: String, newText: String): Message
    suspend fun loadChatHistory(chatId: String, page: Int = 0, size: Int = 50): List<Message>
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
    
    suspend fun addReaction(chatId: String, messageId: String, reaction: String)
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String)
    suspend fun pinMessage(chatId: String, messageId: String)
    suspend fun unpinMessage(chatId: String, messageId: String)
    
    suspend fun getPinnedMessages(chatId: String): List<Message>
    suspend fun readMessage(chatId: String, messageId: String)
    suspend fun getMessageReaders(chatId: String, messageId: String): List<String>
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): List<Message>
}
