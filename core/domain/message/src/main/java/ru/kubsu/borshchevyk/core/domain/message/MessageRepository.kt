package ru.kubsu.borshchevyk.core.domain.message
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent

interface MessageRepository {
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): Message
    suspend fun editMessage(chatId: String, messageId: String, newText: String): Message
    suspend fun loadChatHistory(chatId: String, page: Int = 0, size: Int = 50): List<Message>

    suspend fun connectWebSocket()
    suspend fun disconnectWebSocket()

    fun observeNewMessages(): Flow<NotificationDto.MessageDto>
    fun observeChatEvents(): Flow<NotificationDto.ChatEventDto>
    fun observeDeletedMessages(): Flow<String>
    fun observeTyping(chatId: String): Flow<TypingEvent>
    fun observeReactions(chatId: String): Flow<ReactionEvent>
    fun observePins(chatId: String): Flow<String>
    fun observeUnpins(chatId: String): Flow<String>
    fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent>

    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean)

    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean)
    
    suspend fun addReaction(chatId: String, messageId: String, reaction: String)
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String)
    suspend fun pinMessage(chatId: String, messageId: String)
    suspend fun unpinMessage(chatId: String, messageId: String)
    
    suspend fun getPinnedMessages(chatId: String): List<Message>
    suspend fun readMessage(chatId: String, messageId: String)
    suspend fun getMessageReaders(chatId: String, messageId: String): List<ru.kubsu.borshchevyk.core.model.domain.User>
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): List<Message>
}
