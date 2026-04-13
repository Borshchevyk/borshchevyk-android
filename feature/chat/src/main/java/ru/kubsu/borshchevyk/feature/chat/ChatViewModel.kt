package ru.kubsu.borshchevyk.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.message.AddReactionUseCase
import ru.kubsu.borshchevyk.core.domain.message.DeleteMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetMessageCommentsUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetMessageReadersUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetPinnedMessagesUseCase
import ru.kubsu.borshchevyk.core.domain.message.LoadChatHistoryUseCase
import ru.kubsu.borshchevyk.core.domain.message.MarkMessageAsReadUseCase
import ru.kubsu.borshchevyk.core.domain.message.PinMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.RemoveReactionUseCase
import ru.kubsu.borshchevyk.core.domain.message.SendMessageUseCase
import ru.kubsu.borshchevyk.core.domain.message.UnpinMessageUseCase
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import javax.inject.Inject

data class ChatUiState(
    val chatId: String = "",
    val currentUserId: String = "",
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val commentsByMessageId: Map<String, List<Message>> = emptyMap(),
    val readersByMessageId: Map<String, List<String>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadChatHistoryUseCase: LoadChatHistoryUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val pinMessageUseCase: PinMessageUseCase,
    private val unpinMessageUseCase: UnpinMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val getPinnedMessagesUseCase: GetPinnedMessagesUseCase,
    private val getMessageReadersUseCase: GetMessageReadersUseCase,
    private val getMessageCommentsUseCase: GetMessageCommentsUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow(ChatUiState(chatId = chatId, isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val history = loadChatHistoryUseCase(chatId)
                val pinned = getPinnedMessagesUseCase(chatId)
                _uiState.update { 
                    it.copy(
                        currentUserId = userId,
                        messages = history, 
                        pinnedMessages = pinned,
                        isLoading = false 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val newMessage = sendMessageUseCase(chatId, text)
                _uiState.update { 
                    it.copy(messages = listOf(newMessage) + it.messages)
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
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == messageId },
                        pinnedMessages = state.pinnedMessages.filterNot { it.id == messageId }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onMessageVisible(messageId: String) {
        val currentUserId = _uiState.value.currentUserId
        val message = _uiState.value.messages.find { it.id == messageId } ?: return

        // Ideally we only want to mark message as read if it's not ours and we haven't marked it yet.
        // For now, let's fire and forget.
        if (message.authorId != currentUserId) {
            viewModelScope.launch {
                try {
                    markMessageAsReadUseCase(chatId, messageId)
                } catch (e: Exception) {
                    // Ignore background errors for read receipts
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
                // Refresh pinned messages
                val pinned = getPinnedMessagesUseCase(chatId)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { 
                        if (it.id == messageId) it.copy(isPinned = true) else it 
                    }
                    state.copy(
                        messages = updatedMessages,
                        pinnedMessages = pinned
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onUnpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                unpinMessageUseCase(chatId, messageId)
                // Refresh pinned messages
                val pinned = getPinnedMessagesUseCase(chatId)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { 
                        if (it.id == messageId) it.copy(isPinned = false) else it 
                    }
                    state.copy(
                        messages = updatedMessages,
                        pinnedMessages = pinned
                    )
                }
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
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(reactions = msg.reactions.filterNot { it.reaction == reaction && it.userId == currentUserId })
                            } else msg
                        })
                    }
                } else {
                    addReactionUseCase(chatId, messageId, reaction)
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(reactions = msg.reactions + MessageReaction(currentUserId, reaction))
                            } else msg
                        })
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
