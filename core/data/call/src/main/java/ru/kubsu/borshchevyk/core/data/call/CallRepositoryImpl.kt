package ru.kubsu.borshchevyk.core.data.call

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.call.CallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketDataSource
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(
    private val networkDataSource: CallNetworkDataSource,
    private val webSocketDataSource: WebSocketDataSource
) : CallRepository {

    override suspend fun createCall(participantIds: List<String>): String {
        val response = networkDataSource.createCall(CreateCallRequest(participantIds))
        return response.id
    }

    override suspend fun joinCall(callId: String): String {
        val response = networkDataSource.joinCall(callId)
        return response.token
    }

    override suspend fun leaveCall(callId: String) {
        networkDataSource.leaveCall(callId)
    }

    override suspend fun endCall(callId: String) {
        networkDataSource.endCall(callId)
    }
    
    override fun observeCallEvents(): Flow<NotificationDto.CallEventDto> {
        return webSocketDataSource.observeCallEvents()
    }
}