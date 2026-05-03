package ru.kubsu.borshchevyk.core.network.call

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.CallResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.network.dto.JoinCallResponse
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import ru.kubsu.borshchevyk.core.network.websocket.CallWebSocketDataSource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshCallNetworkDataSource @Inject constructor(
    private val json: Json,
    private val gossipProtocol: MeshGossipProtocol,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CallNetworkDataSource, CallWebSocketDataSource {

    private val _callEvents = MutableSharedFlow<NotificationDto.CallEventDto>(extraBufferCapacity = 64)

    init {
        kotlinx.coroutines.GlobalScope.launch(ioDispatcher) {
            gossipProtocol.incomingEnvelopes
                .filter { it.action == "CALL_SIGNALING" }
                .collect { envelope ->
                    try {
                        val event = json.decodeFromString<NotificationDto.CallEventDto>(envelope.payload)
                        _callEvents.tryEmit(event)
                    } catch (e: Exception) {
                        // Ignore malformed payloads
                    }
                }
        }
    }

    override suspend fun createCall(request: CreateCallRequest): NetworkResult<CallResponse> {
        return withContext(ioDispatcher) {
            val callId = UUID.randomUUID().toString()
            val fakeUser = ShortUserDto(id = "self", firstName = "Me")
            val payload = json.encodeToString(
                NotificationDto.CallEventDto(
                    eventType = "CALL_INITIATED",
                    callId = callId,
                    initiator = fakeUser,
                    actor = fakeUser,
                    timestamp = "now"
                )
            )
            gossipProtocol.broadcast(MeshEnvelope(envelopeId = UUID.randomUUID().toString(), originEndpointId = "self", action = "CALL_SIGNALING", payload = payload))
            NetworkResult.Success(
                CallResponse(
                    id = callId, 
                    roomId = "mesh-room-$callId", 
                    initiator = fakeUser, 
                    status = "INITIATED", 
                    createdAt = "now", 
                    participants = listOf(fakeUser)
                )
            )
        }
    }

    override suspend fun joinCall(callId: String): NetworkResult<JoinCallResponse> {
        return withContext(ioDispatcher) {
            val fakeUser = ShortUserDto(id = "self", firstName = "Me")
            val payload = json.encodeToString(
                NotificationDto.CallEventDto(
                    eventType = "PARTICIPANT_JOINED",
                    callId = callId,
                    initiator = null,
                    actor = fakeUser,
                    timestamp = "now"
                )
            )
            gossipProtocol.broadcast(MeshEnvelope(envelopeId = UUID.randomUUID().toString(), originEndpointId = "self", action = "CALL_SIGNALING", payload = payload))
            NetworkResult.Success(JoinCallResponse(token = "mesh-token-$callId"))
        }
    }

    override suspend fun leaveCall(callId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val fakeUser = ShortUserDto(id = "self", firstName = "Me")
            val payload = json.encodeToString(
                NotificationDto.CallEventDto(
                    eventType = "PARTICIPANT_LEFT",
                    callId = callId,
                    initiator = null,
                    actor = fakeUser,
                    timestamp = "now"
                )
            )
            gossipProtocol.broadcast(MeshEnvelope(envelopeId = UUID.randomUUID().toString(), originEndpointId = "self", action = "CALL_SIGNALING", payload = payload))
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun endCall(callId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val fakeUser = ShortUserDto(id = "self", firstName = "Me")
            val payload = json.encodeToString(
                NotificationDto.CallEventDto(
                    eventType = "CALL_ENDED",
                    callId = callId,
                    initiator = null,
                    actor = fakeUser,
                    timestamp = "now"
                )
            )
            gossipProtocol.broadcast(MeshEnvelope(envelopeId = UUID.randomUUID().toString(), originEndpointId = "self", action = "CALL_SIGNALING", payload = payload))
            NetworkResult.Success(Unit)
        }
    }

    override fun observeCallEvents(): Flow<NotificationDto.CallEventDto> = _callEvents

    override suspend fun connect() {
        // Mesh handles connection via Nearby Connections automatically
    }

    override suspend fun disconnect() {
        // Mesh handles disconnection
    }
}