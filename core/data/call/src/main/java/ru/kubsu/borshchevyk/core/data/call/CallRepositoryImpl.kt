package ru.kubsu.borshchevyk.core.data.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.data.sync.SyncRepository
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import ru.kubsu.borshchevyk.core.model.domain.EventType
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.network.call.CallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.CallWebSocketDataSource
import javax.inject.Inject

/**
 * Implementation of [CallRepository] that manages call signaling and lifecycle.
 *
 * This repository coordinates with the network layer to start, join, and end calls,
 * and uses WebSockets to observe real-time signaling events for WebRTC peer connections.
 *
 * @property networkDataSource Source for REST API call operations.
 * @property webSocketDataSource Source for real-time WebSocket call events.
 */
class CallRepositoryImpl @Inject constructor(
    private val networkDataSource: CallNetworkDataSource,
    private val webSocketDataSource: CallWebSocketDataSource,
    private val syncRepository: SyncRepository
) : CallRepository {

    /**
     * Initiates a new call with the specified participants.
     *
     * @param participantsIds A list of unique user IDs to invite to the call.
     * @return The unique identifier ([String]) of the newly created call.
     */
    override suspend fun createCall(participantsIds: List<String>): String {
        val response = networkDataSource.createCall(CreateCallRequest(participantsIds)).getOrThrow()
        return response.id
    }

    /**
     * Joins an active call.
     *
     * @param callId The unique identifier of the call.
     * @return An access token required for connecting to the call's signaling session.
     */
    override suspend fun joinCall(callId: String): String {
        val response = networkDataSource.joinCall(callId).getOrThrow()
        return response.token
    }

    /**
     * Leaves an ongoing call, notifying the signaling server.
     *
     * @param callId The unique identifier of the call to leave.
     */
    override suspend fun leaveCall(callId: String) {
        networkDataSource.leaveCall(callId).getOrThrow()
    }

    /**
     * Ends the call for all participants. Requires appropriate permissions (e.g., call initiator).
     *
     * @param callId The unique identifier of the call to terminate.
     */
    override suspend fun endCall(callId: String) {
        networkDataSource.endCall(callId).getOrThrow()
    }

    /**
     * Retrieves the ICE (STUN/TURN) servers necessary for establishing WebRTC peer-to-peer connections.
     *
     * @return A list of ICE server configurations. Currently returns an empty list.
     */
    override suspend fun getIceServers(): List<Any> {
        return emptyList()
    }
    
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Subscribes to real-time call events from both WebSocket connection and Background Sync.
     *
     * Maps network data transfer objects (DTOs) and SyncEvents into domain-level call events.
     *
     * @return A [Flow] emitting [ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent] representing events like incoming calls or participant status changes.
     */
    override fun observeCallEvents(): Flow<ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent> {
        val wsFlow = webSocketDataSource.observeCallEvents().map { dto ->
            ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent(
                type = dto.eventType,
                callId = dto.callId,
                initiatorId = dto.initiator?.id ?: dto.actor?.id ?: "",
                participants = emptyList()
            )
        }
        
        val syncFlow = syncRepository.incomingEvents
            .filter { it.eventType == EventType.CALL_EVENT }
            .map { event ->
                // The payload for CALL_EVENT should be a CallEventDto equivalent
                val dto = json.decodeFromString<ru.kubsu.borshchevyk.core.network.dto.NotificationDto.CallEventDto>(event.payload)
                ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent(
                    type = dto.eventType,
                    callId = dto.callId,
                    initiatorId = dto.initiator?.id ?: dto.actor?.id ?: "",
                    participants = emptyList()
                )
            }
            
        return merge(wsFlow, syncFlow)
    }
}
