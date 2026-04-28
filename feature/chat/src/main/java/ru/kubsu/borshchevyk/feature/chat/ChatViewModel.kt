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
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.domain.message.ObserveChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.usecase.ObserveUserPresenceUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.network.client.NetworkMonitor
import ru.kubsu.borshchevyk.feature.chat.handlers.CallHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.MediaVoiceHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.MessageSenderHandler
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val observeChatEventsUseCase: ObserveChatEventsUseCase,
    private val messageUseCases: ChatMessageUseCases,
    private val historyUseCases: ChatHistoryUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases,
    private val networkMonitor: NetworkMonitor,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase,
    private val callHandler: CallHandler,
    private val mediaVoiceHandler: MediaVoiceHandler,
    private val messageSenderHandler: MessageSenderHandler
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
        loadData()
        observeWebSockets()
        observeNetwork()
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

    private fun onInitiateCall() {
        val state = _uiState.value as? ChatUiState.Content ?: return
        viewModelScope.launch {
            try {
                val callId = callHandler.initiateCall(chatId, state.context.currentUserId)
                if (callId != null) {
                    _effect.send(ChatEffect.NavigateToCall(callId))
                } else {
                    _effect.send(ChatEffect.ShowError("No participants to call"))
                }
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Failed to initiate call: ${e.message}"))
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
                val json = Json.encodeToString(payload)
                _effect.send(ChatEffect.NavigateToForwardSelection(json))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Failed to prepare forwarded message"))
            }
        }
    }

    private fun observeNetwork() {
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    val failedIds = failedMessagesData.keys().toList()
                    failedIds.forEach { msgId -> onResendMessage(msgId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onResendMessage(messageId: String) {
        val data = failedMessagesData[messageId] ?: return
        failedMessagesData.remove(messageId)
        
        val state = _uiState.value as? ChatUiState.Content ?: return
        val msg = state.feed.messages.find { it.id == messageId } ?: return
        dispatch(ChatStateAction.MessageSending(messageId, msg.copy(status = ru.kubsu.borshchevyk.core.model.domain.MessageStatus.SENDING)))
        
        performSendMessage(messageId, data.first, data.second, data.third)
    }

    private fun observeWebSockets() {
        historyUseCases.observeChatHistory(chatId)
            .onEach { history ->
                dispatch(ChatStateAction.HistoryUpdated(history))
                history.forEach { msg ->
                    msg.attachments.forEach { att ->
                        resolveAttachmentUrl(att.id)
                    }
                }
            }
            .launchIn(viewModelScope)

        observeChatEventsUseCase(chatId)
            .onEach { event ->
                dispatch(ChatStateAction.ProcessDomainEvent(event))
                
                when (event) {
                    is ChatEvent.MessagePinned, is ChatEvent.MessageUnpinned -> {
                        viewModelScope.launch {
                            try {
                                val pinned = historyUseCases.getPinnedMessages(chatId)
                                dispatch(ChatStateAction.SetPinnedMessages(pinned))
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load pinned messages", e)
                            }
                        }
                    }
                    is ChatEvent.ReadReceipt -> {
                        viewModelScope.launch {
                            try {
                                val readers = historyUseCases.getMessageReaders(chatId, event.event.messageId)
                                dispatch(ChatStateAction.SetReaders(event.event.messageId, readers))
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load readers", e)
                            }
                        }
                    }
                    is ChatEvent.NewMessage -> {
                        viewModelScope.launch { getUserChatsUseCase() }
                        event.message.attachments.forEach { attachment ->
                            resolveAttachmentUrl(attachment.id)
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        viewModelScope.launch {
            dispatch(ChatStateAction.LoadingStarted())
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val chats = getUserChatsUseCase()
                val chat = chats.find { it.id == chatId }
                val isGroup = chat?.type == ChatType.GROUP
                val chatTitle = chat?.title ?: chat?.partnerName ?: if (isGroup) "Group Chat" else "Private Chat"
                
                // Initial data load handles pinned messages and setup, history is loaded reactively
                val pinned = historyUseCases.getPinnedMessages(chatId).filterNot { it.isDeleted }

                dispatch(ChatStateAction.InitialDataLoaded(
                    chatId = chatId,
                    currentUserId = userId,
                    isGroup = isGroup,
                    chatTitle = chatTitle,
                    history = emptyList(), // History is populated by observeChatHistory flow
                    pinned = pinned,
                    forwardPayload = initialForwardPayload
                ))

                val partnerId = chat?.partnerId
                if (partnerId != null && !isGroup) {
                    observeUserPresenceUseCase(partnerId).onEach { presence ->
                        dispatch(ChatStateAction.PresenceUpdated(presence.isOnline, presence.lastSeenAt))
                    }.launchIn(viewModelScope)
                }

                // Trigger network sync
                historyUseCases.syncChatHistory(chatId, 0, 50)
            } catch (e: Exception) {
                dispatch(ChatStateAction.LoadFailed(e.message ?: "Failed to load chat"))
            }
        }
    }

    private fun openSettings() {
        viewModelScope.launch {
            _effect.send(ChatEffect.NavigateToSettings(chatId))
        }
    }

    private fun onTyping() {
        viewModelScope.launch {
            try {
                messageUseCases.sendTypingEvent(chatId, true)
                typingJob?.cancel()
                typingJob = launch {
                    delay(3000)
                    messageUseCases.sendTypingEvent(chatId, false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send typing event", e)
            }
        }
    }

    private fun onSendVoice(bytes: ByteArray, duration: Double) {
        viewModelScope.launch {
            try {
                mediaVoiceHandler.sendVoice(chatId, bytes, duration)
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Failed to send voice message"))
            }
        }
    }

    private fun onSendCircle(bytes: ByteArray, duration: Double) {
        viewModelScope.launch {
            try {
                mediaVoiceHandler.sendCircle(chatId, bytes, duration)
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError("Failed to send video circle"))
            }
        }
    }

    private fun onSendMessage(text: String, attachments: List<AttachmentFile>) {
        val contentState = uiState.value as? ChatUiState.Content ?: return
        val currentUserId = contentState.context.currentUserId
        val forwardPayload = contentState.input.forwardPayload

        if (text.isBlank() && attachments.isEmpty() && forwardPayload == null) return

        val tempId = "temp_${System.currentTimeMillis()}"
        val optimisticMessage = Message(
            id = tempId,
            chatId = chatId,
            authorId = currentUserId,
            text = text.ifBlank { forwardPayload?.text ?: "" },
            createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).toString() + "Z",
            status = ru.kubsu.borshchevyk.core.model.domain.MessageStatus.SENDING,
            source = ru.kubsu.borshchevyk.core.model.domain.MessageSource.ONLINE,
            forwardedFromChatId = forwardPayload?.fromChatId,
            forwardedFromUserId = forwardPayload?.fromUserId,
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

        dispatch(ChatStateAction.MessageSending(tempId, optimisticMessage))
        savedStateHandle.remove<String>("forwardPayloadJson")

        performSendMessage(tempId, text, attachments, forwardPayload)
    }

    private fun performSendMessage(tempId: String, text: String, attachments: List<AttachmentFile>, forwardPayload: ForwardPayload?) {
        viewModelScope.launch {
            try {
                messageSenderHandler.sendMessage(chatId, text, attachments, forwardPayload)
                
                // Let observeNewMessages flow update the real message from DB/Network
                // but we should still clear the temp message from UI state and stop loading
                dispatch(ChatStateAction.MessageSent(tempId))
                
                messageUseCases.sendTypingEvent(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                failedMessagesData[tempId] = Triple(text, attachments, forwardPayload)
                dispatch(ChatStateAction.MessageSendFailed(tempId))
                _effect.send(ChatEffect.ShowError("Failed to send message: ${e.message}"))
            }
        }
    }

    private fun resolveAttachmentUrl(attachmentId: String) {
        val state = _uiState.value as? ChatUiState.Content ?: return
        if (state.feed.attachmentUrls.containsKey(attachmentId)) return

        viewModelScope.launch {
            try {
                val url = attachmentUseCases.getAttachmentUrl(attachmentId)
                dispatch(ChatStateAction.UpdateAttachmentUrl(attachmentId, url))
            } catch (e: Exception) {
                Log.w(TAG, "Silently ignored attachment resolution error: ${e.message}")
            }
        }
    }

    private fun onEditMessage(messageId: String, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            try {
                messageUseCases.editMessage(chatId, messageId, newText)
                // DB observation will handle the update, just clear editing state
                dispatch(ChatStateAction.SetEditingMessage(null))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to edit message"))
            }
        }
    }

    private fun onDeleteMessage(messageId: String, forAll: Boolean = false) {
        viewModelScope.launch {
            try {
                messageUseCases.deleteMessage(chatId, messageId, forAll)
                dispatch(ChatStateAction.MessageRemoved(messageId))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to delete message"))
            }
        }
    }

    private fun onMessageVisible(messageId: String) {
        val state = _uiState.value as? ChatUiState.Content ?: return
        val message = state.feed.messages.find { it.id == messageId } ?: return

        if (message.authorId != state.context.currentUserId) {
            viewModelScope.launch {
                try {
                    messageUseCases.markMessageAsRead(chatId, messageId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark message as read: $messageId", e)
                }
            }
        }
    }

    private fun onLoadReaders(messageId: String) {
        viewModelScope.launch {
            try {
                val readers = historyUseCases.getMessageReaders(chatId, messageId)
                dispatch(ChatStateAction.SetReaders(messageId, readers))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to load readers"))
            }
        }
    }

    private fun onLoadComments(messageId: String) {
        viewModelScope.launch {
            try {
                val comments = historyUseCases.getMessageComments(chatId, messageId)
                dispatch(ChatStateAction.SetComments(messageId, comments))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to load comments"))
            }
        }
    }

    private fun onPinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                messageUseCases.pinMessage(chatId, messageId)
                val pinned = historyUseCases.getPinnedMessages(chatId)
                dispatch(ChatStateAction.SetPinnedMessages(pinned))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to pin message"))
            }
        }
    }

    private fun onUnpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                messageUseCases.unpinMessage(chatId, messageId)
                val pinned = historyUseCases.getPinnedMessages(chatId)
                dispatch(ChatStateAction.SetPinnedMessages(pinned))
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to unpin message"))
            }
        }
    }

    private fun onToggleReaction(messageId: String, reaction: String) {
        val state = _uiState.value as? ChatUiState.Content ?: return
        val currentUserId = state.context.currentUserId
        val message = state.feed.messages.find { it.id == messageId } ?: return
        
        val hasReaction = message.reactions.any { it.reaction == reaction && it.userId == currentUserId }
        
        viewModelScope.launch {
            try {
                if (hasReaction) {
                    messageUseCases.removeReaction(chatId, messageId, reaction)
                    dispatch(ChatStateAction.ReactionToggled(messageId, reaction, currentUserId, isAdded = false))
                } else {
                    messageUseCases.addReaction(chatId, messageId, reaction)
                    dispatch(ChatStateAction.ReactionToggled(messageId, reaction, currentUserId, isAdded = true))
                }
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to update reaction"))
            }
        }
    }
}
