package ru.kubsu.borshchevyk.feature.chat.chatlist

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.feature.chat.chatlist.interactor.ChatListActionHandler
import ru.kubsu.borshchevyk.feature.chat.chatlist.interactor.ChatListEventHandler
import ru.kubsu.borshchevyk.feature.chat.chatlist.interactor.ChatListMeshHandler
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListEffect
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListIntent
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListStateAction
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListUiState
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.reduce
import ru.kubsu.borshchevyk.feature.chat.common.mvi.BaseMviViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val eventHandler: ChatListEventHandler,
    private val actionHandler: ChatListActionHandler,
    private val meshHandler: ChatListMeshHandler
) : BaseMviViewModel<ChatListUiState, ChatListStateAction, ChatListIntent, ChatListEffect>(ChatListUiState(isLoading = true)) {

    override fun reduce(state: ChatListUiState, action: ChatListStateAction): ChatListUiState = state.reduce(action)

    init {
        meshHandler.observeTag().onEach { tag ->
            if (!tag.isNullOrBlank()) dispatch(ChatListStateAction.CurrentUserUpdated(tag))
        }.launchIn(viewModelScope)

        meshHandler.observeNetworkMode().onEach { mode ->
            dispatch(ChatListStateAction.NetworkModeUpdated(mode))
        }.launchIn(viewModelScope)

        meshHandler.observePeers().onEach { peers ->
            dispatch(ChatListStateAction.PeersUpdated(peers.size, peers))
        }.launchIn(viewModelScope)
        
        eventHandler.observeChats().onEach { chats ->
            val sortedChats = chats.sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.createdAt })
            dispatch(ChatListStateAction.ChatsUpdated(sortedChats))
        }.launchIn(viewModelScope)

        eventHandler.observeNewMessages().onEach { Log.d("ChatList", "WS: New message ${it.chatId}") }.launchIn(viewModelScope)
        eventHandler.observeGlobalEvents().onEach { Log.d("ChatList", "WS: Global event ${it.action}") }.launchIn(viewModelScope)

        viewModelScope.launch { meshHandler.connectWs() }
        handleIntent(ChatListIntent.LoadChats)
    }

    override fun handleIntent(intent: ChatListIntent) {
        when (intent) {
            is ChatListIntent.LoadChats -> {
                viewModelScope.launch {
                    dispatch(ChatListStateAction.LoadingStarted())
                    try {
                        actionHandler.loadChats()
                    } catch (e: Exception) {
                        dispatch(ChatListStateAction.LoadFailed(e.message ?: "Failed"))
                    }
                }
            }
            is ChatListIntent.ToggleNetworkMode -> {
                viewModelScope.launch {
                    meshHandler.toggleNetworkMode(uiState.value.networkMode, uiState.value.currentUserName)
                }
            }
            is ChatListIntent.PinChat -> {
                viewModelScope.launch {
                    try { actionHandler.pinChat(intent.chatId) } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
            is ChatListIntent.UnpinChat -> {
                viewModelScope.launch {
                    try { actionHandler.unpinChat(intent.chatId) } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
            is ChatListIntent.CreatePrivateChat -> {
                viewModelScope.launch {
                    try {
                        val id = actionHandler.createPrivateChat(intent.targetUserId)
                        sendEffect(ChatListEffect.NavigateToChat(id))
                    } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
            is ChatListIntent.CreateGroupChat -> {
                viewModelScope.launch {
                    try {
                        val id = actionHandler.createGroupChat(intent.title, intent.description)
                        sendEffect(ChatListEffect.NavigateToChat(id))
                    } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
            is ChatListIntent.JoinChat -> {
                viewModelScope.launch {
                    try {
                        val id = actionHandler.joinChat(intent.inviteCode)
                        sendEffect(ChatListEffect.NavigateToChat(id))
                    } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
            is ChatListIntent.CreatePrivateChatByTag -> {
                viewModelScope.launch {
                    try {
                        val id = actionHandler.createPrivateChatByTag(intent.tag)
                        sendEffect(ChatListEffect.NavigateToChat(id))
                    } catch (e: Exception) { sendEffect(ChatListEffect.ShowError(e.message ?: "Failed")) }
                }
            }
        }
    }
}
