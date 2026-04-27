package ru.kubsu.borshchevyk.core.network.call

import ru.kubsu.borshchevyk.core.model.dto.CallResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.model.dto.JoinCallResponse

interface CallNetworkDataSource {
    suspend fun createCall(request: CreateCallRequest): CallResponse
    suspend fun joinCall(callId: String): JoinCallResponse
    suspend fun leaveCall(callId: String)
    suspend fun endCall(callId: String)
}