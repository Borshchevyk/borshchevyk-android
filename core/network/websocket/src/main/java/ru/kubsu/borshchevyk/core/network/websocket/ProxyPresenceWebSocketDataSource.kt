package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyPresenceWebSocketDataSource @Inject constructor(
    private val transportModeManager: TransportModeManager,
    private val krossbowWebSocket: KrossbowWebSocketDataSource,
    private val meshWebSocket: MeshPresenceWebSocketDataSource
) : PresenceWebSocketDataSource {

    private val currentDataSource: PresenceWebSocketDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.GLOBAL) krossbowWebSocket else meshWebSocket

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePresence(userId: String): Flow<PresenceStatusResponse> {
        return transportModeManager.networkMode.flatMapLatest { mode ->
            if (mode == NetworkMode.GLOBAL) {
                krossbowWebSocket.observePresence(userId)
            } else {
                meshWebSocket.observePresence(userId)
            }
        }
    }

    override suspend fun connect() {
        currentDataSource.connect()
    }

    override suspend fun disconnect() {
        currentDataSource.disconnect()
    }
}
