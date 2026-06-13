package ru.kubsu.borshchevyk.feature.chat.chatlist.mvi

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.mesh.MeshPeer

sealed interface ChatListStateAction {
    data class LoadingStarted(val isFullLoad: Boolean = true) : ChatListStateAction
    data class LoadFailed(val error: String) : ChatListStateAction
    data class ChatsUpdated(val chats: List<Chat>) : ChatListStateAction
    data class NetworkModeUpdated(val mode: NetworkMode) : ChatListStateAction
    data class PeersUpdated(val count: Int, val peers: List<MeshPeer>) : ChatListStateAction
    data class CurrentUserUpdated(val name: String) : ChatListStateAction
}

sealed interface ChatListIntent {
    object LoadChats : ChatListIntent
    object ToggleNetworkMode : ChatListIntent
    data class PinChat(val chatId: String) : ChatListIntent
    data class UnpinChat(val chatId: String) : ChatListIntent
    data class CreatePrivateChat(val targetUserId: String) : ChatListIntent
    data class CreateGroupChat(val title: String, val description: String?) : ChatListIntent
    data class JoinChat(val inviteCode: String) : ChatListIntent
    data class CreatePrivateChatByTag(val tag: String) : ChatListIntent
}

sealed interface ChatListEffect {
    data class ShowError(val message: String) : ChatListEffect
    data class NavigateToChat(val chatId: String) : ChatListEffect
}
