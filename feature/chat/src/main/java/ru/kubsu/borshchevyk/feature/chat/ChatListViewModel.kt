package ru.kubsu.borshchevyk.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.message.CreateGroupChatUseCase
import ru.kubsu.borshchevyk.core.domain.message.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetUserChatsUseCase
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
    private val createGroupChatUseCase: CreateGroupChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val chats = getUserChatsUseCase()
                _uiState.update { it.copy(chats = chats, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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
}
