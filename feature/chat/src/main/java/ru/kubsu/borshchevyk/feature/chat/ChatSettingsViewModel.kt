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
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.feature.chat.handlers.ChatSettingsHandler
import ru.kubsu.borshchevyk.feature.chat.handlers.ContactHandler
import javax.inject.Inject

data class ChatSettingsUiState(
    val currentUserId: String = "",
    val isGroupChat: Boolean = false,
    val members: List<ChatMember> = emptyList(),
    val inviteLink: String? = null,
    val isChatDeleted: Boolean = false,
    val isContact: Boolean = false,
    val partnerId: String? = null,
    val partnerFirstName: String? = null,
    val partnerLastName: String? = null,
    val isDeletable: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val settingsHandler: ChatSettingsHandler,
    private val contactHandler: ContactHandler
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow(ChatSettingsUiState(isLoading = true))
    val uiState: StateFlow<ChatSettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = getUserIdUseCase().firstOrNull() ?: ""
                val chat = getUserChatsUseCase().find { it.id == chatId }
                val membersPage = getChatMembersUseCase(chatId, 0, 100)
                
                val isPrivate = chat?.type == ChatType.PRIVATE
                val isContact = if (isPrivate && chat?.partnerId != null) {
                    contactHandler.isContact(chat.partnerId!!)
                } else false

                _uiState.update { 
                    it.copy(
                        currentUserId = userId,
                        isGroupChat = chat?.type == ChatType.GROUP,
                        members = membersPage.content,
                        isContact = isContact,
                        partnerId = chat?.partnerId,
                        partnerFirstName = chat?.partnerName?.substringBefore(" "),
                        partnerLastName = chat?.partnerName?.substringAfter(" ", missingDelimiterValue = ""),
                        isDeletable = chat?.isDeletable ?: true,
                        isLoading = false 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onAddContact(firstName: String, lastName: String?) {
        val partnerId = _uiState.value.partnerId ?: return
        viewModelScope.launch {
            try {
                contactHandler.addContact(partnerId, firstName, lastName)
                _uiState.update { it.copy(isContact = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add contact") }
            }
        }
    }

    fun onRemoveContact() {
        val partnerId = _uiState.value.partnerId ?: return
        viewModelScope.launch {
            try {
                contactHandler.removeContact(partnerId)
                _uiState.update { it.copy(isContact = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to remove contact") }
            }
        }
    }

    fun onInviteUser(userId: String) = viewModelScope.launch {
        try {
            settingsHandler.inviteUser(chatId, userId)
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onGenerateInviteLink() = viewModelScope.launch {
        try {
            val link = settingsHandler.generateInviteLink(chatId)
            _uiState.update { it.copy(inviteLink = link) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onUpdatePermissions(
        targetUserId: String,
        canSendMessages: Boolean,
        canDeleteMessages: Boolean,
        canInviteUsers: Boolean,
        canChangeInfo: Boolean
    ) = viewModelScope.launch {
        try {
            settingsHandler.updatePermissions(chatId, targetUserId, canSendMessages, canDeleteMessages, canInviteUsers, canChangeInfo)
            val membersPage = getChatMembersUseCase(chatId, 0, 100)
            _uiState.update { it.copy(members = membersPage.content) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onClearHistory(forAll: Boolean) = viewModelScope.launch {
        try { settingsHandler.clearHistory(chatId, forAll) } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
    }

    fun onDeleteChat() = viewModelScope.launch {
        try {
            settingsHandler.deleteChat(chatId)
            _uiState.update { it.copy(isChatDeleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onKickUser(targetUserId: String) = viewModelScope.launch {
        try {
            settingsHandler.kickUser(chatId, targetUserId)
            val membersPage = getChatMembersUseCase(chatId, 0, 100)
            _uiState.update { it.copy(members = membersPage.content) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onLeaveChat() = viewModelScope.launch {
        try {
            settingsHandler.leaveChat(chatId)
            _uiState.update { it.copy(isChatDeleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun onUpdateChatInfo(title: String?, description: String?) = viewModelScope.launch {
        try {
            settingsHandler.updateChatInfo(chatId, title, description)
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }
}
