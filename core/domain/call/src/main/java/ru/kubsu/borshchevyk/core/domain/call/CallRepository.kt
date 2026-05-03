package ru.kubsu.borshchevyk.core.domain.call

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent

/**
 * Repository interface defining operations for managing audio/video calls.
 * 
 * This repository acts as the single source of truth for call-related data
 * and operations, abstracting the underlying network and signaling mechanisms
 * (such as WebRTC or mesh network interactions) from the domain layer.
 */
interface CallRepository {
    /**
     * Initiates a new call with the specified participants.
     *
     * @param participantsIds A list of unique identifiers for the users to be included in the call.
     * @return The unique identifier of the newly created call.
     */
    suspend fun createCall(participantsIds: List<String>): String

    /**
     * Joins an existing, ongoing call.
     *
     * @param callId The unique identifier of the call to join.
     * @return A connection identifier or relevant token for the joined call.
     */
    suspend fun joinCall(callId: String): String

    /**
     * Leaves an ongoing call that the current user is a part of.
     * This does not terminate the call for other participants.
     *
     * @param callId The unique identifier of the call to leave.
     */
    suspend fun leaveCall(callId: String)

    /**
     * Terminates an ongoing call entirely for all participants.
     * Typically, only the creator or an admin can end the call.
     *
     * @param callId The unique identifier of the call to end.
     */
    suspend fun endCall(callId: String)

    /**
     * Retrieves a list of ICE servers required for WebRTC peer-to-peer connection establishment.
     * ICE servers (STUN/TURN) are used to discover public IP addresses and relay traffic
     * if direct peer-to-peer communication fails.
     *
     * @return A list of objects representing the configuration of available ICE servers.
     */
    suspend fun getIceServers(): List<Any>

    /**
     * Observes real-time events related to calls, such as incoming calls, participants joining
     * or leaving, or call termination.
     *
     * @return A [Flow] emitting [DomainCallEvent] instances representing the stream of call events.
     */
    fun observeCallEvents(): Flow<DomainCallEvent>
}