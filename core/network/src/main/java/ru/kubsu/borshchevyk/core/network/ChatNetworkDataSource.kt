package ru.kubsu.borshchevyk.core.network

import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest

interface ChatNetworkDataSource {
    suspend fun createChat(request: CreateChatRequest): ChatResponse
    suspend fun getUserChats(): List<ChatResponse>
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest)
    suspend fun clearChatHistory(chatId: String, forAll: Boolean)
    suspend fun deleteChat(chatId: String)
    
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): MessageResponse
    suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<MessageResponse>
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
}
