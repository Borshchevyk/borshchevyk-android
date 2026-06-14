package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.ObserveUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatHistoryUseCases
import ru.kubsu.borshchevyk.core.domain.message.ObserveChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.usecase.ObserveUserPresenceUseCase
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

data class InitialChatData(
    val chatId: String,
    val currentUserId: String,
    val isGroup: Boolean,
    val chatTitle: String,
    val chatAvatarUrl: String?,
    val history: List<Message>,
    val pinned: List<Message>,
    val forwardPayload: ForwardPayload?
)

class ChatEventHandler @Inject constructor(
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val observeUserChatsUseCase: ObserveUserChatsUseCase,
    private val historyUseCases: ChatHistoryUseCases,
    private val observeChatEventsUseCase: ObserveChatEventsUseCase,
    private val attachmentUseCases: ChatAttachmentUseCases,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase
) {
    suspend fun loadInitialData(chatId: String, initialForwardPayload: ForwardPayload?): InitialChatData {
        val userId = getUserIdUseCase().firstOrNull() ?: ""
        val chats = getUserChatsUseCase()
        val chat = chats.find { it.id == chatId }
        val isGroup = chat?.type == ChatType.GROUP
        val chatTitle = chat?.title ?: chat?.partnerName ?: if (isGroup) "Group Chat" else "Private Chat"
        
        val pinned = historyUseCases.getPinnedMessages(chatId).filterNot { it.isDeleted }
        val initialHistory = historyUseCases.observeChatHistory(chatId).firstOrNull() ?: emptyList()

        try { historyUseCases.syncChatHistory(chatId, 0, 50) } catch (e: Exception) { /* Ignore sync error, rely on cache */ }

        return InitialChatData(
            chatId = chatId,
            currentUserId = userId,
            isGroup = isGroup,
            chatTitle = chatTitle,
            chatAvatarUrl = chat?.partnerAvatarUrl,
            history = initialHistory,
            pinned = pinned,
            forwardPayload = initialForwardPayload
        )
    }

    suspend fun getPartnerId(chatId: String): String? {
        return getUserChatsUseCase().find { it.id == chatId }?.partnerId
    }

    fun observeHistory(chatId: String): Flow<List<Message>> = historyUseCases.observeChatHistory(chatId)

    fun observeEvents(chatId: String): Flow<ChatEvent> = observeChatEventsUseCase(chatId)

    fun observeChats(): Flow<List<Chat>> = observeUserChatsUseCase()

    fun observePresence(partnerId: String): Flow<DomainPresenceStatus> = observeUserPresenceUseCase(partnerId)

    suspend fun getReaders(chatId: String, messageId: String): List<User> = 
        historyUseCases.getMessageReaders(chatId, messageId)

    suspend fun getComments(chatId: String, messageId: String): List<Message> = 
        historyUseCases.getMessageComments(chatId, messageId)

    suspend fun resolveAttachmentUrls(attachment: ru.kubsu.borshchevyk.core.model.domain.Attachment, isThumbnail: Boolean): Map<String, String> {
        val urls = mutableMapOf<String, String>()
        if (attachment.type == DomainAttachmentType.VIDEO) {
            urls[attachment.id] = attachmentUseCases.getAttachmentUrl(attachment.id, false)
            urls[attachment.id + "_thumb"] = attachmentUseCases.getAttachmentUrl(attachment.id, true)
        } else {
            urls[attachment.id] = attachmentUseCases.getAttachmentUrl(attachment.id, isThumbnail)
        }
        return urls
    }

    suspend fun exportAttachment(attachmentId: String): Result<String> = 
        attachmentUseCases.exportAttachment(attachmentId)
}
