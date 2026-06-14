package ru.kubsu.borshchevyk.feature.chat.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.ui.mvi.mviContainer
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ChatSettingsDataLoader
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ChatSettingsHandler
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ContactHandler
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsEffect
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsIntent
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsUiState
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataLoader: ChatSettingsDataLoader,
    private val chatSettingsHandler: ChatSettingsHandler,
    private val contactHandler: ContactHandler
) : ViewModel() {

    private val container = mviContainer<ChatSettingsUiState, ChatSettingsEffect>(ChatSettingsUiState(isLoading = true), viewModelScope)
    val uiState = container.uiState
    val effect = container.effect

    private fun sendEffect(effect: ChatSettingsEffect) {
        container.sendEffect(effect)
    }

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    init {
        handleIntent(ChatSettingsIntent.LoadData)
    }

    private suspend fun reloadData() {
        val data = dataLoader.loadData(chatId)
        container.updateState { it.copy(
            isLoading = false,
            currentUserId = data.currentUserId,
            isGroupChat = data.isGroupChat,
            members = data.members.toPersistentList(),
            isContact = data.isContact,
            partnerId = data.partnerId,
            partnerTag = data.partnerTag,
            partnerFirstName = data.partnerFirstName,
            partnerLastName = data.partnerLastName,
            isDeletable = data.isDeletable,
            chatName = data.chatName,
            chatDescription = data.chatDescription,
            chatAvatarUrl = data.chatAvatarUrl
        ) }
    }

    fun handleIntent(intent: ChatSettingsIntent) {
        when (intent) {
            is ChatSettingsIntent.LoadData -> {
                viewModelScope.launch {
                    container.updateState { it.copy(isLoading = true, error = null) }
                    try {
                        reloadData()
                    } catch (e: Exception) {
                        container.updateState { it.copy(isLoading = false, error = e.message ?: "Failed") }
                    }
                }
            }
            is ChatSettingsIntent.AddContact -> {
                val partnerId = uiState.value.partnerId ?: return
                viewModelScope.launch {
                    try {
                        contactHandler.addContact(partnerId, intent.firstName, intent.lastName)
                        container.updateState { it.copy(isContact = true) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Failed to add contact")) }
                }
            }
            is ChatSettingsIntent.RemoveContact -> {
                val partnerId = uiState.value.partnerId ?: return
                viewModelScope.launch {
                    try {
                        contactHandler.removeContact(partnerId)
                        container.updateState { it.copy(isContact = false) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Failed to remove contact")) }
                }
            }
            is ChatSettingsIntent.InviteUser -> {
                viewModelScope.launch {
                    try {
                        chatSettingsHandler.inviteUser(chatId, intent.userId)
                        reloadData()
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Invite failed")) }
                }
            }
            is ChatSettingsIntent.GenerateInviteLink -> {
                viewModelScope.launch {
                    try {
                        val link = chatSettingsHandler.generateInviteLink(chatId)
                        container.updateState { it.copy(inviteLink = link) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Link generation failed")) }
                }
            }
            is ChatSettingsIntent.UpdatePermissions -> {
                viewModelScope.launch {
                    try {
                        val members = chatSettingsHandler.updatePermissions(
                            chatId, intent.targetUserId, intent.canSendMessages, intent.canDeleteMessages, intent.canInviteUsers, intent.canChangeInfo
                        )
                        container.updateState { it.copy(members = members.toPersistentList()) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Permissions update failed")) }
                }
            }
            is ChatSettingsIntent.ClearHistory -> {
                viewModelScope.launch {
                    try { chatSettingsHandler.clearHistory(chatId, intent.forAll) } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Clear history failed")) }
                }
            }
            is ChatSettingsIntent.DeleteChat -> {
                viewModelScope.launch {
                    try {
                        chatSettingsHandler.deleteChat(chatId)
                        container.updateState { it.copy(isChatDeleted = true) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Delete failed")) }
                }
            }
            is ChatSettingsIntent.KickUser -> {
                viewModelScope.launch {
                    try {
                        val members = chatSettingsHandler.kickUser(chatId, intent.targetUserId)
                        container.updateState { it.copy(members = members.toPersistentList()) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Kick failed")) }
                }
            }
            is ChatSettingsIntent.LeaveChat -> {
                viewModelScope.launch {
                    try {
                        chatSettingsHandler.leaveChat(chatId)
                        container.updateState { it.copy(isChatDeleted = true) }
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Leave failed")) }
                }
            }
            is ChatSettingsIntent.UpdateChatInfo -> {
                viewModelScope.launch {
                    try {
                        chatSettingsHandler.updateChatInfo(chatId, intent.title, intent.description)
                        reloadData()
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Update info failed")) }
                }
            }
        }
    }
}
