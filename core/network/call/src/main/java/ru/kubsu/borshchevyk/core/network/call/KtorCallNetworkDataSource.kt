package ru.kubsu.borshchevyk.core.network.call

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.CallResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateCallRequest
import ru.kubsu.borshchevyk.core.model.dto.JoinCallResponse
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorCallNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CallNetworkDataSource {

    override suspend fun createCall(request: CreateCallRequest): NetworkResult<CallResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/calls") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun joinCall(callId: String): NetworkResult<JoinCallResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/calls/$callId/join")
            }
        }
    }

    override suspend fun leaveCall(callId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/calls/$callId/leave")
            }
        }
    }

    override suspend fun endCall(callId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/calls/$callId/end")
            }
        }
    }
}