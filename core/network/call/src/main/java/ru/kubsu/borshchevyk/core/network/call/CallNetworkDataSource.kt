package ru.kubsu.borshchevyk.core.network.call

import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.CallResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.model.dto.JoinCallResponse

interface CallNetworkDataSource {
    suspend fun createCall(request: CreateCallRequest): NetworkResult<CallResponse>
    suspend fun joinCall(callId: String): NetworkResult<JoinCallResponse>
    suspend fun leaveCall(callId: String): NetworkResult<Unit>
    suspend fun endCall(callId: String): NetworkResult<Unit>
}