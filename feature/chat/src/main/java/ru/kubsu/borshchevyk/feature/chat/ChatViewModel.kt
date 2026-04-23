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
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatHistoryUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.domain.message.ObserveChatEventsUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val observeChatEventsUseCase: ObserveChatEventsUseCase,
    private val messageUseCases: ChatMessageUseCases,
    private val historyUseCases: ChatHistoryUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases
) : ViewModel() {

    private val TAG = "ChatViewModel"
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var typingJob: Job? = null

    init {
        loadData()
        observeWebSockets()
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.OpenSettings -> openSettings()
            is ChatIntent.ChatDeletedLocally -> onChatDeletedLocally()
            is ChatIntent.Typing -> onTyping()
            is ChatIntent.SendMessage -> onSendMessage(intent.text, intent.attachments)
            is ChatIntent.SetEditingMessage -> setEditingMessage(intent.message)
            is ChatIntent.EditMessage -> onEditMessage(intent.messageId, intent.newText)
            is ChatIntent.DeleteMessage -> onDeleteMessage(intent.messageId, intent.forAll)
            is ChatIntent.MessageVisible -> onMessageVisible(intent.messageId)
            is ChatIntent.LoadReaders -> onLoadReaders(intent.messageId)
            is ChatIntent.LoadComments -> onLoadComments(intent.messageId)
            is ChatIntent.PinMessage -> onPinMessage(intent.messageId)
            is ChatIntent.UnpinMessage -> onUnpinMessage(intent.messageId)
            is ChatIntent.ToggleReaction -> onToggleReaction(intent.messageId, intent.reaction)
            is ChatIntent.ResolveAttachmentUrl -> resolveAttachmentUrl(intent.attachmentId)
        }
    }

    private fun observeWebSockets() {
        observeChatEventsUseCase(chatId)
            .onEach { event ->
                _uiState.update { it.reduce(event) }
                
                // Handle side effects of events independently
                when (event) {
                    is ChatEvent.MessagePinned, is ChatEvent.MessageUnpinned -> {
                        viewModelScope.launch {
                            try {
                                val pinned = historyUseCases.getPinnedMessages(chatId)
                                _uiState.update { state -> 
                                    if (state is ChatUiState.Content) {
                                        state.copy(feed = state.feed.copy(pinnedMessages = pinned))
                                    } else state
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load pinned messages", e)
                            }
                        }
                    }
                    is ChatEvent.ReadReceipt -> {
                        viewModelScope.launch {
                            try {
                                val readers = historyUseCases.getMessageReaders(chatId, event.event.messageId)
                                _uiState.update { state ->
                                    if (state is ChatUiState.Content) {
                                        val newMap = state.feed.readersByMessageId.toMutableMap()
                                        newMap[event.event.messageId] = readers
                                        state.copy(feed = state.feed.copy(readersByMessageId = newMap))
                                    } else state
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load readers", e)
                            }
                        }
                    }
                    is ChatEvent.NewMessage -> {
                        // Pre-resolve attachment URLs for new messages
                        event.message.attachments?.forEach { attachment ->
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
            _uiState.value = ChatUiState.Loading
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val chats = getUserChatsUseCase()
                val chat = chats.find { it.id == chatId }
                val isGroup = chat?.type == ChatType.GROUP
                val history = historyUseCases.loadChatHistory(chatId).filterNot { it.isDeleted }
                val pinned = historyUseCases.getPinnedMessages(chatId).filterNot { it.isDeleted }
                
                _uiState.value = ChatUiState.Content(
                    context = ChatContext(
                        chatId = chatId,
                        currentUserId = userId,
                        isGroupChat = isGroup
                    ),
                    feed = MessageFeed(
                        messages = history,
                        pinnedMessages = pinned
                    )
                )

                // Pre-resolve URLs
                history.forEach { msg ->
                    msg.attachments.forEach { att ->
                        resolveAttachmentUrl(att.id)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to load chat")
            }
        }
    }

    private fun openSettings() {
        viewModelScope.launch {
            _effect.send(ChatEffect.NavigateToSettings(chatId))
        }
    }

    private fun onChatDeletedLocally() {
        _uiState.update { state ->
            if (state is ChatUiState.Content) state.copy(isChatDeleted = true) else state
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
                // Ignore silent typing errors
            }
        }
    }

    private fun onSendMessage(text: String, attachments: List<AttachmentFile>) {
        if (text.isBlank() && attachments.isEmpty()) return
        
        _uiState.update { state -> 
            if (state is ChatUiState.Content) state.copy(input = state.input.copy(isSending = true)) else state 
        }

        viewModelScope.launch {
            try {
                val attachmentIds = attachments.map { file ->
                    val type = when {
                        file.contentType.startsWith("image/") -> AttachmentType.PHOTO
                        file.contentType.startsWith("video/") -> AttachmentType.VIDEO
                        file.contentType.startsWith("audio/") -> AttachmentType.VOICE
                        else -> AttachmentType.FILE
                    }
                    attachmentUseCases.uploadAttachment(
                        fileBytes = file.bytes,
                        originalFilename = file.originalFilename,
                        contentType = file.contentType,
                        extension = file.extension,
                        type = type,
                        width = file.width,
                        height = file.height,
                        duration = file.duration?.toDouble()
                    )
                }

                val newMessage = messageUseCases.sendMessage(chatId, text, attachmentIds)
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        if (!state.feed.messages.any { it.id == newMessage.id }) {
                            state.copy(
                                feed = state.feed.copy(messages = listOf(newMessage) + state.feed.messages),
                                input = state.input.copy(isSending = false)
                            )
                        } else {
                            state.copy(input = state.input.copy(isSending = false))
                        }
                    } else state
                }
                
                newMessage.attachments.forEach {
                    resolveAttachmentUrl(it.id)
                }

                messageUseCases.sendTypingEvent(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _uiState.update { state -> 
                    if (state is ChatUiState.Content) state.copy(input = state.input.copy(isSending = false)) else state 
                }
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to send message"))
            }
        }
    }

    private fun resolveAttachmentUrl(attachmentId: String) {
        val state = _uiState.value as? ChatUiState.Content ?: return
        if (state.feed.attachmentUrls.containsKey(attachmentId)) return

        viewModelScope.launch {
            try {
                val url = attachmentUseCases.getAttachmentUrl(attachmentId)
                _uiState.update { s ->
                    if (s is ChatUiState.Content) {
                        val newMap = s.feed.attachmentUrls.toMutableMap()
                        newMap[attachmentId] = url
                        s.copy(feed = s.feed.copy(attachmentUrls = newMap))
                    } else s
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve attachment URL: \${e.message}", e)
            }
        }
    }

    private fun setEditingMessage(message: Message?) {
        _uiState.update { state -> 
            if (state is ChatUiState.Content) state.copy(input = state.input.copy(editingMessage = message)) else state 
        }
    }

    private fun onEditMessage(messageId: String, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            try {
                val updatedMessage = messageUseCases.editMessage(chatId, messageId, newText)
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        state.copy(
                            feed = state.feed.copy(
                                messages = state.feed.messages.map { if (it.id == messageId) updatedMessage else it },
                                pinnedMessages = state.feed.pinnedMessages.map { if (it.id == messageId) updatedMessage else it }
                            ),
                            input = state.input.copy(editingMessage = null)
                        )
                    } else state
                }
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to edit message"))
            }
        }
    }

    private fun onDeleteMessage(messageId: String, forAll: Boolean = false) {
        viewModelScope.launch {
            try {
                messageUseCases.deleteMessage(chatId, messageId, forAll)
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        state.copy(
                            feed = state.feed.copy(
                                messages = state.feed.messages.filterNot { it.id == messageId },
                                pinnedMessages = state.feed.pinnedMessages.filterNot { it.id == messageId }
                            )
                        )
                    } else state
                }
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
                    // Ignore silent read receipt errors
                }
            }
        }
    }

    private fun onLoadReaders(messageId: String) {
        viewModelScope.launch {
            try {
                val readers = historyUseCases.getMessageReaders(chatId, messageId)
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        val newMap = state.feed.readersByMessageId.toMutableMap()
                        newMap[messageId] = readers
                        state.copy(feed = state.feed.copy(readersByMessageId = newMap))
                    } else state
                }
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to load readers"))
            }
        }
    }

    private fun onLoadComments(messageId: String) {
        viewModelScope.launch {
            try {
                val comments = historyUseCases.getMessageComments(chatId, messageId)
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        val newMap = state.feed.commentsByMessageId.toMutableMap()
                        newMap[messageId] = comments
                        state.copy(feed = state.feed.copy(commentsByMessageId = newMap))
                    } else state
                }
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
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        val updatedMessages = state.feed.messages.map {
                            if (it.id == messageId) it.copy(isPinned = true) else it
                        }
                        state.copy(
                            feed = state.feed.copy(
                                messages = updatedMessages,
                                pinnedMessages = pinned
                            )
                        )
                    } else state
                }
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
                _uiState.update { state ->
                    if (state is ChatUiState.Content) {
                        val updatedMessages = state.feed.messages.map {
                            if (it.id == messageId) it.copy(isPinned = false) else it
                        }
                        state.copy(
                            feed = state.feed.copy(
                                messages = updatedMessages,
                                pinnedMessages = pinned
                            )
                        )
                    } else state
                }
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
                    _uiState.update { s ->
                        if (s is ChatUiState.Content) {
                            s.copy(feed = s.feed.copy(messages = s.feed.messages.map { msg ->
                                if (msg.id == messageId) {
                                    msg.copy(reactions = msg.reactions.filterNot { it.reaction == reaction && it.userId == currentUserId })
                                } else msg
                            }))
                        } else s
                    }
                } else {
                    messageUseCases.addReaction(chatId, messageId, reaction)
                    _uiState.update { s ->
                        if (s is ChatUiState.Content) {
                            s.copy(feed = s.feed.copy(messages = s.feed.messages.map { msg ->
                                if (msg.id == messageId) {
                                    msg.copy(reactions = msg.reactions + MessageReaction(currentUserId, reaction))
                                } else msg
                            }))
                        } else s
                    }
                }
            } catch (e: Exception) {
                _effect.send(ChatEffect.ShowError(e.message ?: "Failed to update reaction"))
            }
        }
    }
}
