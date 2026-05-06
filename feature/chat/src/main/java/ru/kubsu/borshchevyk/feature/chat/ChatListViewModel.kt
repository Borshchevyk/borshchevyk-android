package ru.kubsu.borshchevyk.feature.chat

import android.os.Build
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
import ru.kubsu.borshchevyk.core.domain.auth.GetTagUseCase
import ru.kubsu.borshchevyk.core.domain.chat.CreateGroupChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GlobalSearchUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.ObserveUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.PinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.SyncUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UnpinChatUseCase
import ru.kubsu.borshchevyk.core.domain.message.ConnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.domain.message.DisconnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveGlobalChatEventsUseCase
import ru.kubsu.borshchevyk.core.domain.message.ObserveNewMessagesUseCase
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshPeer
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
    val error: String? = null,
    val networkMode: NetworkMode = NetworkMode.GLOBAL,
    val connectedPeersCount: Int = 0,
    val connectedPeers: List<MeshPeer> = emptyList()
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
    private val unpinChatUseCase: UnpinChatUseCase,
    private val transportModeManager: TransportModeManager,
    private val meshConnectionManager: MeshConnectionManager,
    private val disconnectWebSocketUseCase: DisconnectWebSocketUseCase,
    private val getTagUseCase: GetTagUseCase,
    private val globalSearchUseCase: GlobalSearchUseCase
) : ViewModel() {

    private val TAG = "ChatListViewModel"
    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    private var currentUserName = Build.MODEL
    
    /**
     * A state flow representing the current UI state of the chat list.
     */
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        observeChats()
        loadChats()
        connectAndObserveWebSockets()
        observeNetworkModeAndPeers()
        observeUser()
    }

    private fun observeUser() {
        getTagUseCase().onEach { tag ->
            if (!tag.isNullOrBlank()) {
                currentUserName = tag
            }
        }.launchIn(viewModelScope)
    }

    private fun observeNetworkModeAndPeers() {
        transportModeManager.networkMode.onEach { mode ->
            _uiState.update { it.copy(networkMode = mode) }
        }.launchIn(viewModelScope)

        meshConnectionManager.connectedPeers.onEach { peers ->
            _uiState.update { it.copy(connectedPeersCount = peers.size, connectedPeers = peers) }
        }.launchIn(viewModelScope)
    }

    fun toggleNetworkMode() {
        viewModelScope.launch {
            val currentMode = _uiState.value.networkMode
            if (currentMode == NetworkMode.GLOBAL) {
                // Switch to MESH
                transportModeManager.setMode(NetworkMode.MESH)
                try { disconnectWebSocketUseCase() } catch (e: Exception) { Log.e(TAG, "Failed to disconnect WS", e) }
                meshConnectionManager.startAdvertising(currentUserName)
                meshConnectionManager.startDiscovery(currentUserName)
            } else {
                // Switch to GLOBAL
                transportModeManager.setMode(NetworkMode.GLOBAL)
                meshConnectionManager.stopAdvertising()
                meshConnectionManager.stopDiscovery()
                meshConnectionManager.stopAllEndpoints()
                try { connectWebSocketUseCase() } catch (e: Exception) { Log.e(TAG, "Failed to connect WS", e) }
            }
        }
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

    /**
     * Creates a private chat by resolving the user's tag to their ID using local search.
     * Useful in Mesh mode to quickly start a chat from the connected peers list.
     *
     * @param tag The tag of the user.
     * @param onSuccess Callback invoked with the new chat's ID.
     */
    fun onCreatePrivateChatByTag(tag: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "onCreatePrivateChatByTag: Attempting to create chat for tag '$tag'")
            try {
                // Search for the user by tag
                val results = globalSearchUseCase(tag)
                Log.d(TAG, "onCreatePrivateChatByTag: Search returned ${results.users.size} users")
                
                val targetUser = results.users.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                
                if (targetUser != null) {
                    Log.d(TAG, "onCreatePrivateChatByTag: Found exact user match with ID ${targetUser.userId}")
                    val chatId = createPrivateChatUseCase(targetUser.userId)
                    Log.d(TAG, "onCreatePrivateChatByTag: Chat created/found with ID $chatId. Invoking onSuccess.")
                    onSuccess(chatId)
                } else {
                    Log.w(TAG, "onCreatePrivateChatByTag: Exact tag '$tag' not found in results: ${results.users.map { it.tag }}")
                    _uiState.update { it.copy(error = "User $tag not found in local database") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "onCreatePrivateChatByTag: Exception occurred", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
