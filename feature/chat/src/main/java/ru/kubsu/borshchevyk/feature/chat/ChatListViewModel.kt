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
import ru.kubsu.borshchevyk.core.domain.chat.ObserveUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.SyncUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.PinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UnpinChatUseCase
import ru.kubsu.borshchevyk.core.domain.message.ConnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveGlobalChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveNewMessagesUseCase
import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

/**
 * Represents the UI state for the chat list screen.
 *
 * @property chats The list of chats available to the user.
 * @property isLoading Indicates if the chat list is currently being loaded or refreshed.
 * @property error An optional error message if loading or an operation failed.
 */
data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * [ChatListViewModel] manages the data and business logic for the chat list screen.
 * It handles loading chats, real-time updates via WebSockets, and user actions such as
 * creating, pinning, or joining chats.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val observeUserChatsUseCase: ObserveUserChatsUseCase,
    private val syncUserChatsUseCase: SyncUserChatsUseCase,
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
    
    /**
     * A state flow representing the current UI state of the chat list.
     */
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        observeChats()
        loadChats()
        connectAndObserveWebSockets()
    }

    /**
     * Observes the user's chats from the local database and updates the UI state.
     * Sorts the chats by pinned status and creation date.
     */
    private fun observeChats() {
        observeUserChatsUseCase()
            .onEach { chats ->
                val sortedChats = chats.sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.createdAt })
                _uiState.update { it.copy(chats = sortedChats, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Initializes the WebSocket connection and observes global chat events
     * and new messages for real-time synchronization.
     */
    private fun connectAndObserveWebSockets() {
        viewModelScope.launch {
            Log.d(TAG, "Initializing WebSocket connection...")
            try {
                connectWebSocketUseCase()
                
                observeNewMessagesUseCase()
                    .onEach { message ->
                        Log.d(TAG, "WS: Received new message notification for chat ${message.chatId}. UI will update via DB Flow.")
                    }
                    .launchIn(this)
                
                observeGlobalChatEventsUseCase()
                    .onEach { event ->
                        Log.d(TAG, "WS: Received chat event ${event.action} for chat ${event.chat.id}. UI will update via DB Flow.")
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connection failed", e)
            }
        }
    }

    /**
     * Syncs the user's chats with the backend server.
     *
     * @param showLoading Whether to show a loading indicator during the sync.
     */
    fun loadChats(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                syncUserChatsUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Pins a specific chat to the top of the chat list.
     *
     * @param chatId The unique identifier of the chat to pin.
     */
    fun onPinChat(chatId: String) {
        viewModelScope.launch {
            try {
                pinChatUseCase(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Unpins a previously pinned chat.
     *
     * @param chatId The unique identifier of the chat to unpin.
     */
    fun onUnpinChat(chatId: String) {
        viewModelScope.launch {
            try {
                unpinChatUseCase(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Creates a new private chat with another user.
     *
     * @param targetUserId The ID of the user to start a chat with.
     * @param onSuccess Callback invoked with the new chat's ID upon success.
     */
    fun onCreatePrivateChat(targetUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chatId = createPrivateChatUseCase(targetUserId)
                onSuccess(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Creates a new group chat.
     *
     * @param title The title of the group chat.
     * @param description An optional description for the group chat.
     * @param onSuccess Callback invoked with the new chat's ID upon success.
     */
    fun onCreateGroupChat(title: String, description: String?, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chatId = createGroupChatUseCase(title, description)
                onSuccess(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Joins an existing chat using an invite code.
     *
     * @param inviteCode The invite code for the chat.
     * @param onSuccess Callback invoked with the joined chat's ID upon success.
     */
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
