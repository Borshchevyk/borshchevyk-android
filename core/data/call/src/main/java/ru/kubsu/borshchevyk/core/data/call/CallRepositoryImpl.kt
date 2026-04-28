package ru.kubsu.borshchevyk.core.data.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.network.call.CallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketDataSource
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(
    private val networkDataSource: CallNetworkDataSource,
    private val webSocketDataSource: WebSocketDataSource
) : CallRepository {

    override suspend fun createCall(participantsIds: List<String>): String {
        val response = networkDataSource.createCall(CreateCallRequest(participantsIds))
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

    override suspend fun getIceServers(): List<Any> {
        return emptyList()
    }
    
    override fun observeCallEvents(): Flow<ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent> {
        return webSocketDataSource.observeCallEvents().map { dto ->
            ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent(
                type = dto.eventType,
                callId = dto.callId,
                initiatorId = dto.initiator?.id ?: dto.actor?.id ?: "",
                participants = emptyList()
            )
        }
    }
}
