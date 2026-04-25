package ru.kubsu.borshchevyk.core.network.message

import ru.kubsu.borshchevyk.core.model.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest

interface MessageNetworkDataSource {
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): MessageResponse
    suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): MessageResponse
    suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<MessageResponse>
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
    
    suspend fun addReaction(chatId: String, messageId: String, reaction: String)
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String)
    suspend fun pinMessage(chatId: String, messageId: String)
    suspend fun unpinMessage(chatId: String, messageId: String)
    
    suspend fun getPinnedMessages(chatId: String): List<MessageResponse>
    suspend fun readMessage(chatId: String, messageId: String)
    suspend fun getMessageReaders(chatId: String, messageId: String): List<EnrichedUserResponse>
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): List<MessageResponse>
}
