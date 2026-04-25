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
import ru.kubsu.borshchevyk.core.domain.chat.ClearChatHistoryUseCase
import ru.kubsu.borshchevyk.core.domain.chat.DeleteChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GenerateInviteLinkUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.InviteUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.KickUserUseCase
import ru.kubsu.borshchevyk.core.domain.chat.LeaveChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateChatInfoUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UpdateMemberPermissionsUseCase
import ru.kubsu.borshchevyk.core.domain.user.AddContactUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetContactsUseCase
import ru.kubsu.borshchevyk.core.domain.user.RemoveContactUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
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
    private val inviteUserUseCase: InviteUserUseCase,
    private val generateInviteLinkUseCase: GenerateInviteLinkUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val updateMemberPermissionsUseCase: UpdateMemberPermissionsUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val kickUserUseCase: KickUserUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val updateChatInfoUseCase: UpdateChatInfoUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val removeContactUseCase: RemoveContactUseCase
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
                val chats = getUserChatsUseCase()
                val chat = chats.find { it.id == chatId }
                val isGroup = chat?.type == ChatType.GROUP
                val isPrivate = chat?.type == ChatType.PRIVATE
                val membersPage = getChatMembersUseCase(chatId, 0, 100)
                
                var isContact = false
                if (isPrivate && chat?.partnerId != null) {
                    val contacts = getContactsUseCase()
                    isContact = contacts.any { it.contactUserId == chat.partnerId }
                }

                _uiState.update { 
                    it.copy(
                        currentUserId = userId,
                        isGroupChat = isGroup,
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
                addContactUseCase(
                    AddContactRequest(
                        targetUserId = partnerId,
                        firstName = firstName,
                        lastName = lastName?.ifBlank { null }
                    )
                )
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
                removeContactUseCase(partnerId)
                _uiState.update { it.copy(isContact = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to remove contact") }
            }
        }
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

    fun onUpdatePermissions(targetUserId: String, request: UpdatePermissionsRequest) {
        viewModelScope.launch {
            try {
                updateMemberPermissionsUseCase(chatId, targetUserId, request)
                val membersPage = getChatMembersUseCase(chatId, 0, 100)
                _uiState.update { it.copy(members = membersPage.content) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onClearHistory(forAll: Boolean) {
        viewModelScope.launch {
            try {
                clearChatHistoryUseCase(chatId, forAll)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onDeleteChat() {
        viewModelScope.launch {
            try {
                deleteChatUseCase(chatId)
                _uiState.update { it.copy(isChatDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onKickUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                kickUserUseCase(chatId, targetUserId)
                val membersPage = getChatMembersUseCase(chatId, 0, 100)
                _uiState.update { it.copy(members = membersPage.content) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onLeaveChat() {
        viewModelScope.launch {
            try {
                leaveChatUseCase(chatId)
                _uiState.update { it.copy(isChatDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onUpdateChatInfo(title: String?, description: String?) {
        viewModelScope.launch {
            try {
                updateChatInfoUseCase(chatId, UpdateChatInfoRequest(title = title, description = description))
                loadData() // Refresh info
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
