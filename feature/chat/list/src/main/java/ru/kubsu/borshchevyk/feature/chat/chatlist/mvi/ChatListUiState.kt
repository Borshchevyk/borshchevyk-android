package ru.kubsu.borshchevyk.feature.chat.chatlist.mvi

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.mesh.MeshPeer

data class ChatListUiState(
    val chats: PersistentList<Chat> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val networkMode: NetworkMode = NetworkMode.GLOBAL,
    val connectedPeersCount: Int = 0,
    val connectedPeers: PersistentList<MeshPeer> = persistentListOf(),
    val currentUserName: String = android.os.Build.MODEL
)
