package ru.kubsu.borshchevyk.feature.chat.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.feature.chat.common.mvi.BaseMviViewModel
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ChatSettingsDataLoader
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ChatSettingsHandler
import ru.kubsu.borshchevyk.feature.chat.settings.interactor.ContactHandler
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsEffect
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsIntent
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsStateAction
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsUiState
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.reduce
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataLoader: ChatSettingsDataLoader,
    private val settingsHandler: ChatSettingsHandler,
    private val contactHandler: ContactHandler
) : BaseMviViewModel<ChatSettingsUiState, ChatSettingsStateAction, ChatSettingsIntent, ChatSettingsEffect>(ChatSettingsUiState(isLoading = true)) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    override fun reduce(state: ChatSettingsUiState, action: ChatSettingsStateAction): ChatSettingsUiState = state.reduce(action)

    init {
        handleIntent(ChatSettingsIntent.LoadData)
    }

    private suspend fun reloadData() {
        val data = dataLoader.loadData(chatId)
        dispatch(ChatSettingsStateAction.InitialDataLoaded(
            currentUserId = data.currentUserId,
            isGroupChat = data.isGroupChat,
            members = data.members,
            isContact = data.isContact,
            partnerId = data.partnerId,
            partnerTag = data.partnerTag,
            partnerFirstName = data.partnerFirstName,
            partnerLastName = data.partnerLastName,
            isDeletable = data.isDeletable,
            chatName = data.chatName,
            chatDescription = data.chatDescription,
            chatAvatarUrl = data.chatAvatarUrl
        ))
    }

    override fun handleIntent(intent: ChatSettingsIntent) {
        when (intent) {
            is ChatSettingsIntent.LoadData -> {
                viewModelScope.launch {
                    dispatch(ChatSettingsStateAction.LoadingStarted())
                    try {
                        reloadData()
                    } catch (e: Exception) {
                        dispatch(ChatSettingsStateAction.LoadFailed(e.message ?: "Failed"))
                    }
                }
            }
            is ChatSettingsIntent.AddContact -> {
                val partnerId = uiState.value.partnerId ?: return
                viewModelScope.launch {
                    try {
                        contactHandler.addContact(partnerId, intent.firstName, intent.lastName)
                        dispatch(ChatSettingsStateAction.ContactStatusUpdated(true))
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Failed to add contact")) }
                }
            }
            is ChatSettingsIntent.RemoveContact -> {
                val partnerId = uiState.value.partnerId ?: return
                viewModelScope.launch {
                    try {
                        contactHandler.removeContact(partnerId)
                        dispatch(ChatSettingsStateAction.ContactStatusUpdated(false))
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Failed to remove contact")) }
                }
            }
            is ChatSettingsIntent.InviteUser -> {
                viewModelScope.launch {
                    try {
                        settingsHandler.inviteUser(chatId, intent.userId)
                        reloadData()
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Invite failed")) }
                }
            }
            is ChatSettingsIntent.GenerateInviteLink -> {
                viewModelScope.launch {
                    try {
                        val link = settingsHandler.generateInviteLink(chatId)
                        dispatch(ChatSettingsStateAction.InviteLinkGenerated(link))
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Link generation failed")) }
                }
            }
            is ChatSettingsIntent.UpdatePermissions -> {
                viewModelScope.launch {
                    try {
                        val members = settingsHandler.updatePermissions(
                            chatId, intent.targetUserId, intent.canSendMessages, intent.canDeleteMessages, intent.canInviteUsers, intent.canChangeInfo
                        )
                        dispatch(ChatSettingsStateAction.MembersUpdated(members))
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Permissions update failed")) }
                }
            }
            is ChatSettingsIntent.ClearHistory -> {
                viewModelScope.launch {
                    try { settingsHandler.clearHistory(chatId, intent.forAll) } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Clear history failed")) }
                }
            }
            is ChatSettingsIntent.DeleteChat -> {
                viewModelScope.launch {
                    try {
                        settingsHandler.deleteChat(chatId)
                        dispatch(ChatSettingsStateAction.ChatDeleted)
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Delete failed")) }
                }
            }
            is ChatSettingsIntent.KickUser -> {
                viewModelScope.launch {
                    try {
                        val members = settingsHandler.kickUser(chatId, intent.targetUserId)
                        dispatch(ChatSettingsStateAction.MembersUpdated(members))
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Kick failed")) }
                }
            }
            is ChatSettingsIntent.LeaveChat -> {
                viewModelScope.launch {
                    try {
                        settingsHandler.leaveChat(chatId)
                        dispatch(ChatSettingsStateAction.ChatDeleted)
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Leave failed")) }
                }
            }
            is ChatSettingsIntent.UpdateChatInfo -> {
                viewModelScope.launch {
                    try {
                        settingsHandler.updateChatInfo(chatId, intent.title, intent.description)
                        reloadData()
                    } catch (e: Exception) { sendEffect(ChatSettingsEffect.ShowError(e.message ?: "Update info failed")) }
                }
            }
        }
    }
}
