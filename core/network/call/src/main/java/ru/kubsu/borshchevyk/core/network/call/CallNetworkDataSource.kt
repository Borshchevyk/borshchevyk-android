package ru.kubsu.borshchevyk.core.network.call

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.CallResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.network.dto.JoinCallResponse

/**
 * Data source interface defining operations for initiating and managing audio/video calls.
 */
interface CallNetworkDataSource {
    /**
     * Initiates a new call with specified participants.
     *
     * @param request The [CreateCallRequest] detailing the participants.
     * @return A [NetworkResult] containing the [CallResponse] representing the new call session.
     */
    suspend fun createCall(request: CreateCallRequest): NetworkResult<CallResponse>

    /**
     * Joins an existing active call.
     *
     * @param callId The ID of the call to join.
     * @return A [NetworkResult] containing the [JoinCallResponse] (typically includes connection tokens).
     */
    suspend fun joinCall(callId: String): NetworkResult<JoinCallResponse>

    /**
     * Leaves an active call without terminating it for other participants.
     *
     * @param callId The ID of the call to leave.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun leaveCall(callId: String): NetworkResult<Unit>

    /**
     * Completely ends an active call, disconnecting all participants.
     *
     * @param callId The ID of the call to terminate.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun endCall(callId: String): NetworkResult<Unit>
}
