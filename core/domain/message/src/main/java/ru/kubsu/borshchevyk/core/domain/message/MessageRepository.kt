package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainGlobalChatEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import ru.kubsu.borshchevyk.core.model.domain.Message

interface MessageRepository {
    suspend fun sendMessage(
        chatId: String, 
        text: String, 
        attachmentIds: List<String>?, 
        forwardedFromChatId: String?, 
        forwardedFromUserId: String?
    ): Message
    suspend fun editMessage(chatId: String, messageId: String, newText: String): Message
    fun observeChatHistory(chatId: String): Flow<List<Message>>
    suspend fun syncChatHistory(chatId: String, page: Int = 0, size: Int = 50)
    suspend fun connectWebSocket()
    suspend fun disconnectWebSocket()

    fun observeNewMessages(): Flow<Message>
    fun observeChatEvents(): Flow<DomainGlobalChatEvent>
    fun observeDeletedMessages(): Flow<String>
    fun observeTyping(chatId: String): Flow<DomainTypingEvent>
    fun observeReactions(chatId: String): Flow<DomainReactionEvent>
    fun observePins(chatId: String): Flow<String>
    fun observeUnpins(chatId: String): Flow<String>
    fun observeReadReceipts(chatId: String): Flow<DomainReadReceiptEvent>
    fun observePresence(userId: String): Flow<DomainPresenceStatus>
    
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
