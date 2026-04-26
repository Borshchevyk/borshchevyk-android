package ru.kubsu.borshchevyk.core.network.call

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.dto.CallResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.model.dto.JoinCallResponse
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorCallNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CallNetworkDataSource {

    override suspend fun createCall(request: CreateCallRequest): CallResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/calls") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun joinCall(callId: String): JoinCallResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/calls/$callId/join").body()
        }
    }

    override suspend fun leaveCall(callId: String) {
        withContext(ioDispatcher) {
            httpClient.post("api/v1/calls/$callId/leave")
        }
    }

    override suspend fun endCall(callId: String) {
        withContext(ioDispatcher) {
            httpClient.post("api/v1/calls/$callId/end")
        }
    }
}