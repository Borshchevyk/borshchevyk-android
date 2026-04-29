package ru.kubsu.borshchevyk.core.network.message

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest

interface MessageNetworkDataSource {
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse>
    suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse>
    suspend fun loadChatHistory(chatId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>>
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean): NetworkResult<Unit>
    
    suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit>
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit>
    suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit>
    suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit>
    
    suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>>
    suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit>
    suspend fun getMessageReaders(chatId: String, messageId: String): NetworkResult<List<EnrichedUserResponse>>
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>>
}
