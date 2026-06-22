package ru.kubsu.borshchevyk.feature.chat.chatlist.interactor

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.auth.GetTagUseCase
import ru.kubsu.borshchevyk.core.domain.message.ConnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.domain.message.DisconnectWebSocketUseCase
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshPeer
import javax.inject.Inject

class ChatListMeshHandler @Inject constructor(
    private val transportModeManager: TransportModeManager,
    private val meshConnectionManager: MeshConnectionManager,
    private val connectWebSocketUseCase: ConnectWebSocketUseCase,
    private val disconnectWebSocketUseCase: DisconnectWebSocketUseCase,
    private val getTagUseCase: GetTagUseCase
) {
    fun observeTag(): Flow<String?> = getTagUseCase()
    fun observeNetworkMode(): Flow<NetworkMode> = transportModeManager.networkMode
    fun observePeers(): Flow<List<MeshPeer>> = meshConnectionManager.connectedPeers
    
    suspend fun connectWs() {
        try { connectWebSocketUseCase() } catch (e: Exception) {}
    }

    suspend fun toggleNetworkMode(currentMode: NetworkMode, currentUserName: String) {
        if (currentMode == NetworkMode.GLOBAL) {
            transportModeManager.setMode(NetworkMode.MESH)
            try { disconnectWebSocketUseCase() } catch (e: Exception) {}
            meshConnectionManager.startAdvertising(currentUserName)
            meshConnectionManager.startDiscovery(currentUserName)
        } else {
            transportModeManager.setMode(NetworkMode.GLOBAL)
            meshConnectionManager.stopAdvertising()
            meshConnectionManager.stopDiscovery()
            meshConnectionManager.stopAllEndpoints()
            try { connectWebSocketUseCase() } catch (e: Exception) {}
        }
    }
}
