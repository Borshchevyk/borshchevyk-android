package ru.kubsu.borshchevyk.feature.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatHistoryUseCases
import ru.kubsu.borshchevyk.core.domain.message.usecase.ObserveUserPresenceUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.network.client.NetworkMonitor
import ru.kubsu.borshchevyk.feature.chat.handlers.CallHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.ChatEventHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.ChatMessageHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.MediaVoiceHandler
import javax.inject.Inject

/**
 * [ChatViewModel] is the central coordinator for the Chat screen.
 * It delegates complex logic to specialized handlers and maintains the UI state via MVI pattern.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val historyUseCases: ChatHistoryUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases,
    private val networkMonitor: NetworkMonitor,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase,
    private val callHandler: CallHandler,
    private val mediaVoiceHandler: MediaVoiceHandler,
    private val chatMessageHandler: ChatMessageHandler,
    private val chatEventHandler: ChatEventHandler
) : ViewModel() {

    private val TAG = "ChatViewModel"
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val forwardPayloadJson: String? = savedStateHandle["forwardPayloadJson"]
    
    private val initialForwardPayload: ForwardPayload? = try {
        forwardPayloadJson?.let { Json.decodeFromString<ForwardPayload>(it) }
    } catch (e: Exception) {
        null
    }

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var typingJob: Job? = null
    
    private val failedMessagesData = java.util.concurrent.ConcurrentHashMap<String, Triple<String, List<AttachmentFile>, ForwardPayload?>>()

    init {
        loadInitialData()
        observeDataSources()
    }

    private fun dispatch(action: ChatStateAction) {
        _uiState.update { it.reduce(action) }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.OpenSettings -> openSettings()
            is ChatIntent.ChatDeletedLocally -> dispatch(ChatStateAction.ChatDeleted)
            is ChatIntent.Typing -> onTyping()
            is ChatIntent.SendMessage -> onSendMessage(intent.text, intent.attachments)
            is ChatIntent.SendVoice -> onSendVoice(intent.bytes, intent.duration)
            is ChatIntent.SendCircle -> onSendCircle(intent.bytes, intent.duration)
            is ChatIntent.SetEditingMessage -> dispatch(ChatStateAction.SetEditingMessage(intent.message))
            is ChatIntent.EditMessage -> onEditMessage(intent.messageId, intent.newText)
            is ChatIntent.DeleteMessage -> onDeleteMessage(intent.messageId, intent.forAll)
            is ChatIntent.MessageVisible -> onMessageVisible(intent.messageId)
            is ChatIntent.LoadReaders -> onLoadReaders(intent.messageId)
            is ChatIntent.LoadComments -> onLoadComments(intent.messageId)
            is ChatIntent.PinMessage -> onPinMessage(intent.messageId)
            is ChatIntent.UnpinMessage -> onUnpinMessage(intent.messageId)
            is ChatIntent.ToggleReaction -> onToggleReaction(intent.messageId, intent.reaction)
            is ChatIntent.ResolveAttachmentUrl -> resolveAttachmentUrl(intent.attachmentId)
            is ChatIntent.ResendMessage -> onResendMessage(intent.messageId)
            is ChatIntent.ForwardMessage -> onForwardMessage(intent.message)
            is ChatIntent.InitiateCall -> onInitiateCall()
        }
    }

    private fun observeDataSources() {
        chatEventHandler.observe(
            chatId = chatId,
            scope = viewModelScope,
            dispatch = ::dispatch,
            resolveAttachment = ::resolveAttachmentUrl
        )

        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    val failedIds = failedMessagesData.keys().toList()
                    failedIds.forEach { msgId -> onResendMessage(msgId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            dispatch(ChatStateAction.LoadingStarted())
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val chats = getUserChatsUseCase()
                val chat = chats.find { it.id == chatId }
                val isGroup = chat?.type == ChatType.GROUP
                val chatTitle = chat?.title ?: chat?.partnerName ?: if (isGroup) "Group Chat" else "Private Chat"
                
                val pinned = historyUseCases.getPinnedMessages(chatId).filterNot { it.isDeleted }

                dispatch(ChatStateAction.InitialDataLoaded(
                    chatId = chatId,
                    currentUserId = userId,
                    isGroup = isGroup,
                    chatTitle = chatTitle,
                    history = emptyList(),
                    pinned = pinned,
                    forwardPayload = initialForwardPayload
                ))

                chat?.partnerId?.let { partnerId ->
                    if (!isGroup) {
                        observeUserPresenceUseCase(partnerId).onEach { presence ->
                            dispatch(ChatStateAction.PresenceUpdated(presence.isOnline, presence.lastSeenAt))
                        }.launchIn(viewModelScope)
                    }
                }

                try {
                    historyUseCases.syncChatHistory(chatId, 0, 50)
                } catch (e: Exception) {
                    Log.w(TAG, "Sync failed, using cache", e)
                }
            } catch (e: Exception) {
                dispatch(ChatStateAction.LoadFailed(e.message ?: "Failed to load chat"))
            }
        }
    }

    private fun onSendMessage(text: String, attachments: List<AttachmentFile>) {
        val state = uiState.value as? ChatUiState.Content ?: return
        val forwardPayload = state.input.forwardPayload

        if (text.isBlank() && attachments.isEmpty() && forwardPayload == null) return

        val tempId = "temp_${System.currentTimeMillis()}"
        val optimisticMessage = createOptimisticMessage(tempId, text, attachments, state)

        dispatch(ChatStateAction.MessageSending(tempId, optimisticMessage))
        savedStateHandle.remove<String>("forwardPayloadJson")

        performSendMessage(tempId, text, attachments, forwardPayload)
    }

    private fun performSendMessage(tempId: String, text: String, attachments: List<AttachmentFile>, forwardPayload: ForwardPayload?) {
        viewModelScope.launch {
            try {
                chatMessageHandler.sendMessage(chatId, text, attachments, forwardPayload)
                dispatch(ChatStateAction.MessageSent(tempId))
                chatMessageHandler.sendTypingEvent(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                failedMessagesData[tempId] = Triple(text, attachments, forwardPayload)
                dispatch(ChatStateAction.MessageSendFailed(tempId))
                _effect.send(ChatEffect.ShowError("Failed to send: ${e.message}"))
            }
        }
    }

    private fun onEditMessage(messageId: String, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            try {
                chatMessageHandler.editMessage(chatId, messageId, newText)
                dispatch(ChatStateAction.SetEditingMessage(null))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Edit failed: ${e.message}"))
            }
        }
    }

    private fun onDeleteMessage(messageId: String, forAll: Boolean) {
        viewModelScope.launch {
            try {
                chatMessageHandler.deleteMessage(chatId, messageId, forAll)
                dispatch(ChatStateAction.MessageRemoved(messageId))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Delete failed: ${e.message}"))
            }
        }
    }

    private fun onTyping() {
        viewModelScope.launch {
            try {
                chatMessageHandler.sendTypingEvent(chatId, true)
                typingJob?.cancel()
                typingJob = launch {
                    delay(3000)
                    chatMessageHandler.sendTypingEvent(chatId, false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Typing event failed", e)
            }
        }
    }

    private fun onToggleReaction(messageId: String, reaction: String) {
        val state = uiState.value as? ChatUiState.Content ?: return
        val currentUserId = state.context.currentUserId
        val message = state.feed.messages.find { it.id == messageId } ?: return
        val hasReaction = message.reactions.any { it.reaction == reaction && it.userId == currentUserId }
        
        viewModelScope.launch {
            try {
                if (hasReaction) {
                    chatMessageHandler.removeReaction(chatId, messageId, reaction)
                } else {
                    chatMessageHandler.addReaction(chatId, messageId, reaction)
                }
                dispatch(ChatStateAction.ReactionToggled(messageId, reaction, currentUserId, !hasReaction))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Reaction failed: ${e.message}"))
            }
        }
    }

    private fun onPinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatMessageHandler.pinMessage(chatId, messageId)
                dispatch(ChatStateAction.SetPinnedMessages(historyUseCases.getPinnedMessages(chatId)))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Pin failed: ${e.message}"))
            }
        }
    }

    private fun onUnpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatMessageHandler.unpinMessage(chatId, messageId)
                dispatch(ChatStateAction.SetPinnedMessages(historyUseCases.getPinnedMessages(chatId)))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Unpin failed: ${e.message}"))
            }
        }
    }

    private fun onInitiateCall() {
        val state = uiState.value as? ChatUiState.Content ?: return
        viewModelScope.launch {
            try {
                callHandler.initiateCall(chatId, state.context.currentUserId)?.let { callId ->
                    _effect.send(ChatEffect.NavigateToCall(callId))
                } ?: _effect.send(ChatEffect.ShowError("No participants to call"))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Call failed: ${e.message}"))
            }
        }
    }

    private fun onForwardMessage(message: Message) {
        val authorName = message.author?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User"
        val payload = ForwardPayload(
            text = message.text,
            attachmentIds = message.attachments.map { it.id },
            fromChatId = message.chatId,
            fromUserId = message.authorId,
            authorName = authorName
        )
        viewModelScope.launch {
            try {
                _effect.send(ChatEffect.NavigateToForwardSelection(Json.encodeToString(payload)))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Forwarding failed"))
            }
        }
    }

    private fun resolveAttachmentUrl(attachmentId: String) {
        val state = uiState.value as? ChatUiState.Content ?: return
        if (state.feed.attachmentUrls.containsKey(attachmentId)) return
        viewModelScope.launch {
            try {
                val url = attachmentUseCases.getAttachmentUrl(attachmentId)
                dispatch(ChatStateAction.UpdateAttachmentUrl(attachmentId, url))
            } catch (e: Exception) {
                Log.w(TAG, "Attachment resolution failed: $attachmentId", e)
            }
        }
    }

    private fun createOptimisticMessage(tempId: String, text: String, attachments: List<AttachmentFile>, state: ChatUiState.Content): Message {
        return Message(
            id = tempId,
            chatId = chatId,
            authorId = state.context.currentUserId,
            text = text.ifBlank { state.input.forwardPayload?.text ?: "" },
            createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).toString() + "Z",
            status = ru.kubsu.borshchevyk.core.model.domain.MessageStatus.SENDING,
            source = ru.kubsu.borshchevyk.core.model.domain.MessageSource.ONLINE,
            forwardedFromChatId = state.input.forwardPayload?.fromChatId,
            forwardedFromUserId = state.input.forwardPayload?.fromUserId,
            attachments = attachments.map { 
                ru.kubsu.borshchevyk.core.model.domain.Attachment(
                    id = "temp_${it.originalFilename}",
                    type = if (it.contentType.startsWith("image/")) DomainAttachmentType.PHOTO else DomainAttachmentType.FILE,
                    originalFilename = it.originalFilename,
                    extension = it.extension,
                    sizeBytes = it.bytes.size.toLong()
                )
            }
        )
    }

    private fun openSettings() = viewModelScope.launch { _effect.send(ChatEffect.NavigateToSettings(chatId)) }

    private fun onSendVoice(bytes: ByteArray, duration: Double) = viewModelScope.launch {
        try { mediaVoiceHandler.sendVoice(chatId, bytes, duration) } catch (e: Exception) { _effect.send(ChatEffect.ShowError("Voice send failed")) }
    }

    private fun onSendCircle(bytes: ByteArray, duration: Double) = viewModelScope.launch {
        try { mediaVoiceHandler.sendCircle(chatId, bytes, duration) } catch (e: Exception) { _effect.send(ChatEffect.ShowError("Circle send failed")) }
    }

    private fun onResendMessage(messageId: String) {
        val data = failedMessagesData.remove(messageId) ?: return
        val state = uiState.value as? ChatUiState.Content ?: return
        val msg = state.feed.messages.find { it.id == messageId } ?: return
        dispatch(ChatStateAction.MessageSending(messageId, msg.copy(status = ru.kubsu.borshchevyk.core.model.domain.MessageStatus.SENDING)))
        performSendMessage(messageId, data.first, data.second, data.third)
    }

    private fun onMessageVisible(messageId: String) {
        val state = uiState.value as? ChatUiState.Content ?: return
        val message = state.feed.messages.find { it.id == messageId } ?: return
        if (message.authorId != state.context.currentUserId) {
            viewModelScope.launch { try { chatMessageHandler.markAsRead(chatId, messageId) } catch (e: Exception) { Log.e(TAG, "Mark as read failed", e) } }
        }
    }

    private fun onLoadReaders(messageId: String) = viewModelScope.launch {
        try { dispatch(ChatStateAction.SetReaders(messageId, historyUseCases.getMessageReaders(chatId, messageId))) } catch (e: Exception) { _effect.send(ChatEffect.ShowError("Readers load failed")) }
    }

    private fun onLoadComments(messageId: String) = viewModelScope.launch {
        try { dispatch(ChatStateAction.SetComments(messageId, historyUseCases.getMessageComments(chatId, messageId))) } catch (e: Exception) { _effect.send(ChatEffect.ShowError("Comments load failed")) }
    }
}
