package ru.kubsu.borshchevyk.feature.chat.chatlist.mvi

import kotlinx.collections.immutable.toPersistentList

fun ChatListUiState.reduce(action: ChatListStateAction): ChatListUiState {
    return when (action) {
        is ChatListStateAction.LoadingStarted -> this.copy(isLoading = action.isFullLoad, error = null)
        is ChatListStateAction.LoadFailed -> this.copy(isLoading = false, error = action.error)
        is ChatListStateAction.ChatsUpdated -> this.copy(isLoading = false, chats = action.chats.toPersistentList())
        is ChatListStateAction.NetworkModeUpdated -> this.copy(networkMode = action.mode)
        is ChatListStateAction.PeersUpdated -> this.copy(connectedPeersCount = action.count, connectedPeers = action.peers.toPersistentList())
        is ChatListStateAction.CurrentUserUpdated -> this.copy(currentUserName = action.name)
    }
}
