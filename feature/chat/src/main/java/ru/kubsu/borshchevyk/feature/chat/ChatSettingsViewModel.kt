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

/**
 * Represents the UI state for the chat settings screen.
 *
 * @property currentUserId The ID of the currently authenticated user.
 * @property isGroupChat Indicates whether the current chat is a group chat.
 * @property members The list of members in the chat (if applicable).
 * @property inviteLink The generated invite link for the chat, if any.
 * @property isChatDeleted Indicates whether the chat has been deleted.
 * @property isContact Indicates whether the chat partner is in the user's contacts.
 * @property partnerId The ID of the chat partner (for private chats).
 * @property partnerFirstName The first name of the chat partner.
 * @property partnerLastName The last name of the chat partner.
 * @property isDeletable Indicates whether the chat can be deleted.
 * @property chatName The title or display name of the chat.
 * @property chatDescription The description of the chat.
 * @property chatAvatarUrl The URL for the chat's avatar image.
 * @property isLoading Indicates if settings data is currently being loaded.
 * @property error An optional error message if an operation failed.
 */
data class ChatSettingsUiState(
    val currentUserId: String = "",
    val isGroupChat: Boolean = false,
    val members: List<ChatMember> = emptyList(),
    val inviteLink: String? = null,
    val isChatDeleted: Boolean = false,
    val isContact: Boolean = false,
    val partnerId: String? = null,
    val partnerTag: String? = null,
    val partnerFirstName: String? = null,
    val partnerLastName: String? = null,
    val isDeletable: Boolean = true,
    val chatName: String = "",
    val chatDescription: String? = null,
    val chatAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * [ChatSettingsViewModel] manages the settings and configuration for a specific chat.
 * It handles member management, permissions, updating chat info, and contact actions.
 */
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
    
    /**
     * A state flow representing the current UI state of the chat settings.
     */
    val uiState: StateFlow<ChatSettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads the initial settings data including chat details, members, and contact status.
     */
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

                val chatName = if (isPrivate) chat?.partnerName ?: "" else chat?.title ?: ""
                val chatAvatarUrl = if (isPrivate) chat?.partnerAvatarUrl else null // Could also handle group avatar
                
                val partnerTag = if (isPrivate) {
                    val partnerMember = membersPage.content.find { it.userId == chat?.partnerId }
                    partnerMember?.user?.tag
                } else null

                _uiState.update { 
                    it.copy(
                        currentUserId = userId,
                        isGroupChat = chat?.type == ChatType.GROUP,
                        members = membersPage.content,
                        isContact = isContact,
                        partnerId = chat?.partnerId,
                        partnerTag = partnerTag,
                        partnerFirstName = chat?.partnerName?.substringBefore(" "),
                        partnerLastName = chat?.partnerName?.substringAfter(" ", missingDelimiterValue = ""),
                        isDeletable = chat?.isDeletable ?: true,
                        chatName = chatName,
                        chatDescription = chat?.description,
                        chatAvatarUrl = chatAvatarUrl,
                        isLoading = false 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Adds the current chat partner to the user's contacts.
     *
     * @param firstName The first name to save for the contact.
     * @param lastName The last name to save for the contact.
     */
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

    /**
     * Removes the current chat partner from the user's contacts.
     */
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

    /**
     * Invites a new user to the chat.
     *
     * @param userId The ID of the user to invite.
     */
    fun onInviteUser(userId: String) = viewModelScope.launch {
        try {
            settingsHandler.inviteUser(chatId, userId)
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    /**
     * Generates a new invite link for the chat.
     */
    fun onGenerateInviteLink() = viewModelScope.launch {
        try {
            val link = settingsHandler.generateInviteLink(chatId)
            _uiState.update { it.copy(inviteLink = link) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    /**
     * Updates the permissions for a specific member in the chat.
     *
     * @param targetUserId The ID of the user whose permissions are being updated.
     * @param canSendMessages Whether the user can send messages.
     * @param canDeleteMessages Whether the user can delete messages.
     * @param canInviteUsers Whether the user can invite others.
     * @param canChangeInfo Whether the user can change chat info.
     */
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

    /**
     * Clears the chat history.
     *
     * @param forAll Whether to clear the history for all participants.
     */
    fun onClearHistory(forAll: Boolean) = viewModelScope.launch {
        try { settingsHandler.clearHistory(chatId, forAll) } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
    }

    /**
     * Deletes the chat entirely.
     */
    fun onDeleteChat() = viewModelScope.launch {
        try {
            settingsHandler.deleteChat(chatId)
            _uiState.update { it.copy(isChatDeleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    /**
     * Kicks a user from the chat.
     *
     * @param targetUserId The ID of the user to kick.
     */
    fun onKickUser(targetUserId: String) = viewModelScope.launch {
        try {
            settingsHandler.kickUser(chatId, targetUserId)
            val membersPage = getChatMembersUseCase(chatId, 0, 100)
            _uiState.update { it.copy(members = membersPage.content) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    /**
     * Leaves the chat.
     */
    fun onLeaveChat() = viewModelScope.launch {
        try {
            settingsHandler.leaveChat(chatId)
            _uiState.update { it.copy(isChatDeleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    /**
     * Updates the chat's title and description.
     *
     * @param title The new chat title.
     * @param description The new chat description.
     */
    fun onUpdateChatInfo(title: String?, description: String?) = viewModelScope.launch {
        try {
            settingsHandler.updateChatInfo(chatId, title, description)
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }
}
