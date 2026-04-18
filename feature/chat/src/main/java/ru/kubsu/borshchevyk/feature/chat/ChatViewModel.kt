package ru.kubsu.borshchevyk.feature.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.message.AddReactionUseCase
import ru.kubsu.borshchevyk.core.domain.message.DeleteMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.EditMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.GenerateInviteLinkUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetMessageCommentsUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetMessageReadersUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetPinnedMessagesUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.InviteUserUseCase
import ru.kubsu.borshchevyk.core.domain.message.LoadChatHistoryUseCase
import ru.kubsu.borshchevyk.core.domain.message.MarkMessageAsReadUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveDeletedMessagesUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveNewMessagesUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObservePinsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveReactionsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveReadReceiptsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveTypingUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveUnpinsUseCase
import ru.kubsu.borshchevyk.core.domain.message.PinMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.RemoveReactionUseCase
import ru.kubsu.borshchevyk.core.domain.message.SendMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.SendTypingEventUseCase
import ru.kubsu.borshchevyk.core.domain.message.UnpinMessageUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import javax.inject.Inject

data class ChatUiState(
    val chatId: String = "",
    val currentUserId: String = "",
    val isGroupChat: Boolean = false,
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val commentsByMessageId: Map<String, List<Message>> = emptyMap(),
    val readersByMessageId: Map<String, List<String>> = emptyMap(),
    val members: List<ChatMember> = emptyList(),
    val inviteLink: String? = null,
    val showSettings: Boolean = false,
    val editingMessage: Message? = null,
    val typingUsers: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadChatHistoryUseCase: LoadChatHistoryUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val pinMessageUseCase: PinMessageUseCase,
    private val unpinMessageUseCase: UnpinMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val getPinnedMessagesUseCase: GetPinnedMessagesUseCase,
    private val getMessageReadersUseCase: GetMessageReadersUseCase,
    private val getMessageCommentsUseCase: GetMessageCommentsUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val observeNewMessagesUseCase: ObserveNewMessagesUseCase,
    private val observeDeletedMessagesUseCase: ObserveDeletedMessagesUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val observeReactionsUseCase: ObserveReactionsUseCase,
    private val observePinsUseCase: ObservePinsUseCase,
    private val observeUnpinsUseCase: ObserveUnpinsUseCase,
    private val observeReadReceiptsUseCase: ObserveReadReceiptsUseCase,
    private val sendTypingEventUseCase: SendTypingEventUseCase,
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val inviteUserUseCase: InviteUserUseCase,
    private val generateInviteLinkUseCase: GenerateInviteLinkUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModel() {

    private val TAG = "ChatViewModel"
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow(ChatUiState(chatId = chatId, isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingJob: Job? = null

    init {
        loadData()
        observeWebSockets()
    }

    private fun observeWebSockets() {
        observeNewMessagesUseCase()
            .onEach { messageDto ->
                if (messageDto.chatId.equals(chatId, ignoreCase = true)) {
                    Log.d(TAG, "WS Received MessageDto: id=${messageDto.id}, isDeleted=${messageDto.isDeleted}")
                    
                    _uiState.update { state ->
                        if (messageDto.isDeleted) {
                            Log.d(TAG, "WS Filtering deleted message: ${messageDto.id}")
                            state.copy(
                                messages = state.messages.filterNot { it.id == messageDto.id },
                                pinnedMessages = state.pinnedMessages.filterNot { it.id == messageDto.id }
                            )
                        } else {
                            val existingMsg = state.messages.find { it.id == messageDto.id }
                            if (existingMsg != null) {
                                Log.d(TAG, "WS Updating existing message: ${messageDto.id}")
                                val updatedMsg = existingMsg.copy(
                                    text = messageDto.text,
                                    isDeleted = messageDto.isDeleted
                                )
                                state.copy(
                                    messages = state.messages.map { if (it.id == messageDto.id) updatedMsg else it },
                                    pinnedMessages = state.pinnedMessages.map { if (it.id == messageDto.id) updatedMsg else it }
                                )
                            } else {
                                Log.d(TAG, "WS Adding new message: ${messageDto.id}")
                                val newMsg = Message(
                                    id = messageDto.id,
                                    chatId = messageDto.chatId,
                                    authorId = messageDto.authorId,
                                    text = messageDto.text,
                                    createdAt = messageDto.createdAt,
                                    isDeleted = messageDto.isDeleted,
                                    source = ru.kubsu.borshchevyk.core.model.domain.MessageSource.ONLINE,
                                    isPinned = false,
                                    reactions = emptyList(),
                                    commentsCount = 0,
                                    parentMessageId = null,
                                    forwardedFromChatId = null,
                                    forwardedFromUserId = null
                                )
                                state.copy(messages = listOf(newMsg) + state.messages)
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        observeDeletedMessagesUseCase()
            .onEach { messageId ->
                Log.d(TAG, "WS Received Local Delete ID: $messageId")
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == messageId },
                        pinnedMessages = state.pinnedMessages.filterNot { it.id == messageId }
                    )
                }
            }
            .launchIn(viewModelScope)

        observeTypingUseCase(chatId)
            .onEach { event ->
                Log.d(TAG, "WS Received Typing Event: user=${event.userId}, isTyping=${event.isTyping}")
                _uiState.update { state ->
                    val newTypingUsers = state.typingUsers.toMutableSet()
                    if (event.isTyping) newTypingUsers.add(event.userId) else newTypingUsers.remove(event.userId)
                    state.copy(typingUsers = newTypingUsers)
                }
            }
            .launchIn(viewModelScope)

        observeReactionsUseCase(chatId)
            .onEach { event ->
                Log.d(TAG, "WS Received Reaction Event: msg=${event.messageId}, reaction=${event.reaction}, added=${event.isAdded}")
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == event.messageId) {
                            val newReactions = msg.reactions.toMutableList()
                            if (event.isAdded) {
                                if (!newReactions.any { it.reaction == event.reaction && it.userId == event.userId }) {
                                    newReactions.add(MessageReaction(event.userId, event.reaction))
                                }
                            } else {
                                newReactions.removeAll { it.reaction == event.reaction && it.userId == event.userId }
                            }
                            msg.copy(reactions = newReactions)
                        } else msg
                    })
                }
            }
            .launchIn(viewModelScope)

        observePinsUseCase(chatId)
            .onEach { messageId ->
                Log.d(TAG, "WS Received Pin: $messageId")
                val pinned = getPinnedMessagesUseCase(chatId)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { 
                        if (it.id == messageId) it.copy(isPinned = true) else it 
                    }
                    state.copy(messages = updatedMessages, pinnedMessages = pinned)
                }
            }
            .launchIn(viewModelScope)

        observeUnpinsUseCase(chatId)
            .onEach { messageId ->
                Log.d(TAG, "WS Received Unpin: $messageId")
                val pinned = getPinnedMessagesUseCase(chatId)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { 
                        if (it.id == messageId) it.copy(isPinned = false) else it 
                    }
                    state.copy(messages = updatedMessages, pinnedMessages = pinned)
                }
            }
            .launchIn(viewModelScope)
            
        observeReadReceiptsUseCase(chatId)
            .onEach { event ->
                Log.d(TAG, "WS Received Read Receipt: msg=${event.messageId}, user=${event.userId}")
                val readers = getMessageReadersUseCase(chatId, event.messageId)
                _uiState.update { state ->
                    val newReadersMap = state.readersByMessageId.toMutableMap()
                    newReadersMap[event.messageId] = readers
                    state.copy(readersByMessageId = newReadersMap)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val chats = getUserChatsUseCase()
                val chat = chats.find { it.id == chatId }
                val isGroup = chat?.type == ChatType.GROUP
                val history = loadChatHistoryUseCase(chatId).filterNot { it.isDeleted }
                val pinned = getPinnedMessagesUseCase(chatId).filterNot { it.isDeleted }
                val membersPage = getChatMembersUseCase(chatId, 0, 100)
                _uiState.update { 
                    it.copy(
                        currentUserId = userId,
                        isGroupChat = isGroup,
                        messages = history, 
                        pinnedMessages = pinned,
                        members = membersPage.content,
                        isLoading = false 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun onInviteUser(userId: String) {
        viewModelScope.launch {
            try {
                inviteUserUseCase(chatId, userId)
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onGenerateInviteLink() {
        viewModelScope.launch {
            try {
                val link = generateInviteLinkUseCase(chatId)
                _uiState.update { it.copy(inviteLink = link) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onTyping() {
        viewModelScope.launch {
            sendTypingEventUseCase(chatId, true)
            typingJob?.cancel()
            typingJob = launch {
                delay(3000)
                sendTypingEventUseCase(chatId, false)
            }
        }
    }

    fun onSendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val newMessage = sendMessageUseCase(chatId, text)
                _uiState.update { state ->
                    if (!state.messages.any { it.id == newMessage.id }) {
                        state.copy(messages = listOf(newMessage) + state.messages)
                    } else {
                        state
                    }
                }
                sendTypingEventUseCase(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setEditingMessage(message: Message?) {
        _uiState.update { it.copy(editingMessage = message) }
    }

    fun onEditMessage(messageId: String, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            try {
                val updatedMessage = editMessageUseCase(chatId, messageId, newText)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { if (it.id == messageId) updatedMessage else it },
                        pinnedMessages = state.pinnedMessages.map { if (it.id == messageId) updatedMessage else it },
                        editingMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onDeleteMessage(messageId: String, forAll: Boolean = false) {
        viewModelScope.launch {
            try {
                deleteMessageUseCase(chatId, messageId, forAll)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onMessageVisible(messageId: String) {
        val currentUserId = _uiState.value.currentUserId
        val message = _uiState.value.messages.find { it.id == messageId } ?: return

        if (message.authorId != currentUserId) {
            viewModelScope.launch {
                try {
                    markMessageAsReadUseCase(chatId, messageId)
                } catch (e: Exception) {
                }
            }
        }
    }

    fun onLoadReaders(messageId: String) {
        viewModelScope.launch {
            try {
                val readers = getMessageReadersUseCase(chatId, messageId)
                _uiState.update { state ->
                    val newReadersMap = state.readersByMessageId.toMutableMap()
                    newReadersMap[messageId] = readers
                    state.copy(readersByMessageId = newReadersMap)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onLoadComments(messageId: String) {
        viewModelScope.launch {
            try {
                val comments = getMessageCommentsUseCase(chatId, messageId)
                _uiState.update { state ->
                    val newCommentsMap = state.commentsByMessageId.toMutableMap()
                    newCommentsMap[messageId] = comments
                    state.copy(commentsByMessageId = newCommentsMap)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onPinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                pinMessageUseCase(chatId, messageId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onUnpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                unpinMessageUseCase(chatId, messageId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onToggleReaction(messageId: String, reaction: String) {
        val currentUserId = _uiState.value.currentUserId
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        
        val hasReaction = message.reactions.any { it.reaction == reaction && it.userId == currentUserId }
        
        viewModelScope.launch {
            try {
                if (hasReaction) {
                    removeReactionUseCase(chatId, messageId, reaction)
                } else {
                    addReactionUseCase(chatId, messageId, reaction)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
