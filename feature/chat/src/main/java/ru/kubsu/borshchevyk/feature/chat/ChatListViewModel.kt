package ru.kubsu.borshchevyk.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.chat.CreateGroupChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.PinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UnpinChatUseCase
import ru.kubsu.borshchevyk.core.domain.message.ConnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveGlobalChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveNewMessagesUseCase
import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase,
    private val createGroupChatUseCase: CreateGroupChatUseCase,
    private val connectWebSocketUseCase: ConnectWebSocketUseCase,
    private val observeNewMessagesUseCase: ObserveNewMessagesUseCase,
    private val observeGlobalChatEventsUseCase: ObserveGlobalChatEventsUseCase,
    private val joinChatUseCase: JoinChatUseCase,
    private val pinChatUseCase: PinChatUseCase,
    private val unpinChatUseCase: UnpinChatUseCase
) : ViewModel() {

    private val TAG = "ChatListViewModel"
    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChats()
        connectAndObserveWebSockets()
    }

    private fun connectAndObserveWebSockets() {
        viewModelScope.launch {
            Log.d(TAG, "Initializing WebSocket connection...")
            try {
                connectWebSocketUseCase()
                
                observeNewMessagesUseCase()
                    .onEach { message ->
                        Log.d(TAG, "WS: Received new message notification for chat ${message.chatId}. Reloading chats.")
                        loadChats(showLoading = false)
                    }
                    .launchIn(this)
                
                observeGlobalChatEventsUseCase()
                    .onEach { event ->
                        Log.d(TAG, "WS: Received chat event ${event.action} for chat ${event.chatId}. Reloading chats.")
                        if (event.action == "PINNED" || event.action == "UNPINNED") {
                            loadChats(showLoading = false)
                        }
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connection failed", e)
            }
        }
    }

    fun loadChats(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val chats = getUserChatsUseCase()
                val sortedChats = chats.sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.createdAt })
                _uiState.update { it.copy(chats = sortedChats, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onPinChat(chatId: String) {
        viewModelScope.launch {
            try {
                pinChatUseCase(chatId)
                loadChats(showLoading = false)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onUnpinChat(chatId: String) {
        viewModelScope.launch {
            try {
                unpinChatUseCase(chatId)
                loadChats(showLoading = false)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onCreatePrivateChat(targetUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chat = createPrivateChatUseCase(targetUserId)
                onSuccess(chat.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onCreateGroupChat(title: String, description: String?, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chat = createGroupChatUseCase(title, description)
                onSuccess(chat.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onJoinChat(inviteCode: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chat = joinChatUseCase(inviteCode)
                onSuccess(chat.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
