package ru.kubsu.borshchevyk.core.network.call

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.CallResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.network.dto.JoinCallResponse
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.websocket.CallWebSocketDataSource
import ru.kubsu.borshchevyk.core.network.websocket.KrossbowWebSocketDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyCallNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorCallNetworkDataSource,
    private val krossbowWebSocket: KrossbowWebSocketDataSource,
    private val meshDataSource: MeshCallNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : CallNetworkDataSource, CallWebSocketDataSource {

    private val currentDataSource: CallNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    private val currentWebSocket: CallWebSocketDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            krossbowWebSocket
        }

    override suspend fun createCall(request: CreateCallRequest): NetworkResult<CallResponse> =
        currentDataSource.createCall(request)

    override suspend fun joinCall(callId: String): NetworkResult<JoinCallResponse> =
        currentDataSource.joinCall(callId)

    override suspend fun leaveCall(callId: String): NetworkResult<Unit> =
        currentDataSource.leaveCall(callId)

    override suspend fun endCall(callId: String): NetworkResult<Unit> =
        currentDataSource.endCall(callId)

    override fun observeCallEvents(): Flow<NotificationDto.CallEventDto> =
        if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource.observeCallEvents()
        } else {
            krossbowWebSocket.observeCallEvents()
        }

    override suspend fun connect() {
        currentWebSocket.connect()
    }

    override suspend fun disconnect() {
        currentWebSocket.disconnect()
    }
}